package com.littlebytesofpi.tcpconnect;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;


public class ClientsServerThread extends Thread {

	TCPConnectService mService;

	public ClientsServerThread(TCPConnectService service) {
		mService = service;
	}

	/**
	 * Connected control thread 
	 * 
	 * This thread is running while we are connected
	 * this is the client's ServerSocket accept thread, listening for control messages from the server
	 *  
	 */
	private ServerSocket mServerSocket = null; 
	private Socket mSocket = null;

	public int getClientListeningOnPort(){  
		if ( mServerSocket != null )
			return mServerSocket.getLocalPort();
		else
			return -1;
	}

	public boolean OpenSocketConnection(){

		//  determine our client port
		int clientsServerPort = 50000;
		mServerSocket = null;
		while ( mServerSocket == null )
		{
			//  setup a socket connection for the client end of the control connection
			try {
				mServerSocket = new ServerSocket(clientsServerPort);

			} catch (IOException e) {
				clientsServerPort++;
			}

			if ( clientsServerPort > 51000 )
				return false;		//  No open port in 1000? must be a problem
		}

		
		return true;
	}


	public boolean IsConnected(){


		return (mThreadRunning  && mServerSocket != null );
	}

	private boolean mThreadRunning = false;
	private boolean mThreadExit = false;

	public void run() {

		//  we must have a valid socket to start
		if ( mServerSocket == null )
			return;
		
		mThreadRunning = true;

		// Keep listening to the InputStream while connected
		while (mThreadRunning) {

			try {

				//  wait to accept something on this socket
				//  this will block until we have something to read
				mSocket = null;
				mSocket = mServerSocket.accept();

				//  we got something, process it
				ProcessSocketAccept();

			} 
			catch (IOException e) {
				break;	//  thread was stopped on socket close
			} 
			finally{ 

				try{
					if (mSocket != null)
						mSocket.close();
				}
				catch(IOException e){
					Log.e(TAG, "Exception in Connected Control Thread: " + e.toString());
				}
			}
		}

		mThreadRunning = false;
		mThreadExit = true;
	}

	public void cancel() {

		mThreadRunning = false;

		if ( mServerSocket != null )
		{
			try{
				//  close the socket
				mServerSocket.close();

				//  now wait for thread run to exit
				while ( ! mThreadExit )
					Sleep(100);

			} catch(IOException e){
				Log.e(TAG, "Exception in Connected Control Thread: " + e.toString());
			} finally {
				mServerSocket = null;
			}
		}
	}



	/**
	 * ProcessSocketAccept
	 * 
	 * Gets the input stream from the socket and does something with the command
	 * Sends response to the output stream
	 */
	private void ProcessSocketAccept(){

		DataInputStream dataInputStream = null;
		DataOutputStream dataOutputStream = null;

		try{

			dataInputStream = new DataInputStream(mSocket.getInputStream());
			dataOutputStream = new DataOutputStream(mSocket.getOutputStream());
			//String input = dataInputStream.readUTF();

			//  todo - rewrite into loop to handle reads larger than 1024
			byte[] buffer = new byte[1024];
			int readCount  = dataInputStream.read(buffer);
			String input = new String( buffer ).trim();

			dataOutputStream.writeBytes(input);
			dataOutputStream.flush();
			
			//  extract the command and arguments
			Parser parser = new Parser(input, ",");
			String command = parser.GetNextString();
			String arguments = parser.GetRemainingBuffer();

			//  log this event at reception time
			LogEvent logEvent = new LogEvent(System.currentTimeMillis(), command, arguments);
			logEvent.mIpAddressOfSender = mSocket.getInetAddress().toString().substring(1);
			mService.AddLogEvent(logEvent);


		} catch(IOException e){
			Log.e(TAG, "Exception in ProcessControlInput:" + e.toString());
		}
		finally{
			try{
				if (dataOutputStream != null)
					dataOutputStream.close();

				if (dataInputStream != null)
					dataInputStream.close();

			} catch(IOException e){
				Log.e(TAG, "Exception in ProcessControlInput:" + e.toString());
			}
		}
	}


	//  Thread Sleep
	private void Sleep(long millis){
		try{
			Thread.sleep(millis);
		} catch (InterruptedException e){}
	}

	//  debug flags
	private final boolean D = true;
	private final String TAG = "ClientsServerThread";

}
