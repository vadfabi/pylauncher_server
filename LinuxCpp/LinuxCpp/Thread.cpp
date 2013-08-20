
#ifdef DEBUG
#include <cstdio>		//  for printf
#endif


#include "Thread.h"


//  Sleep
//  sleep in milliseconds
//
void Sleep(long millis)
{
	std::this_thread::sleep_for(std::chrono::milliseconds(millis));
}




//  Constructor
//  
Thread::Thread() 
{
	mThread = 0;
	
	mThreadStopped = false;
	mThreadRunning = false;
}


//  Destructor
//  
Thread::~Thread()
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
//  note, if you have a process that never quits, you should add a sleep in your RunFunction to prevent race condition
//
void Run(Thread& thread)
{
	while ( thread.IsRunning() )
	{
		thread.RunFunction();

		//  TODO: this won't compile, there is some question if it is necessary ?
		//  yeild once per cycle to avoid race condition
		//std::this_thread::yeild();
	}

	//  we are out of the run loop, set is stopped flag
	thread.SetIsStopped();
	
	return;
}


//  Start
//  
void Thread::Start()
{
	//  stop thread if it is running
	if ( mThread != 0 )
	{
		//  ASSERT - this is bad, you should stop running thread before starting new one
		Cancel();
	}

	//  initialize thread run variables
	mThreadRunning = true;
	mThreadStopped = false;

	//  start the thread
	mThread = new std::thread(Run,std::ref(*this));

	return;
}



//  Cancel
//  Stop the thread
//  this function will wait until the thread has exited before returning
//
void Thread::Cancel()
{
	mThreadRunning = false;

	if ( mThread )
	{
		//  a proper cancel thread function should have an interrupt here, to kill the thread if it is sleeping
		//  unfortunately, std::thread can not be interrupted (I believe?)
		//  use the BoostThread class if you want an interruptable thread

		//  wait for the thread to stop
		while ( ! mThreadStopped )
		{
			Sleep(50);
#ifdef DEBUG
			printf("Thread::Cancel - Waiting for thread to stop... \n");
#endif
		}

		//  join to the thread to ensure that thread shuts down before exit
		mThread->join();

		//  clean up our mess
		delete mThread;
		mThread = 0;
	}

	return;
}
