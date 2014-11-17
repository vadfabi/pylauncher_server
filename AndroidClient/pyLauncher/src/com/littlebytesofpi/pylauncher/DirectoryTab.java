package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;

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
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class DirectoryTab extends ActionBarActivity {

	//  User Interface Elements
	private ListView ListViewDirectories;
	private ArrayList<PyFile> DirectoryList = new ArrayList<PyFile>();
	private DirectoriesAdapter DirectoryAdapter = new DirectoriesAdapter(DirectoryList,  this);
	
	
	Button ButtonAdd;
	Button ButtonRemove;
	
	//  onCreate
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_directory_tab);
		
		ListViewDirectories = (ListView)findViewById(R.id.listViewDirectories);
		ListViewDirectories.setAdapter(DirectoryAdapter);
		
		ButtonAdd = (Button)findViewById(R.id.buttonAdd);
		ButtonAdd.setOnClickListener(ButtonOnClickListener);
		ButtonRemove = (Button)findViewById(R.id.buttonRemove);
		ButtonRemove.setOnClickListener(ButtonOnClickListener);
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
			mService.GetDirectoryList(DirectoryList);
			DirectoryAdapter.notifyDataSetChanged();
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

			mService.GetDirectoryList(DirectoryList);
			DirectoryAdapter.notifyDataSetChanged();
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
				
				mService.GetDirectoryList(DirectoryList);
				DirectoryAdapter.notifyDataSetChanged();
				
				break;
			}
		}
	};  

View.OnClickListener ButtonOnClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			switch ( v.getId() )
			{
			case R.id.buttonAdd:
			{
				final AlertDialog.Builder alert = new AlertDialog.Builder(DirectoryTab.this);
				alert.setTitle("Enter Directory Name");
				final EditText input = new EditText(DirectoryTab.this);
				
				//  set default file name based on date / time
				input.setText("");
				input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
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
			}
				break;
				
			case R.id.buttonRemove:
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(DirectoryTab.this);  
		           builder.setTitle("Remove Directory");  
		           final ArrayAdapter<PyFile> arrayAdapter = new ArrayAdapter<PyFile>(DirectoryTab.this,  
		                     android.R.layout.select_dialog_singlechoice);  

		            
		           for (PyFile nextFile : DirectoryList )
		           {
		                arrayAdapter.add(nextFile);  
		           }  
		           builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {  
		                @Override  
		                public void onClick(DialogInterface dialog, int which) {  
		                    DirectoryTab.this.mService.RemoveDirectory(arrayAdapter.getItem(which).GetPath());
		                }  
		           });  
		           builder.setPositiveButton("Cancel",  
		                     new DialogInterface.OnClickListener() {  
		                          @Override  
		                          public void onClick(DialogInterface dialog, int which) {  
		                               dialog.dismiss();  
		                          }  
		                     });  
		           AlertDialog alert = builder.create();  
		           alert.show();  
			}
				break;
			
			}
		}
	};
	
	//  Override onBack to save directory state on the way out
	//
	@Override
	public void onBackPressed()
	{
		mService.SaveDirectoryList();
		super.onBackPressed();
	}


	boolean D = false;
	String TAG = "DirectoryTab";
}
