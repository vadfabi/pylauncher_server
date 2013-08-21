#include "string.h"

#include "TheApp.h"
#include "Parser.h"

using namespace std;


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

	//  TheApp is running, we will take input from the command line now
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
		printf("/***\n");
		printf("/***      >");

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
				DEBUG_TRACE("Unexpected return from ProcessCommandModeInput()\n");		//  always a good idea to handle unexpected case in switch statement with default
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
	//  check for known commands	
	if ( input.compare("quit") == 0 || input.compare("exit") == 0 )
	{
		//  'quit'  -  exit the program
		system("clear");
		printf("Exiting program ...\n");
		return -1;
	}
	else if ( input.compare("resume") == 0 )
	{
		//  'resume'  -  break out of command mode
		return 1;
	}
	else if ( input.substr(0, string("broadcast").size()).compare("broadcast") == 0 )
	{
		//  'broadcast'
		theApp.BroadcastMessage(input);
		
		//  contiue in command mode
		printf("/***      >");
		return 0;
	}
	else if ( input.substr(0, string("forwarding").size()).compare("forwarding") == 0 )
	{
		theApp.mForwardMessagesToAllClients = true;
		Parser inputParser(input, " ");
		inputParser.GetNextString();
		string inputOnOff = inputParser.GetNextString();
		
		if ( inputOnOff.compare("on") == 0 )
		{
			theApp.mForwardMessagesToAllClients = true;
			printf("/***      > message forwarding on\n");
		}
		else if (inputOnOff.compare("off") == 0 )
			{
				theApp.mForwardMessagesToAllClients = false;
				printf("/***      > message forwarding off\n");
		}
		else
		{
			printf("/***      > input error, you must enter 'on' or 'off'\n");
		}

		//  contiue in command mode
		printf("/***      >");
		return 0;

	}
	else if ( input.substr(0, string("savelogs").size()).compare("savelogs") == 0 )
	{
		//  'savelogs'
		if ( theApp.SaveLogs(input) )
			printf("/***      > logs saved\n");
		else
			printf("/***      > ! error saving logs\n");

		//  contiue in command mode
		printf("/***      >");
		return 0;
	}
	else if ( input.compare("listlogs") == 0 )
	{
		//  'listlogs' - print all events from event log to terminal
		printf("\n");

		theApp.PrintLogs(stdout);

		//  contiue in command mode
		printf("/***      >");
		return 0;
	}
	else if ( input.compare("clearlogs") == 0 )
	{
		//  'clearlogs'  -  clear the event log
		theApp.ClearLogs();
		printf("/***      > logs cleared\n");
		
		//  contiue in command mode
		printf("/***      >");
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
			printf("/***      > Unknown command\n");
			unknownCommandCounter ++;
			if ( unknownCommandCounter > 3 )
				printedHelp = false;
		}	

		//  contiue in command mode
		printf("/***      >");
		return 0;
	}
}


void PrintCommandHelp()
{
	printf("\n/***\n");
	printf("/***      Commands:\n");
	printf("/***        - broadcast [message]      -  sends message to all connected clients\n");
	printf("/***        - savelogs [filename] -c   -  saves the event log to filename, optional -c to clear logs after save.\n");
	printf("/***        - listlogs                 -  prints all logged events to monitor.\n");
	printf("/***        - clearlogs                -  clears the event log.\n");
	printf("/***        - resume                   -  exits command mode, resume display updates.\n");
	printf("/***        - quit                     -  exit the program\n");
}
