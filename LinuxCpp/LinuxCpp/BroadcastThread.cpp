#include "BroadcastThread.h"
#include "TheApp.h"

BroadcastThread::BroadcastThread(TheApp& theApp) : mTheApp(theApp)
{
	mNotified = false;
}


BroadcastThread::~BroadcastThread()
{
}

void BroadcastThread::AddMessage(timeval eventTime, string eventSender, string message)
{
	BroadcastMessage* newMessage = new BroadcastMessage(eventTime, eventSender, message);

	{
		LockMutex lockMessageQueue(mMessageQueueMutex);
		mMessageQueue.push(newMessage);
	}

	mNotified = true;
	mNotifyMessagesCondition.notify_one();
}


void BroadcastThread::Cancel()
{
	mThreadRunning = false;

	mNotifyMessagesCondition.notify_one();

	Thread::Cancel();
}


void BroadcastThread::RunFunction()
{
	if ( mMessageQueue.size() == 0 && mThreadRunning )
	{
		//  wait for messages
		std::unique_lock<std::mutex> lockNotify(mNotifyMutex);

		//  avoid spurious wakeups
		if ( ! mNotified )
			mNotifyMessagesCondition.wait(lockNotify);

		//  check to see if we were woken up because of shutdown
		if ( ! mThreadRunning )
			return;
	}



	//  send all messages to clients
	BroadcastMessage* nextMessage = NULL;
	while ( mMessageQueue.size() > 0 )
	{
		{
			LockMutex lockQueue(mMessageQueueMutex);
			nextMessage = mMessageQueue.front();
			mMessageQueue.pop();
		}


		mTheApp.SendMessageToAllClients(nextMessage->mEventTime, nextMessage->mEventSender, nextMessage->mMessage);
		delete nextMessage;
	}


	mNotified = false;

	return;

}