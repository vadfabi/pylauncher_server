#include <sys/time.h>
#include <vector>

#include "BroadcastThread.h"
#include "TheApp.h"

using namespace std;



/////////////////////////////////////////////////////////////////////////////
//  BroadcastThread
//  runs a continuous thread waiting for messages to be put into queue
//  sends messages to clients when there is something in the queue
//


//  Constructor
//
BroadcastThread::BroadcastThread(TheApp& theApp) : mTheApp(theApp)
{
	mNotified = false;
}


//  Destructor
//
BroadcastThread::~BroadcastThread()
{
	if ( mThreadRunning )
	{
		Cancel();
	}

	while ( ! mMessageQueue.empty() )
	{
		delete mMessageQueue.front();
		mMessageQueue.pop();
	}
}


//  AddMessage
//  puts a message in the queue, and notifies the thread
//
void BroadcastThread::AddMessage(timeval eventTime, string eventSender, string message)
{
	LogEvent* newMessage = new LogEvent(eventTime, eventSender, message);

	{
		LockMutex lockMessageQueue(mMessageQueueMutex);
		mMessageQueue.push(newMessage);
	}

	Notify();
}


//  Cancel
//
void BroadcastThread::Cancel()
{
	mThreadRunning = false;
	
	Notify();

	Thread::Cancel();
}


//  Notify
//  sets the notification flag and notifies the condition variable
//
void BroadcastThread::Notify()
{
	mNotified = true;
	mNotifyMessagesCondition.notify_one();
}


//  RunFunction
//  the thread run function, will wait on condition
//  and then send messages to all clients when notified
//
void BroadcastThread::RunFunction()
{
	
	if ( mMessageQueue.size() == 0 && mThreadRunning )
	{
		//  wait for messages
		std::unique_lock<std::mutex> lockNotify(mNotifyMutex);

		//  avoid spurious wakeups
		while ( ! mNotified )
			mNotifyMessagesCondition.wait(lockNotify);

		//  check to see if we were woken up because of shutdown
		if ( ! mThreadRunning )
			return;
	}

	list<LogEvent*> eventsToSend;
	//  empty the queue and put the events to send into a list
	{
		LockMutex lockQueue(mMessageQueueMutex);
		LogEvent* nextMessage = NULL;
		while ( mMessageQueue.size() > 0 )
		{
			nextMessage = mMessageQueue.front();
			mMessageQueue.pop();
			eventsToSend.push_back(nextMessage);
		}
	}

	//  send these messages to clients
	mTheApp.SendMessageToAllClients(eventsToSend);

	//  clean up, this will also delete memory
	eventsToSend.remove_if(deleteLogEvent);
	
	mNotified = false;

	return;

}