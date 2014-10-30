package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class EditButtonActivity extends ActionBarActivity implements  AdapterView.OnItemSelectedListener {

	//  Index of button we are editing
	PyLauncherButton mEditingButton;
	
	//  User interface elements
	
	//  icon for the button
	ImageButton mIconButton;
	int mIconIndex  = -1;
	
	//  text view for name
	EditText mEditTextName;
	
	//  List of files is mapped to a spinner
	//
	ArrayList<PyFile> mFilesList = new ArrayList<PyFile>();
	Spinner mSpinnerFileSelector;
	ArrayAdapter<PyFile> mAdapter;

	//  launch button
	Button mButtonRunFile;
	
	//  arguments edit text
	EditText mEditTextArgs;
	
	//  Results adapter
	//
	private ListView mListViewResults;
	private ArrayList<PyLaunchResult> mResultsList = new ArrayList<PyLaunchResult>();
	private ResultAdapter mResultsAdapter = new ResultAdapter(mResultsList,  this);
	
	//  save button
	private Button mButtonSave;
	
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
					Toast.makeText(EditButtonActivity.this,  "You must be connected to the server.",  Toast.LENGTH_SHORT).show();
					return;
				}
				
				//  get the file and send it
				//  get the selected sensor
				PyFile selectedFile = (PyFile)mSpinnerFileSelector.getSelectedItem();
				
				if ( selectedFile == null )
				{
					Toast.makeText(EditButtonActivity.this,  "No python file selected.", Toast.LENGTH_SHORT).show();
					return;
				}
				
				String args = mEditTextArgs.getText().toString();

				//  save arguments for this file
				Editor editPref = PreferenceManager.getDefaultSharedPreferences(EditButtonActivity.this).edit();
				editPref.putString(selectedFile.mFullPath, args);
				// Commit the edits
				editPref.commit();

				mService.RunPyFile(selectedFile, args);
			}
			break;
			
			case R.id.buttonSave:
			{
				//  save this button
				PyFile selectedFile = (PyFile)mSpinnerFileSelector.getSelectedItem();
				
				if ( selectedFile == null )
					selectedFile = new PyFile("");
				
				mEditingButton.mPyFile = selectedFile;
				mEditingButton.mIcon = mIconIndex;
				mEditingButton.mTitle = mEditTextName.getText().toString();
				mEditingButton.mCommandLineArgs = mEditTextArgs.getText().toString();
				
				mService.UpdateButton(mEditingButton);
				
				//  done
				setResult(RESULT_OK);
				finish();
			}
			break;
			
			case R.id.imageButtonIcon:
			{
				// open pick image button activity for result
				Intent intent = new Intent(EditButtonActivity.this, SelectButton.class);
				startActivityForResult(intent, 99);
			}
			break;
			
			}
		}
	};
	
	//  Activity Result
	//
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		switch ( requestCode )
		{
		case 99:
			{
				if ( resultCode == RESULT_OK )
				{
					mIconIndex = data.getIntExtra("select",  -1);
					mIconButton.setImageDrawable(mService.GetButtonDrawable(mIconIndex));
				}
			}
			break;

		default:
			break;
		}
	}
	
	
	//  onCreate
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_editbutton);
		
		mIconButton = (ImageButton)findViewById(R.id.imageButtonIcon);
		mIconButton.setOnClickListener(ButtonOnClickListener);
		
		mEditTextName = (EditText)findViewById(R.id.editTextName);
		mEditTextName.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		
		mSpinnerFileSelector = (Spinner)findViewById(R.id.spinnerFile);
		mAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,  mFilesList);
		mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerFileSelector.setAdapter(mAdapter);
		mSpinnerFileSelector.setOnItemSelectedListener(EditButtonActivity.this);

		mButtonRunFile = (Button)findViewById(R.id.buttonRunFile);
		mButtonRunFile.setOnClickListener(ButtonOnClickListener);
		
		mEditTextArgs = (EditText)findViewById(R.id.editTextArgs);
		mEditTextArgs.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		
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

		mButtonSave = (Button)findViewById(R.id.buttonSave);
		mButtonSave.setOnClickListener(ButtonOnClickListener);
		
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
			
			//  set the index of the button we are editing
			int editButtonIndex = getIntent().getIntExtra("EditIndex",  -1);
			
			ArrayList<PyLauncherButton> visibleButtonList = new ArrayList<PyLauncherButton>();
			mService.getVisibleButtonList(visibleButtonList);
			if ( editButtonIndex >= 0 && visibleButtonList.size() > editButtonIndex )
			{
				//  get button from list
				mEditingButton = visibleButtonList.get(editButtonIndex);
			}
			else
			{
				mEditingButton = new PyLauncherButton();
			}
			

			mIconButton.setImageDrawable(mService.GetButtonDrawable(mIconIndex));
			mEditTextName.setText(mEditingButton.getTitle());
			mEditTextArgs.setText(mEditingButton.getCommandLineArgs());
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
			
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(EditButtonActivity.this);
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
				//  TODO - exit
				//  Intent intent = new Intent(EditButtonActivity.this, ConnectTab.class);
				//startActivity(intent);
			}
			
			//  we are connected
		}
	};


	//  BindToService
	//
	private void BindToService() {
		if ( D ) Log.d(TAG, "bindToService()");

		// bind to the service 
		Intent startIntent = new Intent(EditButtonActivity.this, PyLauncherService.class);
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
		}
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// Another interface callback
	}
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu items for use in the action bar
	   // MenuInflater inflater = getMenuInflater();
	   // inflater.inflate(R.menu.send_tab, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		
		default:
			return super.onOptionsItemSelected(item);
		}

	}
	
	

	boolean D = false;
	String TAG = "EditButton";

}
