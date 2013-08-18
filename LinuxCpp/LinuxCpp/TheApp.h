#ifndef _THEAPP_H
#define _THEAPP_H

#include <string>
#include <list>
#include <mutex>

#include "Thread.h"
#include "ConnectionThread.h"
#include "ConnectedClientThread.h"
#include "ClockThread.h"
#include "CmdIfConfig.h"



//////////////
//  Display Output 
//  The display output runs on it own thread

///
class DisplayThread : public Thread
{
public:
	DisplayThread(TheApp& theApp);
	
	virtual void RunFunction();

protected:
	TheApp& mTheApp;
};


// TheApp
// This class is the main application
// it is running on the main thead
// application sub processes, such as socket connection are launched in their own threads

class TheApp
{
public:
	TheApp();
	virtual ~TheApp();

	bool InitializeInstance();

	void ShutDown();

	//  create a connection to a client
	//  returns the port that this program is listening on for TCP from the client
	//  if fail to create client, returns -1
	int CreateClientConnection(struct sockaddr_in &clientAddress, int clientListeningOnPortNumber);

	//  disconnect a client connection
	void DisconnectClient(struct sockaddr_in &clientAddress);

	//  add event to the event log
	void AddEvent(std::string event);

	//  update display with current program state
	void UpdateDisplay();
	void DisplayUpdateClock();

	void SuspendDisplayUpdates() { mDisplayUpdatesOn = false; }
	void ResumeDisplayUpdates() { mDisplayUpdatesOn = true; }

	void SetUpdateDisplay();


protected:

	std::string mVersionString;

	//  application properties
	CMDifconfig mCMDifconfig;

	int mConnectionServerPort;

	//  server connection thread
	ConnectionThread mConnectionThread;

	//  connected clients
	std::map<std::string, ConnectedClient*> mConnectedClients;

	std::mutex mConnectedClientsMutex;

	//  the event log
	std::list<std::string> mEventLog;
	std::mutex mEventLogMutex;

	//  The display output
	//  this happens on its own thread
	DisplayThread mDisplayThread;
	std::mutex mDisplayUpdateMutex;
	
	bool mDisplayUpdatesOn = true;
	bool mUpdateDisplay = false;

	timeval mTimeOfLastClockUpdate;

	void DisplayWriteHeader();
	void DisplayWriteClientConnections();
	void DisplayWriteLogs();
	

	//  the display output
	ClockThread mClockThread;

};

#endif // _THEAPP_H