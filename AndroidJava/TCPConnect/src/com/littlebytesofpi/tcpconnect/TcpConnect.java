package com.littlebytesofpi.tcpconnect;

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

import com.littlebytesofpi.tcpconnect.TCPConnectService.LocalBinder;

public class TcpConnect extends Activity {

	

	//  User Interface Items
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
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tcp_connect);

			//  get the UI elements
			mTextViewNetStatus = (TextView)findViewById(R.id.textViewNetworkStatus);
			mTextViewServerStatus = (TextView)findViewById(R.id.textViewServerStatus);
			
			//  Buttons, set the click listener function here
			mButtonOne = (Button)findViewById(R.id.button1);
			mButtonOne.setOnClickListener(ButtonOnClickListener);
			mButtonTwo = (Button)findViewById(R.id.button2);
			mButtonTwo.setOnClickListener(ButtonOnClickListener);
			mButtonThree = (Button)findViewById(R.id.button3);
			mButtonThree.setOnClickListener(ButtonOnClickListener);
			
			mEditTextSendMessage = (EditText)findViewById(R.id.editTextSendMessage);
			
			mButtonSend = (Button)findViewById(R.id.buttonSendMessage);
			mButtonSend.setOnClickListener(ButtonOnClickListener);
					
			mListView = (ListView)findViewById(R.id.listViewEventLog);
			mListView.setAdapter(mEventAdapter);
			mListView.setStackFromBottom(true);

			PreferenceManager.setDefaultValues(this,  R.xml.preferences,  false);
	}

	
	@Override
	public void onStart() {
		super.onStart();

		if (mService == null) 
			bindToService();	
	}



	@Override
	public void onResume(){
		super.onResume();

		SetNetworkStateUi();
		SetConnectedStateUi();
		
		mListView.requestFocus();

	}

	@Override
	public void onPause(){
		super.onPause();

	}

	@Override
	public void onStop(){
		super.onStop();

	}

	@Override
	public void onDestroy(){
		super.onDestroy();

		//  start this service so it does not go away
		//  start the service
		Intent startIntent = new Intent(this, TCPConnectService.class);
		this.startService(startIntent);

		unbindFromService();
	}

	/*
	 * Service Handling
	 * 
	 */
	TCPConnectService mService = null;

	/*
	 * Service Connection
	 * Class for interacting with the main interface of the service.
	 */
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
				
			}
		}

		
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.

		}
	};

	private void bindToService() {
		Log.d(TAG, "bindToService()");

		// bind to the service 
		Intent startIntent = new Intent(TcpConnect.this, TCPConnectService.class);
		bindService(startIntent, mConnection, Context.BIND_AUTO_CREATE);
	}

	void unbindFromService() {
		if (mService != null) {

			mService.RemoveHandler(mHandler);

			// Detach our existing connection
			unbindService(mConnection);
		}
	}

	/*
	 * Message Handler for messages back from the service	
	 */
	// The Handler that gets information back from the BluetoothChatService
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







	/**
	 * Format User Interface State functions
	 */
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

	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tcp_connect, menu);
		return true;
	}

	private final int ACTIVITYRESULT_SETTINGS = 99;


	protected void openServerConnectionWithSettings()
	{
		//  get the IP address to connect to
		//  TODO:  this should be replaced with zero conf networking ip address discovery
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(TcpConnect.this);
		String ipAddress = sharedPrefs.getString("pref_serveripaddress", "");

		int serverPort = 0;
		try{
			serverPort =  Integer.parseInt(sharedPrefs.getString("pref_serverport", "48888"));

			if ( ipAddress != "" && serverPort > 1024 )
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


	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent serverIntent = null;
		switch (item.getItemId()) {
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

			Intent intent = new Intent(this, Settings_Activity.class);
			startActivityForResult(intent, ACTIVITYRESULT_SETTINGS);

			return true;

		}
		return false;
	}

	
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if ( requestCode == ACTIVITYRESULT_SETTINGS )
		{
			if ( ! mService.IsConnectedToServer() )
				openServerConnectionWithSettings();
		}

	}




	private final String TAG = "TcpConnect";
	private final boolean D = true;
    
}
