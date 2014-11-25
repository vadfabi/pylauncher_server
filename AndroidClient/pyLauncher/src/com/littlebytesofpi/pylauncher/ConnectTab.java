package com.littlebytesofpi.pylauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class ConnectTab extends ActionBarActivity {

	//  User Interface Elements
	//
	TextView TextViewNetStatus;
	TextView TextViewServerStatus;
	EditText EditTextIpAddress;
	EditText EditTextPort;
	Button ButtonConnect;
	Button ButtonDisconnect;


	//  onCreate
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connect_tab);

		TextViewNetStatus = (TextView)findViewById(R.id.textView_NetworkStatus);
		TextViewServerStatus = (TextView)findViewById(R.id.textView_ServerStatus);
		EditTextIpAddress = (EditText)findViewById(R.id.editText_IpAddress);
		EditTextPort = (EditText)findViewById(R.id.editText_Port);

		ButtonConnect = (Button)findViewById(R.id.buttonConnect);
		ButtonConnect.setOnClickListener(ButtonOnClickListener);

		ButtonDisconnect = (Button)findViewById(R.id.buttonDisconnect);
		ButtonDisconnect.setOnClickListener(ButtonOnClickListener);

		//  setup default preferences
		PreferenceManager.setDefaultValues(this,  R.xml.preferences,  false);
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ConnectTab.this);
		
		//  initialize edit fields
		EditTextIpAddress.setText(sharedPrefs.getString("pref_serveripaddress", ""));
		EditTextPort.setText(sharedPrefs.getString("pref_serverport",  "48888"));

	}

	
	//  onStart
	//
	@Override
	public void onStart() {
		super.onStart();

		if (Service == null) 
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
		//  if we are connected to server, start the service so it stays connected
		UnbindFromService();
		
		super.onDestroy();
	}


	//  Service
	//
	
	//  reference to the service
	PyLauncherService Service = null;

	//  ServiceConnection
	//
	private ServiceConnection Connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder service) {

			//  this is simple intra process, we can just get the service object
			LocalBinder binder = (LocalBinder) service;
			Service = binder.getService();
			Service.AddHandler(mHandler);

			SetNetworkStateUi();
			SetConnectedStateUi();
		}


		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
		}
	};


	//  BindToService
	//
	private void BindToService() {
		if ( D ) Log.d(TAG, "bindToService()");

		// bind to the service 
		Intent startIntent = new Intent(ConnectTab.this, PyLauncherService.class);
		getApplicationContext().bindService(startIntent, Connection, Context.BIND_AUTO_CREATE);
	}


	//  UnbindFromService
	//
	void UnbindFromService() {
		if (Service != null) {

			Service.RemoveHandler(mHandler);

			// Detach our existing connection
			getApplicationContext().unbindService(Connection);
		}
	}


	//  Message Handler
	//
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
			}
		}
	};  


	//  Button Handler
	//
	View.OnClickListener ButtonOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			switch ( v.getId() )
			{
			case R.id.buttonConnect:

				//  check for connected to wifi
				//
				if ( Service.IpWiFiInfo == null || ! Service.IpWiFiInfo.isConnected() )
				{
					Toast.makeText(ConnectTab.this,  "You are not connected to Wi-Fi. You must establish a network connection before connecting to the pyLauncher server. .", Toast.LENGTH_LONG).show();
				}
				
				//  save the current preferences
				Editor editPref = PreferenceManager.getDefaultSharedPreferences(ConnectTab.this).edit();
				editPref.putString("pref_serveripaddress", EditTextIpAddress.getText().toString());
				editPref.putString("pref_serverport", EditTextPort.getText().toString());

				// Commit the edits
				editPref.commit();
				
				OpenServerConnectionWithSettings();

				break;

			case R.id.buttonDisconnect:
				
				Service.closeConnectionToServer();
				break;

			}
		}
	};

	
	//  Open Connection to Server with address:port from settings
	//
	protected void OpenServerConnectionWithSettings()
	{
		//  get the IP address to connect to
		//  TODO:  this should be replaced with zero conf networking ip address discovery
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ConnectTab.this);
		String ipAddress = sharedPrefs.getString("pref_serveripaddress", "");

		int serverPort = 0;
		try{
			serverPort =  Integer.parseInt(sharedPrefs.getString("pref_serverport", "48888"));

			if ( ipAddress != "" && ( serverPort > 1024 && serverPort < 65535) )
			{
				//  open connection with default
				//  message the user that we have initiated a connection
					Service.openConnectionToServer();
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
		if ( Service != null )
		{
			String status = "";
			if ( Service.IpWiFiInfo != null && Service.IpWiFiInfo.isConnected() )
			{
				status = "Connected to WiFi at " + Service.IpWiFiAddress;
			}
			else 
				status = "Not connected to WiFi";

			TextViewNetStatus.setText(status);
		}

	} 


	//  SetConnectedStateUi
	//
	public void SetConnectedStateUi(){

		if ( Service != null )
		{
			String status = "";
			if ( Service.IsConnectedToServer() )
			{
				status = "Connected to Server at: " + Service.getConnectedToServerIp();
				status += "\n  on server port: " + Service.getConnectedToServerOnPort();
				status += "\n  listening on port: " +  Service.getClientListeningOnPort();
			}
			else
				status = "Not connected to Server";

			TextViewServerStatus.setText(status);
		}
	}

	


	boolean D = false;
	String TAG = "ConnectTab";


	//	@Override
	//	public boolean onCreateOptionsMenu(Menu menu) {
	//		// Inflate the menu; this adds items to the action bar if it is present.
	//		getMenuInflater().inflate(R.menu.connect_tab, menu);
	//		return true;
	//	}

}
