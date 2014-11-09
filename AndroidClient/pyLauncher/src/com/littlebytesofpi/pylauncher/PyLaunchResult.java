package com.littlebytesofpi.pylauncher;

import java.io.File;
import java.util.ArrayList;

public class PyLaunchResult {

	public String PathToPyFile = "";
	public String IpOfRequest = "";
	public String TimeRequest = "";
	public String TimeLaunch = "";
	public String TimeComplete = "";
	
	ArrayList<String> mResults = new ArrayList<String>();
	
	public PyLaunchResult(String pathToPyFile, String ipOfRequest, String timeRequest, String timeLaunch, String timeComplete){
		
		PathToPyFile = pathToPyFile;
		IpOfRequest = ipOfRequest;
		TimeRequest = timeRequest;
		TimeLaunch = timeLaunch;
		TimeComplete = timeComplete;
	}
	
	//  user interface flags
	boolean mExpanded = true;
	
	public String GetFileName()
	{
		return new File(PathToPyFile).getName();
	
	}

}
