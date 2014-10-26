package com.littlebytesofpi.pylauncher;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.askerov.dynamicgrid.DynamicGridView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class SendButtonsActivity extends ActionBarActivity {

	TextView mTextViewStatus;
	
	//  Buttons grid view
	DynamicGridView mGridViewButtons;
	ArrayList<PyLauncherButton> mVisibleButtonsList = new ArrayList<PyLauncherButton>();
	private GridViewButtonsAdapter mGridViewAdapter = new GridViewButtonsAdapter(this, mVisibleButtonsList);
	
	//  Results adapter
	//
	private ListView mListViewResults;
	private ArrayList<PyLaunchResult> mResultsList = new ArrayList<PyLaunchResult>();
	private ResultAdapter mResultsAdapter = new ResultAdapter(mResultsList,  this);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_buttons);

		mTextViewStatus = (TextView)findViewById(R.id.textViewStatus);

		//  Buttons View
		mGridViewButtons = (DynamicGridView)findViewById(R.id.gridViewButtons);
		mGridViewButtons.setAdapter( mGridViewAdapter );

		mGridViewButtons.setOnDragListener(new DynamicGridView.OnDragListener() {
			@Override
			public void onDragStarted(int position) {
				Log.d("GridView", "drag started at position " + position);
			}

			@Override
			public void onDragPositionsChanged(int oldPosition, int newPosition) {
				Log.d("GridView", String.format("drag item position changed from %d to %d", oldPosition, newPosition));
			}        
		});

		//     TODO - does this get called
		mGridViewButtons.setOnDropListener(new DynamicGridView.OnDropListener()
		{
			@Override
			public void onActionDrop()
			{
				mGridViewButtons.stopEditMode();
				//  TODO - commit the order of the new list to the preferences
			}
		});


		//  Results List View
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
		
		
		// force the overflow icon to show even if we have physical settings
		// button
		// will only work on android 4+
		try 
		{
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) 
			{
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} 
		catch (Exception ex) 
		{
			// Ignore
		}
			
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
	

	public void FormatConnectionStatus(){
		
		if ( mService != null && mService.IsConnectedToServer() )
		{
			mTextViewStatus.setText(String.format("Connected to " + mService.getConnectedToServerIp() + " : " + mService.getConnectedToServerOnPort()) );
		}
		else
			mTextViewStatus.setText("Please tap settings to connect.");
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
			
			mService.getVisibleButtonList(mVisibleButtonsList);
			mGridViewAdapter.set(mVisibleButtonsList);
			mGridViewAdapter.notifyDataSetChanged();

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
		
		// bind to the service 
		Intent startIntent = new Intent(SendButtonsActivity.this, PyLauncherService.class);
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
				
				//  TODO
				
				break;
				
			case PyLauncherService.MESSAGE_NEWEVENT:
				mService.GetLaunchResults(mResultsList);
				mResultsAdapter.notifyDataSetChanged();
				break;
			}
		}
	};  
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.send_buttons, menu);
		return true;
	}
	
	
	//  Options menu function handlers
	//
	
	
	
	//  Functions for the grid view to call back
	//
	
	//  Grid View Item Click
	//
	public void GridViewItemClick(int position)
	{
		if ( mEditButtonsMode )
		{
			Intent intent = new Intent(SendButtonsActivity.this, EditButtonActivity.class);
			intent.putExtra("EditIndex", position);
			startActivityForResult(intent, ACTIVITYREQUEST_EDITBUTTON);
		}
		else if ( mDeleteButtonsMode )
		{
			//  
			PyLauncherButton thisButton = (PyLauncherButton)mGridViewButtons.getItemAtPosition(position);
			
			mService.RemoveButton(thisButton);
			
			//  update the grid view
			mService.getVisibleButtonList(mVisibleButtonsList);
			mGridViewAdapter.set(mVisibleButtonsList);
			mGridViewAdapter.notifyDataSetChanged();
		}
		else
		{
			//  run this function
			PyLauncherButton thisButton = (PyLauncherButton)mGridViewButtons.getItemAtPosition(position);
			
			mService.RunPyFile(thisButton.getPyFile(),  thisButton.getCommandLineArgs() );
		}
	}
	
	
	//  Grid View Item Long Click
	public void GridViewItemLongClick(int position)
	{
		//  what mode are we in
		if ( mEditButtonsMode || mDeleteButtonsMode )
		{
			return;
		}
		else
			mGridViewButtons.startEditMode(position);
		
	}
	
	
	static final int ACTIVITYREQUEST_NEWBUTTON = 99;
	static final int ACTIVITYREQUEST_EDITBUTTON = 100;
	
	//  Activity Result
	//
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch ( requestCode )
		{
		case ACTIVITYREQUEST_NEWBUTTON:
			{
				if ( resultCode == RESULT_OK )
				{
					mService.getVisibleButtonList(mVisibleButtonsList);
					mGridViewAdapter.set(mVisibleButtonsList);
					mGridViewAdapter.notifyDataSetChanged();
				}
			}
			break;

		case ACTIVITYREQUEST_EDITBUTTON:
		{
			if ( resultCode == RESULT_OK )
			{
				mGridViewAdapter.notifyDataSetChanged();
			}
		}
		default:
			break;
		}
	}

	boolean mEditButtonsMode = false;
	boolean mDeleteButtonsMode = false;
	
	@Override
	public void onBackPressed()
	{
		//  use back to cancel edit mode, otherwise on back
		if ( mEditButtonsMode == true )
		{
			StopEditMode();
			//  TODO - commit the list
		}
		else if ( mDeleteButtonsMode == true )
		{
			StopEditMode();
			//  TODO - commit the list
		}
		else
			super.onBackPressed();
		
		return;
	}
	
	protected void StopEditMode()
	{
		mEditButtonsMode = false;
		mDeleteButtonsMode = false;
		mGridViewButtons.stopWobble(true);
		mGridViewButtons.stopEditMode();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) 
		{
		
		case R.id.action_launcher:
		{
			//  end activity
			finish();
		}
		return true;
		
		case R.id.action_settings: 
		{
			Intent intent = new Intent(SendButtonsActivity.this, ConnectTab.class);
			startActivity(intent);
		}
		return true;
		
		case R.id.action_directories: 
		{
			Intent intent = new Intent(SendButtonsActivity.this, DirectoryTab.class);
			startActivity(intent);
		}
		return true;
		
		case R.id.action_newButton:
		{
			//  stop edit mode
			StopEditMode();
			
			Intent intent = new Intent(SendButtonsActivity.this, EditButtonActivity.class);
			startActivityForResult(intent, ACTIVITYREQUEST_NEWBUTTON);
		}
		return true;
		
		case R.id.action_editButtons:
		{
			if ( mEditButtonsMode )
			{
				StopEditMode();
			}
			else
			{
				mDeleteButtonsMode = false;
				mEditButtonsMode = true;
				mGridViewButtons.startWobbleAnimation();
			}
		
		}
		return true;
		
		case R.id.action_delButtons:
		{
			if ( mDeleteButtonsMode )
			{
				StopEditMode();
			}
			else
			{
				mDeleteButtonsMode = true;
				mEditButtonsMode = false;
				mGridViewButtons.startWobbleAnimation();
			}
		}
		return true;
		
		default:
			return super.onOptionsItemSelected(item);
		}
		
	}
}
