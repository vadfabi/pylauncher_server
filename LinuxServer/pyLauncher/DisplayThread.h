#ifndef _DISPLAYTHREAD_H
#define _DISPLAYTHREAD_H


#include <queue>
#include <condition_variable>


#include "../tcPIp_Sockets/BroadcastThread.h"




class TheApp;

//  DisplayThread
//  Display output runs on its own thread
//
class DisplayThread : public Thread
{
public:
	// this class takes a reference to the TheApp class
	// this is a quick and dirty (and easy) way for classes to communicate without using singletons or global variables
	DisplayThread(TheApp& theApp);
	
	//  thread run function, will wait for condition to update display
	virtual void RunFunction();

	//  override thread cancel so we can send notify to condition before stopping thread
	virtual void Cancel();

	//  tell the display to do full refresh
	void UpdateEverything();

	//  tell the display to update connection info
	void UpdateConnections();

	//  add a log event to be displayed
	void AddEvent(LogEvent* logEvent);

protected:

	//  update flags
	bool mUpdateHeader;
	bool mUpdateConnections;
	
	//  update queue
	std::mutex mEventQueueMutex;
	std::queue<LogEvent*> mEventQueue;

	//  reference to TheApp object so we can call its functions
	//  this class is a friend class of the app, so we can call protected functions
	TheApp& mTheApp;

	//  Thread wake up condition
	std::condition_variable mNotifyEventCondition;
	std::mutex mNotifyMutex;

	void Notify();

	//  notify flag, this is required to avoid spurious wakeups
	//  see http://en.wikipedia.org/wiki/Spurious_wakeup and http://www.codeproject.com/Articles/598695/Cplusplus11-threads-locks-and-condition-variables
	bool mNotified;
};


#endif // _DISPLAYTHREAD_H