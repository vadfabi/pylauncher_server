#ifndef _CONNECTIONTHREAD_H
#define _CONNECTIONTHREAD_H

#include "TCPServerThread.h"


//  pre declaration of TheApp object
class TheApp;

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