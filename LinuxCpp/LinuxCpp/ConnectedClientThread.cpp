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

ConnectedClient::ConnectedClient(TheApp& theApp) :
	mTheApp(theApp)
{
}


ConnectedClient::~ConnectedClient()
{
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
	
	printf("Received from %s: %s\n",addressOfSender, readFromSocket.c_str());


	//  Echo behavior send return message
	//std::string returnMessage;
	//returnMessage = "I got this message ";
	//returnMessage += (buffer+2);

	//n = write(acceptFileDescriptor,returnMessage.c_str(), returnMessage.size());
	
	return;
}