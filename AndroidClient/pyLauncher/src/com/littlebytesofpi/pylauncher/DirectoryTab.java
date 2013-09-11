package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;
import java.util.List;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

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
import android.widget.ListView;

public class DirectoryTab extends Activity {

	private ListView mDirectoryListView;
	private List<String> mDirectoryList = new ArrayList<String>();
	private DirectoriesAdapter mDirectoryAdapter = new DirectoriesAdapter(mDirectoryList,  this);
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_directory_tab);
		
		mDirectoryList.add("Sample");
		
		mDirectoryListView = (ListView)findViewById(R.id.listViewDirectories);
		mDirectoryListView.setAdapter(mDirectoryAdapter);
		
	}
	
	
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

	}

	//  onDestroy
	@Override
	public void onDestroy(){
		//  if we are connected to server, start the service so it stays connected
		UnbindFromService();
		
		super.onDestroy();
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

			//  TODO - fill dir list
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
		Intent startIntent = new Intent(DirectoryTab.this, PyLauncherService.class);
		getApplicationContext().bindService(startIntent, mConnection, Context.BIND_AUTO_CREATE);
	}


	//  UnbindFromService
	void UnbindFromService() {
		if (mService != null) {

			mService.RemoveHandler(mHandler);

			// Detach our existing connection
			getApplicationContext().unbindService(mConnection);
		}
	}



	/*
	 * Message Handler for messages back from the service	
	 */

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case PyLauncherService.MESSAGE_UPDATEDIRECTORIES:
				
				mService.GetDirectoryList(mDirectoryList);
				mDirectoryAdapter.notifyDataSetChanged();
				
				break;
			}
		}
	};  

	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.directory_tab, menu);
		return true;
	}

	boolean D = true;
	String TAG = "DirectoryTab";
}
