//#include <stdio.h>
#include <sys/types.h> 
#include <sys/socket.h>
#include <cstdlib>
#include <strings.h>
#include <unistd.h>
#include <string.h>
#include <string>
#include <arpa/inet.h>


#include "ConnectedClientThread.h"
#include "TheApp.h"
#include "Parser.h"
#include "UtilityFn.h"

ConnectedClient::ConnectedClient(TheApp& theApp, std::string ipAddressOfClient) :
	mTheApp(theApp)
{
	mIpAddressOfClient = ipAddressOfClient;
}


ConnectedClient::~ConnectedClient()
{
	printf("Deleting ConnectedClient %0x\n", (unsigned int)this);
}


//--------------------------------
//  RunFunction
//  waits for a message from the client, responds to known messages
void ConnectedClient::RunFunction()
{
	struct sockaddr_in clientAddress;
	std::string readFromSocket;
	int acceptFileDescriptor = ReadStringFromSocket(&clientAddress, readFromSocket);

	if ( acceptFileDescriptor < 0 )
		return;
	
	char* addressOfSender = inet_ntoa(clientAddress.sin_addr);
	std::string eventToLog = format("Received from %s: %s",addressOfSender, readFromSocket.c_str());
	mTheApp.AddEvent(eventToLog);

	Parser readParser(readFromSocket, ",");
	std::string command = readParser.GetNextString();

	//  Look for recognized commands
	//
	if ( command.compare("$TCP_BUTTON") == 0 )  
	{
		std::string argument = readParser.GetNextString();

	}
	
	


	
	return;
}


void ConnectedClient::Cancel()
{
	//  make sure we shut down connection before we call cancel
	ShutDown();
}

void ConnectedClient::ShutDown()
{
	//  if we are connected, send the disconnect command to the client

	//  shut down the thread
	TCPServerThread::Cancel();

	return;
}


