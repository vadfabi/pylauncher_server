package com.littlebytesofpi.pylauncher;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class SendTab extends ActionBarActivity implements  AdapterView.OnItemSelectedListener {

	
	//  User interface elements

	//  status string
	TextView TextViewStatus;
	
	//  available python files in the spinner
	ArrayList<PyFile> FilesList = new ArrayList<PyFile>();
	Spinner SpinnerFileSelector;
	ArrayAdapter<PyFile> AdapterFileSelector;

	//  launch button
	Button ButtonRunFile;
	
	//  arguments edit text
	EditText EditTextArgs;
	
	//  Results adapter
	//
	private ListView ListViewResults;
	private ArrayList<PyLaunchResult> ResultsList = new ArrayList<PyLaunchResult>();
	private ResultAdapter AdapterResultsList = new ResultAdapter(ResultsList,  this);
	
	private TextView TextViewSupportUs;
	
	//  Properties
	private boolean PaidVersion = true;
	
	//  onCreate
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_tab);

		TextViewStatus = (TextView)findViewById(R.id.textViewStatus);
		
		SpinnerFileSelector = (Spinner)findViewById(R.id.spinnerFile);
		AdapterFileSelector = new ArrayAdapter(this, android.R.layout.simple_spinner_item,  FilesList);
		AdapterFileSelector.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		SpinnerFileSelector.setAdapter(AdapterFileSelector);
		SpinnerFileSelector.setOnItemSelectedListener(SendTab.this);

		ButtonRunFile = (Button)findViewById(R.id.buttonRunFile);
		ButtonRunFile.setOnClickListener(ButtonOnClickListener);
		
		EditTextArgs = (EditText)findViewById(R.id.editTextArgs);
		EditTextArgs.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

		ListViewResults = (ListView)findViewById(R.id.listViewEvents);
		ListViewResults.setAdapter(AdapterResultsList);
		ListViewResults.setClickable(true);
		ListViewResults.setFastScrollEnabled(true);

		ListViewResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

				PyLaunchResult thisResult = ResultsList.get(position);
				thisResult.mExpanded = ! thisResult.mExpanded;
				AdapterResultsList.notifyDataSetChanged();
			}
		});

		TextViewSupportUs = (TextView)findViewById(R.id.textViewSaveButton);
		TextViewSupportUs.setTextColor(Color.parseColor("#FF0000"));
		TextViewSupportUs.setPaintFlags(TextViewSupportUs.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
		TextViewSupportUs.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				Intent intent = new Intent(SendTab.this, Support.class);
				startActivity(intent);
			}
		});
		
		if ( PaidVersion )
			TextViewSupportUs.setVisibility(View.GONE);
		
		//  setup default preferences
		PreferenceManager.setDefaultValues(this,  R.xml.preferences,  false);
		
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		
		//  force the overflow icon to show even if we have physical settings button
		//  will only work on android 4+
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ex) {
			// Ignore
		}
		
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

		if ( Service != null )
		{
			Service.GetFilesList(FilesList);
			AdapterFileSelector.notifyDataSetChanged();

			Service.GetLaunchResults(ResultsList);
			AdapterResultsList.notifyDataSetChanged();
		}
		
		FormatConnectionStatus();
		UpdateButtonUi();

	}

	
	//  onDestroy
	@Override
	public void onDestroy(){
		
		//  if we are connected to server, start the service so it stays connected
		if ( Service.IsConnectedToServer() )
		{
			Intent startIntent = new Intent(this, PyLauncherService.class);
			this.startService(startIntent);
		}
		else
		{
			Service.ShutDown();
		}
		
		UnbindFromService();
		
		super.onDestroy();
	}


	//  Service
	//
	PyLauncherService Service = null;

	//  ServiceConnection
	//
	private ServiceConnection Connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder service) {

			//  this is simple intra process, we can just get the service object
			LocalBinder binder = (LocalBinder) service;
			Service = binder.getService();
			Service.AddHandler(Handler);

			Service.GetFilesList(FilesList);
			AdapterFileSelector.notifyDataSetChanged();
			
			Service.GetLaunchResults(ResultsList);
			AdapterResultsList.notifyDataSetChanged();
			
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
			while ( ! Service.IsConnectedToServer() && (System.currentTimeMillis() - timeStartWait < 3000) )
			{
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {}
			}
			return null;
		}

		protected void onPostExecute(Void result ) {

			//  launch the connection settings if we are not connected
			if ( ! Service.IsConnectedToServer() )
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
		getApplicationContext().bindService(startIntent, Connection, Context.BIND_AUTO_CREATE);
	}


	//  UnbindFromService
	//
	void UnbindFromService() {
		if (Service != null) {

			Service.RemoveHandler(Handler);

			// Detach our existing connection
			getApplicationContext().unbindService(Connection);
		}
	}

	
	//  Message Handler
	//
	private final Handler Handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case PyLauncherService.MESSAGE_UPDATEDIRECTORIES:
				
				Service.GetFilesList(FilesList);
				AdapterFileSelector.notifyDataSetChanged();
				
				break;
				
			case PyLauncherService.MESSAGE_NEWEVENT:
				Service.GetLaunchResults(ResultsList);
				AdapterResultsList.notifyDataSetChanged();
				break;
			}
		}
	};  

//  Button Handlers
	//
	
	private void UpdateButtonUi()
	{
		//  get the python environment
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String env = sharedPrefs.getString("pref_environment", "python");

		ButtonRunFile.setText(env);

	}
	
	//
	//
	View.OnClickListener ButtonOnClickListener = new View.OnClickListener() 
	{
		@Override
		public void onClick(View v) 
		{
			switch ( v.getId() )
			{

			case R.id.buttonRunFile:
			{
				if ( ! Service.IsConnectedToServer() )
				{
					Toast.makeText(SendTab.this,  "You must be connected to the server.",  Toast.LENGTH_SHORT).show();
					return;
				}

				//  get the file and send it
				//  get the selected sensor
				PyFile selectedFile = (PyFile)SpinnerFileSelector.getSelectedItem();

				if ( selectedFile == null )
				{
					Toast.makeText(SendTab.this,  "No python file selected.", Toast.LENGTH_SHORT).show();
					return;
				}

				String args = EditTextArgs.getText().toString();

				//  save arguments for this file
				Editor editPref = PreferenceManager.getDefaultSharedPreferences(SendTab.this).edit();
				editPref.putString(selectedFile.FullPath, args);
				// Commit the edits
				editPref.commit();

				Service.RunPyFile(selectedFile, args);

				break;
			}
			}
		}
	};

	//  Spinner On Item Selected
	//
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, 
			int pos, long id) {

		if ( parent.getId() == R.id.spinnerFile )
		{
			PyFile selectedFile = FilesList.get(pos);
			
			//  see if we have some arguments for this
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(SendTab.this);
			
			//  initialize edit fields
			EditTextArgs.setText(sharedPrefs.getString(selectedFile.FullPath, ""));
		}
	}
	
	//  Spinner On Nothing Selected
	//
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// Another interface callback
	}
	
	//  Options Menu
	//
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.send_tab, menu);
	    return super.onCreateOptionsMenu(menu);
	}

	
	//  Options Menu Selected
	//
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		
		case R.id.action_buttons:
		{
			Intent intent = new Intent(SendTab.this, SendButtonsActivity.class);
			startActivity(intent);
		}
		return true;
		
		case R.id.action_settings: 
		{
			Intent intent = new Intent(SendTab.this, ConnectTab.class);
			startActivity(intent);
		}
		return true;
		
		case R.id.action_directories: 
		{
			Intent intent = new Intent(SendTab.this, DirectoryTab.class);
			startActivity(intent);
		}
		return true;
		

		case R.id.action_envsettings:
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.CustomDialogTheme));  
	           builder.setTitle("Python Environment");  
	           
	           final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice, getResources().getStringArray(R.array.enviro_array));  
	           
	           // get current selection
	           int selection = 0;
	           SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	           String env = sharedPrefs.getString("pref_environment", "python");
	           if ( env.compareTo("python3") == 0 )
	        	   selection = 1;

	           builder.setSingleChoiceItems(arrayAdapter, selection, new DialogInterface.OnClickListener() {  
	                @Override  
	                public void onClick(DialogInterface dialog, int which) {  
	                    //  update the settings
	                
	    				Editor editPref = PreferenceManager.getDefaultSharedPreferences(SendTab.this).edit();
	    				editPref.putString("pref_environment", getResources().getStringArray(R.array.enviro_array)[which]);
	    				// Commit the edits
	    				editPref.commit();
	    				
	    				SendTab.this.UpdateButtonUi();
	    				
	    				dialog.dismiss(); 
	    				
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
	           
			//Intent intent = new Intent(SendTab.this, SettingsActivity.class);
			//startActivity(intent);
		}
		
		default:
			return super.onOptionsItemSelected(item);
		}
		
	}
	
	
	//  Format Connection Status String
	//
	public void FormatConnectionStatus(){
		
		if ( Service != null && Service.IsConnectedToServer() )
		{
			//TextViewStatus.setText(String.format("Connected to " + Service.getConnectedToServerIp() + " on port " + Service.getConnectedToServerOnPort()) );
			TextViewStatus.setVisibility(View.GONE);
		}
		else
		{
			TextViewStatus.setVisibility(View.VISIBLE);
			TextViewStatus.setText("Tap Settings to connect to pyLauncher on the remote computer.");
		}
	}
	
	

	boolean D = false;
	String TAG = "SendTab";

}
