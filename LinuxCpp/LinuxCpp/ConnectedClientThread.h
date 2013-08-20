#ifndef _CONNECTEDCLIENTTHREAD_H
#define _CONNECTEDCLIENTTHREAD_H

#include <netinet/in.h>		//  for struct sockaddr_in

#include "TCPServerThread.h"

using namespace std;

//  pre declaration of TheApp object
class TheApp;

//  ConnectedClient
//  manages the socket connection and interactions with a single client
//
class ConnectedClient : public TCPServerThread
{
public:

	ConnectedClient(TheApp& theApp, const struct sockaddr_in &clientAddress, int clientsListeningOnPortNumber);
	
	virtual ~ConnectedClient();

	//  get the dots and numbers format address of the client, this is used as the key in the client map
	string GetIpAddressOfClient() { return mIpAddressOfClient; }

	//  get the port number that the client is listening for our messages on
	int GetPortNumberClientIsListeningOn() { return mPortNumberClientIsListeningOn; }
	
	//  Server thread for this client will be running in our RunFunction
	virtual void RunFunction();

	
protected:
		
	string mIpAddressOfClient;

	//  where is the client
	struct sockaddr_in mClientsAddress;

	//  the port is this client listening on for messages from us
	int mPortNumberClientIsListeningOn;

	//  reference to the app
	TheApp& mTheApp;
};





#endif  //  _CONNECTEDCLIENTTHREAD_H