#ifndef _CMDIFCONFIG_H
#define _CMDIFCONFIG_H

#include <string>

#include "CMD.h"

using namespace std;


//  ConnectionInfo
//  container class for internet information
//  TODO:  expand this to support ipv6 addresses
class ConnectionInfo
{
public:

	string mType;
	string mInet4Address;

	ConnectionInfo()
	{
		mType = "";
		mInet4Address = "";
	}
};

// CMDifconfig
// 
class CMDifconfig : public CMD
{
public:
	CMDifconfig();

	virtual ~CMDifconfig();

	//  override base class CMD::Parse so we can parse internet address out of system response
	virtual bool Parse();

	ConnectionInfo mEth0Info;
	ConnectionInfo mLoopbackInfo;
	ConnectionInfo mWlanInfo;
};


#endif