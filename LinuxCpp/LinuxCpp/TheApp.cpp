#include <arpa/inet.h>
#include <string>
#include <sys/time.h>
#include <algorithm>
#include <future>

#include "TheApp.h"
#include "Parser.h"
#include "ConnectedClientThread.h"
#include "UtilityFn.h"

using namespace std;



/////////////////////////////////////////////////////////////////////////////
//  TheApp
//  the grand central station of the program,
//  this class runs everything except the keyboard input thread, which is in main
//


//  Constructor
//
TheApp::TheApp() :
	mConnectionThread(*this),  mDisplayThread(*this), mBroadcastThread(*this)
{
	mVersionString = "1.0.0.1";

	mMaxEventsToLog = 9999;

	mDisplayUpdatesOn = true;
	mUpdateDisplay = true;

	mForwardMessagesToAllClients = false;
	mForwardMessageWaitForClientResponse = false;

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
	CMD hostName("hostname");
	hostName.Execute();
	mHostname = hostName.GetCommandResponseLine(0);

	//  start the connection thread
	mConnectionThread.Start();

	//  start the display thread
	mDisplayThread.Start();
	//  flag for update on first pass
	mUpdateDisplay = true;

	mBroadcastThread.Start();

	//  initialization successful
	return true;
}



//  ShutDown
//  stops running threads, and cleans up memory allocated by TheApp object
//
void TheApp::ShutDown()
{
	//  stop the broadcast thread
	if ( mBroadcastThread.IsRunning() )
		mBroadcastThread.Cancel();

	//  disconnect all of our clients
	map<string, ConnectedClient*>::iterator nextClient;
	for ( nextClient = mConnectedClients.begin(); nextClient != mConnectedClients.end(); nextClient++ )
	{
		nextClient->second->Cancel();
		delete nextClient->second;
	}	
	//  done with clients
	mConnectedClients.erase(mConnectedClients.begin(), mConnectedClients.end());

	//  stop the connection thread
	if ( mConnectionThread.IsRunning() )
		mConnectionThread.Cancel();

	//  stop the display thread
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
	{
		LockMutex lockEvents(mEventLogMutex);

		LogEvent* newEvent = new LogEvent(eventTime, eventSender, eventDetails);
		mEventLog.push_front(newEvent);

		if ( mEventLog.size() > mMaxEventsToLog )
		{
			delete mConnectedClients.end()->second;
			mEventLog.pop_back();
		}

		if ( ! mDisplayUpdatesOn )
			printf("%s %s %s\n", FormatTime(eventTime).c_str(), eventSender.c_str(), eventDetails.c_str());

	}//  we are done modifying the event log, release access
	
	//  update display
	SetUpdateDisplay();
}


//  AddEvent
//  add an event to the log
//
void TheApp::AddEvent(string eventSender, string eventDetails)
{
	timeval eventTime;
	gettimeofday(&eventTime, 0);

	AddEvent(eventTime, eventSender, eventDetails);
}



//  CreateClientConnection
//  creates a new connection thread for an individual client
//
int TheApp::CreateClientConnection(const struct sockaddr_in &clientAddress, int clientListeningOnPortNumber)
{
	int servingClientOnPortNumber = -1;

	//  lock on access to map of clients
	{
		LockMutex lockConnectedClients(mConnectedClientsMutex);

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
		 servingClientOnPortNumber = newClient->OpenServerSocket(49000, false);

		if ( servingClientOnPortNumber < 0 )
		{
			delete newClient;
			return -1;
		}

		//  start the thread
		newClient->Start();

		//  add the client to the map
		mConnectedClients[newClient->GetIpAddressOfClient()] =  newClient;

	} //  unlock access to the client map
	
	return servingClientOnPortNumber;
}



//  DisconnectClient
//  closes client server port, stops client thread, and deletes memory allocated for client objects
//
void TheApp::DisconnectClient(struct sockaddr_in &clientAddress)
{
	//  we will be modifying the client map, lock access to it
	{
		LockMutex lockConnectedClients(mConnectedClientsMutex);
		
		//  our map uses dots and numbers address of client as a key
		string clientKey = IpAddressString(clientAddress);

		//  see if this client exists already
		map<string, ConnectedClient*>::iterator it = mConnectedClients.find(clientKey);
		if ( it == mConnectedClients.end() )
		{
			//  this key does not exist, not expected here
			return;
		}

		//  stop client thread
		it->second->Cancel();

		//  delete memory
		delete it->second;

		//  remove client from map
		mConnectedClients.erase(clientKey);
	
	}	//  release access to client map

	//  update display
	SetUpdateDisplay();
}



//  HandleButtonPush
//  this function is called from the connected client thread when we get a push button message
//
void TheApp::HandleButtonPush(timeval eventTime, string eventSender, string eventDetails)
{
	//  parse what was read
	Parser readParser(eventDetails, ",");
	string command = readParser.GetNextString();

	//  get the button number
	string argument = readParser.GetNextString();
	int buttonNumber = atoi(argument.c_str());

	//  BUILD YOUR PROGRAM HERE
	//

	//  Here is where you could take action on receiving the button push
	//  remember, this could be called from any of the n connected client threads, 
	//  so make sure you wrap a lock around access to any shared memory collections
	//
	//

	//  in this simple program, the response to button push from client is to check to see if message forwarding is turned on
	//  if message forwarding is turned on, send the message to all of our clients

	//  log the event
	AddEvent(eventTime, eventSender, eventDetails);

	//  forward message
	if ( mForwardMessagesToAllClients )
	{
		mBroadcastThread.AddMessage(eventTime, eventSender, eventDetails);
	}
}



//  BroadcastMessage
//  this function is called from the connected client thread when we get a broadcast message
//
void TheApp::HandleBroadcastMessage(timeval eventTime, string eventSender, string message)
{
	AddEvent(eventTime, eventSender, message);

	if ( mForwardMessagesToAllClients )
	{
		mBroadcastThread.AddMessage(eventTime, eventSender, message);
	}
}



//  SendMessageToAllClients
//  this function is called from the broadcast thread to send a message to all clients
//
void TheApp::SendMessageToAllClients(timeval eventTime, string eventSender, string message)
{
	LockMutex lockConnectedClients(mConnectedClientsMutex);

	map<string, ConnectedClient*>::iterator nextClient;
	for ( nextClient = mConnectedClients.begin(); nextClient != mConnectedClients.end(); nextClient++ )
	{
		if ( nextClient->second->GetIpAddressOfClient().compare(eventSender) == 0 )
			continue;	//  don't rebroadcast to sender

		string response = nextClient->second->SendMessageToClient(message,  mForwardMessageWaitForClientResponse);

		//  if we are waiting for responses, log the response as an event
		if ( mForwardMessageWaitForClientResponse )
		{
			AddEvent(nextClient->second->GetIpAddressOfClient(),  response);
		}
	}



}




//  SendMessageToAllClients
//  this function is called from the broadcast thread to send a message to all clients
//  this implementation uses async, but it is not stable, too many failures to connect on socket with error code EINTR
//
//void TheApp::SendMessageToAllClients(timeval eventTime, string eventSender, string message)
//{
//	LockMutex lockConnectedClients(mConnectedClientsMutex);
//
//	vector<future<string>> futures;
//
//	map<string, ConnectedClient*>::iterator nextClient;
//	for ( nextClient = mConnectedClients.begin(); nextClient != mConnectedClients.end(); nextClient++ )
//	{
//		if ( nextClient->second->GetIpAddressOfClient().compare(eventSender) == 0 )
//			continue;	//  don't rebroadcast to sender
//
//		futures.push_back(async(launch::async, &ConnectedClient::SendMessageToClient, nextClient->second, message, mForwardMessageWaitForClientResponse));
//	}
//
//	//  if we are waiting for responses, log the response as an event
//	if ( mForwardMessageWaitForClientResponse )
//	{
//		vector<future<string>>::iterator nextFuture;
//		for( nextFuture = futures.begin(); nextFuture != futures.end(); ++nextFuture )
//		{
//			nextFuture->wait();
//			AddEvent("",  nextFuture->get());
//		}
//	}
//
//}







//  Command Line Functions
//  handlers for command functions from arguments entered in main loop
//


void TheApp::BroadcastMessage(string input)
{
	// parse the message out of the input string

	string message = input.substr(string("broadcast").size()+1);

	//  log event time
	timeval eventTime;
	gettimeofday(&eventTime, 0);

	//  log event sender
	//  todo  this assumes you are on wired, we should check for wifi here ?
	string eventSender = mCMDifconfig.mEth0Info.mInet4Address;

	string broadcastMessage = format("$TCP_BROADCAST,%s,%s,%s", eventSender.c_str(), FormatTime(eventTime).c_str(), message.c_str());

	mBroadcastThread.AddMessage(eventTime, eventSender, broadcastMessage);

	//  log the event
	AddEvent(eventTime, eventSender, broadcastMessage);

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

	//  open file
	FILE* outputFile = fopen ( filename.c_str(), "w" );
	if ( outputFile == 0 )
		return false;

	//  accessing the event log list, lock it
	LockMutex lockEventLog(mEventLogMutex);

	//  print all the logs
	for_each(  mEventLog.begin(), mEventLog.end(), bind2nd(mem_fun(&LogEvent::PrintLog), outputFile) );

	//  if remove command, clear the logs
	if ( clearArgument.compare("-c") == 0 )
		mEventLog.remove_if(deleteLogEvent);

	//  close file
	fclose( outputFile );
	
	return true;
}



//  PrintLogs
//  print all events in the event log to the file stream you specify
//
void TheApp::PrintLogs(FILE* stream)
{
	//  lock access to event log list
	LockMutex lockEventLog(mEventLogMutex);

	//  iterate through log list and print logs to stdout
	//  this ta
	for_each(  mEventLog.begin(), mEventLog.end(), bind2nd(mem_fun(&LogEvent::PrintLog),stream) );

}



//  ClearLogs
//  clears the event log
void TheApp::ClearLogs()
{
	//  lock access to event log list
	LockMutex lockEventLog(mEventLogMutex);

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

}





//  Display Output
//


//  SetUpdateDisplay
//  set the flag that the display is to be updated on the next pass
//
void TheApp::SetUpdateDisplay()
{
	LockMutex lockDisplay(mDisplayUpdateMutex);
	mUpdateDisplay = true;
}


//  SuspendDisplayUpdates
//  set flag to suspend display updtes, this is used when main( ) enters command mode for terminal input
//
void TheApp::SuspendDisplayUpdates() 
{
	LockMutex lockDisplay(mDisplayUpdateMutex);
	mDisplayUpdatesOn = false; 
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
	//  TODO:  room for improvement
	//  using a constant polling is inefficient
	//  this thread should be refactored to block on waiting for a message (or signal ?) to indicate that display updates need to happen
	Sleep(50);

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
		LockMutex lockDisplay(mDisplayUpdateMutex);

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
	printf("/***      Message forwarding is %s with %s for response.\n", mForwardMessagesToAllClients ? "on" : "off", mForwardMessageWaitForClientResponse ? "wait" : "no wait");	


}


void TheApp::DisplayWriteClientConnections()
{
	//LockMutex lockClients(mConnectedClientsMutex);
	if ( mConnectedClientsMutex.try_lock() )
	{
	//  write out client connection state
	map<string, ConnectedClient*>::iterator iter;

	for ( iter = mConnectedClients.begin(); iter != mConnectedClients.end(); iter++ )
	{
		printf("/***      - Client at %s connected on port %d.\n", iter->second->GetIpAddressOfClient().c_str(), iter->second->GetConnectedOnPortNumber() );
	}

	mConnectedClientsMutex.unlock();

	}
}


void TheApp::DisplayWriteLogs()
{
	printf("/***\n");
	printf("/***      Event Logs:\n");

	LockMutex lockEvents(mEventLogMutex);

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
}


void TheApp::DisplayWriteTime()
{
	//  put the time as last line of display
	CMD dateCommand("date");
	dateCommand.Execute();
	printf("/***     Time:      %s\n", dateCommand.GetCommandResponseLine(0).c_str());
	printf("/***     Press Enter to enable command mode:>\n");
}


void TheApp::DisplayUpdateClock()
{
	if ( ! mDisplayUpdatesOn )
		return;

	LockMutex lockDisplay(mDisplayUpdateMutex);

	//  this function is called when there is nothing on the display to update except the clock
	//  use this little trick to rewind stdout two lines to reset to start of date string output and avoid redraw entire display
	fputs("\033[A\033[2K",stdout);
	fputs("\033[A\033[2K",stdout);
	rewind(stdout);
	
	//  write the time
	DisplayWriteTime();
}



