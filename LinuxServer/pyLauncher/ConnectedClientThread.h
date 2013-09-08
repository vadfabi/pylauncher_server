#ifndef _CONNECTEDCLIENTTHREAD_H
#define _CONNECTEDCLIENTTHREAD_H

#include <netinet/in.h>		

#include "../tcPIp_Sockets/TCPServerThread.h"

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
	std::string GetIpAddressOfClient() { return mIpAddressOfClient; }

	//  get the port number that the client is listening for our messages on
	int GetPortNumberClientIsListeningOn() { return mPortNumberClientIsListeningOn; }

	//  send a message to the listening client, set waitForResponse = true to get client response
	//  returns response (or empty string if not waiting)
	std::string SendMessageToClient(std::string message, bool waitForResponse);
	
	//  handling for missing clients, we will allow 3 failed attempts to before marking client as inactive
	void IncrementClientConnectionFailureCounter() { mNumberOfClientConnectionFailuers++; }
	
	//  is client still active 
	bool IsActiveClient() { return mNumberOfClientConnectionFailuers < 3; }

	//  override cancel so we can shut down our client socket
	virtual void Cancel();

	//  Server thread for this client will be running in our RunFunction
	virtual void RunFunction();

	
protected:
		
	std::string mIpAddressOfClient;

	//  client socket for sending messages to the client
	int mClientSocketFileDescriptor;
	struct sockaddr_in mClientsListeningServerAddress;

	bool OpenClientSocket();
	bool CloseClientSocket();

	//  socket read and write timeouts (in seconds)
	long mClientReceiveTimeout;
	long mClientSendTimeout;

	//  where is the client
	struct sockaddr_in mClientsAddress;

	//  the port is this client listening on for messages from us
	int mPortNumberClientIsListeningOn;

	//  counter for number of TCP failures, used to detect if maybe client is gone
	int mNumberOfClientConnectionFailuers;


	//  reference to the app
	TheApp& mTheApp;
};





#endif  //  _CONNECTEDCLIENTTHREAD_H