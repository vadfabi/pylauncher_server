//#include <stdio.h>
#include <sys/types.h> 
#include <sys/socket.h>
#include <netinet/in.h>
#include <cstdlib>
#include <strings.h>
#include <unistd.h>
#include <string.h>
#include <string>
#include <arpa/inet.h>


#include "ConnectionThread.h"
#include "TheApp.h"
#include "Parser.h"
#include "UtilityFn.h"

using namespace std;

ConnectionThread::ConnectionThread(TheApp& theApp) :
	mTheApp(theApp)
{
}


ConnectionThread::~ConnectionThread()
{
}


//--------------------------------
//  RunFunction
//  this is the Thread base class override
//  this function gets called each cycle through the thread
void ConnectionThread::RunFunction()
{
	struct sockaddr_in clientAddress;
	std::string readFromSocket;
	int acceptFileDescriptor =  ReadStringFromSocket(&clientAddress, readFromSocket);

	char* addressOfSender = inet_ntoa(clientAddress.sin_addr);
	std::string eventToLog = format("Received from %s: %s",addressOfSender, readFromSocket.c_str());
	mTheApp.AddEvent(eventToLog);

	//  parse the read string for known commands
	//  command syntax is "$TCP_SOMECOMMAND,argument1,argument2,...
	//
	Parser readParser(readFromSocket, ",");
	string command = readParser.GetNextString();

	//  Look for recognized commands
	//
	if ( command.compare("$TCP_CONNECT") == 0 )  
	{
		//  $TCP_CONNECT,portNumberOfClientsServerPort

		string argument1 = readParser.GetNextString();
		
		//  make sure connect command specifies proper port
		//  argument is port number that client is listenting on
		int clientsListeningPort = atoi(argument1.c_str());

		//  check for valid range
		if ( clientsListeningPort < 1024 || clientsListeningPort > 65535 )
		{
			string returnMessage = format("%s %s %s", "$TCP_CONNECT,NAK,", argument1.c_str(),  " is a bad port number.");
			write(acceptFileDescriptor, returnMessage.c_str(), returnMessage.size());
			return;
		}

		//  create a client connection here
		int serverPortForClientConnection = mTheApp.CreateClientConnection(clientAddress, clientsListeningPort);
		if ( serverPortForClientConnection < 0 )
		{
			string returnMessage = "$TCP_CONNECT,NAK,failed to open socket connection.";
			write(acceptFileDescriptor, returnMessage.c_str(), returnMessage.size());
			return;
		}
		
		//  we created a connection, tell client that we are listening on the new server port
		string returnMessage = format("$TCP_CONNECT,ACK,%d", serverPortForClientConnection);
		WriteStringToSocket(acceptFileDescriptor, returnMessage);

		return; 
	}
	else if ( command.compare("$TCP_DISCONNECT") == 0 )
	{
		//  we created a connection, tell client that we are listening on the new server port
		string returnMessage = "$TCP_DISCONNECT,ACK";
		WriteStringToSocket(acceptFileDescriptor, returnMessage);

		//  disconnect function, instruct the app to disconnect us
		//  this will happen in seperate thread, which will shut down the connected client thread
		mTheApp.DisconnectClient(clientAddress);
	}
	else
	{
		//  no other commands recognized by this thread
		return; 
	}
	


	//  Echo behavior send return message
	//std::string returnMessage;
	//returnMessage = "I got this message ";
	//returnMessage += (buffer+2);

	//n = write(acceptFileDescriptor,returnMessage.c_str(), returnMessage.size());
	
	
}