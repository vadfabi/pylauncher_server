#ifndef _CONNECTEDCLIENTTHREAD_H
#define _CONNECTEDCLIENTTHREAD_H

#include <netinet/in.h>		//  for struct sockaddr_in

#include "TCPServerThread.h"



class TheApp;

class ConnectedClient : public TCPServerThread
{
public:

	ConnectedClient(TheApp& theApp, std::string ipAddressOfClient);
	virtual ~ConnectedClient();

	std::string GetIpAddressOfClient() { return mIpAddressOfClient; }

	//  where is the client
	struct sockaddr_in mClientsAddress;
	int mClientsListeningPortNumber;
	
	virtual void RunFunction();

	virtual void Cancel();

	void ShutDown();

protected:
	
	
	std::string mIpAddressOfClient;

	//  reference to the app
	TheApp& mTheApp;
};





#endif  //  _CONNECTEDCLIENTTHREAD_H