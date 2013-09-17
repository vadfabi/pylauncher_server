#include "CMDifconfig.h"
#include "Parser.h"

using namespace std;


/////////////////////////////////////////////////////////////////////////////
//  CMDifconfig
//  Command "ifconfig"
//

//  Constructor
//
CMDifconfig::CMDifconfig()
{
	Command = "ifconfig";

}

CMDifconfig::~CMDifconfig()
{
}


//  Parse
//  override of base class CMD::Parse to extract specific bits of the ifconfig response
//
bool CMDifconfig::Parse()
{
	Parser parser("", "");

	for ( int i = 0; i < CommandResponse.size(); i++ )
	{

		string nextString = CommandResponse[i];


		parser.SetBuffer(nextString, " ");

		string getString = parser.GetNextString();
		while ( getString.size() != 0 )
		{
			if ( getString == "eth0" )
			{
				mEth0Info.mType = getString;

				//  parse the wired connection

				nextString = CommandResponse[++i];
				parser.SetBuffer(nextString, " :");

				//  discard inet addr:
				getString = parser.GetNextString();
				getString = parser.GetNextString();

				//  parse the IP address
				mEth0Info.mInet4Address = parser.GetNextString();

				//  done with this connection
				break;
			}
			else if ( getString == "lo" )
			{
				mLoopbackInfo.mType = getString;

				//  parse the wired connection
				nextString = CommandResponse[++i];
				parser.SetBuffer(nextString, " :");

				//  discard inet addr:
				getString = parser.GetNextString();
				getString = parser.GetNextString();

				//  parse the IP address
				mLoopbackInfo.mInet4Address = parser.GetNextString();

				//  done with this connection
				break;
			}
			else if ( getString == "wlan0" )
			{
				mWlanInfo.mType = getString;

				//  parse the wired connection
				nextString = CommandResponse[++i];
				parser.SetBuffer(nextString, " :");

				//  discard inet addr:
				getString = parser.GetNextString();
				getString = parser.GetNextString();

				//  parse the IP address
				mWlanInfo.mInet4Address = parser.GetNextString();

				//  done with this connection
				break;
			}
			else
			{
				break;
			}
		}
	}


	return true;
}

