#include <sys/time.h>
#include <vector>

#include "PyLaunchThread.h"
#include "TheApp.h"

using namespace std;



	PyLaunch::PyLaunch(timeval eventTime, std::string eventAddress, std::string args)
	{ 
		mEventTime = eventTime;
		mEventAddress = eventAddress;

		Parser argParser(args,",");
		mFileName = argParser.GetNextString();
		mArguments = argParser.GetNextString();
	}

/////////////////////////////////////////////////////////////////////////////
//  PyLaunchThread
//  runs a continuous thread waiting for messages to be put into queue
//  sends messages to clients when there is something in the queue
//


//  Constructor
//
PyLaunchThread::PyLaunchThread(TheApp& theApp) : mTheApp(theApp)
{
	mNotified = false;
}


//  Destructor
//
PyLaunchThread::~PyLaunchThread()
{
	if ( mThreadRunning )
	{
		Cancel();
	}

	while ( ! mLaunchQueue.empty() )
	{
		delete mLaunchQueue.front();
		mLaunchQueue.pop();
	}
}

void PyLaunchThread::AddLaunchEvent(timeval eventTime, string eventSender, string args)
{
	PyLaunch* newEvent = new PyLaunch(eventTime, eventSender, args);

	{
		LockMutex lockMessageQueue(mQueueMutex);
		mLaunchQueue.push(newEvent);
	}

	mNotified = true;
	mNotifyEventCondition.notify_one();
}


void PyLaunchThread::Cancel()
{
	mThreadRunning = false;

	mNotifyEventCondition.notify_one();

	Thread::Cancel();
}


void PyLaunchThread::RunFunction()
{
	
	if ( mLaunchQueue.size() == 0 && mThreadRunning )
	{
		//  wait for messages
		std::unique_lock<std::mutex> lockNotify(mNotifyMutex);

		//  avoid spurious wakeups
		if ( ! mNotified )
			mNotifyEventCondition.wait(lockNotify);

		//  check to see if we were woken up because of shutdown
		if ( ! mThreadRunning )
			return;
	}


	while ( ! mLaunchQueue.empty() )
	{
		PyLaunch* nextEvent = 0;

		{
			LockMutex lockQueue(mQueueMutex);
			nextEvent = mLaunchQueue.front();
			mLaunchQueue.pop();
		}

		gettimeofday(&nextEvent->mStartLaunch, 0);
		string launchCommand = "python " + nextEvent->mFileName;
		if ( nextEvent->mArguments.size() > 0 )
			launchCommand += (" " + nextEvent->mArguments);

		launchCommand += " 2>&1";

		CMD command(launchCommand);
		command.Execute();
		gettimeofday(&nextEvent->mEndLaunch, 0);

		//  build a message to send back to client
		string launchResult = format("$TCP_PYRESULT,%s,%s,%s,%s,%s,", nextEvent->mFileName.c_str(),
			nextEvent->mEventAddress.c_str(), FormatTime(nextEvent->mEventTime).c_str(), FormatTime(nextEvent->mStartLaunch).c_str(), FormatTime(nextEvent->mEndLaunch).c_str());

		//  format the results into comma delimited string
		string results = "";
		for( int i = 0; i < command.GetCommandResponseSize(); i++ )
		{
			results += command.GetCommandResponseLine(i);

			if ( i != command.GetCommandResponseSize() -1 )
				results += ",";
		}

		launchResult += results;

		mTheApp.BroadcastMessage( nextEvent->mEndLaunch,  mTheApp.GetIpAddress(), launchResult);


		delete nextEvent;
	}
	
	mNotified = false;

	return;

}