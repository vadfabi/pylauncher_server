package com.littlebytesofpi.tcpipsockets;

public class Parser {

	private String mString = "";
	private String mStringBuffer = "";
	private String mDelimiter = "";
	
	public Parser(String string, String delimiter){
	
		mString = string;
		mStringBuffer = string;
		mDelimiter = delimiter;
	}
	
	String GetNextString(){
		
		String returnString = "";
		int index = mStringBuffer.indexOf(mDelimiter);
		
		if ( index < 1 )
		{
			returnString = mStringBuffer;
			mStringBuffer = "";
			return returnString;
		}
			
		returnString = mStringBuffer.substring(0, index);
		mStringBuffer = mStringBuffer.substring(index+1);
		return returnString;
	}
	
	String GetRemainingBuffer(){
		return mStringBuffer;
	}
}
