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


//  Helper classe for the main app
//

//  DisplayThread
//  The display output runs on it own thread
//
class DisplayThread : public Thread
{
public:
	// this class takes a reference to the TheApp class
	// this is a quick and dirty (and easy) way for classes to communicate without using singletons or global variables
	DisplayThread(TheApp& theApp);
	
	virtual void RunFunction();

protected:
	//  reference to TheApp object so we can call its functions
	TheApp& mTheApp;
};



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

	//  shut down application components for exit
	void ShutDown();

	//  add event to the event log
	void AddEvent(timeval eventTime,  string eventSender, string eventDetails);

	//  create a connection to a client
	//  returns the port that this program is listening on for TCP from the client
	//  if fail to create client, returns -1
	int CreateClientConnection(const struct sockaddr_in &clientAddress, int clientListeningOnPortNumber);

	//  disconnect a client connection
	void DisconnectClient(struct sockaddr_in &clientAddress);

	//  function to process the button push message from any connected client
	void HandleButtonPush(timeval eventTime, string eventSender, string eventDetails);

	//  pass along a broadcast message to all clients
	void BroadcastMessageToClients(timeval eventTime, string eventSender, string message);

	//  flag for message forwarding
	bool mForwardMessagesToAllClients;
	bool mForwardMessageWaitForClientResponse;


	//  Display Handling
	//
	//  set flag to update display
	void SetUpdateDisplay();
	//  set flag to suspend display updtes
	void SuspendDisplayUpdates();
	//  set flag to resume display updates
	void ResumeDisplayUpdates();


	//  command line functions
	void BroadcastMessage(string input);
	bool SaveLogs(string input);
	void PrintLogs(FILE* stream);
	void ClearLogs();
		

protected:

	//  remember ifconfig properties
	CMDifconfig mCMDifconfig;
	string mHostname;

	//  connection server port, listening for connect / disconnect requests on this port
	int mConnectionServerPort;

	//  server connection thread
	ConnectionThread mConnectionThread;

	//  connected clients
	map<string, ConnectedClient*> mConnectedClients;

	//  mutex to lock access to connected clients map
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

	string mVersionString;
};



#endif // _THEAPP_H