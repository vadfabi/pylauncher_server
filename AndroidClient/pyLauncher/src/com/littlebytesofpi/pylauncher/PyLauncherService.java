package com.littlebytesofpi.pylauncher;

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
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class PyLauncherService extends Service {

	/*
	 * Messages
	 * 
	 */
	public static final int MESSAGE_NETSTATECHANGE = 0;
	//
	public static final int MESSAGE_CONNECTEDSTATECHANGE = 1;
	//
	public static final int MESSAGE_EVENTRECEIVED = 2;
	//
	public static final int MESSAGE_NEWEVENT = 3;
	//
	public static final int MESSAGE_UPDATEDIRECTORIES = 4;

	
	//  Constructor
	//
	public PyLauncherService(){
		
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
	


	//  onDestroy
	//
	@Override
	public void onDestroy() {

		if ( mClientsServerThread != null && mClientsServerThread.IsConnected() )
			mClientsServerThread.cancel();

		super.onDestroy();
	}


	//  onStartCommand
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		//  register for network state change messages
		registerReceiver(mNetworkStateIntentReceiver, mNetworkStateChangedFilter);

		showNotification();

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}


	//  LocalBinder
	//  this is a simple intraprocess application, so we use the LocalBinder method
	//
	private LocalBinder mBinder = null;
	//
	public class LocalBinder extends Binder {
		PyLauncherService getService() {
			return PyLauncherService.this;
		}
	}


	// onBind
	//
	@Override
	public IBinder onBind(Intent arg0) {

		if ( mBinder == null )
		{
			//  create the binder object
			mBinder = new LocalBinder();
		}

		//  hook up the network state listener
		registerReceiver(mNetworkStateIntentReceiver, mNetworkStateChangedFilter);

		//  query current network state
		SetNetworkStatus();

		//  show notification
		showNotification();
		
		//  setup default preferences
		PreferenceManager.setDefaultValues(this,  R.xml.preferences,  false);

		//  attempt connection to the server
		if ( ! IsConnectedToServer() )
		{

			//  get the IP address to connect to
			//  TODO:  this should be replaced with zero conf networking ip address discovery
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			String ipAddress = sharedPrefs.getString("pref_serveripaddress", "");

			int serverPort = 0;
			try{
				serverPort =  Integer.parseInt(sharedPrefs.getString("pref_serverport", "48888"));

				if ( ipAddress != "" && ( serverPort > 1024 && serverPort < 65535) )
				{
					Toast.makeText(this, "Connecting to to server at: " + ipAddress + " on port: " + serverPort, Toast.LENGTH_SHORT).show();
					openConnectionToServer(ipAddress, serverPort);
				}
			} 
			catch (NumberFormatException e){
			}
		}

		return mBinder;
	}



	//  Notification Manager
	//
	private final int NOTIFICATION_ID = 99;
	private NotificationManager mNM;

	//  showNotification
	//
	public void showNotification() {

		CharSequence text = "pyLauncher";
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher, text,	System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = null;
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, PyLauncher.class), 0);

		//  set string with state info
		String notificationText;
		if ( IsConnectedToServer() )
			notificationText = "Connected to server";
		else
			notificationText = "Not connected to server";

		notification.setLatestEventInfo(this, text, notificationText, contentIntent);

		//  set this as a persistent notification
		notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		//  specify that the notification should be in the foreground
		startForeground(NOTIFICATION_ID, notification);
	}


	
	
	//  Message Handling
	//
	
	//  List of Handlers we will send messages to
	private  ArrayList<Handler> mHandlerList = new ArrayList<Handler>(); 
	//
	public synchronized void AddHandler(Handler handler){
		if ( mHandlerList.contains(handler) )
			return;
		mHandlerList.add(handler);

	}
	//
	public synchronized void RemoveHandler(Handler handler){
		mHandlerList.remove(handler);
	}

	// SendMessage
	//
	public synchronized void SendMessage(int message)
	{
		for ( Handler handler : mHandlerList )
		{
			Message messageToSend = Message.obtain(null, message);

			messageToSend.setTarget(handler);
			messageToSend.sendToTarget();
		}
	}

	//  SendMessage
	//
	public synchronized void SendMessage(int message, int arg1)
	{
		for ( Handler handler : mHandlerList )
		{
			Message messageToSend = Message.obtain(null, message, arg1, 0);

			messageToSend.setTarget(handler);
			messageToSend.sendToTarget();
		}
	}





	/*
	 * Event Log
	 * 
	 */

	//  List of events in the log
	private ArrayList<PyLaunchResult> mResultsList = new ArrayList<PyLaunchResult>();
	//
	public synchronized void AddLaunchResult(PyLaunchResult result){

		mResultsList.add(result);
		
		if ( mResultsList.size() > 1 )
			mResultsList.get(mResultsList.size()-2).mExpanded = false;
		
		//  message the UI
		SendMessage(MESSAGE_NEWEVENT);
	}
	//
	public synchronized void GetLaunchResults(ArrayList<PyLaunchResult> list){

		for ( int i = list.size(); i < mResultsList.size(); i++ )
			list.add(0, mResultsList.get(i));
	}
	//
	public synchronized void ClearLogs()
	{
		mResultsList.clear();
		SendMessage(MESSAGE_NEWEVENT);
	}



	/*
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
	ClientsServer mClientsServerThread = null;
	public int getClientListeningOnPort(){  
		if ( mClientsServerThread != null )
			return mClientsServerThread.GetClientListeningOnPort();
		else
			return -1;
	}


	//  IsConnectedToServer
	//  returns connection state
	//
	public synchronized boolean IsConnectedToServer()
	{
		//  we are connected when control thread is running and control socket is open
		return mClientsServerThread != null && mClientsServerThread.IsConnected();
	}


	//  SendStringToConnectedOnCommandPort
	//  
	private String SendStringToConnectedOnCommandPort(String message){
		return IpFunctions.SendStringToPort(mConnectedToServerIp, mConnectedToServerOnPort, message);
	}


	//  OpenConnectionToServer
	//
	public void openConnectionToServer(String connectAddress, int connectPort)
	{
		//  close previous connection to the server if it is open
		if ( mClientsServerThread != null )
		{
			mClientsServerThread.cancel();
			mClientsServerThread = null;
		}

		mServerPort = connectPort;

		mClientsServerThread = new ClientsServer(this);

		//  launch the connection task
		new OpenConnectionTask().execute(connectAddress);
	}
	//
	//  OpenConnectionTask
	private class OpenConnectionTask extends AsyncTask<String, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			if ( mClientsServerThread.OpenSocketConnection() )
			{
				//  connect to server command:
				// $TCP_CONNECT,clientsControlPort
				mConnectedToServerIp = param[0];
				readResponse = IpFunctions.SendStringToPort(mConnectedToServerIp, mServerPort, ClientsServer.TCP_CONNECT +"," + getClientListeningOnPort());

				if ( readResponse.contains(ClientsServer.TCP_CONNECT) && readResponse.contains(ClientsServer.TCP_ACK) )
				{
					//  parse response:  
					//  $TPC_CONNECT,ACK,serversControlPort
					Parser parser = new Parser(readResponse, ",");

					String command = parser.GetNextString();	//  $TCP_CONNECT
					String pass = parser.GetNextString();		//  $TCP_ACK
					String serversControlPort = parser.GetNextString();		//  the server's listening port

					try{
						mConnectedToServerOnPort =  Integer.parseInt(serversControlPort);
					} catch (NumberFormatException e){
						Toast.makeText(PyLauncherService.this, "Error starting connection, unable to parse server control port number!", Toast.LENGTH_SHORT).show();
						return 0;
					}

					//  start the client's listening server thread
					mClientsServerThread.start();

					//  wait until our connected condition is true
					long timeStartWait = System.currentTimeMillis();
					while ( ! IsConnectedToServer() && (System.currentTimeMillis() - timeStartWait) < 2000 )
					{
						try{
							Thread.sleep(500);
						}
						catch(InterruptedException e){
						}
					}

					return 1;
				}
			}


			return 0;
		}

		//  Post Execute
		protected void onPostExecute(Integer result ) {

			if ( result == 1 )
			{
				//  message success
				PyLauncherService.this.SendMessage(MESSAGE_CONNECTEDSTATECHANGE);
				
				//  update directories on new connect
				new GetDirectoryListTask().execute();
			}
			else
			{
				mClientsServerThread = null;

				Toast.makeText(PyLauncherService.this, "Failed to open server connection!", Toast.LENGTH_SHORT).show();
				mConnectedToServerIp = "";
			}

			//  update android notification
			showNotification();
		}
	}
	
	

	//  CloseConnectionToServer
	//
	public void closeConnectionToServer(){

		if ( mClientsServerThread == null )
			return; //  closed already

		//  launch the close connection task
		new CloseConnectionTask().execute();
	}
	//
	//  CloseConnectionTask
	private class CloseConnectionTask extends AsyncTask<String, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			readResponse = IpFunctions.SendStringToPort(mConnectedToServerIp, mServerPort, "$TCP_DISCONNECT");

			return 1;
		}

		protected void onPostExecute(Integer result ) {

			//  shut down the server thread
			mClientsServerThread.cancel();

			if ( readResponse.contains(ClientsServer.TCP_DISCONNECT) && readResponse.contains(ClientsServer.TCP_ACK) )
				Toast.makeText(PyLauncherService.this, "Server closed connection.", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(PyLauncherService.this, "Error closing server connection", Toast.LENGTH_SHORT).show();

			//  TODO:  clear out file and dir list
			
			
			showNotification();

			//  update UI
			PyLauncherService.this.SendMessage(MESSAGE_CONNECTEDSTATECHANGE);	
		}
	}

	

	/*
	 * List of directories and files on the server
	 * 
	 */
	
	protected ArrayList<PyFile> mDirectoryList = new ArrayList<PyFile>();
	public synchronized void GetDirectoryList(ArrayList<PyFile> dirList)
	{
		dirList.clear();
		dirList.addAll(mDirectoryList);		
	}
	
	protected ArrayList<PyFile> mFilesList = new ArrayList<PyFile>();
	public synchronized void GetFilesList(ArrayList<PyFile> filesList)
	{
		filesList.clear();
		filesList.addAll(mFilesList);
	}
	
	
	//  GetDirectoryListTask
	//
	private class GetDirectoryListTask extends AsyncTask<Void, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(Void... param ) {

			if ( IsConnectedToServer() )
			{
				readResponse = IpFunctions.SendStringToPort(mConnectedToServerIp, mConnectedToServerOnPort, "$TCP_LISTDIR");

				if ( ! ParseListDir(readResponse) )
					return 0;

				//  get the list of files
				readResponse = IpFunctions.SendStringToPort(mConnectedToServerIp, mConnectedToServerOnPort, "$TCP_LISTFILES");

				if ( ! ParseListFiles(readResponse) )
					return 0;
				
				return 1;
			}
			else
				return 0;
		}

		//  Post Execute
		protected void onPostExecute(Integer result ) {

			if ( result == 1 )
			{
				//  message success
				PyLauncherService.this.SendMessage(MESSAGE_UPDATEDIRECTORIES);
			}
			else
			{
				Toast.makeText(PyLauncherService.this, "Failed to get list of directories!", Toast.LENGTH_SHORT).show();		
			}

			showNotification();
		}
	}
	
	//  RemoveDirectory
	//
	public void RemoveDirectory()
	{
		new RemoveDirectoryTask().execute();
	}
	//
	private class RemoveDirectoryTask extends AsyncTask<Void, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(Void... param ) {

			String removeCommand = ClientsServer.TCP_REMOVEDIR + ",";
			synchronized (PyLauncherService.this) {
			//  build a list of directories to remove
				for ( PyFile nextFile : mDirectoryList )
				{
					if ( nextFile.mSet )
						removeCommand += nextFile.getPath();
				}
			}
			
			readResponse = IpFunctions.SendStringToPort(mConnectedToServerIp, mConnectedToServerOnPort, removeCommand);

			if ( readResponse.contains(ClientsServer.TCP_REMOVEDIR) && readResponse.contains(ClientsServer.TCP_ACK) )
				return 1;
			else
				return 0;
		}

		//  Post Execute
		protected void onPostExecute(Integer result ) {

			if ( result == 0 )
			{
				Toast.makeText(PyLauncherService.this, "Failed to get list of directories!", Toast.LENGTH_SHORT).show();		
			}

			showNotification();
		}
	}
	
	
//  RemoveDirectory
	//
	public void AddDirectory(String dirToAdd)
	{
		new AddDirectoryTask().execute(dirToAdd);
	}
	//
	private class AddDirectoryTask extends AsyncTask<String, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			String addCommand = ClientsServer.TCP_ADDDIR + "," + param[0];
			
			
			readResponse = IpFunctions.SendStringToPort(mConnectedToServerIp, mConnectedToServerOnPort, addCommand);

			if ( readResponse.contains(ClientsServer.TCP_ADDDIR) && readResponse.contains(ClientsServer.TCP_ACK) )
				return 1;
			else
				return 0;
		}

		//  Post Execute
		protected void onPostExecute(Integer result ) {

			if ( result == 0 )
			{
				Toast.makeText(PyLauncherService.this, "Failed to add directory!", Toast.LENGTH_SHORT).show();		
			}

			showNotification();
		}
	}

	
	//  ParseListDir
	//  parses the response to $TCP_LISTDIR
	//
	synchronized boolean ParseListDir(String input)
	{
		//  $TPC_LISTDIR,ACK,dir1,dir2,...
		Parser parser = new Parser(input, ",");

		// parse $TPC_LISTDIR
		if ( parser.GetNextString().compareTo(ClientsServer.TCP_LISTDIR) != 0 )
			return false;		
		
		//  parse ACK
		if ( parser.GetNextString().compareTo(ClientsServer.TCP_ACK) != 0 )
			return false;

		// update directory list
		mDirectoryList.clear();
		//
		String nextDir = parser.GetNextString();
		while ( nextDir.length() != 0 )
		{
			mDirectoryList.add(new PyFile(nextDir));
			nextDir = parser.GetNextString();
		}
		
		return true;
	}


	//  ParseListFiles
	//  parses the response to $TPC_LISTFILES
	//
	synchronized boolean ParseListFiles(String input)
	{
		//  $TPC_LISTFILES,ACK,file1,file2,...
		Parser parser = new Parser(input, ",");

	//  parse out $TPC_LISTFILES
		if ( parser.GetNextString().compareTo(ClientsServer.TCP_LISTFILES) != 0 )
			return false;
		
	//  parse out ACK
		if ( parser.GetNextString().compareTo(ClientsServer.TCP_ACK) != 0 )
			return false;

		//  update files list
		mFilesList.clear();
		//
		String nextFile = parser.GetNextString();
		while ( nextFile.length() != 0 )
		{
			mFilesList.add(new PyFile(nextFile));
			nextFile = parser.GetNextString();
		}
		
		return true;
	}


	/*
	 * Launching Python Files on the Server
	 * 
	 */
	
	//  RunPyFile
	//
	public void RunPyFile(PyFile fileToRun, String args)
	{
		new RunPyFileTask().execute(fileToRun.mFullPath, args);
	}
	
	//
	//  GetDirectoryListTask
	private class RunPyFileTask extends AsyncTask<String, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			if ( IsConnectedToServer() )
			{
				readResponse = IpFunctions.SendStringToPort(mConnectedToServerIp, mConnectedToServerOnPort, "$TCP_PYLAUNCH," + param[0] + "," + param[1]);

				if ( ! readResponse.contains("$TCP_PYLAUNCH,ACK") )
					return 0;
				
				return 1;
			}
			else
				return 0;
		}

		//  Post Execute
		protected void onPostExecute(Integer result ) {

			if ( result == 0 )
			{
				//  todo message from server in failure toast
				Toast.makeText(PyLauncherService.this, "Failed to launch python file!", Toast.LENGTH_SHORT).show();		
			}

			showNotification();
		}
	}




	/*
	 * Network State Monitor
	 */

	private BroadcastReceiver mNetworkStateIntentReceiver;
	private IntentFilter mNetworkStateChangedFilter;
	//
	NetworkInfo mIpWiFiInfo;
	NetworkInfo mIpMobileInfo;
	public String mIpWiFiAddress = "";


	//  HandleNetworkStatusChange
	//
	private void HandleNetworkStatusChange(){		
		SetNetworkStatus();
	}


	//  SetNetworkStatus
	//
	protected void SetNetworkStatus()
	{
		//  get the LAN IP address of this device
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		mIpWiFiAddress = IpFunctions.GetLocalIpAddress(wifiManager);

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




	//  Debug Flags
	//
	private final boolean D = true;
	private final String TAG = "PyLauncherService";

}
