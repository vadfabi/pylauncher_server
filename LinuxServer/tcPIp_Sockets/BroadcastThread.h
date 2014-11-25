#ifndef _BROADCASTTHREAD_H
#define _BROADCASTTHREAD_H

#include <condition_variable>
#include <queue>

#include "Thread.h"
#include "UtilityFn.h"

class TheApp;




//
//  LogEvent
//  class to hold event info that we are logging
//
class LogEvent
{
public:
	LogEvent();
	LogEvent(timeval eventTime, std::string eventAddress, std::string eventDescription)
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
	std::string mEventAddress;
	std::string mEvent;
};


//  deletion helper function
inline static bool deleteLogEvent( LogEvent* eventToDelete ) { delete eventToDelete; return true; } 




//
//  BroadcastThread
//  a thread that waits for messages to be added to the queue,
//  then broadcasts messages to all clients
//

class BroadcastThread : public Thread
{
public:

	BroadcastThread(TheApp& theApp);
	
	virtual ~BroadcastThread();

	//  add a message to the broadcast queue, and signal thread to wake up
	void AddMessage(timeval eventTime, std::string eventSender, std::string message);

	//  override of Thread::Cancel so we can wake thread up before stopping run function
	virtual void Cancel();

	//  RunFunction
	virtual void RunFunction();

protected:

	//  message queue, protected with mutex
	std::mutex mMessageQueueMutex;
	std::queue<LogEvent*> mMessageQueue;


	//  condition variable is used for notification of thread to wake up
	bool mNotified;
	std::condition_variable mNotifyMessagesCondition;
	std::mutex mNotifyMutex;

	void Notify();

	//  reference to TheApp
	TheApp& mTheApp;
};





#endif //_BROADCASTTHREAD_H