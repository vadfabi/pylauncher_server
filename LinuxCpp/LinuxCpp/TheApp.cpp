#include <arpa/inet.h>
#include <future>
#include <string>

#include "TheApp.h"
#include "Parser.h"
#include "ConnectedClientThread.h"
#include "UtilityFn.h"



////////



TheApp::TheApp() :
	mConnectionThread(*this), mClockThread(*this), mDisplayThread(*this)
{
	mVersionString = "1.0.0.1";


}



TheApp::~TheApp()
{

	ShutDown();

}



bool TheApp::InitializeInstance()
{
	//  open the server socket
	mConnectionServerPort = mConnectionThread.OpenServerSocket(48888, false);
	if ( mConnectionServerPort < 0 )
	{
		printf("Failed to open server socket!");
		return false;
	}

	//  get and remember the ip info of the machine for display purposes
	mCMDifconfig.Execute();

	//  start the display
	mClockThread.Start();


	//  start the connection thread
	mConnectionThread.Start();

	mDisplayThread.Start();

	mUpdateDisplay = true;

	return true;
}


void TheApp::ShutDown()
{
	//  disconnect all of our clients
	std::map<std::string, ConnectedClient*>::iterator iter;

	for ( iter = mConnectedClients.begin(); iter != mConnectedClients.end(); iter++ )
	{
		iter->second->ShutDown();
		delete iter->second;
	}

	mConnectedClients.erase(mConnectedClients.begin(), mConnectedClients.end());

	//  cancel the connection thread
	mConnectionThread.Cancel();

	mDisplayThread.Cancel();

	//  cancel the clock thread
	mClockThread.Cancel();
}


std::string IpAddressString(struct sockaddr_in &address)
{
	//  create string from 
	char* addressBuffer = inet_ntoa(address.sin_addr);
	return std::string(addressBuffer);
}


int TheApp::CreateClientConnection(struct sockaddr_in &clientAddress, int clientListeningOnPortNumber)
{
	//  Thread safety on access to map of clients
	mConnectedClientsMutex.lock();

	//  get the dots and numbers string for client, this is used as the key in our clients map
	std::string  addressOfSender = IpAddressString(clientAddress);

	//  see if this client exists already
	std::map<std::string, ConnectedClient*>::iterator it = mConnectedClients.find(addressOfSender);
	if ( it != mConnectedClients.end() )
	{
		//  shut down old connection and delete this client
		it->second->ShutDown();
		mConnectedClients.erase(addressOfSender);
		delete it->second;
	}

	//  create connection thread for the client
	ConnectedClient* newClient = new ConnectedClient(*this, addressOfSender);
	printf("Created Client: %0x\n", (unsigned int)newClient);

	int portNumber = newClient->OpenServerSocket(50000, false);

	if ( portNumber < 0 )
	{
		delete newClient;
		mConnectedClientsMutex.unlock();
		return -1;
	}

	newClient->mClientsAddress = clientAddress;
	newClient->mClientsListeningPortNumber = clientListeningOnPortNumber;
	newClient->Start();

	mConnectedClients[addressOfSender] =  newClient;

	mConnectedClientsMutex.unlock();


	return portNumber;
}




void TheApp::DisconnectClient(struct sockaddr_in &clientAddress)
{
	mConnectedClientsMutex.lock();

	std::string clientKey = IpAddressString(clientAddress);

	ConnectedClient *existingClient = 0;

	try{
		//  see if existing client exists
		existingClient = mConnectedClients.at(clientKey);
		//  documentation says that if item is not in list, that at() will throw exception, 
	}
	catch(out_of_range const&e)
	{
		//  this key does not exist, not expected here
		DEBUG_TRACE("TheApp::DisconnectClient - client does not exist !!!\n");
		return;
	}

	existingClient->Cancel();

	mConnectedClients.erase(clientKey);

	delete existingClient;


	mConnectedClientsMutex.unlock();

	SetUpdateDisplay();
}


void TheApp::AddEvent(std::string eventString)
{
	mEventLogMutex.lock();

	mEventLog.push_front(eventString);

	mEventLogMutex.unlock();

	mDisplayUpdateMutex.lock();
	mUpdateDisplay = true;
	mDisplayUpdateMutex.unlock();
}



//////////////
//  Display Output 
//  The display output runs on it own thread
DisplayThread::DisplayThread(TheApp& theApp) : mTheApp(theApp)
{
}

void DisplayThread::RunFunction()
{
	Sleep(10);
	mTheApp.UpdateDisplay();
}



void TheApp::SetUpdateDisplay()
{
	mDisplayUpdateMutex.lock();
	mUpdateDisplay = true;
	mDisplayUpdateMutex.unlock();
}

void TheApp::UpdateDisplay()
{
	if ( ! mDisplayUpdatesOn )
		return;

	timeval timeNow;
	gettimeofday(&timeNow, 0);

	if ( mUpdateDisplay )
	{
		mDisplayUpdateMutex.lock();

		DisplayWriteHeader();
		//
		DisplayWriteClientConnections();
		//
		DisplayWriteLogs();

		//  put the time as last line of display
		CMD date("date");
		date.Execute();
		printf("/***     Time:      %s\n", date.GetCommandResponseLine(0).c_str());
	
		mTimeOfLastClockUpdate = timeNow;

		mUpdateDisplay = false;

		mDisplayUpdateMutex.unlock();
	}
	else
	{
		//  do we need to update system clock
		if ( DurationMilliseconds(mTimeOfLastClockUpdate, timeNow) > 1000 )
		{
			DisplayUpdateClock();
			mTimeOfLastClockUpdate = timeNow;
		}
	}

	

}



void TheApp::DisplayWriteHeader()
{
	system("clear");

	printf("/*****************************************************************************\n");
	printf("/******  Simple Linux Connect TCP/IP Program \n");
	printf("/******  %s:\n", mVersionString.c_str());
	printf("/******\n");
	printf("/******      Connected on eth0: %s\n",mCMDifconfig.mEth0Info.mInet4Address.size() == 0 ? "not enabled " : mCMDifconfig.mEth0Info.mInet4Address.c_str());
	printf("/******      Connected on wlan: %s\n",mCMDifconfig.mWlanInfo.mInet4Address.size() == 0 ? "not enabled " : mCMDifconfig.mWlanInfo.mInet4Address.c_str());
	printf("/******      Server is listening on port: %d\n", mConnectionServerPort);	


}



void TheApp::DisplayWriteClientConnections()
{
	mConnectedClientsMutex.lock();

	//  write out client connection state
	std::map<std::string, ConnectedClient*>::iterator iter;

	for ( iter = mConnectedClients.begin(); iter != mConnectedClients.end(); iter++ )
	{
		printf("/******      - Client at %s connected on port %d.\n", iter->second->GetIpAddressOfClient().c_str(), iter->second->mClientsListeningPortNumber);
	}

	mConnectedClientsMutex.unlock();
}


void TheApp::DisplayWriteLogs()
{
	printf("/******\n");
	printf("/******      Event Logs:\n");

	mEventLogMutex.lock();

	//  write out the last 5 logs
	int logSize = mEventLog.size();
	for ( int i = 4; i >= 0; i-- )
	{
		if ( i < logSize )
		{
			std::list<std::string>::iterator it = mEventLog.begin();
			std::advance(it, i);
			printf("/******        > %s\n", it->c_str());
		}
		else
			printf("/******        >\n");
	}

	printf("/******\n");

	mEventLogMutex.unlock();
}


void TheApp::DisplayUpdateClock()
{
	if ( ! mDisplayUpdatesOn )
		return;

	mDisplayUpdateMutex.lock();

	fputs("\033[A\033[2K",stdout);
	rewind(stdout);
	//  had to get rid of this next call, would not compile on openSuse
	//  ftruncate(1,0); // you probably want this as well 
	//  put the date as last line of display
	CMD date("date");
	date.Execute();
	printf("/***     Time:      %s\n", date.GetCommandResponseLine(0).c_str());

	mDisplayUpdateMutex.unlock();
}

