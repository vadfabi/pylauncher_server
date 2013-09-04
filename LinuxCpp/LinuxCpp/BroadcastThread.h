#ifndef _BROADCASTTHREAD_H
#define _BROADCASTTHREAD_H


#include <condition_variable>
#include <queue>

#include "Thread.h"


class TheApp;

class BroadcastMessage
{
public:
	BroadcastMessage(timeval eventTime, string eventSender, string message)
	{
		mEventTime = eventTime;
		mEventSender = eventSender;
		mMessage = message;
	}

	timeval mEventTime;
	string mEventSender;
	string mMessage;
};


class BroadcastThread : public Thread
{
public:

	BroadcastThread(TheApp& theApp);
	
	virtual ~BroadcastThread();

	void AddMessage(timeval eventTime, string eventSender, string message);

	virtual void Cancel();

	virtual void RunFunction();

	

protected:

	TheApp& mTheApp;

	std::mutex mMessageQueueMutex;
	std::queue<BroadcastMessage*> mMessageQueue;

	bool mNotified;
	std::condition_variable mNotifyMessagesCondition;
	std::mutex mNotifyMutex;


	
};





#endif //_BROADCASTTHREAD_H