package com.littlebytesofpi.tcpconnect;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class TCPConnectService extends Service {



	/*
	 * Messages
	 */
	public static final int MESSAGE_NETSTATECHANGE = 0;
	//
	public static final int MESSAGE_CONNECTEDSTATECHANGE = 1;
	//
	public static final int MESSAGE_EVENTRECEIVED = 2;
	//
	public static final int MESSAGE_NEWEVENT = 3;





	/**
	 * TCPConnectService 
	 * 
	 * Constructor and Lifecycle
	 */
	public TCPConnectService() {

		//  This service will monitor network status, so setup a network state broadcast receiver
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


	@Override
	public void onDestroy() {

		if ( mClientsServerThread != null && mClientsServerThread.IsConnected() )
			mClientsServerThread.cancel();

		super.onDestroy();
	}




	/**
	 * Service Binding and Messaging Functions
	 * 
	 */

	//  we keep a list of message handlers registered with the service
	//  all handlers will receive all messages that are sent out
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
	//  this is a simple intraprocess application, so we use the LocalBinder method
	private LocalBinder mBinder = null;

	public class LocalBinder extends Binder {
		TCPConnectService getService() {
			return TCPConnectService.this;
		}
	}


	@Override
	public IBinder onBind(Intent arg0) {

		if ( mBinder == null )
		{
			//  create the binder object
			mBinder = new LocalBinder();
		}

		//  hook up the 
		registerReceiver(mNetworkStateIntentReceiver, mNetworkStateChangedFilter);

		SetNetworkStatus();

		showNotification();

		return mBinder;
	}



	//  notification manager
	private final int NOTIFICATION_ID = 99;
	private NotificationManager mNM;

	public void showNotification() {

		CharSequence text = "TCP Client";

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher, text,	System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = null;
		String description;

		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, TcpConnect.class), 0);

		//  set event info
		notification.setLatestEventInfo(this, text, "Service Running", contentIntent);

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



	/**
	 * Message Handling
	 * this is a simple intraprocess implementation, 
	 * so you can pass objects to handlers
	 * 
	 */

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

	//  send message to registered handler
	public synchronized void SendMessage(int message, int arg1)
	{
		for ( Handler handler : mHandlerList )
		{
			Message messageToSend = Message.obtain(null, message, arg1, 0);

			messageToSend.setTarget(handler);
			messageToSend.sendToTarget();
		}
	}


	/**
	 * 
	 * Event Log
	 * 
	 */

	private ArrayList<LogEvent> mLogEventList = new ArrayList<LogEvent>();
	public synchronized void AddLogEvent(LogEvent event){

		mLogEventList.add(event);
		//  message the UI
		SendMessage(MESSAGE_NEWEVENT);
	}

	public synchronized void GetLogEvents(List<LogEvent> list){

		for ( int i = list.size(); i < mLogEventList.size(); i++ )
			list.add(mLogEventList.get(i));
	}



	/**
	 *  IP Connection to the server
	 *  
	 *  
	 */

	//  Port that we connect to the server on (this must be set in the settings)
	//  TODO:  replace user settings with zero-conf networking discovery of server service
	private int mServerPort = 48888;
	public int getServerPort()
	{
		return mServerPort;
	}

	//  ip address of the server we are connected to
	private String mConnectedToServerIp = "";
	public String getConnectedToServerIp(){
		return mConnectedToServerIp;
	}

	//  when connected, the server opens a socket connection on this port to receive our control commands
	private int mConnectedToServerOnPort = 0; 
	public int getConnectedToServerOnPort(){
		return mConnectedToServerOnPort;
	}


	//  when connected we open a socket connection on this port for the server to send to us
	//  and we run a socket accept thread
	ClientsServerThread mClientsServerThread = null;
	public int getClientListeningOnPort(){  
		if ( mClientsServerThread != null )
			return mClientsServerThread.getClientListeningOnPort();
		else
			return -1;
	}


	//  return connection state
	public synchronized boolean IsConnectedToServer()
	{
		//  we are connected when control thread is running and control socket is open
		return mClientsServerThread != null && mClientsServerThread.IsConnected();
	}


	/**
	 * OpenConnectionToServer
	 * 
	 * Launches task to open a connection to the server
	 * 
	 */
	public void openConnectionToServer(String connectAddress, int connectPort)
	{
		//  close previous connection to the server if it is open
		if ( mClientsServerThread != null )
		{
			mClientsServerThread.cancel();
			mClientsServerThread = null;
		}
		
		mServerPort = connectPort;

		mClientsServerThread = new ClientsServerThread(this);
		
		//  launch the connection task
		new OpenConnectionTask().execute(connectAddress);
	}

	//  open connection task
	private class OpenConnectionTask extends AsyncTask<String, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			if ( mClientsServerThread.OpenSocketConnection() )
			{
				//  connect to server command:
				// $TCP_CONNECT,clientsControlPort
				mConnectedToServerIp = param[0];
				readResponse = IpFunctions.sendStringToPort(mConnectedToServerIp, mServerPort, "$TCP_CONNECT," + getClientListeningOnPort());
				return 1;
			}
			else
				return 0;
		}

		//  Post Execute
		protected void onPostExecute(Integer result ) {

			if ( result == 1 && readResponse.contains("$TCP_CONNECT,ACK") )
			{
				//  parse response:  
				//  $TPC_CONNECT,ACK,serversControlPort
				Parser parser = new Parser(readResponse, ",");

				String command = parser.GetNextString();
				String pass = parser.GetNextString();
				String serversControlPort = parser.GetNextString();
				
				try{
					mConnectedToServerOnPort =  Integer.parseInt(serversControlPort);
				} catch (NumberFormatException e){
					Toast.makeText(TCPConnectService.this, "Error starting connection, unable to parse server control port number!", Toast.LENGTH_SHORT).show();
					return;
				}

			
				//  start the client's listening server thread
				mClientsServerThread.start();


				showNotification();

				//  message success
				TCPConnectService.this.SendMessage(MESSAGE_CONNECTEDSTATECHANGE);
			}
			else
			{
				mClientsServerThread = null;
				
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

		if ( mClientsServerThread == null )
			return; //  closed already

		//  launch the close connection task
		new CloseConnectionTask().execute();
	}

	//  close connection task
	private class CloseConnectionTask extends AsyncTask<String, Void, Integer> {


		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			readResponse = IpFunctions.sendStringToPort(mConnectedToServerIp, mServerPort, "$TCP_DISCONNECT");

			return 1;
		}

		protected void onPostExecute(Integer result ) {

			//  shut down the server thread
			mClientsServerThread.cancel();
			

			if ( readResponse.contains("$TCP_DISCONNECT,ACK") )
				Toast.makeText(TCPConnectService.this, "Server closed connection.", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(TCPConnectService.this, "Error closing server connection", Toast.LENGTH_SHORT).show();



			showNotification();

			//  update UI
			TCPConnectService.this.SendMessage(MESSAGE_CONNECTEDSTATECHANGE);	
		}
	}



	public void PressButton(int buttonNumber){

		new PressButtonTask().execute(buttonNumber);

	}

	//  close connection task
	private class PressButtonTask extends AsyncTask<Integer, Void, Integer> {

		String readResponse = "";

		protected Integer doInBackground(Integer... param ) {

			readResponse = TCPConnectService.this.sendStringToConnectedOnCommandPort("$TCP_BUTTON,"+param[0]);

			return 1;
		}

		protected void onPostExecute(Integer result ) {

			//  todo:  process read response


		}
	}


	//  TODO - move echo test to its own thread
	EchoTest echoTestThread = null;
	public void EchoTest()
	{
		if ( echoTestThread == null )
		{
			echoTestThread = new EchoTest(this);
			echoTestThread.start();
		}
		else
		{
			echoTestThread.cancel();
			echoTestThread = null;
		}

	}






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
	 * TCP/IP Helper functions
	 */

	private String sendStringToConnectedOnCommandPort(String message){
		return IpFunctions.sendStringToPort(mConnectedToServerIp, mConnectedToServerOnPort, message);
	}



	//  Network status monitors
	private BroadcastReceiver mNetworkStateIntentReceiver;
	private IntentFilter mNetworkStateChangedFilter;

	NetworkInfo mIpWiFiInfo;
	NetworkInfo mIpMobileInfo;
	public String mIpWiFiAddress = "";



	private void HandleNetworkStatusChange(){

		
		SetNetworkStatus();
	}

	protected void SetNetworkStatus()
	{
	//  get the LAN IP address of this device
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			mIpWiFiAddress = IpFunctions.getLocalIpAddress(wifiManager);

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







	//  debug flags
	private final boolean D = true;
	private final String TAG = "TCPConnectService";

}
