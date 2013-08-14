#include "ClockThread.h"
#include "TheApp.h"


ClockThread::ClockThread(TheApp& theApp) :
	mTheApp(theApp)
{
	mSleepDuration = 1000;

	mUpdateMonitorDisplay = true;
}

ClockThread::~ClockThread()
{
}



void ClockThread::WriteHeader()
{
	system("clear");

	printf("/*****************************************************************************\n");
	printf("/******  Simple Linux Connect TCP/IP Program \n");
	printf("/******  %s:\n", mTheApp.mVersionString.c_str());
	printf("/******\n");
	printf("/******      Connected on eth0: %s\n",mTheApp.mCMDifconfig.mEth0Info.mInet4Address.size() == 0 ? "not enabled " : mTheApp.mCMDifconfig.mEth0Info.mInet4Address.c_str());
	printf("/******      Connected on wlan: %s\n",mTheApp.mCMDifconfig.mWlanInfo.mInet4Address.size() == 0 ? "not enabled " : mTheApp.mCMDifconfig.mWlanInfo.mInet4Address.c_str());
	printf("/******\n");	




}



void ClockThread::SetUpdateMonitorDisplay()
{
	mUpdateDisplayMutex.lock();

	mUpdateMonitorDisplay = true;

	mUpdateDisplayMutex.unlock();
}


void ClockThread::RunFunction()
{
	mUpdateDisplayMutex.lock();

	if ( mUpdateMonitorDisplay )
	{
		WriteHeader();

			//  put the date as last line of display
		CMD date("date");
		date.Execute();
		printf("/***     Time:      %s\n", date.GetCommandResponseLine(0).c_str());

		mUpdateMonitorDisplay = false;
	}
	else
	{
		fputs("\033[A\033[2K",stdout);
		rewind(stdout);
		//  had to get rid of this next call, would not compile on openSuse
		//  ftruncate(1,0); /* you probably want this as well */
		
		//  put the date as last line of display
		CMD date("date");
		date.Execute();
		printf("/***     Time:      %s\n", date.GetCommandResponseLine(0).c_str());
	}

	mUpdateDisplayMutex.unlock();
}