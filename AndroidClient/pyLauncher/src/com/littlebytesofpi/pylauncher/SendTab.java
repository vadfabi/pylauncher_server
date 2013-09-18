package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;

import android.app.Activity;
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
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class SendTab extends Activity implements  AdapterView.OnItemSelectedListener {

	
	//  User interface elements
	
	//  List of files is mapped to a spinner
	//
	ArrayList<PyFile> mFilesList = new ArrayList<PyFile>();
	Spinner mSpinnerFileSelector;
	ArrayAdapter<PyFile> mAdapter;
	
	//  launch button
	Button mButtonRunFile;
	//
	View.OnClickListener ButtonOnClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			//  get the file and send it
		//  get the selected sensor
			PyFile selectedFile = (PyFile)mSpinnerFileSelector.getSelectedItem();
			String args = mEditTextArgs.getText().toString();
			
			//  save arguments for this file
			Editor editPref = PreferenceManager.getDefaultSharedPreferences(SendTab.this).edit();
			editPref.putString(selectedFile.mFullPath, args);
			// Commit the edits
			editPref.commit();
			
			mService.RunPyFile(selectedFile, args);
		}
	};
	
	//  arguments edit text
	EditText mEditTextArgs;
	
	//  Results adapter
	//
	private ListView mListViewResults;
	private ArrayList<PyLaunchResult> mResultsList = new ArrayList<PyLaunchResult>();
	private ResultAdapter mResultsAdapter = new ResultAdapter(mResultsList,  this);
	
	
	//  onCreate
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_tab);

		mSpinnerFileSelector = (Spinner)findViewById(R.id.spinnerFile);
		mAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,  mFilesList);
		mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerFileSelector.setAdapter(mAdapter);
		mSpinnerFileSelector.setOnItemSelectedListener(SendTab.this);

		mButtonRunFile = (Button)findViewById(R.id.buttonRunFile);
		mButtonRunFile.setOnClickListener(ButtonOnClickListener);
		
		mEditTextArgs = (EditText)findViewById(R.id.editTextArgs);

		mListViewResults = (ListView)findViewById(R.id.listViewEvents);
		mListViewResults.setAdapter(mResultsAdapter);
		mListViewResults.setClickable(true);

		mListViewResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

				PyLaunchResult thisResult = mResultsList.get(position);
				thisResult.mExpanded = ! thisResult.mExpanded;


				mResultsAdapter.notifyDataSetChanged();
			}
		});

		//  setup default preferences
		PreferenceManager.setDefaultValues(this,  R.xml.preferences,  false);
		
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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

		if ( mService != null )
		{
			mService.GetFilesList(mFilesList);
			mAdapter.notifyDataSetChanged();

			mService.GetLaunchResults(mResultsList);
			mResultsAdapter.notifyDataSetChanged();
		}

	}

	
	//  onDestroy
	@Override
	public void onDestroy(){
		
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

			mService.GetFilesList(mFilesList);
			mAdapter.notifyDataSetChanged();
			
			mService.GetLaunchResults(mResultsList);
			mResultsAdapter.notifyDataSetChanged();
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
		Intent startIntent = new Intent(SendTab.this, PyLauncherService.class);
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
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case PyLauncherService.MESSAGE_UPDATEDIRECTORIES:
				
				mService.GetFilesList(mFilesList);
				mAdapter.notifyDataSetChanged();
				
				break;
				
			case PyLauncherService.MESSAGE_NEWEVENT:
				mService.GetLaunchResults(mResultsList);
				mResultsAdapter.notifyDataSetChanged();
				break;
			}
		}
	};  

//  select a sensor from the spinner
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, 
			int pos, long id) {

		if ( parent.getId() == R.id.spinnerFile )
		{
			PyFile selectedFile = mFilesList.get(pos);
			
			//  see if we have some arguments for this
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(SendTab.this);
			
			//  initialize edit fields
			mEditTextArgs.setText(sharedPrefs.getString(selectedFile.mFullPath, ""));
		}
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// Another interface callback
	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.directory_tab, menu);
		return true;
	}

	boolean D = true;
	String TAG = "SendTab";

}
