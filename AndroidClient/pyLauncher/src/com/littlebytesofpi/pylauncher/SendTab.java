package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class SendTab extends ActionBarActivity implements  AdapterView.OnItemSelectedListener {

	
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

			switch ( v.getId() )
			{

			case R.id.buttonRunFile:
			{
				if ( ! mService.IsConnectedToServer() )
				{
					Toast.makeText(SendTab.this,  "You must be connected to the server.",  Toast.LENGTH_SHORT).show();
					return;
				}
				
				//  get the file and send it
				//  get the selected sensor
				PyFile selectedFile = (PyFile)mSpinnerFileSelector.getSelectedItem();
				
				if ( selectedFile == null )
				{
					Toast.makeText(SendTab.this,  "No python file selected.", Toast.LENGTH_SHORT).show();
					return;
				}
				
				String args = mEditTextArgs.getText().toString();

				//  save arguments for this file
				Editor editPref = PreferenceManager.getDefaultSharedPreferences(SendTab.this).edit();
				editPref.putString(selectedFile.mFullPath, args);
				// Commit the edits
				editPref.commit();

				mService.RunPyFile(selectedFile, args);
				
				break;
			}

			

			}
		}
	};

	
	
	TextView mTextViewStatus;
	
	//  arguments edit text
	EditText mEditTextArgs;
	
	//  Results adapter
	//
	private ListView mListViewResults;
	private ArrayList<PyLaunchResult> mResultsList = new ArrayList<PyLaunchResult>();
	private ResultAdapter mResultsAdapter = new ResultAdapter(mResultsList,  this);
	
	private boolean PaidVersion = true;
	private TextView mTextViewSupportUs;
	
	//  onCreate
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_tab);

		mTextViewStatus = (TextView)findViewById(R.id.textViewStatus);
		
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
		mListViewResults.setFastScrollEnabled(true);

		mListViewResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

				PyLaunchResult thisResult = mResultsList.get(position);
				thisResult.mExpanded = ! thisResult.mExpanded;


				mResultsAdapter.notifyDataSetChanged();
			}
		});

		mTextViewSupportUs = (TextView)findViewById(R.id.textView1);
		mTextViewSupportUs.setTextColor(Color.parseColor("#FF0000"));
		mTextViewSupportUs.setPaintFlags(mTextViewSupportUs.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
		mTextViewSupportUs.setOnClickListener( new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent intent = new Intent(SendTab.this, Support.class);
				startActivity(intent);
			}
		});
		
		if ( PaidVersion )
			mTextViewSupportUs.setVisibility(View.GONE);
		
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
		
		FormatConnectionStatus();

	}

	
	//  onDestroy
	@Override
	public void onDestroy(){
		
		//  if we are connected to server, start the service so it stays connected
		if ( mService.IsConnectedToServer() )
		{
			Intent startIntent = new Intent(this, PyLauncherService.class);
			this.startService(startIntent);
		}
		else
		{
			mService.ShutDown();
		}
		
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
			
			new WaitForConnectionTask().execute();
		}


		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
		}
	};
	
	
	
	//  WaitForConnectionTask
	//
	class WaitForConnectionTask extends AsyncTask<Void, Void, Void> {

		protected Void doInBackground(Void... param ) {
			
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(SendTab.this);
			String ipAddress = sharedPrefs.getString("pref_serveripaddress", "");

			if ( ipAddress.length() == 0 )
				return null;
			
			long timeStartWait = System.currentTimeMillis();
			while ( ! mService.IsConnectedToServer() && (System.currentTimeMillis() - timeStartWait < 3000) )
			{
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {}
			}
			return null;
		}

		protected void onPostExecute(Void result ) {

			//  launch the connection settings if we are not connected
			if ( ! mService.IsConnectedToServer() )
			{
				Intent intent = new Intent(SendTab.this, ConnectTab.class);
				startActivity(intent);
			}
			else
			{
				FormatConnectionStatus();
			}
		}
	};


	//  BindToService
	//
	private void BindToService() {
		if ( D ) Log.d(TAG, "bindToService()");

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

	//  select a python file from the spinner
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
	
	
	
	public void FormatConnectionStatus(){
		
		if ( mService != null && mService.IsConnectedToServer() )
		{
			mTextViewStatus.setText(String.format("Connected to " + mService.getConnectedToServerIp() + " on port " + mService.getConnectedToServerOnPort()) );
		}
		else
			mTextViewStatus.setText("Not connected. \nPlease tap connection settings.");
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.send_tab, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		case R.id.action_settings: 
		{
			Intent intent = new Intent(SendTab.this, ConnectTab.class);
			startActivity(intent);
		}
		case R.id.action_directories: 
		{
			Intent intent = new Intent(SendTab.this, DirectoryTab.class);
			startActivity(intent);
		}
		
			
		return true;

		default:
			return super.onOptionsItemSelected(item);
		}

	}
	
	

	boolean D = false;
	String TAG = "SendTab";

}
