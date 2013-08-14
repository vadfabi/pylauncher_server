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

	//  parse the read string for known commands
	//  command syntax is "$TCP_SOMECOMMAND,argument1,argument2,...
	//
	Parser readParser(readFromSocket, ",");
	string command = readParser.GetNextString();

	//  Look for recognized commands
	//
	if ( command.compare("$TPC_CONNECT") == 0 )  
	{
		//  $TCP_CONNECT,portNumberOfClientsServerPort

		string argument1 = readParser.GetNextString();
		string argument2 = readParser.GetNextString();		//  todo - port will be arg1

		//  make sure connect command specifies proper port
		//  argument is port number that client is listenting on
		int clientsListeningPort = atoi(argument2.c_str());

		//  check for valid range
		if ( clientsListeningPort < 1024 || clientsListeningPort > 65535 )
		{
			string returnMessage = format("%s %s %s", "$TCP_CONNECT,NAK,", argument1.c_str(),  " is a bad port number.");
			write(acceptFileDescriptor, returnMessage.c_str(), returnMessage.size());
			return;
		}

		//  create a client connection here
		int serverPortForClientConnection = mTheApp.CreateClientConnection(&clientAddress, clientsListeningPort);
		if ( serverPortForClientConnection < 0 )
		{
			string returnMessage = "$TPC_CONNECT,NAK,failed to open socket connection.";
			write(acceptFileDescriptor, returnMessage.c_str(), returnMessage.size());
			return;
		}
		
		//  we created a connection, tell client that we are listening on the new server port
		string returnMessage = format("$TPC_CONNECT,ACK,%d", serverPortForClientConnection);
		WriteStringToSocket(acceptFileDescriptor, returnMessage);

		return; 
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