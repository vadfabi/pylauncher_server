//  pyLauncher
//  by LittleBytesOfPi.com

/* 
Modified BSD-2 Clause License for pyLauncher

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
#include "../tcPIp_Sockets/Parser.h"

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
	else if ( input.compare("resume") == 0 || input.size() == 0  )
	{
		//  'resume'  -  break out of command mode
		return 1;
	}
	else if ( input.compare("connection") == 0 )
	{
		theApp.ShowConnectionStatus();
		//  contiue in command mode
		printf(" >");
		return 0;
	}
	else if ( input.compare("refresh") == 0 )
	{
		theApp.RefreshFiles();
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
	
		//  contiue in command mode
		printf(" >");
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
	printf("/***********************************************************************************\n");
	printf("/*** pyLauncher - by LittleBytesOfPi.com \n");
	printf("/***\n");
	printf("/***   Commands:\n");
	printf("/***     - connection               -  print out the current connection status\n");
	printf("/***     - refresh                  -  refresh list of python files from directoryList.txt\n");
	printf("/***     - savelogs filename [-c]   -  save the event log to filename, -c clears after save\n");
	printf("/***     - listlogs                 -  print all logged events to monitor\n");
	printf("/***     - clearlogs                -  clear the event log\n");
	printf("/***     - resume                   -  done with command mode, resume display updates\n");
	printf("/***     - quit                     -  exit the program\n");
}
