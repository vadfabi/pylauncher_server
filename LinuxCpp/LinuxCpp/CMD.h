#ifndef _CMD_H
#define _CMD_H

#include <string>
#include <vector>


using namespace std;

//  CMD
//  a class to execute "system(yourCommand)" and store the response as a list of strings

class CMD
{
public:

	CMD();
	CMD(string command);

	virtual ~CMD();

	
	//  calls system("yourCommand") then parses result
	//  returns false if response is new, otherwise returns true
	bool Execute();

	//  performs system("yourCommand")
	bool System();

	//  override this function if you want to parse your command response into specific values
	//  see CMDIfConfig for an example
	virtual bool Parse() { return true; }

	//  compare function, returns if every string in response list is same as last time command was executed
	bool Compare();

	//  get the command string formatted to go into the system( ) call
	//  you can override this function if you need to wrap some extra text around your command
	virtual string GetCommandLineString();

	//  get your command string
	string	GetCommand() { return Command; }
	
	//  get the number of lines in the command response
	int		GetCommandResponseSize() { return CommandResponse.size(); }

	//  get a specific line of the command response
	string	GetCommandResponseLine(int i);


protected:

	string	Command;
	vector<string> CommandResponse;
	vector<string> LastCommandResponse;
};

#endif