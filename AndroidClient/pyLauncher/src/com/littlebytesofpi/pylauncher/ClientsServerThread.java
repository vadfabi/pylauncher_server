package com.littlebytesofpi.pylauncher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;


public class ClientsServerThread extends Thread {

	/*
	 * ClientsServerThread 
	 * 
	 * This thread is running while we are connected
	 * this is the client's ServerSocket accept thread, listening for control messages from the server
	 *  
	 */

	//  ClientServerThread
	//  Constructor
	//
	public ClientsServerThread(PyLauncherService service) {
		mService = service;
	}

	//  reference to the service
	private PyLauncherService mService;



	/*
	 * Socket Port Handling
	 */

	private ServerSocket mServerSocket = null; 
	private Socket mSocket = null;

	//  GetClientListeningOnPort
	//  return port number that client is listening on (client's server port)
	//
	public int GetClientListeningOnPort(){  
		if ( mServerSocket != null )
			return mServerSocket.getLocalPort();
		else
			return -1;
	}


	//  IsConnected
	//  returns connection to server state
	public boolean IsConnected(){
		return (mThreadRunning  && mServerSocket != null );
	}




	//  OpenSocketConnection
	//
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



	//  Thread flags
	//  low tech, but effective to manage state of running thread
	private boolean mThreadRunning = false;
	private boolean mThreadExit = false;

	
	//  Thread run function
	//
	public void run() {

		//  we must have a valid socket to start
		if ( mServerSocket == null )
			return;

		//  infinite loop while thread is running
		mThreadRunning = true;
		//
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

	
	//  Thread cancel function
	//
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



	//  ProcessSocketAccept
	//  Gets the input stream from the socket and does something with the command
	//  Sends response to the output stream
	//
	private void ProcessSocketAccept(){

		DataInputStream dataInputStream = null;
		DataOutputStream dataOutputStream = null;

		try{
			
			//  setup socket read and write streams
			mSocket.setSoTimeout(2000);
			dataInputStream = new DataInputStream(mSocket.getInputStream());
			dataOutputStream = new DataOutputStream(mSocket.getOutputStream());

			//  read what was sent on the socket
			String inputRead = IpFunctions.ReadStringFromInputSocket(dataInputStream);

			String response = "";

			//  time tag for this received event
			long timeOfEvent = System.currentTimeMillis();
			
			//  parse the read string into individual event strings, separated by newline
			Parser inputParser = new Parser(inputRead, "\n");
			String nextMessage = inputParser.GetNextString();
			while ( nextMessage.length() > 0 )
			{
				response += "$TPC_RECEIVED," + nextMessage + "\n";

				//  log the event
				Parser nextMessageParser = new Parser(nextMessage, ",");
				String command = nextMessageParser.GetNextString();
				String arguments = nextMessageParser.GetRemainingBuffer();
				//
				LogEvent logEvent = new LogEvent(timeOfEvent, command, arguments);
				logEvent.mIpAddressOfSender = mSocket.getInetAddress().toString().substring(1);
				mService.AddLogEvent(logEvent);

				nextMessage = inputParser.GetNextString();
			}

			//  write the response
			dataOutputStream.writeBytes(response);
			dataOutputStream.flush();


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
