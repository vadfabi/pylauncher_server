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

	// set the send and receive timeouts (in seconds)
	// default send time out is 3 seconds
	// default receive timeout is 3 seconds
	bool SetClientSocketTimeouts(long sendTimeout, long receiveTimeout);

	//  get the dots and numbers format address of the client, this is used as the key in the client map
	string GetIpAddressOfClient() { return mIpAddressOfClient; }

	//  get the port number that the client is listening for our messages on
	int GetPortNumberClientIsListeningOn() { return mPortNumberClientIsListeningOn; }

	string SendMessageToClient(string message, bool waitForResponse);
	
	//  override cancel so we can shut down our client socket
	virtual void Cancel();

	//  Server thread for this client will be running in our RunFunction
	virtual void RunFunction();

	
protected:
		
	string mIpAddressOfClient;

	

	//  socket read and write timeouts (in seconds)
	long mClientReceiveTimeout;
	long mClientSendTimeout;

	//  where is the client
	struct sockaddr_in mClientsAddress;

	//  the port is this client listening on for messages from us
	int mPortNumberClientIsListeningOn;

	//  reference to the app
	TheApp& mTheApp;
};





#endif  //  _CONNECTEDCLIENTTHREAD_H