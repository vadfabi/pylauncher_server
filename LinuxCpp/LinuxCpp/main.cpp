#include "TheApp.h"
#include "Parser.h"



int main(int argc, char *argv[])
{
	TheApp theApp;
	bool returnValue = theApp.InitializeInstance();
	if ( returnValue == false )
	{
		printf("Failed to start application.");
		return 1;
	}
	
	//  wait for program exit command
	while ( true )
	{
		//  read input from terminal
		char readline[1024];
		char* p = get_line(readline, 1024, stdin);
		string input = p;

		//  see if it is quit command
		if ( input.compare("quit") == 0 || input.compare("exit") == 0 )
		{

			system("clear");
			printf("Exiting ...\n");
			Sleep(1000);

			theApp.ShutDown();
			break;
		}
		else
		{
			system("clear");
			printf("Type 'quit' or 'exit' to shut down.\n");
			Sleep(1000);
		}
	}

	return 0; 
}