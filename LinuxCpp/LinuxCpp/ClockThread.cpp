#include "ClockThread.h"
#include "TheApp.h"


ClockThread::ClockThread(TheApp& theApp) :
	mTheApp(theApp)
{
	mSleepDuration = 1000;

}

ClockThread::~ClockThread()
{
}




void ClockThread::RunFunction()
{
	mTheApp.DisplayUpdateClock();
	
}