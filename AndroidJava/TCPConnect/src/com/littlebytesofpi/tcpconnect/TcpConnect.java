package com.littlebytesofpi.tcpconnect;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.littlebytesofpi.tcpconnect.TCPConnectService.LocalBinder;

public class TcpConnect extends Activity {

	

	

	/*
	 * Activity Lifecycle
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tcp_connect);

		
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

		//SetConnectedStateUi();

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

			SetNetworkStateUi();
			SetConnectedStateUi();

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


			}
		}
	};  







	/**
	 * Format User Interface State functions
	 */
	public void SetNetworkStateUi(){
		if ( mService != null )
		{/*
			String status = "";
			if ( mService.mIpWiFiInfo != null && mService.mIpWiFiInfo.isConnected() )
			{
				status = "Connected to WiFi at " + mService.mIpWiFiAddress;
			}
			else 
				status = "Not connected to WiFi";

			mTextViewNetStatus.setText(status);*/
		}

	} 

	public void SetConnectedStateUi(){

		/*
		if ( mService != null )
		{
			String status = "";
			if ( mService.IsConnectedToServer() )
			{
			
				status = "Connected to Server at " + mService.getConnectedToServerIp();
				
				//  hook status
				status += mService.mPhoneState.getIsOffTheHook() ?  "\n- Phone is off the hook" : "\n- Phone is on the hook";
			
				//  line status
				ArrayList<Integer> lineNumbers = mService.mPhoneState.getActiveLineNumbers();
				for ( int lineNumber : lineNumbers )
				{
					ClientPhoneLine nextLine = mService.mPhoneState.getPhoneLine(lineNumber);
					String connected = nextLine.getIsConnected() ? "connected to " : "not connected";
					String callerId = nextLine.getIsConnected() ? nextLine.getCallerNumber() : "";
					String onHold = nextLine.getIsConnected() ? (nextLine.getIsOnHold() ? " : on hold" : "") : "";
					
					status += "\n - Line " + lineNumber + ": " + connected + callerId + onHold;
				}
				
				//  setup talk button
				if ( mService.mPhoneState.getIsConnected() )
					mButtonTalk.setText("Flash");
				else 
					mButtonTalk.setText("Talk");

				
			}
			else
				status = "Not connected to Server";

			mTextViewServerStatus.setText(status);
		}*/

	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_connect_ip_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.menu_connect:
		{
			if ( mService == null )
				return true;

			mService.openConnectionToServer();

			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			String ipAddress = sharedPrefs.getString("pref_server_ip_address", "192.168.0.101");
			Toast.makeText(this, "Connecting to to server at: " + ipAddress, Toast.LENGTH_SHORT).show();

			return true;
		}

		case R.id.menu_disconnect:
		{
			mService.closeConnectionToServer();

			return true;
		}


		case R.id.menu_settings:

			Intent intent = new Intent(this, TPCSettingsActivity.class);
			startActivity(intent);

			return true;

		}*/
		return false;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		/*
		if (requestCode == ACTIVITYREQUEST_CONTACTS && resultCode == Activity.RESULT_OK)
		{
			Uri contactData = data.getData();
			// Get the URI that points to the selected contact
			Uri contactUri = data.getData();
			// We only need the NUMBER column, because there will be only one row in the result
			String[] phoneNumber = {Phone.NUMBER};
			String[] normalizedNumber = {Phone.NORMALIZED_NUMBER};
			String[] displayName = {Phone.DISPLAY_NAME_PRIMARY};

			// Perform the query on the contact to get the NUMBER column
			// We don't need a selection or sort order (there's only one result for the given URI)
			// CAUTION: The query() method should be called from a separate thread to avoid blocking
			// your app's UI thread. (For simplicity of the sample, this code doesn't do that.)
			// Consider using CursorLoader to perform the query.
			Cursor cursor = getContentResolver()
					.query(contactUri, phoneNumber, null, null, null);
			cursor.moveToFirst();

			// Retrieve the phone number from the NUMBER column
			int column = cursor.getColumnIndex(Phone.NUMBER);
			String number = cursor.getString(column);

			cursor = getContentResolver()
					.query(contactUri, normalizedNumber, null, null, null);
			cursor.moveToFirst();

			// Retrieve the phone number from the NUMBER column
			column = cursor.getColumnIndex(Phone.NORMALIZED_NUMBER);
			String normalNumber = cursor.getString(column);

			cursor = getContentResolver()
					.query(contactUri, displayName, null, null, null);
			cursor.moveToFirst();

			// Retrieve the phone number from the NUMBER column
			column = cursor.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY);
			String contactName = cursor.getString(column);

			// Do something with the phone number...
			mEditTextPhoneNumber.setText(number);

		}*/



	}




	private final String TAG = "TPC_CLientMain";
	private final boolean D = true;
    
}
