#ifndef _THREAD_H
#define _THREAD_H

#include <thread>

using namespace std;


//  Thread
//  a simple framework for using std::thread 
//  to have a thread, derive your function from Thread
//  and then define the RunFunction()
//  if you want to do specific shutdown when the thread is canceled, also override Cancel()
//

//  sleep in the current thread for millis milliseconds
void Sleep(long millis);

class Thread
{
public:

	Thread();
	virtual ~Thread();

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
	//  it is safe to call this function even if the thread is not running
	virtual void Cancel();

	//  The RunFunction
	//  override this funciton in your derived class, and put the actions in here that this thread should do on each pass
	virtual void RunFunction() = 0;

protected:
	
	//  pointer to the std::thread object
	std::thread* mThread;

	//  running flags
	bool mThreadRunning;
	bool mThreadStopped;
};

#endif