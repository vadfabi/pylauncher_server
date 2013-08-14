#include "CMD.h"

#include <algorithm>


CMD::CMD()
{
	Command = "";
	
}

CMD::CMD(std::string command)
{
	Command = command;
	
}


CMD::~CMD()
{
	Command = "";
}


/// 
///  Execute
///  Calls system and parse
///
bool CMD::Execute()
{
	CommandResponse.clear();

	if ( System() && Parse() )
		return Compare();
	
	return false;
}


///
///  System
///  calls system(yourCommand)
///  puts response into a std::string list (with newlines stripped off)
///
bool CMD::System()
{
	//  issue the ifconfig command
	std::string commandLineString = GetCommandLineString();
	FILE *cmdResponse = popen(commandLineString.c_str(), "r");

	if ( cmdResponse == 0 )
		return false;

	//  parse the return into string list
	char buffer[1024];
	char *nextLine = fgets(buffer, sizeof(buffer), cmdResponse);
	while ( nextLine )
	{
		//  string object from char*
		std::string nextResponse = nextLine;

		//  remove newlines and carriage returns
		nextResponse.erase(std::remove(nextResponse.begin(), nextResponse.end(), '\n'), nextResponse.end());
		nextResponse.erase(std::remove(nextResponse.begin(), nextResponse.end(), '\r'), nextResponse.end());

		//  add to the end of the response list
		CommandResponse.push_back(nextResponse);

		//  look for the next line of the command
		nextLine = fgets(buffer, sizeof(buffer), cmdResponse);
	}

	pclose(cmdResponse);
	return true;
}


///
/// Compare
///
bool CMD::Compare()
{
	//  response same size
	if ( CommandResponse.size() != LastCommandResponse.size() )
	{
		LastCommandResponse.clear();
		for ( int i = CommandResponse.size()-1; i >= 0 ; i-- )
			LastCommandResponse.push_back( CommandResponse[i]);

		return false;
	}

	//  all strings in response compare
	for ( int i = 0; i < CommandResponse.size(); i++ )
	{
		if ( CommandResponse[i].compare(LastCommandResponse[i]) != 0 )
		{
			LastCommandResponse.clear();
			for ( int j = 0; j < CommandResponse.size(); j++ )
				LastCommandResponse.insert(LastCommandResponse.end(), CommandResponse[j]);

			return false;
		}
	}

	return true;
}

///
/// GetCommandLineString
///
std::string CMD::GetCommandLineString()
{
	return Command;
}


std::string CMD::GetCommandResponseLine(int i)
{
	if ( i >= CommandResponse.size() )
		return "";

	return CommandResponse[i];
}