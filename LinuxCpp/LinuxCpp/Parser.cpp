//#include <stdlib.h>

#include <cstring>
//#include <iostream>
//#include <sstream>
//#include <cstdio>
//#include <thread>
//#include <string>


#include "Parser.h"


using namespace std;


char *get_line (char *s, size_t n, FILE *f)
{
  char *p = fgets (s, n, f);

  if (p != 0) {
    size_t last = strlen (s) - 1;

    if (s[last] == '\n') s[last] = '\0';
  }
  return p;
}


void Tokenize(const string& str, vector<string>& tokens, const string& delimiters /*= " "*/)
{
    // Skip delimiters at beginning.
    string::size_type lastPos = str.find_first_not_of(delimiters, 0);
    // Find first "non-delimiter".
    string::size_type pos     = str.find_first_of(delimiters, lastPos);

    while (string::npos != pos || string::npos != lastPos)
    {
        // Found a token, add it to the vector.
        tokens.push_back(str.substr(lastPos, pos - lastPos));
        // Skip delimiters.  Note the "not_of"
        lastPos = str.find_first_not_of(delimiters, pos);
        // Find next "non-delimiter"
        pos = str.find_first_of(delimiters, lastPos);
    }
}


Parser::Parser(string buffer, string delimiters)
{
	//  set the string and delimiters
	mBuffer = buffer;
	mDelimiters = delimiters;

	//  parse the string into tokens
	Tokenize(mBuffer, mTokens, mDelimiters);
}

Parser::~Parser()
{
}

void Parser::SetBuffer(string buffer)
{
	//  clear out the old
	mTokens.clear();
	
	// set the new
	mBuffer = buffer;

	//  parse the buffer into the tokens
	Tokenize(mBuffer, mTokens, mDelimiters);
}

void Parser::SetBuffer(string buffer, string delimiters)
{
	mDelimiters = delimiters;
	SetBuffer(buffer);
}

string Parser::GetNextString()
{
	if ( mTokens.size() == 0 )
		return "";

	string nextString = mTokens.front();
	mTokens.erase(mTokens.begin());
	return nextString;
}


//  iterator example
//for(std::vector<string>::iterator it = tokens.begin(); it != tokens.end(); ++it)
//			{
//				 std::cout << *it;
//			}