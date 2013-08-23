#ifndef _PARSER_H
#define _PARSER_H

#include <iostream>


using namespace std;




//  Parser
//  a class to take a string and return the value of substring elements


class Parser
{
public:

	Parser(string buffer, string delimiters);
	virtual ~Parser();

	void SetBuffer(string buffer);
	void SetBuffer(string buffer, string delimiters);

	//  returns the next item as a string
	string GetNextString();

	//  returns the next item as an int
	int GetNextInt();

	//  returns the remaining buffer as a string
	string GetRemainingBuffer();
	

protected:

	string mOriginalBuffer;
	string mDelimiters;
	string mRemainingBuffer;
};


#endif