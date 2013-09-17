package com.littlebytesofpi.pylauncher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;


public class ClientsServer extends Thread {

	/*
	 * ClientsServer 
	 * 
	 * This thread is running while we are connected
	 * this is the client's ServerSocket accept thread, listening for control messages from the server
	 *  
	 */

	
	/*
	 * Client <=> Server Commands
	 */
	public static final String TCP_CONNECT = "$TCP_CONNECT";
	public static final String TCP_DISCONNECT = "$TCP_DISCONNECT";
	public static final String TCP_LISTDIR = "$TCP_LISTDIR";
	public static final String TCP_LISTFILES = "$TCP_LISTFILES";
	public static final String TCP_PYLAUNCH = "$TCP_PYLAUNCH";
	public static final String TCP_PYRESULT = "$TCP_PYRESULT";
	public static final String TCP_CLIENTACCEPT = "$TCP_CLIENTACCEPT";
	public static final String TCP_REMOVEDIR = "$TCP_REMOVEDIR";
	public static final String TCP_ADDDIR = "$TCP_ADDDIR";
	public static final String TCP_ACK = "ACK";
	

	
	//  reference to the service
	private PyLauncherService mService;

	
	//  ClientServerThread
	//  Constructor
	//
	public ClientsServer(PyLauncherService service) {
		mService = service;
	}

	
	
	//  Socket
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
			mSocket.setSoTimeout(5000);
			dataInputStream = new DataInputStream(mSocket.getInputStream());
			dataOutputStream = new DataOutputStream(mSocket.getOutputStream());

			//  read what was sent on the socket
			String inputRead = IpFunctions.ReadStringFromInputSocket(dataInputStream);

			//  write the response right away in case server is waiting to read
			String response = TCP_CLIENTACCEPT + "," + TCP_ACK;
			dataOutputStream.writeBytes(response);
			dataOutputStream.flush();
			
			//  and close the socket so the server can get back to its business
			dataOutputStream.close();
			dataOutputStream = null;
			dataInputStream.close();
			dataInputStream = null;

			//  Process this input
			long timeOfEvent = System.currentTimeMillis();
			
			//  parse the read string into individual event strings, separated by newline
			Parser inputParser = new Parser(inputRead, "\n");
			String nextMessage = inputParser.GetNextString();
			while ( nextMessage.length() > 0 )
			{
				if ( nextMessage.contains(TCP_PYRESULT) )
				{
					//  $TCP_PYRESULT,pyFileName,ipOfRequester,timeOfRequest,timeOfLaunch,timeComplete,outputLine1,outputLine2,...
					Parser nextMessageParser = new Parser(nextMessage, ",");
					String command = nextMessageParser.GetNextString();		// $TCP_PYRESULT

					String fileName = nextMessageParser.GetNextString();		//  full path of python file launched
					String ipOfRequest = nextMessageParser.GetNextString();		//  ip address of client requesting launch
					String timeRequest = nextMessageParser.GetNextString();		//  time launch request was received at server
					String timeLaunch = nextMessageParser.GetNextString();		//  time launch was started by server
					String timeComplete = nextMessageParser.GetNextString();	//  time pyton script was completed

					//  create an object to hold the info about this result
					PyLaunchResult lanuchResult = new PyLaunchResult(fileName, ipOfRequest, timeRequest, timeLaunch, timeComplete);

					//  now get outputLine1 to outputLine_n
					String result = nextMessageParser.GetNextString();
					while ( result.length() != 0 )
					{
						lanuchResult.mResults.add(result);
						result = nextMessageParser.GetNextString();
					}

					mService.AddLaunchResult(lanuchResult);
					
				}
				else if ( nextMessage.contains(TCP_LISTDIR) )
				{
					if ( mService.ParseListDir(nextMessage) )
						mService.SendMessage(PyLauncherService.MESSAGE_UPDATEDIRECTORIES);
				}
				else if ( nextMessage.contains(TCP_LISTFILES) )
				{
					if ( mService.ParseListFiles(nextMessage) )
						mService.SendMessage(PyLauncherService.MESSAGE_UPDATEDIRECTORIES);
				}

				nextMessage = inputParser.GetNextString();
			}

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
	private final String TAG = "ClientsServer";

}
