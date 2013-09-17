#ifndef _THEAPP_H
#define _THEAPP_H

#include <string>
#include <list>
#include <mutex>
#include <map>

#include "../tcPIp_Sockets/Thread.h"
#include "../tcPIp_Sockets/ConnectionThread.h"
#include "../tcPIp_Sockets/BroadcastThread.h"
#include "../tcPIp_Sockets/CMDifconfig.h"
#include "../tcPIp_Sockets/UtilityFn.h"
#include "../tcPIp_Sockets/TerminalDisplay.h"


#include "ConnectedClientThread.h"
#include "PyLaunchThread.h"






#define DIRLISTFILE "directoryList.txt"

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

	std::string BuildDirList();
	std::string BuildFileList();

	//  add a directory to the collection, save the directoryList.txt file when done
	bool HandleAddDirectory(timeval eventTime, std::string eventSender, std::string dirName);

	//  remove directory from the collection, save the directoryList.txt file when done
	bool HandleRemoveDirectory(timeval eventTime, std::string eventSender, std::string dirName);
	 
	//  launch python file, args is "filename,arguments"
	void HandlePythonLaunch(timeval eventTime, std::string eventSender, std::string args);


	//  flags for broadcast of messages to clients
	bool mForwardMessagesToAllClients;
	bool mForwardMessageWaitForClientResponse;

	//  send an event message to all clients
	void SendMessageToAllClients(std::list<LogEvent*>& eventsToSend);

	void BroadcastMessage(timeval eventTime, std::string eventSender, std::string message);



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
	void RefreshFiles();
	bool SaveLogs(std::string input);
	void PrintLogs(FILE* stream);
	void ClearLogs();
	void ShowConnectionStatus();

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

	//  Directory and Files List
	//
	std::list<std::string> mDirectoryList;
	std::list<std::string> mFilesList;
	std::mutex mFilesListMutex;

	void LoadPythonFileDirectoryFile();
	void LoadPythonFilesList();

	void LiveUpdatePythonFiles();
	
	


	BroadcastThread mBroadcastThread;

	PyLaunchThread mPyLaunchThread;


	//  the event log
	std::list<LogEvent*> mEventLog;
	std::mutex mEventLogMutex;
	int mMaxEventsToLog;
	bool mLogSysEvents;

	//  The display output
	TerminalDisplay mTerminalDisplay;
	//
	std::mutex mDisplayUpdateMutex;
	//
	bool mDisplayUpdatesOn;
	bool mUpdateDisplay;
	//
	timeval mTimeOfLastClockUpdate;
	
	//  update display function, 
	void DisplayUpdate();
	void DisplayWriteHeader();
	void DisplayWriteConnectionStatus();
	void DisplayWriteEvent(LogEvent event);
	void DisplayWriteTime();
	void DisplayUpdateClock();

	std::string mVersionString;
};


#define SYSEVENT "systemEvent"


#endif // _THEAPP_H