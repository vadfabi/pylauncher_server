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


//  SendMessageToClient
//
bool ConnectedClient::SendMessageToClient(string message, string& response)
{
	int clientSocketFileDescriptor;
	clientSocketFileDescriptor = socket(AF_INET, SOCK_STREAM, 0);

	//  set the socket timeouts  
	struct timeval timeout;
	timeout.tv_usec = 0;
	//
	//  send timeout
	//if ( mSendTimeout > 0 )
	//{
		timeout.tv_sec = 1;
		if ( setsockopt(clientSocketFileDescriptor, SOL_SOCKET, SO_SNDTIMEO, (char *)&timeout, sizeof(timeout)) < 0 )
			return -1;
	//}
	//
	//  receive timeout
	//if ( mReceiveTimeout > 0 )
	//{
		timeout.tv_sec = 3;
		if ( setsockopt(clientSocketFileDescriptor, SOL_SOCKET, SO_RCVTIMEO, (char *)&timeout, sizeof(timeout)) < 0 )
			return -1;
	//}


	struct hostent *server;
	server = gethostbyname(mIpAddressOfClient.c_str());

    if (server == 0) {
        fprintf(stderr,"ERROR, no such host\n");
        exit(0);
    }

	struct sockaddr_in serverAddress;
	memset(&serverAddress, 0, sizeof(struct sockaddr_in)); 

	serverAddress.sin_family = AF_INET;
	bcopy((char*)server->h_addr, (char*)&serverAddress.sin_addr.s_addr, server->h_length);
	serverAddress.sin_port = htons(mPortNumberClientIsListeningOn);
	

	if ( connect(clientSocketFileDescriptor, (struct sockaddr *)&serverAddress, sizeof(serverAddress)) < 0 )
	{
		return false;
	}

	if ( write(clientSocketFileDescriptor, message.c_str(), message.size()) != message.size() )
	{
		return false;
	}

	char buffer[TCPSERVER_READBUFFERSIZE+1];
	memset(buffer, 0, TCPSERVER_READBUFFERSIZE+1);

	int bytesRead = 0;
	while((bytesRead = read(clientSocketFileDescriptor, buffer, TCPSERVER_READBUFFERSIZE)) > 0)
	{
		/* ensure null-terminated */
		buffer[bytesRead] = '\0';
		response += buffer;
		memset(buffer, 0, TCPSERVER_READBUFFERSIZE+1);
	}

	shutdown(clientSocketFileDescriptor, SHUT_RDWR);
	close(clientSocketFileDescriptor);

	return (bytesRead != 0);
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
	else if ( command.compare("$TPC_BROADCASTMESSAGE") )
	{
		//  broadcast message command
		string returnMessage = "$TCP_BROADCASTMESSAGE,ACK";
		write(acceptFileDescriptor, returnMessage.c_str(), returnMessage.size());

		string message = readParser.GetNextString();
		mTheApp.BroadcastMessageToClients(eventTime, eventSender, message);
	}
	else if ( command.compare("$TCP_ECHOTEST") == 0 )
	{
		//  echo test
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

