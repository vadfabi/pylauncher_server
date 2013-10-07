package com.littlebytesofpi.pylauncher;

import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;



public class PyLauncher extends TabActivity {

	TabHost mTabHost;
	
	//  onCreate
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_py_launcher);

		//  setup the tabs
		Resources ressources = getResources(); 
		mTabHost = getTabHost();

		// Connect Tab
		Intent intentConnect = new Intent().setClass(this, ConnectTab.class);
		TabSpec tabSpecConnect = mTabHost.newTabSpec("Connect")
				.setIndicator("Connect", ressources.getDrawable(R.drawable.ic_connect))
				.setContent(intentConnect);

		// Directory Tab
		Intent intentDirectory = new Intent().setClass(this, DirectoryTab.class);
		TabSpec tabSpecDirectory = mTabHost.newTabSpec("Directory")
				.setIndicator("Directory", ressources.getDrawable(R.drawable.ic_directory))
				.setContent(intentDirectory);

//		// Launch Tab
//		Intent intentSend = new Intent().setClass(this, SendTab.class);
//		TabSpec tabSpecSend = mTabHost.newTabSpec("Launch")
//				.setIndicator("Launch", ressources.getDrawable(R.drawable.ic_send))
//				.setContent(intentSend);

		mTabHost.addTab(tabSpecConnect);
		mTabHost.addTab(tabSpecDirectory);
		//mTabHost.addTab(tabSpecSend);
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
		
	}

	//  onDestroy
	@Override
	public void onDestroy(){
		super.onDestroy();

	

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
		Intent startIntent = new Intent(PyLauncher.this, PyLauncherService.class);
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



	//  Message Handler
	//
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			}
		}
	};  

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.py_launcher, menu);
		return true;
	}

	boolean D = false;
	String TAG = "PyLauncher";

}
