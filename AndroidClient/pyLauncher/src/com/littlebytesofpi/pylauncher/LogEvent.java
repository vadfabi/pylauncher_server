package com.littlebytesofpi.pylauncher;

import java.text.SimpleDateFormat;
import java.util.Date;


public class LogEvent {
	
	public long EventSystemTime;
	public String EventType;
	public String Event;
	public String IpAddressOfSender;
	
	public LogEvent(long systemTime){
		EventSystemTime = systemTime;
		EventType = "";
		Event = "";
		IpAddressOfSender = "";
	}
	
	public LogEvent(long systemTime, String eventType, String event){
		EventSystemTime = systemTime;
		EventType = eventType;
		Event = event;
		
		IpAddressOfSender = "";
	}

	public String toString(){
		
		String returnString = String.format("Time: " + formatTime() + "\nType: " + EventType + "\nFrom: " + IpAddressOfSender + "\n>  " + Event);
		return returnString;
	}
	
	public String formatTime(){
		
		Date date = new Date();
		date.setTime(EventSystemTime);
		SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss.SSS a");//08:30 am
		return timeFormatter.format(date);
	}
}
