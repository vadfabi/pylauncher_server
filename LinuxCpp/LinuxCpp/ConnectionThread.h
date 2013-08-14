#ifndef _CONNECTIONTHREAD_H
#define _CONNECTIONTHREAD_H

#include "TCPServerThread.h"

class TheApp;

class ConnectionThread : public TCPServerThread
{
public:

	ConnectionThread(TheApp& theApp);
	virtual ~ConnectionThread();

	virtual void RunFunction();

protected:

	TheApp& mTheApp;
};


#endif