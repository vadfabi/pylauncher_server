#ifndef _CONNECTIONTHREAD_H
#define _CONNECTIONTHREAD_H

#include "TCPServerThread.h"
#include "UtilityFn.h"


//  pre declaration of TheApp object
class TheApp;



/////////////////
//  LogEvent
//  class to hold log events
//
class LogEvent
{
public:
	LogEvent();
	LogEvent(timeval eventTime, string eventAddress, string eventDescription)
	{ 
		mEventTime = eventTime;
		mEventAddress = eventAddress;
		mEvent = eventDescription;
	}

	void PrintLog( FILE* stream )
	{
		fprintf( stream, "%s,%s,%s\n", FormatTime(mEventTime).c_str(), mEventAddress.c_str(), mEvent.c_str() );
	}

	timeval mEventTime;
	string mEventAddress;
	string mEvent;
};

//  deletion helper function
inline static bool deleteLogEvent( LogEvent* eventToDelete ) { delete eventToDelete; return true; } 




//  ConnectionThread
//  manages the main server connection thread, listens for connection requests from clients
//

class ConnectionThread : public TCPServerThread
{
public:

	ConnectionThread(TheApp& theApp);
	virtual ~ConnectionThread();

	//  TCP server is running in the RunFunction
	virtual void RunFunction();

protected:

	TheApp& mTheApp;
};


#endif