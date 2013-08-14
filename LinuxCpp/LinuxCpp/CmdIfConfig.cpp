#include "CmdIfConfig.h"
#include "Parser.h"

CMDifconfig::CMDifconfig()
{
	Command = "ifconfig";

}

CMDifconfig::~CMDifconfig()
{

}


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
			else if ( getString == "wlan" )
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



//if ( lsofFile_p != 0 )
//	{
//		char buffer[1024];
//		char *line_p = fgets(buffer, sizeof(buffer), lsofFile_p);
//		while ( line_p )
//		{
//			printf(line_p);
//			fflush(stdout);
//
//			string stringLine = line_p;
//
//			Parser parser(stringLine, " ");
//
//			string nextString = parser.GetNextString();
//			while ( nextString.size() != 0 )
//			{
//				const char* stringBuffer = nextString.c_str();
//				printf("%s ", stringBuffer);
//				fflush(stdout);
//				nextString = parser.GetNextString();
//			}
//
//			line_p = fgets(buffer, sizeof(buffer), lsofFile_p);
//		}
//
//		 pclose(lsofFile_p);
//	}

//
//const char* stringBuffer = nextString.c_str();
//			printf("%s ", stringBuffer);
//			fflush(stdout);
//			nextString = parser.GetNextString();