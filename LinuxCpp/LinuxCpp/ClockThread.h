#ifndef _CLOCKTHREAD_H
#define _CLOCKTHREAD_H

#include <thread>
#include <mutex>
#include "BoostThread.h"


//  ClockThread
//  This class is the 'ui thread'
//  Currently, this class has a simple implementation  where it updates the display output on a 1hz timer.  
//  TODO: A better design would be to have the UI thread respond to events, and not wait for the next timer tick to update dispaly.


class TheApp;

class ClockThread : public BoostThread
{
public:

	ClockThread(TheApp& theApp);
	virtual ~ClockThread();

	
	void SetUpdateMonitorDisplay();
	
	virtual void RunFunction();
	

protected:

	TheApp& mTheApp;
	
	std::mutex mUpdateDisplayMutex;
	
	void WriteHeader();


	bool mUpdateMonitorDisplay;
};



#endif // _CLOCKTHREAD_H