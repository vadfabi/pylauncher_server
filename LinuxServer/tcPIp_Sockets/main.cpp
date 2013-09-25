//  tcPIp_Sockets
//  by LittleBytesOfPi.com

/* 
Modified BSD-2 Clause License for tcPIp_Sockets

Copyright (c) 2013, Little Bytes of Pi

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
including for commerical purposes, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions 
and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
and the following disclaimer in the documentation and/or other materials provided with the distribution.


THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR 
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY 
WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/




#include <string>

#include "TheApp.h"
#include "Parser.h"

using namespace std;



/////////////////////////////////////////////////////////////////////////////
//  main
//

//  define for stdin buffer read size
#define FGETS_READBUFFERSIZE 1024


//  local function declarations
int ProcessCommandModeInput(string input);
void PrintCommandHelp();

//  local variables for printing help if user enters unknown commands
bool printedHelp;
int unknownCommandCounter;

//  command line input will get read into this buffer
char readline[FGETS_READBUFFERSIZE];

//  TheApp object
//  this object is instantiated in the main function
//  and then the main function just waits for command line input while TheApp object runs the program
 TheApp theApp;


//  main
//
int main(int argc, char *argv[])
{
	//  startup the app
	if ( ! theApp.InitializeInstance() )
	{
		//  failure here is probably related to problem opening socket port in connection thread
		//  this is fatal error, bail out
		printf("! Failed to start application.");
		return 1;
	}

	//  TheApp is running, the user input is running in main thread
	//  this loops until the user quits the program
	while ( true )
	{
		//  init our buffer to null
		memset(readline, 0, FGETS_READBUFFERSIZE);

		//  read input from terminal
		//  the get_line function returns a pointer to your readLine buffer
		//  fgets always reads your size-1 to ensure you will have a newline at the end of the buffer
		char* readBuffer = get_line(readline, FGETS_READBUFFERSIZE, stdin);

		//  create a string from the buffer, not efficient but easy
		string input = readBuffer;

		//  suspend display updates while in command mode
		theApp.SuspendDisplayUpdates();

		// print a prompter on the screen
		printf("/***   Command Line Input: \n");
		printf(" >");

		//  init the help display counter
		printedHelp = false;
		unknownCommandCounter = 0;

		//  loop until user types 'quit' to end program, or 'resume' to exit command mode
		bool inCommandMode = true;
		while ( inCommandMode )
		{
			//  get next input
			memset(readline, 0, FGETS_READBUFFERSIZE);
			readBuffer = get_line(readline, FGETS_READBUFFERSIZE, stdin);
			input = readBuffer;

			switch( ProcessCommandModeInput(input) )
			{
			default:
				//  todo log error
			case 0:		//  continue in command mode
				break;

			case 1:		//  break out of command mode (user typed 'resume')
				inCommandMode = false;
				break;

			case -1:	//  application exit (user typed 'quit')
				theApp.ShutDown();
				return 0;
			}
		}

		// we are done in command mode
		theApp.ResumeDisplayUpdates();
	}

	return 0; 
}



//  ProcessCommandModeInput
//  takes input from terminal and processes recognized commands
//  will print help after unrecognized commands
//
int ProcessCommandModeInput(string input)
{
	//  disable log listing
	theApp.mListingLogs = false;

	//  check for known commands	
	if ( input.compare("quit") == 0 || input.compare("exit") == 0 )
	{
		//  'quit'  -  exit the program
		system("clear");
		printf("Exiting program ...\n");
		return -1;
	}
	else if ( input.compare("resume") == 0 || input.size() == 0 )
	{
		//  'resume'  -  break out of command mode
		return 1;
	}
	else if ( input.substr(0, string("message").size()).compare("message") == 0 && input.size() > string("message").size() )
	{
		//  'broadcast'
		theApp.BroadcastMessage(input);
		
		//  contiue in command mode
		printf(" >");
		return 0;
	}
	else if ( input.substr(0, string("forwarding").size()).compare("forwarding") == 0 && input.size() > string("forwarding").size() )
	{
		theApp.mForwardMessagesToAllClients = true;
		Parser inputParser(input, " ");
		inputParser.GetNextString();
		string argument = inputParser.GetNextString();
		
		if ( argument.compare("on") == 0 )
		{
			theApp.mForwardMessagesToAllClients = true;
			printf(" > message forwarding on\n");
		}
		else if (argument.compare("off") == 0 )
			{
				theApp.mForwardMessagesToAllClients = false;
				printf(" > message forwarding off\n");
		}
		else if ( argument.compare("wait") == 0 )
		{
			theApp.mForwardMessageWaitForClientResponse = true;
			printf(" > wait for response on\n");
		}
		else if ( argument.compare("nowait") == 0 )
		{
			theApp.mForwardMessageWaitForClientResponse = false;
			printf(" > wait for response off\n");
		}
		else
		{
			printf(" > input error, you must enter 'on' or 'off'\n");
		}

		//  contiue in command mode
		printf(" >");
		return 0;

	}
	else if ( input.substr(0, string("savelogs").size()).compare("savelogs") == 0 && input.size() > string("savelogs").size()  )
	{
		//  'savelogs'
		if ( theApp.SaveLogs(input) )
			printf(" > logs saved\n");
		else
			printf(" > ! error saving logs\n");

		//  contiue in command mode
		printf(" >");
		return 0;
	}
	else if ( input.compare("listlogs") == 0 )
	{
		//  'listlogs' - print all events from event log to terminal
		printf(" > ListLogs \n");

		theApp.PrintLogs(stdout);
		theApp.mListingLogs = true;
	
		return 0;
	}
	else if ( input.compare("clearlogs") == 0 )
	{
		//  'clearlogs'  -  clear the event log
		theApp.ClearLogs();
		printf(" > logs cleared\n");
		
		//  contiue in command mode
		printf(" >");
		return 0;
	}
	else
	{
		if ( ! printedHelp )
		{
			printedHelp = true;
			PrintCommandHelp();
			unknownCommandCounter = 0;
		}
		else
		{
			printf(" > Unknown command\n");
			unknownCommandCounter ++;
			if ( unknownCommandCounter > 3 )
				printedHelp = false;
		}	

		//  contiue in command mode
		printf(" >");
		return 0;
	}
}


void PrintCommandHelp()
{
	printf("\n/***\n");
	printf("/***   Commands:\n");
	printf("/***     - message toSend           -  sends toSend to all connected clients\n");
	printf("/***     - forwarding arg           -  arg = on | off to control forwarding of messages\n");
	printf("/***     - forwarding arg           -  arg = wait | nowait to control wait for forwarded response\n");
	printf("/***     - savelogs filename [-c]   -  saves the event log to filename, -c to clear logs after save.\n");
	printf("/***     - listlogs                 -  prints all logged events to monitor.\n");
	printf("/***     - clearlogs                -  clears the event log.\n");
	printf("/***     - resume                   -  exits command mode, resume display updates.\n");
	printf("/***     - quit                     -  exit the program\n");
}
