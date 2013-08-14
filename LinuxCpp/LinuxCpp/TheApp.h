#ifndef _THEAPP_H
#define _THEAPP_H

#include "ConnectionThread.h"
#include "ConnectedClientThread.h"
#include "ClockThread.h"
#include "CmdIfConfig.h"
#include <string>

// TheApp
// This class is the main application

class TheApp
{
public:
	TheApp();
	virtual ~TheApp();

	bool InitializeInstance();

	void ShutDown();

	//  create a connection to a client
	//  returns the port that this program is listening on for TCP from the client
	//  if fail to create client, returns -1
	int CreateClientConnection(struct sockaddr_in *clientAddress, int clientListeningOnPortNumber);

protected:

	std::string mVersionString;

	//  application properties
	CMDifconfig mCMDifconfig;

	//  server connection thread
	ConnectionThread mConnectionThread;

	//  connected clients
	ConnectedClient *mConnectedClient;
	
	//  the display output
	ClockThread mClockThread;
	friend class ClockThread;

};

#endif // _THEAPP_H