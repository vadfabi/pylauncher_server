#ifndef _CMDIFCONFIG_H
#define _CMDIFCONFIG_H

#include <string>

#include "CMD.h"

// CMDifconfig
// 

class ConnectionInfo
{
public:

	std::string mType;
	std::string mInet4Address;

	ConnectionInfo()
	{
		mType = "";
		mInet4Address = "";
	}
};


class CMDifconfig : public CMD
{
public:
	CMDifconfig();
	virtual ~CMDifconfig();

	virtual bool Parse();


	ConnectionInfo mEth0Info;
	ConnectionInfo mLoopbackInfo;
	ConnectionInfo mWlanInfo;

};


#endif