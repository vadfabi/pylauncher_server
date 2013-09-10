package com.littlebytesofpi.pylauncher;

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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class ConnectTab extends Activity {

	//  User Interface Elements
	//
	TextView textViewNetStatus;
	TextView textViewServerStatus;
	EditText editTextIpAddress;
	EditText editTextPort;
	Button buttonConnect;
	Button buttonDisconnect;

	//  Button Handler
	//

	//
	View.OnClickListener ButtonOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			switch ( v.getId() )
			{
			case R.id.buttonConnect:
				break;

			case R.id.buttonDisconnect:
				break;

			}
		}
	};
	


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connect_tab);

		textViewNetStatus = (TextView)findViewById(R.id.textView_NetworkStatus);
		textViewServerStatus = (TextView)findViewById(R.id.textView_ServerStatus);
		editTextIpAddress = (EditText)findViewById(R.id.editText_IpAddress);
		editTextPort = (EditText)findViewById(R.id.editText_Port);

		buttonConnect = (Button)findViewById(R.id.buttonConnect);
		buttonConnect.setOnClickListener(ButtonOnClickListener);
		
		buttonDisconnect = (Button)findViewById(R.id.buttonDisconnect);
		buttonDisconnect.setOnClickListener(ButtonOnClickListener);

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
			Intent startIntent = new Intent(this, PyLauncherService.class);
			this.startService(startIntent);
		}
		
		UnbindFromService();
	}
	

	/*
	 * Service Handling
	 * 
	 */
	PyLauncherService mService = null;

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

				//mService.GetLogEvents(mLogEventList);
				//mEventAdapter.notifyDataSetChanged();
				//mListView.smoothScrollToPosition(0);	

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
		Intent startIntent = new Intent(ConnectTab.this, PyLauncherService.class);
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

			case PyLauncherService.MESSAGE_NETSTATECHANGE:
				SetNetworkStateUi();
				break;

			case PyLauncherService.MESSAGE_CONNECTEDSTATECHANGE:
				SetConnectedStateUi();
				break;

			case PyLauncherService.MESSAGE_NEWEVENT:
				//mService.GetLogEvents(mLogEventList);
				//mEventAdapter.notifyDataSetChanged();
				//mListView.smoothScrollToPosition(0);
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
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ConnectTab.this);
		String ipAddress = "192.168.1.201"; // sharedPrefs.getString("pref_serveripaddress", "");

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

			textViewNetStatus.setText(status);
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

			textViewServerStatus.setText(status);
		}
	}


	
	boolean D = true;
	String TAG = "ConnectTab";
	

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.connect_tab, menu);
//		return true;
//	}

}
