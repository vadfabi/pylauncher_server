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
#include <sys/time.h>


#include "ConnectionThread.h"
#include "TheApp.h"
#include "Parser.h"
#include "UtilityFn.h"

using namespace std;


//  ConnectionThread
//  manages the main server connection thread, listens for connection requests from clients
//


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
	string readFromSocket;
	int acceptFileDescriptor =  ReadStringFromSocket(&clientAddress, readFromSocket);

	if ( acceptFileDescriptor < 0 )
		return;

	//  log event time
	timeval eventTime;
	gettimeofday(&eventTime, 0);

	//  log event sender
	char* addressOfSender = inet_ntoa(clientAddress.sin_addr);
	string eventSender = string(addressOfSender);

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

		//  make sure connect command specifies proper port
		//  argument is port number that client is listenting on
		int clientsListeningPort = readParser.GetNextInt();

		//  check for valid range
		if ( clientsListeningPort < 1024 || clientsListeningPort > 65535 )
		{
			string returnMessage = format("$TCP_CONNECT,NAK, %d is a bad port number.", clientsListeningPort);
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

		//  log the event
		mTheApp.AddEvent(eventTime, eventSender, readFromSocket);

		return; 
	}
	else if ( command.compare("$TCP_DISCONNECT") == 0 )
	{
		//  we created a connection, tell client that we are listening on the new server port
		string returnMessage = "$TCP_DISCONNECT,ACK";
		WriteStringToSocket(acceptFileDescriptor, returnMessage);

		//  disconnect function, instruct the app to disconnect us
		mTheApp.DisconnectClient(clientAddress);

		//  log the event
		mTheApp.AddEvent(eventTime, eventSender, readFromSocket);
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

}