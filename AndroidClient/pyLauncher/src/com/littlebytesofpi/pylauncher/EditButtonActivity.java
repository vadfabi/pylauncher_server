package com.littlebytesofpi.pylauncher;

import java.lang.reflect.Field;
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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
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
	PyLauncherButton EditingButton;
	
	//  User interface elements
	
	//  icon for the button
	ImageButton IconButton;
	int IconIndex  = -1;
	
	//  text view for name
	EditText EditTextName;
	
	//  List of files is mapped to a spinner
	//
	ArrayList<PyFile> FilesList = new ArrayList<PyFile>();
	Spinner SpinnerFileSelector;
	ArrayAdapter<PyFile> FileAdapter;

	Spinner SpinnerEnvironment;
	
	//  launch button
	Button ButtonRunFile;
	
	//  arguments edit text
	EditText EditTextArgs;
	
	//  Results adapter
	//
	private ListView ListViewResults;
	private ArrayList<PyLaunchResult> ResultsList = new ArrayList<PyLaunchResult>();
	private ResultAdapter ResultsAdapter = new ResultAdapter(ResultsList,  this);
	
	//  save button
	private Button ButtonSave;
	

	//  onCreate
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_editbutton);
		
		IconButton = (ImageButton)findViewById(R.id.imageButtonIcon);
		IconButton.setOnClickListener(ButtonOnClickListener);
		
		EditTextName = (EditText)findViewById(R.id.editTextName);
		EditTextName.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		
		SpinnerEnvironment = (Spinner)findViewById(R.id.spinnerEnvironment);
		SpinnerEnvironment.setOnItemSelectedListener(EditButtonActivity.this);
		
		SpinnerFileSelector = (Spinner)findViewById(R.id.spinnerFile);
		FileAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,  FilesList);
		FileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		SpinnerFileSelector.setAdapter(FileAdapter);
		SpinnerFileSelector.setOnItemSelectedListener(EditButtonActivity.this);

		ButtonRunFile = (Button)findViewById(R.id.buttonRunFile);
		ButtonRunFile.setOnClickListener(ButtonOnClickListener);
		
		EditTextArgs = (EditText)findViewById(R.id.editTextArgs);
		EditTextArgs.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		
		ListViewResults = (ListView)findViewById(R.id.listViewEvents);
		ListViewResults.setAdapter(ResultsAdapter);
		ListViewResults.setClickable(true);
		ListViewResults.setFastScrollEnabled(true);

		ListViewResults.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

				PyLaunchResult thisResult = ResultsList.get(position);
				thisResult.mExpanded = ! thisResult.mExpanded;
				ResultsAdapter.notifyDataSetChanged();
			}
		});

		ButtonSave = (Button)findViewById(R.id.buttonSave);
		ButtonSave.setOnClickListener(ButtonOnClickListener);

		//  setup default preferences
		PreferenceManager.setDefaultValues(this,  R.xml.preferences,  false);
		
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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
			FileAdapter.notifyDataSetChanged();

			Service.GetLaunchResults(ResultsList);
			ResultsAdapter.notifyDataSetChanged();
		}
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
		
		UnbindFroService();
		
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
			FileAdapter.notifyDataSetChanged();
			
			Service.GetLaunchResults(ResultsList);
			ResultsAdapter.notifyDataSetChanged();
			
			//  set the index of the button we are editing
			int editButtonIndex = getIntent().getIntExtra("EditIndex",  -1);
			
			ArrayList<PyLauncherButton> visibleButtonList = new ArrayList<PyLauncherButton>();
			Service.getVisibleButtonList(visibleButtonList);
			if ( editButtonIndex >= 0 && visibleButtonList.size() > editButtonIndex )
			{
				//  get button from list
				EditingButton = visibleButtonList.get(editButtonIndex);
				
				//  setup the environment spinner
				if ( EditingButton.Environment.compareTo("python3") == 0 )
					SpinnerEnvironment.setSelection(1);
				
				//  find the matching py file
				int i = 0;
				for ( i = 0; i < FilesList.size(); i++ )
				{
					if ( FilesList.get(i).FullPath.compareTo(EditingButton.PyFile.FullPath) == 0)
					{
						SpinnerFileSelector.setSelection(i);
						break;
					}
				}
				
				if ( i == FilesList.size() )
				{
					//  did not find their file, it must be gone
					Toast.makeText(EditButtonActivity.this,  "The python file for this button can not be found.", Toast.LENGTH_LONG).show();
					SpinnerFileSelector.setSelection(-1);
				}
			}
			else
			{
				EditingButton = new PyLauncherButton();

				//  get the python environment
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(EditButtonActivity.this);
				String env = sharedPrefs.getString("pref_environment", "python");

				EditingButton.Environment = env;
			}

			//  set the icon for this button
			IconIndex = EditingButton.getIcon();
			

			IconButton.setImageDrawable(Service.GetButtonDrawable(IconIndex));
			EditTextName.setText(EditingButton.getTitle());
			EditTextArgs.setText(EditingButton.getCommandLineArgs());
			
			EditButtonActivity.this.UpdateButtonUi();
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
		getApplicationContext().bindService(startIntent, Connection, Context.BIND_AUTO_CREATE);
	}


	//  UnbindFroService
	//
	void UnbindFroService() {
		if (Service != null) {

			Service.RemoveHandler(Handler);

			// Detach our existing connection
			getApplicationContext().unbindService(Connection);
		}
	}

	
	//  Message Handler
	private final Handler Handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case PyLauncherService.MESSAGE_UPDATEDIRECTORIES:
				
				Service.GetFilesList(FilesList);
				FileAdapter.notifyDataSetChanged();
				
				break;
				
			case PyLauncherService.MESSAGE_NEWEVENT:
				Service.GetLaunchResults(ResultsList);
				ResultsAdapter.notifyDataSetChanged();
				break;
			}
		}
	};  

	
	private void UpdateButtonUi()
	{
		if ( EditingButton != null )
			ButtonRunFile.setText(EditingButton.getEnvironment());

	}
	
	
	//  Buttons On CLick Listener
	//
	View.OnClickListener ButtonOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			switch ( v.getId() )
			{

			case R.id.buttonRunFile:
			{
				if ( ! Service.IsConnectedToServer() )
				{
					Toast.makeText(EditButtonActivity.this,  "You must be connected to the server.",  Toast.LENGTH_SHORT).show();
					return;
				}
				
				//  get the file and send it
				//  get the selected sensor
				PyFile selectedFile = (PyFile)SpinnerFileSelector.getSelectedItem();
				
				if ( selectedFile == null )
				{
					Toast.makeText(EditButtonActivity.this,  "No python file selected.", Toast.LENGTH_SHORT).show();
					return;
				}
				
				String args = EditTextArgs.getText().toString();

				//  save arguments for this file
				Editor editPref = PreferenceManager.getDefaultSharedPreferences(EditButtonActivity.this).edit();
				editPref.putString(selectedFile.FullPath, args);
				// Commit the edits
				editPref.commit();

				Service.RunPyFile(EditButtonActivity.this.EditingButton.getEnvironment(),  selectedFile, args);
			}
			break;
			
			case R.id.buttonSave:
			{
				//  save this button
				PyFile selectedFile = (PyFile)SpinnerFileSelector.getSelectedItem();
				
				if ( selectedFile == null )
					selectedFile = new PyFile("");
				
				EditingButton.Environment = SpinnerEnvironment.getSelectedItem().toString();
				EditingButton.PyFile = selectedFile;
				EditingButton.Icon = IconIndex;
				EditingButton.Title = EditTextName.getText().toString();
				EditingButton.CommandLineArgs = EditTextArgs.getText().toString();
				
				Service.UpdateButton(EditingButton);
				
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
					IconIndex = data.getIntExtra("select",  -1);
					IconButton.setImageDrawable(Service.GetButtonDrawable(IconIndex));
				}
			}
			break;

		default:
			break;
		}
	}
	
	
	//  Files spinner on item selected
	//
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, 
			int pos, long id) {

		switch ( parent.getId() )
		{
		case R.id.spinnerFile:
			PyFile selectedFile = FilesList.get(pos);
			break;
			
		case R.id.spinnerEnvironment:
			if ( EditingButton != null )
			{
				EditingButton.Environment = SpinnerEnvironment.getSelectedItem().toString();
				UpdateButtonUi();
			}
		}
	}
	
	//  On nothing selected
	//
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// Another interface callback
	}
	
	
	//  Create Options Menu
	//
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu items for use in the action bar
//		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.editbuttonactivity, menu);
		return super.onCreateOptionsMenu(menu);
	}
	

	
	//  Options Menu Selected
	//
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		
//		case R.id.action_envsettings:
//		{
//			Intent intent = new Intent(this, SettingsActivity.class);
//			startActivity(intent);
//		}
		
		default:
			return super.onOptionsItemSelected(item);
		}
		
	}
	
	
	
	

	boolean D = false;
	String TAG = "EditButton";

}
