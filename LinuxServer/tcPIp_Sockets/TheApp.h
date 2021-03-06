#ifndef _THEAPP_H
#define _THEAPP_H

#include <string>
#include <list>
#include <mutex>
#include <map>

#include "Thread.h"
#include "ConnectionThread.h"
#include "ConnectedClientThread.h"
#include "BroadcastThread.h"
#include "CMDifconfig.h"
#include "UtilityFn.h"


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
	void AddEvent(timeval eventTime,  std::string eventSender, std::string eventDetails);
	void AddEvent(std::string eventSender, std::string eventDetails);

	//  create a connection to a client
	//  returns the port that this program is listening on for TCP from the client
	//  if fail to create client, returns -1
	int CreateClientConnection(const struct sockaddr_in &clientAddress, int clientListeningOnPortNumber);

	//  disconnect a client connection
	void DisconnectClient(struct sockaddr_in &clientAddress);


	//  function to process the button push message from a connected client
	void HandleButtonPush(timeval eventTime, std::string eventSender, std::string eventDetails);

	//  function to process the broadcast message from a connected client
	void HandleBroadcastMessage(timeval eventTime, std::string eventSender, std::string message);

	//  flags for broadcast of messages to clients
	bool mForwardMessagesToAllClients;
	bool mForwardMessageWaitForClientResponse;

	//  send an event message to all clients
	void SendMessageToAllClients(std::list<LogEvent*>& eventsToSend);



	//  User Interface:  Display
	//
	//  set flag to update display
	void SetUpdateDisplay();
	//  set flag to suspend display updtes
	void SuspendDisplayUpdates();
	//  set flag to resume display updates
	void ResumeDisplayUpdates();


	//  User Interface:  Input

	//  command line input function handlers
	void BroadcastMessage(std::string input);
	bool SaveLogs(std::string input);
	void PrintLogs(FILE* stream);
	void ClearLogs();

	bool mListingLogs;
		
	//  function to return IP address of server
	std::string GetIpAddress();

protected:

	//  remember ifconfig properties
	CMDifconfig mCMDifconfig;
	std::string mHostname;

	//  connection server port, listening for connect / disconnect requests on this port
	int mConnectionServerPort;

	//  server connection thread
	ConnectionThread mConnectionThread;

	//  connected clients
	std::map<std::string, ConnectedClient*> mConnectedClients;

	//  mutex to lock access to connected clients map
	std::mutex mConnectedClientsMutex;

	BroadcastThread mBroadcastThread;


	//  the event log
	std::list<LogEvent*> mEventLog;
	std::mutex mEventLogMutex;
	int mMaxEventsToLog;
	bool mLogSysEvents;

	//  The display output
	//  this happens on its own thread
	DisplayThread mDisplayThread;
	friend class DisplayThread;
	std::mutex mDisplayUpdateMutex;
	
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

	std::string mVersionString;
};


#define SYSEVENT "systemEvent"


#endif // _THEAPP_H