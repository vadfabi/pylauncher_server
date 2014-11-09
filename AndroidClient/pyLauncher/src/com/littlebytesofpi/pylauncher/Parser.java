package com.littlebytesofpi.pylauncher;

public class Parser {

	private String OriginalString = "";
	private String StringBuffer = "";
	private String mDelimiter = "";
	
	public Parser(String string, String delimiter){
	
		OriginalString = string;
		StringBuffer = string;
		mDelimiter = delimiter;
	}
	
	String GetNextString(){
		
		String returnString = "";
		int index = StringBuffer.indexOf(mDelimiter);
		
		if ( index < 1 )
		{
			returnString = StringBuffer;
			StringBuffer = "";
			return returnString;
		}
			
		returnString = StringBuffer.substring(0, index);
		StringBuffer = StringBuffer.substring(index+1);
		return returnString;
	}
	
	String GetRemainingBuffer(){
		return StringBuffer;
	}
}
