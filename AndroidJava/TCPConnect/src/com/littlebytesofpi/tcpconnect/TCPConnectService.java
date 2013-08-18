package com.littlebytesofpi.tcpconnect;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class TCPConnectService extends Service {

	

	/*
	 * Messages
	 */
	public static final int MESSAGE_NETSTATECHANGE = 0;
	public static final int MESSAGE_CONNECTEDSTATECHANGE = 1;
	





	/**
	 * TCPConnectService 
	 * 
	 * Constructor
	 */
	public TCPConnectService() {

		//  This service will monitor network status
		mNetworkStateChangedFilter = new IntentFilter();
		mNetworkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		//
		mNetworkStateIntentReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
					HandleNetworkStatusChange();
				}
			}
		};

	}


	/*
	 * Service Binding and Messaging Functions
	 * 
	 */
	private  ArrayList<Handler> mHandlerList = new ArrayList<Handler>(); 
	public synchronized void AddHandler(Handler handler){
		if ( mHandlerList.contains(handler) )
			return;
		mHandlerList.add(handler);

	}

	public synchronized void RemoveHandler(Handler handler){
		mHandlerList.remove(handler);
	}


	//  for binding with the service
	public class LocalBinder extends Binder {
		TCPConnectService getService() {
			return TCPConnectService.this;
		}
	}

	private final LocalBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent arg0) {

		registerReceiver(mNetworkStateIntentReceiver, mNetworkStateChangedFilter);

		SetNetworkStatus();

		showNotification();

		return mBinder;
	}

	private final int NOTIFICATION_ID = 99;
	private NotificationManager mNM;




	public void showNotification() {

		CharSequence text = "Lithium Client";

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher, text,	System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = null;
		String description;
		
		PendingIntent.getActivity(this, 0, new Intent(this, TcpConnect.class), 0);

		//  todo - specify the notification text based on current state
		notification.setLatestEventInfo(this, text, "Notify", contentIntent);

		//  set this as a persistent notification
		notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

		//  specify that the notification should be in the foreground
		startForeground(NOTIFICATION_ID, notification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		//  register for network state change messages
		registerReceiver(mNetworkStateIntentReceiver, mNetworkStateChangedFilter);

		showNotification();

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	//  send message to registered handler
	public synchronized void SendMessage(int message)
	{
		for ( Handler handler : mHandlerList )
		{
			Message messageToSend = Message.obtain(null, message);

			messageToSend.setTarget(handler);
			messageToSend.sendToTarget();
		}
	}

	public synchronized void SendMessage(int message, int arg1)
	{
		for ( Handler handler : mHandlerList )
		{
			Message messageToSend = Message.obtain(null, message, arg1, 0);

			messageToSend.setTarget(handler);
			messageToSend.sendToTarget();
		}
	}

	@Override
	public void onDestroy() {

		if ( mConnectedControlThread != null )
			mConnectedControlThread.cancel();

		super.onDestroy();
	}



	/*
	 *  IP Connection to the server
	 *  
	 *  
	 */

	//  server connect port is hard coded for now
	//  TODO:  this is to be replaced with zero config networking or maybe something like RTP / SIP handling
	private final int SERVER_CONNECT_PORT = 48888;

	//  ip address of the server we are connected to
	private String mConnectedToServerIp = "";
	public String getConnectedToServerIp(){
		return mConnectedToServerIp;
	}

	//  when connected, the server opens a socket connection on this port to receive our control commands
	private int mConnectedToServerControlOnPort = 0; 
	public int getConnectedToServerControlOnPort(){
		return mConnectedToServerControlOnPort;
	}

	//  when off the hook, the server opens up a UDP connection on this port connection to receive PCM data for phone line out
	private int mConnectedToServerLineOnPort = 0;
	public int getConnectedToServerLineOnPort(){
		return mConnectedToServerLineOnPort;
	}

	//  when connected we open a socket connection on this port for the server to send us control commands 
	public int getClientControlPort(){  
		if ( mConnectedControlServerSocket != null )
			return mConnectedControlServerSocket.getLocalPort();
		else
			return -1;
	}



	//  when off the hook, the server will specify the length of UDP data blobs it will send us
	private int mServerUdpDataLength;
	public int getServerUdpDataLength(){
		return mServerUdpDataLength;
	}


	//  return connection state
	public synchronized boolean IsConnectedToServer()
	{
		//  we are connected when control thread is running and control socket is open
		return (mConnectedControlThread != null && mConnectedControlServerSocket != null );
	}



	/**
	 * OpenConnectionToServer
	 * 
	 * Launches task to open a connection to the server
	 * 
	 */
	public void openConnectionToServer()
	{
		//  close previous connection to the server if it is open
		if ( mConnectedControlThread != null )
			mConnectedControlThread.cancel();
		mConnectedControlServerSocket = null;

		//  get the ip address to connect to
		//  TODO:  This is to be replaced with automatic connection
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String connectPort = sharedPrefs.getString("pref_server_ip_address", "192.168.0.101");

		//  launch the connection task
		new OpenConnectionTask().execute(connectPort);
	}

	//  open connection task
	private class OpenConnectionTask extends AsyncTask<String, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			//  determine our client port
			int clientsControlPort = 50000;
			mConnectedControlServerSocket = null;
			while ( mConnectedControlServerSocket == null )
			{
				//  setup a socket connection for the client end of the control connection
				try {
					mConnectedControlServerSocket = new ServerSocket(clientsControlPort);

				} catch (IOException e) {
					clientsControlPort++;
				}

				if ( clientsControlPort > 51000 )
					return 0;		//  No open port in 1000? must be a problem
			}

			//  connect to server command:
			// $TPC_CONNECT,clientsIpAddress,clientsControlPort
			mConnectedToServerIp = param[0];
			readResponse = sendStringToPort(mConnectedToServerIp, SERVER_CONNECT_PORT, "$TPC_CONNECT," +mIpWiFiAddress + "," +getClientControlPort());
			return 1;
		}

		//  Post Execute
		protected void onPostExecute(Integer result ) {

			if ( result == 1 && readResponse.contains("$TPC_CONNECT,ACK") )
			{
				//  parse response:  
				//  $TPC_CONNECT,ACK,serversControlPort
				Parser parser = new Parser(readResponse, ",");

				String command = parser.GetNextString();
				String pass = parser.GetNextString();
				String serversControlPort = parser.GetNextString();
				//String xtra = parser.GetNextString();

				try{
					mConnectedToServerControlOnPort =  Integer.parseInt(serversControlPort);
				} catch (NumberFormatException e){
					Toast.makeText(TCPConnectService.this, "Error starting connection, unable to parse server control port number!", Toast.LENGTH_SHORT).show();
					return;
				}

				if ( mConnectedControlServerSocket == null )
				{
					//  uncaught error above ?
					Toast.makeText(TCPConnectService.this, "Error starting connection, failed to start client control socket!", Toast.LENGTH_SHORT).show();
					return;
				}

				//  start the connection thread
				mConnectedControlThread = new ConnectedControlThread();
				mConnectedControlThread.start();

				
				showNotification();

				//  message success
				TCPConnectService.this.SendMessage(MESSAGE_CONNECTEDSTATECHANGE);

			}
			else
			{
				Toast.makeText(TCPConnectService.this, "Failed to open server connection!", Toast.LENGTH_SHORT).show();
				mConnectedToServerIp = "";
			}
		}
	}


	/**
	 * CloseConnectionToServer
	 * 
	 * Launches task to close the server connection.
	 * 
	 */
	public void closeConnectionToServer(){

		if ( mConnectedControlServerSocket == null || mConnectedControlThread == null )
			return; //  closed already

		//  launch the close connection task
		new CloseConnectionTask().execute();
	}

	//  close connection task
	private class CloseConnectionTask extends AsyncTask<String, Void, Integer> {


		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			readResponse = TCPConnectService.this.sendStringToConnectedOnCommandPort("$TPC_DISCONNECT");

			return 1;
		}

		protected void onPostExecute(Integer result ) {

			
			//  shut down the server thread
			if ( mConnectedControlThread != null )
				mConnectedControlThread.cancel();
			mConnectedControlThread = null;

			
			if ( readResponse.contains("$TPC_DISCONNECT,ACK") )
				Toast.makeText(TCPConnectService.this, "Server closed connection.", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(TCPConnectService.this, "Error closing server connection", Toast.LENGTH_SHORT).show();

			

			showNotification();

			//  update UI
			TCPConnectService.this.SendMessage(MESSAGE_CONNECTEDSTATECHANGE);	
		}
	}


	/*
	 * Connected control thread 
	 * 
	 * This thread is running while we are connected
	 * this is the client's ServerSocket accept thread, listening for control messages from the server
	 *  
	 */
	private ServerSocket mConnectedControlServerSocket = null; 
	private Socket mControlSocket = null;

	private ConnectedControlThread mConnectedControlThread = null;  
	private class ConnectedControlThread extends Thread {  

		private boolean mThreadRunning = false;
		private boolean mThreadExit = false;

		public void run() {

			mThreadRunning = true;

			// Keep listening to the InputStream while connected
			while (mThreadRunning) {

				try {
					//  read from the socket, we will block here
					mControlSocket = null;
					mControlSocket = mConnectedControlServerSocket.accept();

					//  process the input
					ProcessControlInput();

				} 
				catch (IOException e) {
					break;	//  thread was stopped on socket close
				} 
				finally{

					try{
						if (mControlSocket != null)
							mControlSocket.close();
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

			if ( mConnectedControlServerSocket != null )
			{
				try{
					//  close the socket
					mConnectedControlServerSocket.close();

					//  now wait for thread run to exit
					while ( ! mThreadExit )
						Sleep(100);

				} catch(IOException e){
					Log.e(TAG, "Exception in Connected Control Thread: " + e.toString());
				} finally {
					mConnectedControlServerSocket = null;
				}
			}
		}
	}


	/**
	 * ProcessControlInput
	 * 
	 * Gets the input stream from the socket and does something with the command
	 * Sends response to the output stream
	 */
	private void ProcessControlInput(){

		DataInputStream dataInputStream = null;
		DataOutputStream dataOutputStream = null;

		try{

			dataInputStream = new DataInputStream(mControlSocket.getInputStream());
			dataOutputStream = new DataOutputStream(mControlSocket.getOutputStream());
			String input = dataInputStream.readUTF();

			//  extract the command and arguments
			Parser parser = new Parser(input, ",");
			String command = parser.GetNextString();
			String arguments = parser.GetRemainingBuffer();

			if ( command.contains("$TPC_RINGING") )
			{
				
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




	/*
	 *   Telephone Control
	 *   
	 *   
	 */

	

	/* 
	 * Device State Management 
	 * 
	 * This state flag is used to suppress action commands while we are in the middle of another action
	 * 
	 */
	public static final int STATE_UNKNOWN					=-1;
	public static final int STATE_STEADY 					= 0;
	public static final int STATE_CHANGING			 		= 1;
	//
	private int mServiceState = 0; 



	



	/*
	 * IP Helper functions
	 */

	private String sendStringToConnectedOnCommandPort(String message){
		return sendStringToPort(mConnectedToServerIp, mConnectedToServerControlOnPort, message);
	}

	private String sendStringToPort(String ipAddress, int portNumber, String message){

		String response = "";


		int attempts = 1;

		while ( attempts <= 1 )
		{
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
				attempts ++;	//  retries
				continue;
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
		}

		return response;

	}

	boolean SendBytesToServerLinePort(byte[] bytes, int length){

		try{
			DatagramSocket clientSocket = new DatagramSocket();
			DatagramPacket sendPacket = new DatagramPacket(bytes, length, InetAddress.getByName(mConnectedToServerIp), mConnectedToServerLineOnPort);
			clientSocket.send(sendPacket);
			clientSocket.close();
		} catch(SocketException e){
			if (D) Log.e(TAG, "Exception in SendBytesToServerLinePort " + e.toString());
			return false;
		} catch(UnknownHostException e){
			if (D) Log.e(TAG, "Exception in SendBytesToServerLinePort " + e.toString());
			return false;
		} catch(IOException e){
			if (D) Log.e(TAG, "Exception in SendBytesToServerLinePort " + e.toString());
			return false;
		}

		return true;
	}



	//  Network status monitors
	private BroadcastReceiver mNetworkStateIntentReceiver;
	private IntentFilter mNetworkStateChangedFilter;

	NetworkInfo mIpWiFiInfo;
	NetworkInfo mIpMobileInfo;
	public String mIpWiFiAddress = "";

	public String getLocalIpAddress() {

		try{
			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();

			String ip = intToIp(ipAddress);
			return ip;


		} catch (Exception e){
			Log.e(TAG, "Exception in getLocalIpAddress: " + e.toString());
		}

		return "";

	}

	public String intToIp(int i) {

		return ( i & 0xFF)  + "." +
				((i >> 8 ) & 0xFF) + "." +
				((i >> 16 ) & 0xFF) + "." +    
				((i >> 24 ) & 0xFF ) ;
	}

	private void HandleNetworkStatusChange(){

		//  handle change of network status
		//  items to address:
		//
		//  1)  IP address of client and server were changed while client was connected


		// TODO:  with zeroConf networking, we should launch automatic connect or recovery when we detect network state change


		//  Update the status and notify the UI
		SetNetworkStatus();
	}


	private void SetNetworkStatus() {

		//  get the LAN IP address of this device
		mIpWiFiAddress = getLocalIpAddress();

		//  get the info about wifi and mobile connection
		mIpWiFiInfo = null;
		mIpMobileInfo = null;

		ConnectivityManager connectivity = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = connectivity.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) 
		{
			switch ( ni.getType() )
			{
			case ConnectivityManager.TYPE_WIFI:
				mIpWiFiInfo = ni;
				break;

			case ConnectivityManager.TYPE_MOBILE:
				mIpMobileInfo = ni;
				break;
			}
		}

		//  message the UI
		SendMessage(MESSAGE_NETSTATECHANGE);
	}



	//  Thread Sleep
	private void Sleep(long millis){
		try{
			Thread.sleep(millis);
		} catch (InterruptedException e){}
	}


	//  debug flags
	private final boolean D = true;
	private final String TAG = "TCPConnectService";

}
