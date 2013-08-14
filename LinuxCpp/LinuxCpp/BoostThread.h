#ifndef _BOOSTTHREAD_H
#define _BOOSTTHREAD_H

#include <boost/thread.hpp>
#include <boost/thread/thread.hpp>

//  BoostThread
//  a simple framework for using boost::thread

//  Boost thread sleep function
//  Returns true if sleep for entire duration without interrupt
bool BoostSleep(long milis);


class BoostThread
{
public:

	BoostThread();
	virtual ~BoostThread();

	//  Returns the state of the thread running.
	bool IsRunning() { return mThreadRunning; }

	//  Returns true when the Run() function has exited.
	bool IsStopped() { return mThreadStopped; }

	//  Set the stoped flag when exiting the Run() function.
	void SetIsStopped() { mThreadStopped = true; }
	
	//  Start running the thread.
	//  If the thread is already running, it will be shut down before restarting.
	virtual void Start();

	//  Cancel the running thread.
	virtual void Cancel();

	//  Set the desired interval in ms for your Run function, the minimum interval is 1 ms.
	//  Every thread will sleep for some time during each cycle of the Run() function to prevent race condition.
	void SetSleepDuration(long sleepMs);
	//
	//  returns true if sleep with no interrupt, otherwise false
	bool ThreadSleep();

	//  the Run function will call this each cycle
	virtual void RunFunction() = 0;

protected:
	
	//  pointer to boost::thread object
	boost::thread* mThread;

	//  running state flags
	bool mThreadRunning;
	bool mThreadStopped;

	//  sleep duration
	long mSleepDuration;

};

#endif  // _BOOSTTHREAD_H