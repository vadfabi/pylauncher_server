#ifndef _PARSER_H
#define _PARSER_H

//#include <stdlib.h>
#include <iostream>
#include <vector>
//#include <string>
//#include <cstring>

using namespace std;


char *get_line (char *s, size_t n, FILE *f);



void Tokenize(const string& str, vector<string>& tokens, const string& delimiters = " ");



class Parser
{
public:

	Parser(string buffer, string delimiters);
	virtual ~Parser();

	void SetBuffer(string buffer);
	void SetBuffer(string buffer, string delimiters);

	string mBuffer;
	string mDelimiters;

	vector<string> mTokens;

	string GetNextString();


};

#endif