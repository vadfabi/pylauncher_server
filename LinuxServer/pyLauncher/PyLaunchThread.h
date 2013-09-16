#ifndef _PYLAUNCHTHREAD_H
#define _PYLAUNCHTHREAD_H

#include <condition_variable>
#include <queue>

#include "../tcPIp_Sockets/Thread.h"
#include "../tcPIp_Sockets/UtilityFn.h"
#include "../tcPIp_Sockets/Parser.h"

class TheApp;




//
//  PyLaunch
//  class to hold request to launch python file
//
class PyLaunch
{
public:
	PyLaunch();
	PyLaunch(timeval eventTime, std::string eventAddress, std::string args);
	

	timeval mEventTime;
	std::string mEventAddress;
	std::string mFileName;
	std::string mArguments;

	timeval mStartLaunch;
	timeval mEndLaunch;
};


//  deletion helper function
inline static bool deletePyLaunch( PyLaunch* launchToDelete ) { delete launchToDelete; return true; } 




//
//  PyLaunchThread
//  a thread that waits for launch events to be added to the queue,
//  then executes file launch
//

class PyLaunchThread : public Thread
{
public:

	PyLaunchThread(TheApp& theApp);
	
	virtual ~PyLaunchThread();

	//  add a message to the broadcast queue, and signal thread to wake up
	void AddLaunchEvent(timeval eventTime, std::string eventSender, std::string args);

	//  override of Thread::Cancel so we can wake thread up before stopping run function
	virtual void Cancel();

	//  RunFunction
	virtual void RunFunction();

protected:

	//  message queue, protected with mutex
	std::mutex mQueueMutex;
	std::queue<PyLaunch*> mLaunchQueue;

	//  condition variable is used for notification of thread to wake up
	bool mNotified;
	std::condition_variable mNotifyEventCondition;
	std::mutex mNotifyMutex;

	
	//  reference to TheApp
	TheApp& mTheApp;
};





#endif //_PYLAUNCHTHREAD_H