#ifndef _THEAPP_H
#define _THEAPP_H

#include <string>
#include <list>
#include <mutex>
#include <map>

#include "Thread.h"
#include "ConnectionThread.h"
#include "ConnectedClientThread.h"
#include "CMDifconfig.h"
#include "UtilityFn.h"

using namespace std;


//////////////
//  Display Output 
//  The display output runs on it own thread
//
class DisplayThread : public Thread
{
public:
	DisplayThread(TheApp& theApp);
	
	virtual void RunFunction();

protected:
	TheApp& mTheApp;
};


/////////////////
//  LogEvent
//  class to hold log events
//


class LogEvent
{
public:
	LogEvent();
	LogEvent(timeval eventTime, string eventAddress, string eventDescription)
	{ 
		mEventTime = eventTime;
		mEventAddress = eventAddress;
		mEvent = eventDescription;
	}

	void PrintLog( FILE* stream )
	{
		fprintf( stream, "%s,%s,%s\n", FormatTime(mEventTime).c_str(), mEventAddress.c_str(), mEvent.c_str() );
	}

	timeval mEventTime;
	string mEventAddress;
	string mEvent;
};

//  deletion helper function
inline static bool deleteLogEvent( LogEvent* eventToDelete ) { delete eventToDelete; return true; } 


// TheApp
// This class is the main application
// it is running on the main thead
// application sub processes, such as socket connection and display update are launched in their own threads
//
class TheApp
{
public:

	TheApp();
	virtual ~TheApp();

	//  initialize app components for startup
	bool InitializeInstance();

	//  shut down applicatio components for exit
	void ShutDown();

	//  create a connection to a client
	//  returns the port that this program is listening on for TCP from the client
	//  if fail to create client, returns -1
	int CreateClientConnection(const struct sockaddr_in &clientAddress, int clientListeningOnPortNumber);

	//  disconnect a client connection
	void DisconnectClient(struct sockaddr_in &clientAddress);

	//  add event to the event log
	void AddEvent(timeval eventTime,  string eventAddress, string eventDetails);

	//  pass along a broadcast message to all clients
	void BroadcastClientsMessage(timeval eventTime, string eventAddress, string message);


	//  Display Handling
	//
	//  set flag to update display
	void SetUpdateDisplay();
	//  set flag to suspend display updtes
	void SuspendDisplayUpdates();
	//  set flag to resume display updates
	void ResumeDisplayUpdates();


	//  command line functions
	bool SaveLogs(string input);
	void PrintLogs(FILE* stream);
	void ClearLogs();

	

protected:

	string mVersionString;

	//  remember ifconfig properties
	CMDifconfig mCMDifconfig;

	//  connection server port, listening for connect / disconnect requests on this port
	int mConnectionServerPort;

	//  server connection thread
	ConnectionThread mConnectionThread;

	//  connected clients
	map<string, ConnectedClient*> mConnectedClients;

	mutex mConnectedClientsMutex;

	//  the event log
	list<LogEvent*> mEventLog;
	mutex mEventLogMutex;
	int mMaxEventsToLog;

	//  The display output
	//  this happens on its own thread
	DisplayThread mDisplayThread;
	friend class DisplayThread;
	mutex mDisplayUpdateMutex;
	
	bool mDisplayUpdatesOn;
	bool mUpdateDisplay;

	timeval mTimeOfLastClockUpdate;
	
	//  update display function, 
	void DisplayUpdate();
	void DisplayWriteHeader();
	void DisplayWriteClientConnections();
	void DisplayWriteLogs();
	void DisplayWriteTime();
	void DisplayUpdateClock();
};



#endif // _THEAPP_H