#include "DisplayThread.h"
#include "TheApp.h"


using namespace std;




//  The display output runs on it own thread, this class deffinition is here
//  
DisplayThread::DisplayThread(TheApp& theApp) : mTheApp(theApp)
{
	mNotified = false;
}



//  RunFunction
//
void DisplayThread::RunFunction()
{
	if ( mEventQueue.size() == 0 && mThreadRunning )
	{
		//  wait for messages
		unique_lock<mutex> lockNotify(mNotifyMutex);

		//  avoid spurious wakeups
		while ( ! mNotified )
			mNotifyEventCondition.wait(lockNotify);

		//  check to see if we were woken up because of shutdown
		if ( ! mThreadRunning )
			return;
	}

	if ( ! mTheApp.mDisplayUpdatesOn )
	{
		mNotified = false;
		return;
	}

	//  write header if we need to
	if ( mUpdateHeader )
	{
		mTheApp.DisplayWriteHeader();
		mUpdateHeader = false;
	}

	//  write connections if we need to
	if ( mUpdateConnections )
	{
		mTheApp.DisplayWriteConnectionStatus();
		mUpdateConnections = false;
	}

	while ( mEventQueue.size() > 0 )
	{
		LogEvent* nextEvent = NULL;
		{
			LockMutex lockQueue(mEventQueueMutex);
			nextEvent = mEventQueue.front();
				mEventQueue.pop();
		}
		
		mTheApp.DisplayWriteEvent(*nextEvent);
	}

	mNotified = false;
}



//  Cancel
//
void DisplayThread::Cancel()
{
	mThreadRunning = false;

	Notify();

	Thread::Cancel();
}



//  Notify
//  sets the notification flag and notifies the condition variable
//
void DisplayThread::Notify()
{
	mNotified = true;
	mNotifyEventCondition.notify_one();
}


//  UpdateEverything
//
void DisplayThread::UpdateEverything()
{
	mUpdateHeader = true;
	mUpdateConnections = true;

	Notify();
}


//  UpdateConnections
//
void DisplayThread::UpdateConnections()
{
	mUpdateConnections = true;

	Notify();
}



//  AddEvent
//
void DisplayThread::AddEvent(LogEvent* logEvent)
{
	{
		LockMutex lockMessageQueue(mEventQueueMutex);
		mEventQueue.push(logEvent);
	}

	Notify();
}