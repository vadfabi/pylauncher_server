#ifndef _CMDIFCONFIG_H
#define _CMDIFCONFIG_H

#include <string>

#include "CMD.h"



//  ConnectionInfo
//  container class for internet information
//
class ConnectionInfo
{
public:

	//  TODO:  expand this to support ipv6 addresses
	std::string mType;
	std::string mInet4Address;

	ConnectionInfo()
	{
		mType = "";
		mInet4Address = "";
	}
};



// CMDifconfig
// Command "ifconfig"
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