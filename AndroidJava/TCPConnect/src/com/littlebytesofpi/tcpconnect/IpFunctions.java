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

	static public final int TCP_READBUFFER_SIZE = 1024;
	
	/*
	 * SendStringToPort
	 * sends the string 'message' to the specified port number and address
	 * returns the response from the port, will return empty string if no response
	 */
	static public String SendStringToPort(String ipAddress, int portNumber, String message){

		String response = "";

		Socket socket = null;
		DataOutputStream dataOutputStream = null;
		DataInputStream dataInputStream = null;

		try {
			socket = new Socket(ipAddress, portNumber);
			socket.setSoTimeout(5000);		//  TODO:  remove hard coded wait time, pass in wait time as parameter
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			dataInputStream = new DataInputStream(socket.getInputStream());
			
			dataOutputStream.writeBytes(message);
			dataOutputStream.flush();

			response = ReadStringFromInputSocket(dataInputStream);

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
	
	
	/*
	 * ReadStringFromInputSocket
	 * reads a string from the socket
	 */
	static String ReadStringFromInputSocket(DataInputStream dataInputStream ) throws IOException 
	{
		String inputRead = "";
		
		//  read the buffer, add one byte to make sure buffer is null at end
		byte[] buffer = new byte[TCP_READBUFFER_SIZE+1];

		//  read until we have reached end of file
		try{

			int readCount  = dataInputStream.read(buffer, 0, TCP_READBUFFER_SIZE);
			inputRead = new String(buffer).trim();

			//  did we read max buffer, if so keep going
			if ( readCount == TCP_READBUFFER_SIZE )
			{
				while ( readCount > 0 )
				{
					//  reinit the buffer, this is the most efficient way to zero out the memory in java ?
					buffer = new byte[TCP_READBUFFER_SIZE+1];
					readCount = dataInputStream.read(buffer, 0, TCP_READBUFFER_SIZE);
					inputRead += new String(buffer).trim();	
				}
			}
		}
		catch (IOException e){
			throw e;
		}

		return inputRead;
	}


	/*
	 * GetLocalIpAddress
	 * gets the ip address of the android device
	 */
	static public String GetLocalIpAddress(WifiManager wifiManager) {

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
