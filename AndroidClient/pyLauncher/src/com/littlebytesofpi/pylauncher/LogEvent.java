package com.littlebytesofpi.pylauncher;

import java.text.SimpleDateFormat;
import java.util.Date;


public class LogEvent {
	
	public long mEventSystemTime;
	public String mEventType;
	public String mEvent;
	public String mIpAddressOfSender;
	
	public LogEvent(long systemTime){
		mEventSystemTime = systemTime;
		mEventType = "";
		mEvent = "";
		mIpAddressOfSender = "";
	}
	
	public LogEvent(long systemTime, String eventType, String event){
		mEventSystemTime = systemTime;
		mEventType = eventType;
		mEvent = event;
		
		mIpAddressOfSender = "";
	}

	public String toString(){
		
		String returnString = String.format("Time: " + formatTime() + "\nType: " + mEventType + "\nFrom: " + mIpAddressOfSender + "\n>  " + mEvent);
		return returnString;
	}
	
	public String formatTime(){
		
		Date date = new Date();
		date.setTime(mEventSystemTime);
		SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss.SSS a");//08:30 am
		return timeFormatter.format(date);
	}
}
