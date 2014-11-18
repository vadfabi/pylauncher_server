#include <arpa/inet.h>
#include <string>
#include <sys/time.h>
#include <algorithm>
#include <future>
#include <fstream>
#include <iostream>
#include <sys/stat.h>
#include <dirent.h>
#include <limits.h>
#include <unistd.h>


#include "TheApp.h"
#include "../tcPIp_Sockets/Parser.h"
#include "ConnectedClientThread.h"
#include "../tcPIp_Sockets/UtilityFn.h"
#include "../tcPIp_Sockets/UtilityFn.h"


using namespace std;



/////////////////////////////////////////////////////////////////////////////
//  TheApp
//  the grand central station of the program,
//  this class runs everything except the keyboard input thread, which is in main
//


//  Constructor
//
TheApp::TheApp() :
	mConnectionThread(*this),   
	mBroadcastThread(*this), 
	mPyLaunchThread(*this), 
	mDisplayThread(*this)
{
	mVersionString = "1.1.0";

	mMaxEventsToLog = 9999;
	mLogSysEvents = true;

	mDisplayUpdatesOn = true;
	

	mForwardMessagesToAllClients = true;
	mForwardMessageWaitForClientResponse = true;
}


//  Destructor
//
TheApp::~TheApp()
{
	ShutDown();
}


//  InitializeInstance
//  perform all the required steps for the application to start
//
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
	CMD hostName("hostname");
	hostName.Execute();
	mHostname = hostName.GetCommandResponseLine(0);

	LoadHeaderFile();
	LoadPythonFileDirectoryFile();
	LoadPythonFilesList();


	//  start the connection thread
	mConnectionThread.Start();


	mBroadcastThread.Start();

	mPyLaunchThread.Start();

	mDisplayThread.Start();
	mDisplayThread.UpdateEverything();

	//  initialization successful
	return true;
}




//  ShutDown
//  stops running threads, and cleans up memory allocated by TheApp object
//
void TheApp::ShutDown()
{
	//  stop the broadcast thread
	if ( mBroadcastThread.IsRunning() )
		mBroadcastThread.Cancel();

	if ( mPyLaunchThread.IsRunning() )
		mPyLaunchThread.Cancel();

	if ( mDisplayThread.IsRunning() )
		mDisplayThread.Cancel();

	//  disconnect all of our clients
	map<string, ConnectedClient*>::iterator nextClient;
	for ( nextClient = mConnectedClients.begin(); nextClient != mConnectedClients.end(); ++nextClient )
	{
		nextClient->second->Cancel();
		delete nextClient->second;
	}	
	//  done with clients
	mConnectedClients.erase(mConnectedClients.begin(), mConnectedClients.end());

	//  stop the connection thread
	if ( mConnectionThread.IsRunning() )
		mConnectionThread.Cancel();

	

	//  delete the log event objects in the mEventLog list
	//  this method uses the helper function deleteLogEvent( )
	mEventLog.remove_if(deleteLogEvent);
}


void TheApp::LoadHeaderFile()
{
	mHeaderList.clear();

	ifstream headerFile;
	headerFile.open(HEADERFILE);
	if ( ! headerFile.is_open() )
	{
		mHeaderList.push_back("pyLauncher:    by LittleBytesOfPi.com");
		mHeaderList.push_back(format("version: %s", mVersionString.c_str()));

		mHeaderList.push_back("  - modify header.txt to put your text here ... ");

		ofstream writeHeaderFile;
		writeHeaderFile.open(HEADERFILE);
		if (writeHeaderFile.is_open())
		{
			list<string>::iterator nextString;
			for (nextString = mHeaderList.begin(); nextString != mHeaderList.end(); ++nextString)
			{
				writeHeaderFile << *nextString + "\n";
			}

			writeHeaderFile.close();

			CMD cmd(format("chmod 666 %s", HEADERFILE));
			cmd.Execute();
		}
	}
	else
	{
		string readLine;
		while (!headerFile.eof())
		{
			getline(headerFile, readLine);
			if (readLine.size() != 0)
				mHeaderList.push_back(readLine);
		}

		headerFile.close();
	}
}


void TheApp::LoadPythonFileDirectoryFile()
{
	LockMutex lockFiles(mFilesListMutex);

	mDirectoryList.clear();

	//  fill the dir list from the file
	ifstream dirFile;
	dirFile.open(DIRLISTFILE);

	if (  dirFile.is_open() )
	{
		string readLine;
		while (!dirFile.eof())
		{
			getline(dirFile, readLine);
			if (readLine.size() != 0)
			{
				//  TODO - make sure this dir exists before putting it on the list
				mDirectoryList.push_back(readLine);
			}
		}

		dirFile.close();
	}
	else
	{
		//  There is no directory list file
		char result[ PATH_MAX ];
		ssize_t count = readlink( "/proc/self/exe", result, PATH_MAX );

		//  remove pyLauncher from path
		string path = string(result, count > 0 ? count : 0);
		path = path.substr(0, path.find_last_of("/"));
		
		//  open file
		ofstream outputFile;
		outputFile.open(DIRLISTFILE);
		
		if (outputFile.is_open())
		{
			outputFile << format("%s\n", path.c_str());
			mDirectoryList.push_back(path);
			outputFile.close();

			CMD cmd(format("chmod 666 %s", DIRLISTFILE));
			cmd.Execute();
		}

		//  create the help file
		outputFile.open("programHelp.py");
		if (outputFile.is_open())
		{

			outputFile << "print \"pyLauncher Program Help\"\n";
			outputFile << "print \"1) Register python file locations on the Directory.\"\n";
			outputFile << "print \"2) Select a python file to launch.\"\n";
			outputFile << "print \"3) Input command line arguments (optional).\"\n";
			outputFile << "print \"4) Tap [Run] to launch file.\"\n";

			outputFile.close();

			CMD cmd(format("chmod 666 %s", "programHelp.py"));
			cmd.Execute();
		}
	}
}


void TheApp::LoadPythonFilesList()
{
	LockMutex lockFiles(mFilesListMutex);

	mFilesList.clear();

	list<string>::iterator nextDir;
	for ( nextDir = mDirectoryList.begin(); nextDir != mDirectoryList.end(); ++nextDir )
	{
		DIR *dir;
		struct dirent *ent;
		if ( (dir = opendir((*nextDir).c_str())) != 0 ) 
		{
			//  look for all the .py files in the directories
			while ((ent = readdir (dir)) != 0) 
			{
				string fileName = ent->d_name;
				if ( fileName.size() < 4 )
					continue;	//  skip files too small to have .py

				if ( (int)fileName.find("._",0,2) >= 0 )
					continue;

				if ( fileName.find(".py",0,3) == (fileName.size() -3) )
				{
					string fileFullPath = *nextDir + "/" + fileName;
					mFilesList.push_back(fileFullPath);
				}

			}
			closedir (dir);
		} 
		else 
		{
			/* could not open directory */
			continue;
		}
	}
}



void TheApp::LiveUpdatePythonFiles()
{
	LoadPythonFilesList();

	timeval eventTime;
	gettimeofday(&eventTime, 0);

	string listOfDir = BuildDirList();
	string dirMessage = format("$TCP_LISTDIR,ACK,%s", listOfDir.c_str());

	BroadcastMessage(eventTime, GetIpAddress(), dirMessage);

	listOfDir = BuildFileList();
	dirMessage = format("$TCP_LISTFILES,ACK,%s", listOfDir.c_str());

	
	BroadcastMessage(eventTime, GetIpAddress(), dirMessage);

}



//  AddEvent
//  add an event to the event log
//
void TheApp::AddEvent(timeval eventTime, string eventSender, string eventDetails)
{
	if ( ! mLogSysEvents && eventSender.compare(SYSEVENT) == 0 )
		return;

	//  we will be modifying the event log list, lock access to it
	{
		LockMutex lockEvents(mEventLogMutex);

		LogEvent* newEvent = new LogEvent(eventTime, eventSender, eventDetails);
		mEventLog.push_front(newEvent);

		if ( mEventLog.size() > mMaxEventsToLog )
		{
			delete mConnectedClients.end()->second;
			mEventLog.pop_back();
		}

		//  update display
		mDisplayThread.AddEvent(newEvent);

	}//  we are done modifying the event log, release access


}


//  AddEvent
//  add an event to the log
//
void TheApp::AddEvent(string eventSender, string eventDetails)
{
	if ( ! mLogSysEvents && eventSender.compare(SYSEVENT) == 0 )
		return;

	timeval eventTime;
	gettimeofday(&eventTime, 0);

	AddEvent(eventTime, eventSender, eventDetails);
}



//  CreateClientConnection
//  creates a new connection thread for an individual client
//
int TheApp::CreateClientConnection(const struct sockaddr_in &clientAddress, int clientListeningOnPortNumber)
{
	int servingClientOnPortNumber = -1;

	//  lock on access to map of clients
	{
		LockMutex lockConnectedClients(mConnectedClientsMutex);

		//  get the dots and numbers string for client, this is used as the key in our clients map
		string  addressOfSender = IpAddressString(clientAddress);

		//  see if this client exists already
		map<string, ConnectedClient*>::iterator it = mConnectedClients.find(addressOfSender);
		if ( it != mConnectedClients.end() )
		{
			//  shut down old connection and delete this client
			it->second->Cancel();
			mConnectedClients.erase(addressOfSender);
			delete it->second;
		}

		//  create connection thread for the client
		ConnectedClient* newClient = new ConnectedClient(*this, clientAddress, clientListeningOnPortNumber);

		//  open a port to serve this client
		servingClientOnPortNumber = newClient->OpenServerSocket(49000, false);

		if ( servingClientOnPortNumber < 0 )
		{
			delete newClient;
			return -1;
		}

		//  start the thread
		newClient->Start();

		//  add the client to the map
		mConnectedClients[newClient->GetIpAddressOfClient()] =  newClient;

	} //  unlock access to the client map

	mDisplayThread.UpdateConnections();

	return servingClientOnPortNumber;
}



//  DisconnectClient
//  closes client server port, stops client thread, and deletes memory allocated for client objects
//
void TheApp::DisconnectClient(struct sockaddr_in &clientAddress)
{
	//  we will be modifying the client map, lock access to it
	{
		LockMutex lockConnectedClients(mConnectedClientsMutex);

		//  our map uses dots and numbers address of client as a key
		string clientKey = IpAddressString(clientAddress);

		//  see if this client exists already
		map<string, ConnectedClient*>::iterator it = mConnectedClients.find(clientKey);
		if ( it == mConnectedClients.end() )
		{
			//  this key does not exist, not expected here
			return;
		}

		//  stop client thread
		it->second->Cancel();

		//  delete memory
		delete it->second;

		//  remove client from map
		mConnectedClients.erase(clientKey);

	}	//  release access to client map

	mDisplayThread.UpdateConnections();

}


//  BuildDirList
//
string TheApp::BuildDirList()
{
	LockMutex lockFiles(mFilesListMutex);

	//  print list out to file
	string listOfDir = "";
	list<string>::iterator nextString;
	for( nextString = mDirectoryList.begin(); nextString != mDirectoryList.end(); ++nextString )
	{
		if ( nextString != mDirectoryList.begin() && nextString != mDirectoryList.end() )
			listOfDir += ",";
		listOfDir += *nextString;
	}

	return listOfDir;

}


//  BuildFileList
//
string TheApp::BuildFileList()
{
	LockMutex lockFiles(mFilesListMutex);

	//  print list out to file
	string listOfFiles = "";
	list<string>::iterator nextString;
	for( nextString = mFilesList.begin(); nextString != mFilesList.end(); ++nextString )
	{
		if ( nextString != mFilesList.begin() && nextString != mFilesList.end() )
			listOfFiles += ",";
		listOfFiles += *nextString;
	}

	return listOfFiles;
}


//  HandleAddDirectory
//
bool TheApp::HandleAddDirectory(timeval eventTime, std::string eventSender, std::string dirName)
{
	//  make sure this directory exists
	struct stat sb;

	if (stat(dirName.c_str(), &sb) == 0 && S_ISDIR(sb.st_mode))
	{
		LockMutex lockFiles(mFilesListMutex);

		if (find(mDirectoryList.begin(), mDirectoryList.end(), dirName) != mDirectoryList.end())
		{
			//  already in the list
			return true;
		}

		mDirectoryList.push_back(dirName);

		//  save out the new dir file
		//  open file
		FILE* outputFile = fopen(DIRLISTFILE, "w");
		if (outputFile == 0)
			return false;

		//  print list out to file
		list<string>::iterator nextString;
		for (nextString = mDirectoryList.begin(); nextString != mDirectoryList.end(); ++nextString)
		{
			fprintf(outputFile, "%s\n", nextString->c_str());
		}

		fclose(outputFile);

		LiveUpdatePythonFiles();

		return true;
	}
	else
	{
		//  This directory does not exist
		return false;
	}
}

//  function to remove directory from the collection
bool TheApp::HandleRemoveDirectory(timeval eventTime, std::string eventSender, std::string dirName)
{
	{
		LockMutex lockFiles(mFilesListMutex);

		Parser dirNameParser(dirName, ",");
		string nextDir = dirNameParser.GetNextString();
		while (nextDir.size() != 0 )
		{
			mDirectoryList.remove(nextDir);
			nextDir = dirNameParser.GetNextString();
		}

		//  open file
		FILE* outputFile = fopen ( DIRLISTFILE, "w" );
		if ( outputFile == 0 )
			return false;

		list<string>::iterator nextString;
		for ( nextString = mDirectoryList.begin(); nextString != mDirectoryList.end(); ++nextString )
		{
			fprintf( outputFile, "%s\n", nextString->c_str());
		}
	

		//  close file
		fclose( outputFile );
	}

	LiveUpdatePythonFiles();

	return true;
}

//  HandlePythonLaunch
//
void TheApp::HandlePythonLaunch(timeval eventTime, std::string eventSender, std::string args)
{
	mPyLaunchThread.AddLaunchEvent(eventTime, eventSender, args);

	return;
}


//  SendMessageToAllClients
//  this function is called from the broadcast thread to send a message to all clients
//
void TheApp::SendMessageToAllClients(list<LogEvent*>& eventsToSend)
{
	//  lock the connected clients map
	LockMutex lockConnectedClients(mConnectedClientsMutex);

	map<string, ConnectedClient*>::iterator nextClient;
	for ( nextClient = mConnectedClients.begin(); nextClient != mConnectedClients.end(); nextClient++ )
	{
		//  skip dormant clients
		if ( ! nextClient->second->IsActiveClient() )
			continue;

		//  build a string of all events to send
		string sendString;

		//  iterate through list of events for each client, so we can filter out events that came from this client
		list<LogEvent*>::iterator nextEvent;
		for ( nextEvent = eventsToSend.begin(); nextEvent != eventsToSend.end(); nextEvent++ )
		{
			if ( nextClient->second->GetIpAddressOfClient().compare((*nextEvent)->mEventAddress) == 0 )
				continue;

			sendString += (*nextEvent)->mEvent + "\n";

		}

		string response = nextClient->second->SendMessageToClient(sendString,  mForwardMessageWaitForClientResponse);

		//  if we are waiting for responses, log the response as an event
		if ( mForwardMessageWaitForClientResponse )
		{
			timeval eventTime;
			gettimeofday(&eventTime, 0);

			//  parse response into different events
			Parser responseParser(response, "\n");
			string nextResponse = responseParser.GetNextString();
			while ( nextResponse.size() > 0 )
			{
				AddEvent(eventTime, nextClient->second->GetIpAddressOfClient(),  nextResponse);
				nextResponse = responseParser.GetNextString();
			}
		}
	}
}


//  SendMessageToAllClients
//  this function is called from the broadcast thread to send a message to all clients
//  this implementation uses async, but it is not stable, too many failures to connect on socket with error code EINTR
//
//void TheApp::SendMessageToAllClients(timeval eventTime, string eventSender, string message)
//{
//	LockMutex lockConnectedClients(mConnectedClientsMutex);
//
//	vector<future<string>> futures;
//
//	map<string, ConnectedClient*>::iterator nextClient;
//	for ( nextClient = mConnectedClients.begin(); nextClient != mConnectedClients.end(); nextClient++ )
//	{
//		if ( nextClient->second->GetIpAddressOfClient().compare(eventSender) == 0 )
//			continue;	//  don't rebroadcast to sender
//
//		futures.push_back(async(launch::async, &ConnectedClient::SendMessageToClient, nextClient->second, message, mForwardMessageWaitForClientResponse));
//	}
//
//	//  if we are waiting for responses, log the response as an event
//	if ( mForwardMessageWaitForClientResponse )
//	{
//		vector<future<string>>::iterator nextFuture;
//		for( nextFuture = futures.begin(); nextFuture != futures.end(); ++nextFuture )
//		{
//			nextFuture->wait();
//			AddEvent("",  nextFuture->get());
//		}
//	}
//
//}



//  GetIpAddress
//  
string TheApp::GetIpAddress()
{
	if ( mCMDifconfig.mEth0Info.mInet4Address.size() > 0 )
		return mCMDifconfig.mEth0Info.mInet4Address;
	else if ( mCMDifconfig.mWlanInfo.mInet4Address.size() > 0 )
		return mCMDifconfig.mWlanInfo.mInet4Address;
	else
		return "Not connected";
}





//  BroadcastMessage
//  this function is called from the connected client thread when we get a broadcast message
//
void TheApp::BroadcastMessage(timeval eventTime, string eventSender, string message)
{
	AddEvent(eventTime, eventSender, message);

	if ( mForwardMessagesToAllClients )
	{
		mBroadcastThread.AddMessage(eventTime, eventSender, message);
	}
}







//  Command Line Functions
//  handlers for command functions from arguments entered in main loop
//


//  RefreshFiles
//  reloads dir list and broadcasts to clients
void TheApp::RefreshFiles()
{
	LoadPythonFileDirectoryFile();
	LiveUpdatePythonFiles();
}

//  SaveLogs
//  save logs out to a log file
//
bool TheApp::SaveLogs(string input)
{
	Parser inputParser(input, " ");
	string command = inputParser.GetNextString();
	string filename = inputParser.GetNextString();
	string clearArgument = inputParser.GetNextString();

	//  open file
	FILE* outputFile = fopen ( filename.c_str(), "w" );
	if ( outputFile == 0 )
		return false;

	//  accessing the event log list, lock it
	LockMutex lockEventLog(mEventLogMutex);

	//  print all the logs
	for_each(  mEventLog.begin(), mEventLog.end(), bind2nd(mem_fun(&LogEvent::PrintLog), outputFile) );

	//  if remove command, clear the logs
	if ( clearArgument.compare("-c") == 0 )
		mEventLog.remove_if(deleteLogEvent);

	//  close file
	fclose( outputFile );

	return true;
}



//  PrintLogs
//  print all events in the event log to the file stream you specify
//
void TheApp::PrintLogs(FILE* stream)
{
	//  lock access to event log list
	LockMutex lockEventLog(mEventLogMutex);

	////  iterate through log list and print logs to stdout
	//list<LogEvent*>::reverse_iterator nextLog = mEventLog.rbegin();
	//for ( ; nextLog != mEventLog.rend(); ++nextLog )
	//{
	//	(*nextLog)->PrintLog(stream);
	//}



	//  very cool way to iterate through a list does it work for back to front?
	for_each(  mEventLog.rbegin(), mEventLog.rend(), bind2nd(mem_fun(&LogEvent::PrintLog),stream) );

}



//  ClearLogs
//  clears the event log
void TheApp::ClearLogs()
{
	//  lock access to event log list
	LockMutex lockEventLog(mEventLogMutex);

	//  delete the log event objects in the mEventLog list
	//  this method uses the helper function deleteLogEvent( )
	mEventLog.remove_if(deleteLogEvent);

	/*
	//  this is instead of doing something more old fashioned such as
	//  clean up the event log
	list<LogEvent*>::iterator nextEvent;
	for ( nextEvent = mEventLog.begin(); nextEvent != mEventLog.end(); nextEvent++ )
	{
	delete *nextEvent;
	}
	*/

}



// Show connection status
void TheApp::ShowConnectionStatus()
{
	mDisplayThread.UpdateConnections();
}





//  Display Output
//




//  SuspendDisplayUpdates
//  set flag to suspend display updtes, this is used when main( ) enters command mode for terminal input
//
void TheApp::SuspendDisplayUpdates() 
{

	mDisplayUpdatesOn = false; 
	mTerminalDisplay.SetColour(BRIGHT,WHITE,BLACK);
}


//  ResumeDisplayUpdates
//  set flag to resume display updates
//
void TheApp::ResumeDisplayUpdates()
{

	//  turn updates back on
	mDisplayUpdatesOn = true; 

	mDisplayThread.UpdateEverything();
}






void TheApp::DisplayWriteHeader()
{
	if ( ! mDisplayUpdatesOn )
		return;

	CMD clear("clear");
	clear.Execute();

	mTerminalDisplay.InitDimensions();
	mTerminalDisplay.SetBackground(BLACK);

	//  Print the header
	//
	mTerminalDisplay.PrintAcross ("**", BLACK,RED);
	mTerminalDisplay.PrintLine("**", BLACK,RED, "",RED,BLACK);
	//  all lines from header file
	list<string>::iterator nextString;
	for (nextString = mHeaderList.begin(); nextString != mHeaderList.end(); ++nextString)
	{
		mTerminalDisplay.PrintLine("**", BLACK, RED, format("  %s", nextString->c_str()), RED, BLACK);
	}

	//
	mTerminalDisplay.PrintLine("**", BLACK,RED, "",RED,BLACK);
	mTerminalDisplay.PrintAcross("**", BLACK,RED, "*",RED,BLACK);

	//  Print connection status
	mTerminalDisplay.PrintLine("**", BLACK,RED, "",RED,BLACK);
	mTerminalDisplay.PrintLine("**", BLACK,RED, "  Network Connection Status", RED,BLACK);
	mTerminalDisplay.PrintLine("**", BLACK,RED, format("   - Connected on eth0: %s",mCMDifconfig.mEth0Info.mInet4Address.size() == 0 ? "not enabled " : mCMDifconfig.mEth0Info.mInet4Address.c_str()), RED,BLACK);
	mTerminalDisplay.PrintLine("**", BLACK,RED, format("   - Connected on wlan: %s",mCMDifconfig.mWlanInfo.mInet4Address.size() == 0 ? "not enabled " : mCMDifconfig.mWlanInfo.mInet4Address.c_str()), RED,BLACK);
	mTerminalDisplay.PrintLine("**", BLACK,RED, format("   - Server is listening on port %d", mConnectionServerPort), RED,BLACK);
	mTerminalDisplay.PrintLine("**", BLACK,RED, "", RED,BLACK);
	mTerminalDisplay.PrintAcross("**", BLACK,RED, "-",RED,BLACK);
}


void TheApp::DisplayWriteConnectionStatus()
{
	if ( ! mDisplayUpdatesOn )
		return;

	//LockMutex lockClients(mConnectedClientsMutex);
	if ( mConnectedClientsMutex.try_lock() )
	{
		CMD dateCommand("date");
		dateCommand.Execute();

		mTerminalDisplay.PrintAcross("**", BLACK,RED, "-",RED,BLACK);
		mTerminalDisplay.PrintLine("**", BLACK,RED, "",RED,BLACK);
		mTerminalDisplay.PrintLine("**", BLACK,RED, format("  Client Connection Status at %s:", dateCommand.GetCommandResponseLine(0).c_str()).c_str(), RED,BLACK);

		if ( mConnectedClients.size() == 0 )
		{
			mTerminalDisplay.PrintLine("**", BLACK,RED, "   - None connected", RED,BLACK);
		}
		else
		{
			//  write out client connection state
			map<string, ConnectedClient*>::iterator iter;

			for ( iter = mConnectedClients.begin(); iter != mConnectedClients.end(); iter++ )
			{
				mTerminalDisplay.PrintLine("**", BLACK,RED, format("   - Client at %s connected on port %d %s.", iter->second->GetIpAddressOfClient().c_str(), iter->second->GetConnectedOnPortNumber(), iter->second->IsActiveClient() ? "" : "(not active)" ).c_str(), RED,BLACK);

			}
		}

		mConnectedClientsMutex.unlock();

		mTerminalDisplay.PrintLine("**", BLACK,RED, "",RED,BLACK);
		mTerminalDisplay.PrintAcross("**", BLACK,RED, "-",RED,BLACK);
	}
}


void TheApp::DisplayWriteEvent(LogEvent event)
{
	if ( ! mDisplayUpdatesOn )
		return;

	if ( mListingLogs )
		event.PrintLog(stdout);
	else
	{
		if ( event.mEvent.substr(0, string("$TCP_PYRESULT").size()).compare("$TCP_PYRESULT") == 0  )
		{
			Parser resultParser(event.mEvent, ",");
			
			//  strip off $TCP_PYRESULT
			resultParser.GetNextString();		
			
			//   get the details

			string file = resultParser.GetNextString();
			string ipRequestor = resultParser.GetNextString();
			string timeRequest = resultParser.GetNextString();
			string timeLaunch = resultParser.GetNextString();
			string timeComplete = resultParser.GetNextString();
	
			mTerminalDisplay.PrintAcross("**", BLACK,GREEN, "-",GREEN,BLACK);
			mTerminalDisplay.PrintLine("**", BLACK,GREEN, "",GREEN,BLACK);
			mTerminalDisplay.PrintLine("**", BLACK,GREEN, format("  Python File: %s", file.c_str()), GREEN,BLACK);
			mTerminalDisplay.PrintLine("**", BLACK,GREEN, format("   - Launched by: %s", ipRequestor.c_str()), GREEN,BLACK);
			mTerminalDisplay.PrintLine("**", BLACK,GREEN, format("   - Launched at: %s", timeLaunch.c_str()), GREEN,BLACK);

			string nextResult =  resultParser.GetNextString();
			while ( nextResult.size() != 0 )
			{
				mTerminalDisplay.PrintLine(nextResult.c_str(), BLACK,WHITE);

				nextResult = resultParser.GetNextString();
			}

			mTerminalDisplay.PrintLine("**", BLACK,GREEN, format("   - Run completed at: %s", timeComplete.c_str()),GREEN,BLACK);
			mTerminalDisplay.PrintLine("**", BLACK,GREEN, "",GREEN,BLACK);
			mTerminalDisplay.PrintAcross("**", BLACK,GREEN, "-",GREEN,BLACK);
		}
	}
}




