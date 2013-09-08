#include <unistd.h>
#include <string>
#include <sys/time.h>
#include <netinet/in.h>
#include <netdb.h> 

#include "ConnectedClientThread.h"
#include "TheApp.h"
#include "Parser.h"
#include "UtilityFn.h"

using namespace std;



/////////////////////////////////////////////////////////////////////////////
//  ConnectedClient
//  manages the connection to a single client
//

//  Constructor
//
ConnectedClient::ConnectedClient(TheApp& theApp, const struct sockaddr_in &clientAddress, int clientsListeningOnPortNumber) :
	mTheApp(theApp)
{
	mClientsAddress = clientAddress;
	mPortNumberClientIsListeningOn = clientsListeningOnPortNumber;
	mIpAddressOfClient = IpAddressString(mClientsAddress);

	mClientSocketFileDescriptor = -1;

	mClientReceiveTimeout = 3;
	mClientSendTimeout = 3;

	mNumberOfClientConnectionFailuers = 0;
}


//  Destructor
//
ConnectedClient::~ConnectedClient()
{
	if ( mThreadRunning )
	{
		Cancel();
	}
}




//  SetClientSocketTimeouts
//
bool ConnectedClient::SetClientSocketTimeouts(long sendTimeout, long receiveTimeout)
{
	if ( mThread != 0 )
		return false;	//  can't change timeouts when we are running

	if ( sendTimeout < 1 )
		mSendTimeout = 1;
	else
		mSendTimeout = sendTimeout;
	
	mReceiveTimeout = receiveTimeout;

	return true;
}


//  OpenClientSocket
//  opens a socket file description to the client's listening port and connects
//
bool ConnectedClient::OpenClientSocket()
{
	mClientSocketFileDescriptor = -1;

	int newSocketFileDescriptor = socket(AF_INET, SOCK_STREAM, 0);

	//  set the socket timeouts  
	struct timeval timeout;
	timeout.tv_usec = 0;
	//
	//  send timeout
	if ( mClientSendTimeout > 0 )
	{
		timeout.tv_sec = mClientSendTimeout;
		if ( setsockopt(newSocketFileDescriptor, SOL_SOCKET, SO_SNDTIMEO, (char *)&timeout, sizeof(timeout)) < 0 )
		{
			mTheApp.AddEvent( SYSEVENT, format("Error - OpenClientSocket() fail to setsockopt send, errno = %d",errno) );
			close(newSocketFileDescriptor);
			return false;
		}
	}
	//
	//  receive timeout
	if ( mClientReceiveTimeout > 0 )
	{
		//  set receive timeout, if we are not waiting, receive timeout is 0
		timeout.tv_sec = mClientReceiveTimeout;
		if ( setsockopt(newSocketFileDescriptor, SOL_SOCKET, SO_RCVTIMEO, (char *)&timeout, sizeof(timeout)) < 0 )
		{
			mTheApp.AddEvent( SYSEVENT, format("Error - OpenClientSocket() fail to setsockopt receive, errno = %d",errno) );
			close(newSocketFileDescriptor);
			return false;
		}
	}

	struct hostent *server;
	server = gethostbyname(mIpAddressOfClient.c_str());

    if (server == 0) 
	{
		mTheApp.AddEvent( SYSEVENT, format("Error - OpenClientSocket() fail gethostbyname(), errno = %d",errno) );
		close(newSocketFileDescriptor);
        return false;
    }

	//
	mClientsListeningServerAddress;
	memset(&mClientsListeningServerAddress, 0, sizeof(struct sockaddr_in)); 

	mClientsListeningServerAddress.sin_family = AF_INET;
	bcopy((char*)server->h_addr, (char*)&mClientsListeningServerAddress.sin_addr.s_addr, server->h_length);
	mClientsListeningServerAddress.sin_port = htons(mPortNumberClientIsListeningOn);

	mClientSocketFileDescriptor = newSocketFileDescriptor;

	Sleep(50);

	int connectAttepmts = 0;
	int connected = connect(mClientSocketFileDescriptor, (struct sockaddr *)&mClientsListeningServerAddress, sizeof(mClientsListeningServerAddress));
	while ( connected == -1 && connectAttepmts < 10 )
	{
		//  retries for fail to connect on EINTR
		if ( errno != EINTR )
		{
			mTheApp.AddEvent( SYSEVENT, format("Error - OpenClientSocket() fail to connect errno = %d",errno) );
			return false;
		}
		
		mTheApp.AddEvent( SYSEVENT, format("Warning - OpenClientSocket() Connection retry %d",connectAttepmts+1) );
		Sleep(50);
		
		connected = connect(mClientSocketFileDescriptor, (struct sockaddr *)&mClientsListeningServerAddress, sizeof(mClientsListeningServerAddress));
		connectAttepmts++;
	}

	if ( connected < 0 )
	{
		mTheApp.AddEvent( SYSEVENT, format("Error - OpenClientSocket() fail to connect errno = %d",errno) );
		return false;
	}

	return true;
}


//  Closes the socket to the client's listening port
//
bool ConnectedClient::CloseClientSocket()
{
	if ( mClientSocketFileDescriptor >= 0 )
		close(mClientSocketFileDescriptor);
	
	mClientSocketFileDescriptor = -1;
}


//  SendMessageToClient
//
string ConnectedClient::SendMessageToClient(string message,  bool waitForResponse)
{
	string response = "";

	//  if we have not opened this socket yet, open it now
	if ( ! OpenClientSocket() )
	{
		IncrementClientConnectionFailureCounter();
		return response;
	}

	if ( write(mClientSocketFileDescriptor, message.c_str(), message.size()) != message.size() )
	{
		mTheApp.AddEvent( SYSEVENT, format("Error - SendMessageToClient() fail send message %s errno = %d",message.c_str(), errno) );
		return response;
	}

	int bytesRead = 0;
	if ( waitForResponse )
	{
		char buffer[TCPSERVER_READBUFFERSIZE+1];
		memset(buffer, 0, TCPSERVER_READBUFFERSIZE+1);

		while((bytesRead = read(mClientSocketFileDescriptor, buffer, TCPSERVER_READBUFFERSIZE)) > 0)
		{
			/* ensure null-terminated */
			buffer[bytesRead] = '\0';
			response += buffer;
			memset(buffer, 0, TCPSERVER_READBUFFERSIZE+1);
		}
	}

	//  shutdown to end transmission
	shutdown(mClientSocketFileDescriptor, SHUT_WR);
	CloseClientSocket();
	
	return response;
}



//  Cancel
//
void ConnectedClient::Cancel()
{
	CloseClientSocket();

	TCPServerThread::Cancel();
}



//  RunFunction
//  waits for a message from the client, known messages get response and action taken
//
void ConnectedClient::RunFunction()
{
	struct sockaddr_in clientAddress;
	string readFromSocket;

	//  this function blocks on the socket read
	int acceptFileDescriptor = ReadStringFromSocket(&clientAddress, readFromSocket);

	//  if nothing was read, bail out (this should be because you have stopped this thread and closed the socket
	if ( acceptFileDescriptor < 0 )
		return;

	//  we read something, reset the conection flag
	mNumberOfClientConnectionFailuers = 0;

	//  log event time
	timeval eventTime;
	gettimeofday(&eventTime, 0);

	//  log event sender
	string eventSender = IpAddressString(clientAddress);
	
	//  parse what was read
	Parser readParser(readFromSocket, ",");
	string command = readParser.GetNextString();

	//  Look for recognized commands
	//
	if ( command.compare("$TCP_BUTTON") == 0 )  
	{
		//  button n command
		string argument = readParser.GetNextString();
		string returnMessage = format("$TCP_BUTTON,ACK,%s", argument.c_str());

		write(acceptFileDescriptor, returnMessage.c_str(), returnMessage.size());

		mTheApp.HandleButtonPush(eventTime, eventSender, readFromSocket);
	}
	else if ( command.compare("$TCP_MESSAGE") == 0 )
	{
		//  message from client command
		string argument = readParser.GetNextString();
		string returnMessage = format("$TCP_MESSAGE,ACK,%s", argument.c_str());
		write(acceptFileDescriptor, returnMessage.c_str(), returnMessage.size());

		//  broadcast this message to clients
		mTheApp.HandleBroadcastMessage(eventTime, eventSender, readFromSocket);
	}
	else if ( command.compare("$TCP_ECHOTEST") == 0 )
	{
		//  echo back
		write(acceptFileDescriptor, readFromSocket.c_str(), readFromSocket.size());

		//  log the event
		mTheApp.AddEvent(eventTime, eventSender, readFromSocket);
	}
	else
	{
		//  unknown command
		string returnMessage = format("$TCP_NAK,unknown command: %s", readFromSocket.c_str());
		write(acceptFileDescriptor, returnMessage.c_str(), returnMessage.size());

		//  log event
		string eventToLog = format("  ! Unknown command received:  %s", readFromSocket.c_str());
		mTheApp.AddEvent(eventTime, eventSender, eventToLog);
	}
	
	


	
	return;
}

