//#include <stdlib.h>

#include <cstring>
//#include <iostream>
//#include <sstream>
//#include <cstdio>
//#include <thread>
//#include <string>


#include "Parser.h"


using namespace std;




//  Constructor
//
Parser::Parser(string buffer, string delimiters)
{
	//  set the string and delimiters
	mOriginalBuffer = buffer;
	mRemainingBuffer = mOriginalBuffer;
	mDelimiters = delimiters;

	
}


//  Destructor
//
Parser::~Parser()
{
}


//  SetBuffer
//
void Parser::SetBuffer(string buffer)
{
	mOriginalBuffer = buffer;
	mRemainingBuffer = mOriginalBuffer;
}


//  SetBuffer
//
void Parser::SetBuffer(string buffer, string delimiters)
{
	mDelimiters = delimiters;
	SetBuffer(buffer);
}


//  GetNextString
//
string Parser::GetNextString()
{
	if ( mRemainingBuffer == "" )
		return "";

	string returnString = "";
	size_t index = mRemainingBuffer.find_first_of(mDelimiters, 0);

	//Skip delimiters at beginning.
	string::size_type lastPos = mRemainingBuffer.find_first_not_of(mDelimiters, 0);
	// Find first "non-delimiter".
	string::size_type pos     = mRemainingBuffer.find_first_of(mDelimiters, lastPos);

	if (string::npos != pos || string::npos != lastPos)
		returnString = mRemainingBuffer.substr(lastPos, pos - lastPos);
	else
	{
		returnString = mRemainingBuffer;
		mRemainingBuffer = "";
		return returnString;
	}

	mRemainingBuffer = mRemainingBuffer.substr(pos+1);
	return returnString;
}


//  GetNextInt
//
int Parser::GetNextInt()
{
	string getIntString = GetNextString();

	//  TODO:  Room for Improvement
	//  this is not very fault tolerant, atoi will cause undefined behavior if string can not be parsed to int
	return atoi(getIntString.c_str());
}


//  GetRemainingBuffer
//
string Parser::GetRemainingBuffer()
{
	return mRemainingBuffer;
}





//  this function is useful to parse an entire string into vector of substrings at once
//void Tokenize(const string& str, vector<string>& tokens, const string& delimiters /*= " "*/)
//{
//    // Skip delimiters at beginning.
//    string::size_type lastPos = str.find_first_not_of(delimiters, 0);
//    // Find first "non-delimiter".
//    string::size_type pos     = str.find_first_of(delimiters, lastPos);
//
//    while (string::npos != pos || string::npos != lastPos)
//    {
//        // Found a token, add it to the vector.
//        tokens.push_back(str.substr(lastPos, pos - lastPos));
//        // Skip delimiters.  Note the "not_of"
//        lastPos = str.find_first_not_of(delimiters, pos);
//        // Find next "non-delimiter"
//        pos = str.find_first_of(delimiters, lastPos);
//    }
//}