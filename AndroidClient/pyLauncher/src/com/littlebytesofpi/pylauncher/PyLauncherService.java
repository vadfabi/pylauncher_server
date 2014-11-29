package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
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
		NetworkStateChangedFilter = new IntentFilter();
		NetworkStateChangedFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		//
		NetworkStateIntentReceiver = new BroadcastReceiver() {
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

		if ( ClientsServerThread != null && ClientsServerThread.IsConnected() )
			ClientsServerThread.Cancel();

		super.onDestroy();
	}


	//  onStartCommand
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		//  register for network state change messages
		registerReceiver(NetworkStateIntentReceiver, NetworkStateChangedFilter);

		showNotification();

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}


	//  LocalBinder
	//  this is a simple intraprocess application, so we use the LocalBinder method
	//
	private LocalBinder Binder = null;
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

		if ( Binder == null )
		{
			//  create the binder object
			Binder = new LocalBinder();
		}

		//  hook up the network state listener
		registerReceiver(NetworkStateIntentReceiver, NetworkStateChangedFilter);

		//  query current network state
		SetNetworkStatus();

		//  show notification
		showNotification();
		
		//  setup default preferences
		PreferenceManager.setDefaultValues(this,  R.xml.preferences,  false);

		//  setup buttons
		//  user defined buttons
		LoadButtonDrawableArray();
		LoadButtonsFromPreferences();
			
			
		//  attempt connection to the server
		if ( ! IsConnectedToServer() )
		{
			openConnectionToServer();
		}

		return Binder;
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
		contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, SendTab.class), 0);

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


	public void ShutDown(){
		
		if ( ClientsServerThread != null )
			ClientsServerThread.Cancel();
		ClientsServerThread = null;
		
		stopSelf();
	}
	
	
	//  Message Handling
	//
	
	//  List of Handlers we will send messages to
	private  ArrayList<Handler> HandlerList = new ArrayList<Handler>(); 
	//
	public synchronized void AddHandler(Handler handler){
		if ( HandlerList.contains(handler) )
			return;
		HandlerList.add(handler);

	}
	//
	public synchronized void RemoveHandler(Handler handler){
		HandlerList.remove(handler);
	}

	// SendMessage
	//
	public synchronized void SendMessage(int message)
	{
		for ( Handler handler : HandlerList )
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
		for ( Handler handler : HandlerList )
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
	private ArrayList<PyLaunchResult> ResultsList = new ArrayList<PyLaunchResult>();
	//
	public synchronized void AddLaunchResult(PyLaunchResult result){

		ResultsList.add(result);
		
		if ( ResultsList.size() > 1 )
			ResultsList.get(ResultsList.size()-2).mExpanded = false;
		
		//  message the UI
		SendMessage(MESSAGE_NEWEVENT);
	}
	//
	public synchronized void GetLaunchResults(ArrayList<PyLaunchResult> list){

		for ( int i = list.size(); i < ResultsList.size(); i++ )
			list.add(0, ResultsList.get(i));
	}
	//
	public synchronized void ClearLogs()
	{
		ResultsList.clear();
		SendMessage(MESSAGE_NEWEVENT);
	}



	/*
	 *  IP Connection to the server
	 *  
	 *  
	 */

	//  Port that we connect to the server on (this must be set in the settings)
	//  TODO:  replace user settings with zero-conf networking discovery of server service
	private int ServerPort = 48888;
	public int getServerPort()
	{
		return ServerPort;
	}


	//  ip address of the server we are connected to
	private String ConnectedToServerIp = "";
	public String getConnectedToServerIp(){
		return ConnectedToServerIp;
	}


	//  when connected, the server opens a socket connection on this port to receive our control commands
	private int ConnectedToServerOnPort = 0; 
	public int getConnectedToServerOnPort(){
		return ConnectedToServerOnPort;
	}


	//  when connected we open a socket connection on this port for the server to send to us
	//  and we run a socket accept thread
	ClientsServer ClientsServerThread = null;
	public int getClientListeningOnPort(){  
		if ( ClientsServerThread != null )
			return ClientsServerThread.GetClientListeningOnPort();
		else
			return -1;
	}


	//  IsConnectedToServer
	//  returns connection state
	//
	public synchronized boolean IsConnectedToServer()
	{
		//  we are connected when control thread is running and control socket is open
		return ClientsServerThread != null && ClientsServerThread.IsConnected();
	}


	//  SendStringToConnectedOnCommandPort
	//  
	private String SendStringToConnectedOnCommandPort(String message){
		return IpFunctions.SendStringToPort(ConnectedToServerIp, ConnectedToServerOnPort, message);
	}

	
	boolean ConnectingToServer = false;

	//  OpenConnectionToServer
	//
	public void openConnectionToServer()
	{
		//  are we trying to connect already
		if ( ConnectingToServer )
		{
			Toast.makeText(this,  "Connection to server is in progress, please be patient.",  Toast.LENGTH_SHORT).show();
			return;
		}
		//  we are connecting now, lock out other attempts
		ConnectingToServer = true;
		
		
		//  get the IP address to connect to
		//  TODO:  this should be replaced with zero conf networking ip address discovery
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String ipAddress = sharedPrefs.getString("pref_serveripaddress", "");
		String serverPortString = sharedPrefs.getString("pref_serverport", "48888");

		int serverPort = 0;
		try{
			serverPort =  Integer.parseInt(serverPortString);
		} 
		catch (NumberFormatException e){
			Toast.makeText(PyLauncherService.this, "Error starting connection, unable to parse server control port number!", Toast.LENGTH_SHORT).show();
			serverPort = 0;
		}

		//  close previous connection to the server if it is open
		if ( ClientsServerThread != null )
		{
			ClientsServerThread.Cancel();
			ClientsServerThread = null;
		}

		ServerPort = serverPort;
		ConnectedToServerIp = ipAddress;

		ClientsServerThread = new ClientsServer(this);

		if ( ConnectedToServerIp.length() != 0 )
			//  starting connection, tell user
			Toast.makeText(PyLauncherService.this, "Connecting to to server at: " + ConnectedToServerIp + " on port: " + ServerPort, Toast.LENGTH_SHORT).show();
		
		//  launch the connection task
		new OpenConnectionTask().execute();
	}
	//
	//  OpenConnectionTask
	private class OpenConnectionTask extends AsyncTask<Void, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(Void... param ) {

			//  if we have never connected, bail out so we can show the settings page as first action
			if ( ConnectedToServerIp.length() == 0 || ServerPort == 0 )
				return 2;
			
			if ( ClientsServerThread.OpenSocketConnection() )
			{
				//  connect to server command:
				// $TCP_CONNECT,clientsControlPort
			
				readResponse = IpFunctions.SendStringToPort(ConnectedToServerIp, ServerPort, ClientsServer.TCP_CONNECT +"," + getClientListeningOnPort());

				if ( readResponse.contains(ClientsServer.TCP_CONNECT) && readResponse.contains(ClientsServer.TCP_ACK) )
				{
					//  parse response:  
					//  $TPC_CONNECT,ACK,serversControlPort
					Parser parser = new Parser(readResponse, ",");

					String command = parser.GetNextString();	//  $TCP_CONNECT
					String pass = parser.GetNextString();		//  $TCP_ACK
					String serversControlPort = parser.GetNextString();		//  the server's listening port

					try{
						ConnectedToServerOnPort =  Integer.parseInt(serversControlPort);
					} catch (NumberFormatException e){
						
						return 0;
					}

					//  start the client's listening server thread
					ClientsServerThread.start();

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
			else if ( result == 2 )
			{

				//  There is nothing to make a connection with
				ConnectingToServer = false;
				return;
			}
			else
			{
				ClientsServerThread = null;

				Toast.makeText(PyLauncherService.this, "Failed to open server connection!", Toast.LENGTH_SHORT).show();
				ConnectedToServerIp = "";
			}

			//  update android notification
			showNotification();
			
			ConnectingToServer = false;
		}
	}
	
	

	//  CloseConnectionToServer
	//
	public void closeConnectionToServer(){

		if ( ClientsServerThread == null )
			return; //  closed already

		//  launch the close connection task
		new CloseConnectionTask().execute();
	}
	//
	//  CloseConnectionTask
	private class CloseConnectionTask extends AsyncTask<String, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			readResponse = IpFunctions.SendStringToPort(ConnectedToServerIp, ServerPort, "$TCP_DISCONNECT");

			return 1;
		}

		protected void onPostExecute(Integer result ) {

			//  shut down the server thread
			ClientsServerThread.Cancel();

			if ( readResponse.contains(ClientsServer.TCP_DISCONNECT) && readResponse.contains(ClientsServer.TCP_ACK) )
				Toast.makeText(PyLauncherService.this, "Server closed connection.", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(PyLauncherService.this, "Error closing server connection", Toast.LENGTH_SHORT).show();

			//  TODO:  clear out file and dir list
			
			
			showNotification();
			
			mFilesList.clear();
			DirectoryList.clear();

			//  update UI
			PyLauncherService.this.SendMessage(MESSAGE_CONNECTEDSTATECHANGE);	
			PyLauncherService.this.SendMessage(MESSAGE_UPDATEDIRECTORIES);
		}
	}

	

	/*
	 * List of directories and files on the server
	 * 
	 */
	
	protected ArrayList<PyFile> DirectoryList = new ArrayList<PyFile>();
	public synchronized void GetDirectoryList(ArrayList<PyFile> dirList)
	{
		dirList.clear();
		
		dirList.addAll(DirectoryList);		
	}
	
	protected ArrayList<PyFile> mFilesList = new ArrayList<PyFile>();
	public synchronized void GetFilesList(ArrayList<PyFile> filesList)
	{
		filesList.clear();

		for ( PyFile nextFile : mFilesList )
		{
			//  see if this file is in visible directory
			for ( PyFile nextDir : DirectoryList )
			{
				if ( nextDir.GetDirectoryPath().compareTo(nextFile.GetDirectoryPath()) == 0 )
				{
					if ( nextDir.mSet )
						filesList.add(nextFile);
					break;
				}
			}
		}
	}
	
	public PyFile GetHelpFile()
	{
		for ( PyFile nextFile : mFilesList )
		{
			if ( nextFile.toString().compareTo("programHelp.py") == 0 )
				return nextFile;
		}
		
		return null;
	}
	
	
	//  GetDirectoryListTask
	//
	private class GetDirectoryListTask extends AsyncTask<Void, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(Void... param ) {

			if ( IsConnectedToServer() )
			{
				readResponse = IpFunctions.SendStringToPort(ConnectedToServerIp, ConnectedToServerOnPort, "$TCP_LISTDIR");

				if ( ! ParseListDir(readResponse) )
					return 0;

				//  get the list of files
				readResponse = IpFunctions.SendStringToPort(ConnectedToServerIp, ConnectedToServerOnPort, "$TCP_LISTFILES");

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
	public void RemoveDirectory(String dirToRemove)
	{
		new RemoveDirectoryTask().execute(dirToRemove);
	}
	//
	private class RemoveDirectoryTask extends AsyncTask<String, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			String removeCommand = ClientsServer.TCP_REMOVEDIR + "," + param[0];
			
			readResponse = IpFunctions.SendStringToPort(ConnectedToServerIp, ConnectedToServerOnPort, removeCommand);

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
	
	
	//  AddDirectory
	//
	public void AddDirectory(String dirToAdd)
	{
		new AddDirectoryTask().execute(dirToAdd);
	}
	//
	private class AddDirectoryTask extends AsyncTask<String, Void, Integer> {

		String dirName = "";
		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			String addCommand = ClientsServer.TCP_ADDDIR + "," + param[0];
			dirName = param[0];
			
			//  update the dir list before command
			PyFile newDir = new PyFile(dirName);
			newDir.mSet = true;
			DirectoryList.add(newDir);
			SaveDirectoryList();
			
			readResponse = IpFunctions.SendStringToPort(ConnectedToServerIp, ConnectedToServerOnPort, addCommand);

			if ( readResponse.contains(ClientsServer.TCP_ADDDIR) && readResponse.contains(ClientsServer.TCP_ACK) )
				return 1;
			else
			{
				DirectoryList.remove(newDir);
				return 0;
			}
		}

		//  Post Execute
		protected void onPostExecute(Integer result ) {

			if ( result == 0 )
			{
				Toast.makeText(PyLauncherService.this, "Failed to add directory!", Toast.LENGTH_SHORT).show();		
			}
			else
			{
				//  TODO - what for success
			}

			//  TODO - is this necessary?
			showNotification();
		}
	}

	
	//  ParseListDir
	//  parses the response to $TCP_LISTDIR
	//
	synchronized boolean ParseListDir(String input)
	{
		//  $TPC_LISTDIR,ACK,dir1,dir2,...
		Parser parser = new Parser(input.trim(), ",");

		// parse $TPC_LISTDIR
		//  TODO - there is an extra \n now
		if ( parser.GetNextString().compareTo(ClientsServer.TCP_LISTDIR) != 0 )
			return false;		
		
		//  parse ACK
		if ( parser.GetNextString().compareTo(ClientsServer.TCP_ACK) != 0 )
			return false;

		// update directory list
		DirectoryList.clear();
		//
		String nextDir = parser.GetNextString();
		while ( nextDir.length() != 0 )
		{
			DirectoryList.add(new PyFile(nextDir));
			nextDir = parser.GetNextString();
		}
		
		//  match the directory list with settings to retain visibility flag
		LoadDirsFromPreferences();
		
		return true;
	}


	//  ParseListFiles
	//  parses the response to $TPC_LISTFILES
	//
	synchronized boolean ParseListFiles(String input)
	{
		//  $TPC_LISTFILES,ACK,file1,file2,...
		Parser parser = new Parser(input.trim(), ",");

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
		
		LoadButtonsFromPreferences();
		
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
		//  get the python environment
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String env = sharedPrefs.getString("pref_environment", "python");
		
		new RunPyFileTask().execute(env,fileToRun.FullPath, args);
	}
	
	//  RunPyFile
	//
	public void RunPyFile(String environment, PyFile fileToRun, String args)
	{
		new RunPyFileTask().execute(environment,fileToRun.FullPath, args);
	}
	
	//
	//  GetDirectoryListTask
	private class RunPyFileTask extends AsyncTask<String, Void, Integer> {

		String readResponse = "";
		protected Integer doInBackground(String... param ) {

			if ( IsConnectedToServer() )
			{
				readResponse = IpFunctions.SendStringToPort(ConnectedToServerIp, ConnectedToServerOnPort, "$TCP_PYLAUNCH," + param[0] + "," + param[1] + "," + param[2]);

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

	
	//  Buttons
	//
	private static final String BNAME = "Name";
	private static final String BENV = "Environment";
	private static final String BPATH = "Path";
	private static final String BARGS = "Args";
	private static final String BICON = "Icon";
	private static final String PREF_BUTTONS = "buttonsList";
	
	//  Buttons List
	protected ArrayList<PyLauncherButton> ButtonsList = null;
	
	//  Load Buttons from Preferences
	//
	protected void LoadButtonsFromPreferences()
	{
		if ( ButtonsList != null )
			return;
		
		ButtonsList = new ArrayList<PyLauncherButton>();
		
		//  Load the buttons from the settings
		SharedPreferences prefs = getSharedPreferences("pref_hidden",MODE_PRIVATE);
		
		//  the array of button objects is encoded in a JSON object
		try 
		{
			JSONArray jsonArray = new JSONArray(prefs.getString("buttonsList","[]"));
			for (int i = 0; i < jsonArray.length(); i++) 
			{
				//  parse the button object
				try
				{
					JSONObject nextButton = jsonArray.getJSONObject(i);
					String environment = nextButton.getString(BENV);
					String name = nextButton.getString(BNAME);
					String path = nextButton.getString(BPATH);
					String args = nextButton.getString(BARGS);
					Integer icon = nextButton.getInt(BICON);
					
					//  make the button
					ButtonsList.add(new PyLauncherButton(environment, new PyFile(path), args, name, icon));
				}
				catch (Exception e)
				{
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	
	//  Save Buttons to Preferences
	//
	protected void SaveButtonsList()
	{
		//  put the list of buttons in json array	
		JSONArray buttonObjectsList = new JSONArray();

		for (PyLauncherButton nextButton : ButtonsList) 
		{
			try 
			{
				// save this to json object
				JSONObject buttonObject = new JSONObject();
				buttonObject.put(BENV, nextButton.getEnvironment());
				buttonObject.put(BNAME, nextButton.getTitle());
				buttonObject.put(BPATH, nextButton.getPyFile().GetPath());
				buttonObject.put(BARGS, nextButton.getCommandLineArgs());
				buttonObject.put(BICON, nextButton.getIcon());

				buttonObjectsList.put(buttonObject);
			} 
			catch (JSONException e) 
			{
				continue;
			}
		}
		
		//  save it
		SharedPreferences prefs = getSharedPreferences("pref_hidden",MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putString("buttonsList", buttonObjectsList.toString());
		editor.commit();
	}
	
	
	//  Directory List
	//
	static final String DNAME = "Name";
	static final String DCHK = "Checked";
	static final String PREF_DIR = "dirList";

	
	//  Save Directories to Preferences
	//
	protected void SaveDirectoryList()
	{
		//  put the directory list into a JSON array
		JSONArray dirObjectList = new JSONArray();

		for ( PyFile nextFile : DirectoryList )
		{
			try
			{
				JSONObject dirObject = new JSONObject();
				dirObject.put(DNAME, nextFile.GetPath() );
				dirObject.put(DCHK, nextFile.mSet );

				dirObjectList.put(dirObject);
			}
			catch (JSONException e)
			{
				continue;
			}
		}

		//  save the dir list to the settings
		SharedPreferences prefs = getSharedPreferences("pref_hidden",MODE_PRIVATE);			
		Editor editor = prefs.edit();
		editor.putString("dirList", dirObjectList.toString());
		editor.commit();
	}
	
	//  Load Directories from Preferences
	//
	protected void LoadDirsFromPreferences()
	{
		ArrayList<PyFile> dirList = new ArrayList<PyFile>();
		
		//  Load the directories from the settings
		SharedPreferences prefs = getSharedPreferences("pref_hidden",MODE_PRIVATE);
		
		try 
		{
			JSONArray jsonArray = new JSONArray(prefs.getString("dirList","[]"));
			for (int i = 0; i < jsonArray.length(); i++) 
			{
				//  parse the button object
				try
				{
					JSONObject nextDir = jsonArray.getJSONObject(i);
					String name = nextDir.getString(DNAME);
					boolean checked = nextDir.getBoolean(DCHK);
					
					//  make the dir
					PyFile newDir = new PyFile(name);
					newDir.mSet = checked;
					
					dirList.add(newDir);
				}
				catch (Exception e)
				{
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//  now set the state of each dir in the list from the server
		for ( PyFile nextDir : DirectoryList )
		{
			nextDir.mSet = true;
			
			//  see if this directory is in the settings list
			for ( PyFile settingsDir : dirList )
			{
				if ( settingsDir.GetPath().compareTo(nextDir.GetPath()) == 0 )
				{
					nextDir.mSet = settingsDir.mSet;
					break;
				}
			}
		}
	}
	
	//  Get Visible Button List
	//  adds the buttons that are visible to your list
	public void getVisibleButtonList(ArrayList<PyLauncherButton> buttonList)
	{
		buttonList.clear();
		
		//  filter buttons by directory visibility
		for ( PyLauncherButton nextButton : ButtonsList )
		{
			//  see if this button is in visible directory
			for ( PyFile nextDir : DirectoryList )
			{
				if ( nextDir.GetDirectoryPath().compareTo(nextButton.getPyFile().GetDirectoryPath()) == 0 )
				{
					if ( nextDir.mSet )
						buttonList.add(nextButton);
					break;
				}
			}
		}
	}
	
	
	
	public void UpdateButton(PyLauncherButton button)
	{
		//  see if this button exists already
		if ( ! ButtonsList.contains(button) )
		{
			ButtonsList.add(button);
		}
		
		SaveButtonsList();
	}
	
	
	public void RemoveButton(PyLauncherButton button)
	{
		if ( ButtonsList.contains(button) )
		{
			ButtonsList.remove(button);
		}
		
		SaveButtonsList();
	}

	
	public void UpdateButtonsList(List<?> newButtonList)
	{
		//  create an array of the original indices of these buttons
		ArrayList<Integer> indexList = new ArrayList<Integer>();
		
		//  determine the original indices of the buttons in the list
		for ( PyLauncherButton nextButton : ButtonsList )
		{
			if ( newButtonList.contains(nextButton) )
			{
				indexList.add(ButtonsList.indexOf(nextButton));
			}
		}
		
		//  rearrange the list
		for ( int i = 0; i < newButtonList.size(); i++ )
		{
			PyLauncherButton nextButton = (PyLauncherButton)newButtonList.get(i);
			
			//  get the current index of this button
			int currentIndex = ButtonsList.indexOf(nextButton);
			
			//  get the insert index
			int insertionIndex = indexList.get(i);
			
			//  see if this button has new location
			if ( currentIndex != insertionIndex )
			{
				//  insert the new button
				ButtonsList.add(insertionIndex, nextButton);
				
				//  remove the old button
				if ( currentIndex < insertionIndex )
				{
					//  inserted below, remove the object above
					ButtonsList.remove(currentIndex);
				}
				else
				{
					//  inserted above, remove the object below which is now at index + 1
					ButtonsList.remove(currentIndex+1);
				}
			}
		}
		
		//  Save
		SaveButtonsList();
		
		
		
	}
	
	

	/*
	 * Network State Monitor
	 */

	private BroadcastReceiver NetworkStateIntentReceiver;
	private IntentFilter NetworkStateChangedFilter;
	//
	NetworkInfo IpWiFiInfo;
	NetworkInfo IpMobileInfo;
	public String IpWiFiAddress = "";


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
		IpWiFiAddress = IpFunctions.GetLocalIpAddress(wifiManager);

		//  get the info about wifi and mobile connection
		IpWiFiInfo = null;
		IpMobileInfo = null;

		ConnectivityManager connectivity = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = connectivity.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) 
		{
			switch ( ni.getType() )
			{
			case ConnectivityManager.TYPE_WIFI:
				IpWiFiInfo = ni;
				break;

			case ConnectivityManager.TYPE_MOBILE:
				IpMobileInfo = ni;
				break;
			}
		}

		//  message the UI
		SendMessage(MESSAGE_NETSTATECHANGE);
	}

	
	//  Button Array
	//
	protected static ArrayList<Drawable> ButtonDrawableArrayList = null;
	//
	protected void LoadButtonDrawableArray()
	{
		if ( ButtonDrawableArrayList != null )
			return;
		
		ButtonDrawableArrayList = new ArrayList<Drawable>();
		
		Integer nextButtonIndex = 0;
		Integer drawableId = 1;
		while ( drawableId > 0 )
		{
			String nextIcon = "ic_"+ nextButtonIndex.toString();
			drawableId = this.getResources().getIdentifier(nextIcon,"drawable", this.getPackageName());
			
			if ( drawableId > 0 )
			{
				ButtonDrawableArrayList.add(getResources().getDrawable(drawableId) );
				nextButtonIndex ++;
			}
		}
	}
	
	//  Get Count of Button Icons
	//
	public int GetButtonDrawableCount()
	{
		//  return size 
		return ButtonDrawableArrayList.size() ;
	}
	
	
	//  Get the button icon for index
	//
	public Drawable GetButtonDrawable(int index)
	{
		if ( index >= 0 && ButtonDrawableArrayList.size() > index )
			return ButtonDrawableArrayList.get(index);
		else
			return this.getResources().getDrawable(R.drawable.ic_unknown);
	}



	//  Debug Flags
	//
	private final boolean D = false;
	private final String TAG = "PyLauncherService";

}
