package com.littlebytesofpi.pylauncher;

import java.io.File;
import java.util.ArrayList;

public class PyLaunchResult {

	public String mPathToPyFile = "";
	public String mIpOfRequest = "";
	public String mTimeRequest = "";
	public String mTimeLaunch = "";
	public String mTimeComplete = "";
	
	ArrayList<String> mResults = new ArrayList<String>();
	
	public PyLaunchResult(String pathToPyFile, String ipOfRequest, String timeRequest, String timeLaunch, String timeComplete){
		
		mPathToPyFile = pathToPyFile;
		mIpOfRequest = ipOfRequest;
		mTimeRequest = timeRequest;
		mTimeLaunch = timeLaunch;
		mTimeComplete = timeComplete;
	}
	
	//  user interface flags
	boolean mExpanded = true;
	
	public String GetFileName()
	{
		return new File(mPathToPyFile).getName();
	
	}

}
