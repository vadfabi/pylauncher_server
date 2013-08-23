#include <sys/types.h> 
#include <sys/socket.h>
#include <cstdlib>
#include <strings.h>
#include <unistd.h>
#include <string.h>
#include <string>
#include <sys/time.h>

#include <stdio.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <netdb.h> 


#include "ConnectedClientThread.h"
#include "TheApp.h"
#include "Parser.h"
#include "UtilityFn.h"

using namespace std;



//  Constructor
//
ConnectedClient::ConnectedClient(TheApp& theApp, const struct sockaddr_in &clientAddress, int clientsListeningOnPortNumber) :
	mTheApp(theApp)
{
	mClientsAddress = clientAddress;
	mPortNumberClientIsListeningOn = clientsListeningOnPortNumber;
	mIpAddressOfClient = IpAddressString(mClientsAddress);

	mClientReceiveTimeout = 3;
	mClientSendTimeout = 3;
}


//  Destructor
//
ConnectedClient::~ConnectedClient()
{
	if ( mThreadRunning )
	{
		DEBUG_TRACE("Destroying ConnectedClient while thread still running !\n");
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




//  SendMessageToClient
//
string ConnectedClient::SendMessageToClient(string message,  bool waitForResponse)
{
	string response = "";

	//  connected client object has a pipe open back to the client for sending messages
	int clientSocketFileDescriptor;
	clientSocketFileDescriptor = socket(AF_INET, SOCK_STREAM, 0);

	//  set the socket timeouts  
	struct timeval timeout;
	timeout.tv_usec = 0;
	//
	//  send timeout
	if ( mClientSendTimeout > 0 )
	{
		timeout.tv_sec = mClientSendTimeout;
		if ( setsockopt(clientSocketFileDescriptor, SOL_SOCKET, SO_SNDTIMEO, (char *)&timeout, sizeof(timeout)) < 0 )
		{
			close(clientSocketFileDescriptor);
			return response;
		}
	}
	//
	//  receive timeout
	if ( mClientReceiveTimeout > 0 )
	{
		timeout.tv_sec = mClientReceiveTimeout;
		if ( setsockopt(clientSocketFileDescriptor, SOL_SOCKET, SO_RCVTIMEO, (char *)&timeout, sizeof(timeout)) < 0 )
		{
			close(clientSocketFileDescriptor);
			return response;
		}
	}

	struct hostent *server;
	server = gethostbyname(mIpAddressOfClient.c_str());

    if (server == 0) 
	{
       close(clientSocketFileDescriptor);
        return response;
    }
	
	//
	struct sockaddr_in clientsListeningServerAddress;
	memset(&clientsListeningServerAddress, 0, sizeof(struct sockaddr_in)); 

	clientsListeningServerAddress.sin_family = AF_INET;
	bcopy((char*)server->h_addr, (char*)&clientsListeningServerAddress.sin_addr.s_addr, server->h_length);
	clientsListeningServerAddress.sin_port = htons(mPortNumberClientIsListeningOn);

	if ( connect(clientSocketFileDescriptor, (struct sockaddr *)&clientsListeningServerAddress, sizeof(clientsListeningServerAddress)) < 0 )
	{
		printf("Connected client thread: fail to connect %d \n", errno);
		close(clientSocketFileDescriptor);
		return response;
	}

	if ( write(clientSocketFileDescriptor, message.c_str(), message.size()) != message.size() )
	{
		close(clientSocketFileDescriptor);
		return response;
	}

	int bytesRead = 0;
	if ( waitForResponse )
	{
		char buffer[TCPSERVER_READBUFFERSIZE+1];
		memset(buffer, 0, TCPSERVER_READBUFFERSIZE+1);

		while((bytesRead = read(clientSocketFileDescriptor, buffer, TCPSERVER_READBUFFERSIZE)) > 0)
		{
			/* ensure null-terminated */
			buffer[bytesRead] = '\0';
			response += buffer;
			memset(buffer, 0, TCPSERVER_READBUFFERSIZE+1);
		}
	}

	//  shutdown
	shutdown(clientSocketFileDescriptor, SHUT_RDWR);
	close(clientSocketFileDescriptor);

	return response;
}



//  Cancel
//
void ConnectedClient::Cancel()
{
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

	//  we read something

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
	else if ( command.compare("$TCP_BROADCAST") == 0 )
	{
		//  broadcast message command
		string returnMessage = "$TCP_BROADCAST,ACK";
		write(acceptFileDescriptor, returnMessage.c_str(), returnMessage.size());

		// get the message from the input
		readParser.GetNextString();
		string message = readParser.GetRemainingBuffer();

		//  broadcast this message to clients
		mTheApp.BroadcastMessageToClients(eventTime, eventSender, message);
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

