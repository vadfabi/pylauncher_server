package com.littlebytesofpi.tcpipsockets;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.littlebytesofpi.tcpconnect.R;
import com.littlebytesofpi.tcpipsockets.TCPConnectService.LocalBinder;

public class TcpConnect extends Activity {


	
	/*
	 * User Interface Items
	 */
	
	TextView mTextViewNetStatus;
	TextView mTextViewServerStatus;

	Button mButtonOne;
	Button mButtonTwo;
	Button mButtonThree;
	Button mButtonSend;
	EditText mEditTextSendMessage;

	ListView mListView;

	//  create records adapter for event log objects
	private List<LogEvent> mLogEventList = new ArrayList<LogEvent>();
	private LogEventAdapter mEventAdapter = new LogEventAdapter(mLogEventList, this);


	//  ButtonOnClickListener
	//
	View.OnClickListener ButtonOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			if ( v.getId() == R.id.button1 )
			{
				mService.PressButton(1);
			}
			else if ( v.getId() == R.id.button2 )
			{
				mService.PressButton(2);
			}
			else if ( v.getId() == R.id.button3 )
			{
				mService.PressButton(3);
			}
			else if ( v.getId() == R.id.buttonSendMessage )
			{
				mService.SendMessageToServer(mEditTextSendMessage.getText().toString());
			}

		}
	};



	/*
	 * Activity Lifecycle
	 */
	
	//  onCreate
	//
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tcp_connect);

		//  get the UI elements
		mTextViewNetStatus = (TextView)findViewById(R.id.textViewNetworkStatus);
		mTextViewServerStatus = (TextView)findViewById(R.id.textViewServerStatus);

		//  buttons, set the click listener function here
		mButtonOne = (Button)findViewById(R.id.button1);
		mButtonOne.setOnClickListener(ButtonOnClickListener);
		mButtonTwo = (Button)findViewById(R.id.button2);
		mButtonTwo.setOnClickListener(ButtonOnClickListener);
		mButtonThree = (Button)findViewById(R.id.button3);
		mButtonThree.setOnClickListener(ButtonOnClickListener);

		//  send message edit box
		mEditTextSendMessage = (EditText)findViewById(R.id.editTextSendMessage);
		//
		mButtonSend = (Button)findViewById(R.id.buttonSendMessage);
		mButtonSend.setOnClickListener(ButtonOnClickListener);

		//  log event list view
		mListView = (ListView)findViewById(R.id.listViewEventLog);
		mListView.setAdapter(mEventAdapter);
		mListView.setStackFromBottom(true);
		mListView.setFastScrollEnabled(true);

		//  setup default preferences
		PreferenceManager.setDefaultValues(this,  R.xml.preferences,  false);
	}


	//  onStart
	//
	@Override
	public void onStart() {
		super.onStart();

		if (mService == null) 
			BindToService();	
	}


	//  onResume
	@Override
	public void onResume(){
		super.onResume();

		SetNetworkStateUi();
		SetConnectedStateUi();
	}

	//  onDestroy
	@Override
	public void onDestroy(){
		super.onDestroy();

		//  if we are connected to server, start the service so it stays connected
		
		if ( mService.IsConnectedToServer() )
		{
			Intent startIntent = new Intent(this, TCPConnectService.class);
			this.startService(startIntent);
		}
		
		UnbindFromService();
	}

	
	
	
	
	/*
	 * Service Handling
	 * 
	 */
	TCPConnectService mService = null;

	//  ServiceConnection
	//
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder service) {

			//  this is simple intra process, we can just get the service object
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mService.AddHandler(mHandler);

			//  attempt automatic connection to the server
			if ( ! mService.IsConnectedToServer() )
			{
				if ( ! mService.IsConnectedToServer() )
					openServerConnectionWithSettings();
			}
			else
			{
				SetNetworkStateUi();
				SetConnectedStateUi();

				mService.GetLogEvents(mLogEventList);
				mEventAdapter.notifyDataSetChanged();
				mListView.smoothScrollToPosition(0);	

			}
		}


		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.

		}
	};

	
	//  BindToService
	//
	private void BindToService() {
		Log.d(TAG, "bindToService()");

		// bind to the service 
		Intent startIntent = new Intent(TcpConnect.this, TCPConnectService.class);
		bindService(startIntent, mConnection, Context.BIND_AUTO_CREATE);
	}

	
	//  UnbindFromService
	void UnbindFromService() {
		if (mService != null) {

			mService.RemoveHandler(mHandler);

			// Detach our existing connection
			unbindService(mConnection);
		}
	}

	
	
	/*
	 * Message Handler for messages back from the service	
	 */
	
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case TCPConnectService.MESSAGE_NETSTATECHANGE:
				SetNetworkStateUi();
				break;

			case TCPConnectService.MESSAGE_CONNECTEDSTATECHANGE:
				SetConnectedStateUi();
				break;

			case TCPConnectService.MESSAGE_NEWEVENT:
				mService.GetLogEvents(mLogEventList);
				mEventAdapter.notifyDataSetChanged();
				mListView.smoothScrollToPosition(0);
			}
		}
	};  

	
	/*
	 * Open Connection to Server with address:port from settings
	 */
	
	protected void openServerConnectionWithSettings()
	{
		//  get the IP address to connect to
		//  TODO:  this should be replaced with zero conf networking ip address discovery
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(TcpConnect.this);
		String ipAddress = sharedPrefs.getString("pref_serveripaddress", "");

		int serverPort = 0;
		try{
			serverPort =  Integer.parseInt(sharedPrefs.getString("pref_serverport", "48888"));

			if ( ipAddress != "" && ( serverPort > 1024 && serverPort < 65535) )
			{
				//  open connection with default
				//  message the user that we have initiated a connection
				Toast.makeText(this, "Connecting to to server at: " + ipAddress + " on port: " + serverPort, Toast.LENGTH_SHORT).show();
				mService.openConnectionToServer(ipAddress, serverPort);
			}
			else
			{
				Toast.makeText(this,  "IP Address: '" + ipAddress + "' or port '" + serverPort + "' is invalid.", Toast.LENGTH_LONG).show();
			}
		} 
		catch (NumberFormatException e){
			Toast.makeText(this,  "IP Address: '" + ipAddress + "' or port '" + serverPort + "' is invalid.", Toast.LENGTH_LONG).show();
		}
		finally{

			SetNetworkStateUi();
			SetConnectedStateUi();
		}

		return;
	}


	
	/*
	 * Format User Interface State functions
	 */
	
	//  SetNetworkStateUi
	//
	public void SetNetworkStateUi(){
		if ( mService != null )
		{
			String status = "";
			if ( mService.mIpWiFiInfo != null && mService.mIpWiFiInfo.isConnected() )
			{
				status = "Connected to WiFi at " + mService.mIpWiFiAddress;
			}
			else 
				status = "Not connected to WiFi";

			mTextViewNetStatus.setText(status);
		}

	} 

	
	//  SetConnectedStateUi
	//
	public void SetConnectedStateUi(){

		if ( mService != null )
		{
			String status = "";
			if ( mService.IsConnectedToServer() )
			{
				status = "Connected to Server at: " + mService.getConnectedToServerIp();
				status += "\n  on server port: " + mService.getConnectedToServerOnPort();
				status += "\n  listening on port: " +  mService.getClientListeningOnPort();
			}
			else
				status = "Not connected to Server";

			mTextViewServerStatus.setText(status);
		}
	}



	/*
	 * Options Menu Handling
	 * 
	 */
	
	private final int ACTIVITYRESULT_SETTINGS = 99;

	
	//  onCreateOptionsMenu
	//
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tcp_connect, menu);
		return true;
	}


	//  onOptionsItemSelected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) 
		{
		case R.id.menu_connect:
		{
			if ( mService == null )
				return true;

			openServerConnectionWithSettings();

			return true;
		}

		case R.id.menu_disconnect:
		{
			mService.closeConnectionToServer();

			return true;
		}

		case R.id.menu_settings:
		{
			Intent intent = new Intent(this, Settings_Activity.class);
			startActivityForResult(intent, ACTIVITYRESULT_SETTINGS);

			return true;
		}

		case R.id.menu_clearLogs:
		{
			mLogEventList.clear();
			mService.ClearLogs();
			return true;
		}
		
		}

		return false;
	}



	//  onActivityResult
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if ( requestCode == ACTIVITYRESULT_SETTINGS )
		{
			if ( ! mService.IsConnectedToServer() )
				openServerConnectionWithSettings();
		}
	}



	//  Debug Flags
	//
	private final String TAG = "TcpConnect";
	private final boolean D = false;

}
