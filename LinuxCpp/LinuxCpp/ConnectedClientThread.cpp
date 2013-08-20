#include <sys/types.h> 
#include <sys/socket.h>
#include <cstdlib>
#include <strings.h>
#include <unistd.h>
#include <string.h>
#include <string>
#include <sys/time.h>

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

		//  log the event
		mTheApp.AddEvent(eventTime, eventSender, readFromSocket);
	}
	else if ( command.compare("$TPC_BROADCASTMESSAGE") )
	{
		//  broadcast message command
		string returnMessage = "$TCP_BROADCASTMESSAGE,ACK";
		write(acceptFileDescriptor, returnMessage.c_str(), returnMessage.size());

		string message = readParser.GetNextString();
		mTheApp.BroadcastClientsMessage(eventTime, eventSender, message);
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

