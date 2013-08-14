#include "TheApp.h"
#include "Parser.h"
#include "ConnectedClientThread.h"


TheApp::TheApp() :
	mConnectionThread(*this), mClockThread(*this)
{
	mVersionString = "1.0.0.1";
	mConnectedClient = 0;

}



TheApp::~TheApp()
{
	ShutDown();

}



bool TheApp::InitializeInstance()
{
	//  open the server socket
	if ( mConnectionThread.OpenServerSocket(48888, true) != 48888 )
	{
#ifdef DEBUG
		printf("Failed to open server socket!");
#endif
		return false;
	}

	//  get and remember the ip info of the machine for display purposes
	mCMDifconfig.Execute();

	//  start the display
	mClockThread.Start();


	//  start the connection thread
	mConnectionThread.Start();

	return true;
}


void TheApp::ShutDown()
{
	if ( mConnectedClient )
	{
		mConnectedClient->Cancel();
		delete mConnectedClient;
		mConnectedClient = 0;
	}

	mConnectionThread.Cancel();

	mClockThread.Cancel();
}



int TheApp::CreateClientConnection(struct sockaddr_in *clientAddress, int clientListeningOnPortNumber)
{
	//if ( mConnectedClient )
	//{
	//	mConnectedClient->Cancel();
	//	delete mConnectedClient;
	//}

	//  create connection thread for the client
	mConnectedClient = new ConnectedClient(*this);

	int portNumber = mConnectedClient->OpenServerSocket(50000, false);

	if ( portNumber < 0 )
		return -1;

	mConnectedClient->mClientsAddress = *clientAddress;
	mConnectedClient->mClientsListeningPortNumber = clientListeningOnPortNumber;
	mConnectedClient->Start();

	return portNumber;
}