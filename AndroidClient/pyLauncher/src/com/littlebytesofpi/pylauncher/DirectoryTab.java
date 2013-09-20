package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class DirectoryTab extends Activity {

	//  User Interface Elements
	private ListView mListViewDirectories;
	private ArrayList<PyFile> mDirectoryList = new ArrayList<PyFile>();
	private DirectoriesAdapter mDirectoryAdapter = new DirectoriesAdapter(mDirectoryList,  this);
	
	
	Button mButtonAdd;
	Button mButtonRemove;
	
	View.OnClickListener ButtonOnClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			mDirectoryList.clear();
			mDirectoryAdapter.notifyDataSetChanged();
			
			switch ( v.getId() )
			{
			case R.id.buttonAdd:
				
				final AlertDialog.Builder alert = new AlertDialog.Builder(DirectoryTab.this);
				alert.setTitle("Enter Directory Name");
				final EditText input = new EditText(DirectoryTab.this);
				
				//  set default file name based on date / time
			
				input.setText("/home/grahambriggs/Source/Python3/");
				alert.setView(input);
				
				//  alert dialog button handlers
				alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						
						mService.AddDirectory(value);
					}
				});

				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
				alert.show();    
				break;
				
			case R.id.buttonRemove:
				mService.RemoveDirectory();
				break;
			
			}
		}
	};
	
	//  onCreate
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_directory_tab);
		
		mListViewDirectories = (ListView)findViewById(R.id.listViewDirectories);
		mListViewDirectories.setAdapter(mDirectoryAdapter);
		
		mButtonAdd = (Button)findViewById(R.id.buttonAdd);
		mButtonAdd.setOnClickListener(ButtonOnClickListener);
		mButtonRemove = (Button)findViewById(R.id.buttonRemove);
		mButtonRemove.setOnClickListener(ButtonOnClickListener);
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
	//
	@Override
	public void onResume(){
		super.onResume();

		if ( mService != null )
		{
			mService.GetDirectoryList(mDirectoryList);
			mDirectoryAdapter.notifyDataSetChanged();
		}

	}

	
	//  onDestroy
	//
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

			mService.GetDirectoryList(mDirectoryList);
			mDirectoryAdapter.notifyDataSetChanged();
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


	//  Message Handler
	//
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

	


	boolean D = true;
	String TAG = "DirectoryTab";
}
