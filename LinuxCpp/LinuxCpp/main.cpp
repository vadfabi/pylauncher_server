#include "string.h"


#include "TheApp.h"
#include "Parser.h"



//  PrintCommandHelp
//  prints out command reference when in command mode
//
void PrintCommandHelp()
{
	printf("\n/***\n");
	printf("/***      Commands:\n");
	printf("/***        - savelogs [filename] -c   -  saves the event log to filename, optional -c to clear logs after save.\n");
	printf("/***        - listlogs                 -  prints all logged events to monitor.\n");
	printf("/***        - clearlogs                -  clears the event log.\n");
	printf("/***        - resume                   -  exits command mode, resume display updates.\n");
	printf("/***        - quit                     -  exit the program\n");
	printf("/***      >");
}


#define FGETS_READBUFFER 1024



//  main
//
int main(int argc, char *argv[])
{
	//  The application object
	//  this object is the central control for the application
	TheApp theApp;
	
	//  startup the app
	if ( ! theApp.InitializeInstance() )
	{
		//  failure here is probably related to problem opening socket port in connection thread
		printf("! Failed to start application.");
		return 1;
	}

	//  TheApp is now running, we will take input from the command line now
	char readline[FGETS_READBUFFER];

	//  this loops until the user quits the program
	while ( true )
	{
		//  init our buffer to null
		memset(readline, 0, FGETS_READBUFFER);

		//  read input from terminal
		char* readBuffer = get_line(readline, FGETS_READBUFFER, stdin);
		string input = readBuffer;

		//  suspend updates while in command mode
		theApp.SuspendDisplayUpdates();
		
		// Update screen,
		printf("/***\n");
		printf("/***      >");

		//  be prepared to print help if user enters bad command
		bool printedHelp = false;
		int unknownCommandCounter = 0;

		//  loop until user types 'quit' to end program, or 'resume' to exit command mode
		while ( true )
		{
			//  check for known commands
			//			
			if ( input.compare("quit") == 0 || input.compare("exit") == 0 )
			{
				//  'quit'  -  exit the program
				system("clear");
				printf("Exiting program ...\n");
				theApp.ShutDown();
				return 0;
			}

			else if ( input.compare("resume") == 0 )
			{
				//  'resume'  -  break out of command mode
				break;
			}
			else if ( input.compare("listlogs") == 0 )
			{
				//  'listlogs' - print all events from event log to terminal
				printf("\n");

				theApp.PrintLogs(stdout);
				printf("/***      >");
			}
			else if ( input.compare("clearlogs") == 0 )
			{
				//  'clearlogs'  -  clear the event log
				theApp.ClearLogs();
				printf("/***      > logs cleared\n");
				printf("/***      >");
			}
			else if ( input.find("savelogs") != string::npos )
			{
				//  'savelogs'  -  save logs to output file
				if ( theApp.SaveLogs(input) )
					printf("/***      > logs saved\n");
				else
					printf("/***      > ! error saving logs\n");

				printf("/***      >");
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
					printf("/***      >");
					unknownCommandCounter ++;
					if ( unknownCommandCounter > 3 )
						printedHelp = false;
				}	
			}

			//  get next input
			memset(readline, 0, FGETS_READBUFFER);
			readBuffer = get_line(readline, FGETS_READBUFFER, stdin);
			input = readBuffer;
		}

		theApp.ResumeDisplayUpdates();
		//  resume waiting for command mode
	}

	return 0; 
}



