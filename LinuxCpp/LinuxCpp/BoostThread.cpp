#include "BoostThread.h"


//  BoostSleep
//  Sleep function with interrupt
bool BoostSleep(long milis)
{
	try
	{
		boost::this_thread::sleep(boost::posix_time::milliseconds(milis));
	}
	catch(boost::thread_interrupted const& e)
	{
#ifdef DEBUG
		printf("BoostSleep - thread interrupt\n");
#endif
		return false;
	}

	return true;
}


//  BoostThread
//  constructor
BoostThread::BoostThread() 
{
	mThread = 0;
	
	mThreadStopped = false;
	mThreadRunning = false;

	mSleepDuration = 1;		//  set minimum pause
}


//  ~BoostThread
//  destructor
BoostThread::~BoostThread()
{
	//  stop running thread before destruction
	if ( mThreadRunning )
	{
		//  ASSERT - this is bad, you should stop before destruction
		Cancel();
	}

	//  clean up the memory
	if ( mThread != 0 )
		delete mThread;
}



//  Run
//  This is the thread run function
//  it takes a reference to the Thread class, and calls the Thread::RunFunction() for each cycle
//  then it calls sleep for every cycle (to prevent race condition)
void Run(BoostThread* thread)
{
	while ( thread->IsRunning() )
	{
		thread->RunFunction();

		if ( ! thread->ThreadSleep() )
		{
			//  we were interrupted - bail out
			break;
		}
	}

	thread->SetIsStopped();

	return;
}



//  Start
//  Start the thread running
void BoostThread::Start()
{
	//  stop thread if it is running
	if ( mThread != 0 )
	{
		//  ASSERT - this is bad, you should stop running thread before starting new one
		Cancel();
	}

	mThreadRunning = true;
	mThreadStopped = false;

	mThread = new boost::thread(Run,this);

	return;
}



//  Cancel
//  Stop the thread
//  this function will wait until the thread has exited before returning
void BoostThread::Cancel()
{
	mThreadRunning = false;

	if ( mThread != 0 )
	{
		//  call interrupt to cancel the thread while it is sleeping
		mThread->interrupt();

		//  wait for the thread to stop
		while ( ! mThreadStopped )
		{
			BoostSleep(10);
			printf("BoostThread - Waiting for thread to stop... \n");
		}

		//  join to the thread to ensure that thread shuts down before exit
		mThread->join();

		//  clean up our mess
		delete mThread;
		mThread = 0;
	}

	return;
}



//  SetSleepDuration
//  set the sleep duration (minimum is 1 millisecond)
void BoostThread::SetSleepDuration(long sleepMs)
{
	//  one milisecond sleep is minimum to prevent race condition
	if ( sleepMs < 1 )
		mSleepDuration = 1;
	else
		mSleepDuration = sleepMs;
}


//  ThreadSleep
//  sleep for the set duration
bool BoostThread::ThreadSleep()
{
	return BoostSleep(mSleepDuration);
}
