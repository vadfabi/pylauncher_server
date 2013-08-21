package com.littlebytesofpi.tcpconnect;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class IpFunctions {

	static public String sendStringToPort(String ipAddress, int portNumber, String message){

		String response = "";

		Socket socket = null;
		DataOutputStream dataOutputStream = null;
		DataInputStream dataInputStream = null;

		try {
			socket = new Socket(ipAddress, portNumber);
			socket.setSoTimeout(5000);		//  TODO:  I have big problems with this on slow wifi network, must find proper method (timeout / retries ?)
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			dataInputStream = new DataInputStream(socket.getInputStream());
			dataOutputStream.writeBytes(message);
			dataOutputStream.flush();

			byte[] buffer = new byte[1024];

			int readCount  = dataInputStream.read(buffer);
			response = new String( buffer ).trim();
			return response;

		} catch ( SocketTimeoutException e ){
			
		} catch (UnknownHostException e) {
			if (D) Log.e(TAG, "Exception in sendStringToPort " + e.toString());
			return response;
		} catch (IOException e) {
			if (D) Log.e(TAG, "Exception in sendStringToPort " + e.toString());
			return response;
		} finally{
			try{
				if (socket != null)
					socket.close();

				if (dataOutputStream != null)
					dataOutputStream.close();

				if (dataInputStream != null)
					dataInputStream.close();
			}
			catch(IOException e){
				if (D) Log.e(TAG, "Exception in sendStringToPort finally " + e.toString());
			}
		}


		return response;

	}


	static public String getLocalIpAddress(WifiManager wifiManager) {

		try{
			
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();

			String ip = intToIp(ipAddress);
			return ip;


		} catch (Exception e){
			Log.e(TAG, "Exception in getLocalIpAddress: " + e.toString());
		}

		return "";

	}

	static public String intToIp(int i) {

		return ( i & 0xFF)  + "." +
				((i >> 8 ) & 0xFF) + "." +
				((i >> 16 ) & 0xFF) + "." +    
				((i >> 24 ) & 0xFF ) ;
	}
	
	
	//  debug flags
	private static final boolean D = true;
	private static final String TAG = "IpFunctions";
}
