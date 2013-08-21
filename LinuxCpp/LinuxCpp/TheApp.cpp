#include <arpa/inet.h>
#include <future>
#include <string>
#include <sys/time.h>
#include <algorithm>
#include <iostream>

#include "TheApp.h"
#include "Parser.h"
#include "ConnectedClientThread.h"
#include "UtilityFn.h"

using namespace std;




//  Constructor
//
TheApp::TheApp() :
	mConnectionThread(*this),  mDisplayThread(*this)
{
	mVersionString = "1.0.0.1";

	mMaxEventsToLog = 9999;

	mDisplayUpdatesOn = true;
	mUpdateDisplay = true;

	mForwardMessagesToAllClients = true;

}


//  Destructor
//
TheApp::~TheApp()
{
	ShutDown();
}


//  InitializeInstance
//  perform all the required steps for the application to start
//
bool TheApp::InitializeInstance()
{
	//  open the server socket
	mConnectionServerPort = mConnectionThread.OpenServerSocket(48888, false);
	if ( mConnectionServerPort < 0 )
	{
		printf("Failed to open server socket!");
		return false;
	}

	//  get and remember the ip info of the machine for display purposes
	mCMDifconfig.Execute();

	//  start the connection thread
	mConnectionThread.Start();

	//  start the display thread
	mDisplayThread.Start();
	//  flag for update on first pass
	mUpdateDisplay = true;

	//  initialization successful
	return true;
}



//  ShutDown
//  stops running threads, and cleans up memory allocated by TheApp object
//
void TheApp::ShutDown()
{
	//  disconnect all of our clients
	map<string, ConnectedClient*>::iterator nextClient;
	for ( nextClient = mConnectedClients.begin(); nextClient != mConnectedClients.end(); nextClient++ )
	{
		nextClient->second->Cancel();
		delete nextClient->second;
	}	
	//  done with clients
	mConnectedClients.erase(mConnectedClients.begin(), mConnectedClients.end());

	//  cancel the connection thread
	if ( mConnectionThread.IsRunning() )
		mConnectionThread.Cancel();

	if ( mDisplayThread.IsRunning() )
		mDisplayThread.Cancel();

	//  delete the log event objects in the mEventLog list
	//  this method uses the helper function deleteLogEvent( )
	mEventLog.remove_if(deleteLogEvent);
}



//  AddEvent
//  add an event to the event log
//
void TheApp::AddEvent(timeval eventTime, string eventSender, string eventDetails)
{
	//  we will be modifying the event log list, lock access to it
	mEventLogMutex.lock();

	LogEvent* newEvent = new LogEvent(eventTime, eventSender, eventDetails);
	mEventLog.push_front(newEvent);

	if ( mEventLog.size() > mMaxEventsToLog )
	{
		delete mConnectedClients.end()->second;
		mEventLog.pop_back();
	}

	//  we are done modifying the event log, release access
	mEventLogMutex.unlock();

	//  update display
	SetUpdateDisplay();
}






//  CreateClientConnection
//  creates a new connection thread for an individual client
//
int TheApp::CreateClientConnection(const struct sockaddr_in &clientAddress, int clientListeningOnPortNumber)
{
	//  Thread safety on access to map of clients
	mConnectedClientsMutex.lock();

	//  get the dots and numbers string for client, this is used as the key in our clients map
	string  addressOfSender = IpAddressString(clientAddress);

	//  see if this client exists already
	map<string, ConnectedClient*>::iterator it = mConnectedClients.find(addressOfSender);
	if ( it != mConnectedClients.end() )
	{
		//  shut down old connection and delete this client
		it->second->Cancel();
		mConnectedClients.erase(addressOfSender);
		delete it->second;
	}

	//  create connection thread for the client
	ConnectedClient* newClient = new ConnectedClient(*this, clientAddress, clientListeningOnPortNumber);
	
	//  open a port to serve this client
	int servingClientOnPortNumber = newClient->OpenServerSocket(49000, false);

	if ( servingClientOnPortNumber < 0 )
	{
		delete newClient;
		mConnectedClientsMutex.unlock();
		return -1;
	}
	
	//  start this thread 
	newClient->Start();

	//  add the client to the map
	mConnectedClients[newClient->GetIpAddressOfClient()] =  newClient;

	//  unlock access to the client map
	mConnectedClientsMutex.unlock();

	return servingClientOnPortNumber;
}



//  DisconnectClient
//  closes client server port, stops client thread, and deletes memory allocated for client objects
//
void TheApp::DisconnectClient(struct sockaddr_in &clientAddress)
{
	//  we will be modifying the client map, lock access to it
	mConnectedClientsMutex.lock();

	//  our map uses dots and numbers address of client as a key
	string clientKey = IpAddressString(clientAddress);

	//  see if this client exists already
	map<string, ConnectedClient*>::iterator it = mConnectedClients.find(clientKey);

	if ( it == mConnectedClients.end() )
	{
		//  this key does not exist, not expected here
		DEBUG_TRACE("TheApp::DisconnectClient - client does not exist !!!\n");
		return;
	}

	//  stop client thread
	it->second->Cancel();

	//  delete memory
	delete it->second;

	//  remove client from map
	mConnectedClients.erase(clientKey);

	//  done with access to client map
	mConnectedClientsMutex.unlock();

	//  update display
	SetUpdateDisplay();
}



//  HandleButtonPush
//
void TheApp::HandleButtonPush(timeval eventTime, string eventSender, string eventDetails)
{
	//  parse what was read
	Parser readParser(eventDetails, ",");
	string command = readParser.GetNextString();

	//  get the button number
	string argument = readParser.GetNextString();
	int buttonNumber = atoi(argument.c_str());

	//  Here is where you could take action on receiving the button push
	//  remember, this could be called from any of the n connected client threads, 
	//  so make sure you wrap a lock around access to any shared memory collections
	//
	//


	//  if message forwarding is turned on, send the message to all of our clients
	if ( mForwardMessagesToAllClients )
	{
		mConnectedClientsMutex.lock();

		map<string, ConnectedClient*>::iterator nextClient;
		for ( nextClient = mConnectedClients.begin(); nextClient != mConnectedClients.end(); nextClient++ )
		{
			if ( nextClient->second->GetIpAddressOfClient().compare(eventSender) == 0 )
				continue;	//  don't rebroadcast to sender

			string response;
			nextClient->second->SendMessageToClient(eventDetails, response);
		}

		mConnectedClientsMutex.unlock();
	}

	AddEvent(eventTime, eventSender, eventDetails);
}




//  BroadcastClientsMessage
//  takes a message in from one client, and broadcasts to all other connected clients
//
void  TheApp::BroadcastMessageToClients(timeval eventTime, string eventSender, string message)
{
	string broadcastMessage = format("$TCP_BROADCAST,%s,%s,%s", eventSender.c_str(), FormatTime(eventTime).c_str(), message.c_str());
	
	//  lock access to connected clients map
	mConnectedClientsMutex.lock();

	//  send the message to all of our clients
	map<string, ConnectedClient*>::iterator nextClient;
	for ( nextClient = mConnectedClients.begin(); nextClient != mConnectedClients.end(); nextClient++ )
	{
		string response;
		nextClient->second->SendMessageToClient(broadcastMessage, response);
	}	
		
	//  unlock access to the connected clients map
	mConnectedClientsMutex.unlock();

	//  log the event
	AddEvent(eventTime, eventSender, broadcastMessage);
}




//  Command Line Functions
//  handlers for command arguments entered in main loop
//


void TheApp::BroadcastMessage(string input)
{
	// parse the message out of the input string
	string message = input.substr(string("broadcast").size()+1);

	//  log event time
	timeval eventTime;
	gettimeofday(&eventTime, 0);

	//  log event sender
	//  todo - change to host name
	string eventSender = mCMDifconfig.mEth0Info.mInet4Address;

	BroadcastMessageToClients(eventTime, eventSender, message);

	return;
}


//  SaveLogs
//  save logs out to a log file
//
bool TheApp::SaveLogs(string input)
{
	Parser inputParser(input, " ");
	string command = inputParser.GetNextString();
	string filename = inputParser.GetNextString();
	string clearArgument = inputParser.GetNextString();

	FILE* outputFile = fopen ( filename.c_str(), "w" );
	if ( outputFile == 0 )
		return false;

	//  accessing the event log list, lock it
	mEventLogMutex.lock();

	//  print all the logs
	for_each(  mEventLog.begin(), mEventLog.end(), bind2nd(mem_fun(&LogEvent::PrintLog), outputFile) );

	if ( clearArgument.compare("-c") == 0 )
		mEventLog.remove_if(deleteLogEvent);

	mEventLogMutex.unlock();

	fclose( outputFile );
	
	return true;
}



//  PrintLogs
//  print all events in the event log to the file stream you specify
//
void TheApp::PrintLogs(FILE* stream)
{
	//  lock access to event log list
	mEventLogMutex.lock();

	//  iterate through log list and print logs to stdout
	//  this ta
	for_each(  mEventLog.begin(), mEventLog.end(), bind2nd(mem_fun(&LogEvent::PrintLog),stream) );

	//  unlock access to event log list
	mEventLogMutex.unlock();
}



//  ClearLogs
//  clears the event log
void TheApp::ClearLogs()
{
	//  we will be modifying the event log list, so lock its access
	mEventLogMutex.lock();

	//  delete the log event objects in the mEventLog list
	//  this method uses the helper function deleteLogEvent( )
	mEventLog.remove_if(deleteLogEvent);

	/*
	//  this is instead of doing something more old fashioned such as
	//  clean up the event log
	list<LogEvent*>::iterator nextEvent;
	for ( nextEvent = mEventLog.begin(); nextEvent != mEventLog.end(); nextEvent++ )
	{
		delete *nextEvent;
	}
	*/

	//  unlock access to event log list
	mEventLogMutex.unlock();
}





//  Display Output
//


//  SetUpdateDisplay
//  set the flag that the display is to be updated on the next pass
//
void TheApp::SetUpdateDisplay()
{
	mDisplayUpdateMutex.lock();
	mUpdateDisplay = true;
	mDisplayUpdateMutex.unlock();
}


//  SuspendDisplayUpdates
//  set flag to suspend display updtes, this is used when main( ) enters command mode for terminal input
//
void TheApp::SuspendDisplayUpdates() 
{
	mDisplayUpdateMutex.lock();
	mDisplayUpdatesOn = false; 
	mDisplayUpdateMutex.unlock();
}


//  ResumeDisplayUpdates
//  set flag to resume display updates
//
void TheApp::ResumeDisplayUpdates()
{
	//  no need to lock the mutex here, since no display updates are running in this state

	// force refresh
	mUpdateDisplay = true;

	//  turn updates back on
	mDisplayUpdatesOn = true; 
}



//  The display output runs on it own thread, this class deffinition is here
//  
DisplayThread::DisplayThread(TheApp& theApp) : mTheApp(theApp)
{
}

void DisplayThread::RunFunction()
{
	//  TODO:  refactor this from a timer wait to scheme where this thread waits for a signal that there is an update to process
	Sleep(10);

	//  update display
	mTheApp.DisplayUpdate();
}



//  DisplayUpdate
//  the master function to call all the display update parts
//
void TheApp::DisplayUpdate()
{
	//  if updates are suspended, then just return
	if ( ! mDisplayUpdatesOn )
		return;

	//  time tag
	timeval timeNow;
	gettimeofday(&timeNow, 0);

	if ( mUpdateDisplay )
	{
		//  lock the display update
		mDisplayUpdateMutex.lock();

		//  Redraw the whole display
		system("clear");

		DisplayWriteHeader();
		//
		DisplayWriteClientConnections();
		//
		DisplayWriteLogs();
		// 
		DisplayWriteTime();
		mTimeOfLastClockUpdate = timeNow;

		mUpdateDisplay = false;

		//  unlock the display update
		mDisplayUpdateMutex.unlock();
	}
	else
	{
		//  do we need to update system clock
		if ( DurationMilliseconds(mTimeOfLastClockUpdate, timeNow) > 1000 )
		{
			DisplayUpdateClock();
			mTimeOfLastClockUpdate = timeNow;
		}
	}
}



void TheApp::DisplayWriteHeader()
{
	printf("/***********************************************************************************\n");
	printf("/***  Simple Linux Connect TCP/IP Program \n");
	printf("/***  %s:\n", mVersionString.c_str());
	printf("/***\n");
	printf("/***      Connected on eth0: %s\n",mCMDifconfig.mEth0Info.mInet4Address.size() == 0 ? "not enabled " : mCMDifconfig.mEth0Info.mInet4Address.c_str());
	printf("/***      Connected on wlan: %s\n",mCMDifconfig.mWlanInfo.mInet4Address.size() == 0 ? "not enabled " : mCMDifconfig.mWlanInfo.mInet4Address.c_str());
	printf("/***      Server is listening on port: %d\n", mConnectionServerPort);	


}


void TheApp::DisplayWriteClientConnections()
{
	mConnectedClientsMutex.lock();

	//  write out client connection state
	map<string, ConnectedClient*>::iterator iter;

	for ( iter = mConnectedClients.begin(); iter != mConnectedClients.end(); iter++ )
	{
		printf("/***      - Client at %s connected on port %d.\n", iter->second->GetIpAddressOfClient().c_str(), iter->second->GetConnectedOnPortNumber() );
	}

	mConnectedClientsMutex.unlock();
}


void TheApp::DisplayWriteLogs()
{
	printf("/***\n");
	printf("/***      Event Logs:\n");

	mEventLogMutex.lock();

	//  write out the last 5 logs
	int logSize = mEventLog.size();
	for ( int i = 4; i >= 0; i-- )
	{
		if ( i < logSize )
		{
			list<LogEvent*>::iterator it = mEventLog.begin();
			advance(it, i);
			printf("/***        > %s : %s : %s\n", FormatTime((*it)->mEventTime).c_str(), (*it)->mEventAddress.c_str(), (*it)->mEvent.c_str());
		}
		else
			printf("/***        >\n");
	}

	printf("/***\n");

	mEventLogMutex.unlock();
}


void TheApp::DisplayWriteTime()
{
	//  put the time as last line of display
	CMD dateCommand("date");
	dateCommand.Execute();
	printf("/***     Time:      %s\n", dateCommand.GetCommandResponseLine(0).c_str());
	printf("/***     Press Enter to enable command mode:\n");
}


void TheApp::DisplayUpdateClock()
{
	if ( ! mDisplayUpdatesOn )
		return;

	mDisplayUpdateMutex.lock();

	//  rewind stdout two lines to reset to start of date string output
	fputs("\033[A\033[2K",stdout);
	fputs("\033[A\033[2K",stdout);
	rewind(stdout);
	
	//  write the time
	DisplayWriteTime();

	mDisplayUpdateMutex.unlock();
}



