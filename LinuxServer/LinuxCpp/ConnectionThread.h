#ifndef _CONNECTIONTHREAD_H
#define _CONNECTIONTHREAD_H

#include "TCPServerThread.h"
#include "UtilityFn.h"

class TheApp;




//  ConnectionThread
//  manages the main server connection thread, listens for connection and disconnection requests from clients
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