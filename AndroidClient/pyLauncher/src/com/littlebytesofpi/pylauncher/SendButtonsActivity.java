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
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class SendButtonsActivity extends ActionBarActivity {

	//  User Interface Elements
	//
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
	
	
	//  On Create
	//
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_buttons);

		mTextViewStatus = (TextView)findViewById(R.id.textViewStatus);

		//  Buttons Grid View View
		mGridViewButtons = (DynamicGridView)findViewById(R.id.gridViewButtons);
		mGridViewButtons.setAdapter( mGridViewAdapter );
		mGridViewButtons.setOnDragListener(GridViewOnDragListener);
		mGridViewButtons.setOnDropListener(GridViewOnDropListener);
	
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

		if (Service == null) 
			BindToService();	
	}


	//  onResume
	@Override
	public void onResume(){
		super.onResume();

		if ( Service != null )
		{
			ResetGridView();
		
			Service.GetLaunchResults(mResultsList);
			mResultsAdapter.notifyDataSetChanged();
		}
		
		FormatStatus();

	}

	
	//  onDestroy
	//
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
	

	//  Service Handling
	//
	PyLauncherService Service = null;

	//  ServiceConnection
	//
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder service) {

			//  this is simple intra process, we can just get the service object
			LocalBinder binder = (LocalBinder) service;
			Service = binder.getService();
			Service.AddHandler(Handler);
			
			ResetGridView();
			FormatStatus();

			Service.GetLaunchResults(mResultsList);
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
		if (Service != null) {

			Service.RemoveHandler(Handler);

			// Detach our existing connection
			getApplicationContext().unbindService(mConnection);
		}
	}
	
	

	//  Message Handler
	private final Handler Handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case PyLauncherService.MESSAGE_UPDATEDIRECTORIES:
				
				ResetGridView();
				
				break;
				
			case PyLauncherService.MESSAGE_NEWEVENT:
				Service.GetLaunchResults(mResultsList);
				mResultsAdapter.notifyDataSetChanged();
				
				break;
			}
		}
	};  
	
	
	//  Create Options Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.send_buttons, menu);
		return true;
	}
	
	
	// Options Menu Selected
	//
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
			StopEditingMode(true);
			
			if ( ! Service.IsConnectedToServer() )
			{
				Toast.makeText(this,  "You must be connected to the server to create buttons.", Toast.LENGTH_LONG).show();
				return true;
			}
			
			Intent intent = new Intent(SendButtonsActivity.this, EditButtonActivity.class);
			startActivityForResult(intent, ACTIVITYREQUEST_NEWBUTTON);
		}
		return true;
		
		case R.id.action_editButtons:
		{
			StartEditMode();
		
		}
		return true;
		
		case R.id.action_delButtons:
		{
			StartDeleteMode();
		}
		return true;
		
		default:
			return super.onOptionsItemSelected(item);
		}
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
					ResetGridView();
				}
			}
			break;

		case ACTIVITYREQUEST_EDITBUTTON:
		{
			if ( resultCode == RESULT_OK )
			{
				ResetGridView();
			}
		}
		default:
			break;
		}
	}
	
	
	//  Override onBack to handle exit edit mode
	//
	@Override
	public void onBackPressed()
	{
		//  use back to cancel edit mode, otherwise on back
		if ( GridEditMode == true )
		{
			StopEditingMode(true);
		}
		else if ( GridDeleteMode == true )
		{
			StopEditingMode(true);
		}
		else if ( GridDragMode )
		{
			StopEditingMode(true);
		}
		else
		{
			super.onBackPressed();
		}
		
		return;
	}
	

	//  Grid View Handling
	//
	boolean GridEditMode = false;
	boolean GridDeleteMode = false;
	boolean GridDragMode = false;
	int 	mDragIndex = -1;
	
	//  On Drag Listener
	//
	private DynamicGridView.OnDragListener GridViewOnDragListener = new DynamicGridView.OnDragListener() {

		@Override
		public void onDragStarted(int position) {
			Log.d("GridView", "drag started at position " + position);
		}

		@Override
		public void onDragPositionsChanged(int oldPosition, int newPosition) {
			Log.d("GridView", String.format("drag item position changed from %d to %d", oldPosition, newPosition));
		}        
	};

	
	//  On Drop Listener
	//
	private DynamicGridView.OnDropListener GridViewOnDropListener = new DynamicGridView.OnDropListener() 
	{
		@Override
		public void onActionDrop() {
			
			Log.d("GridView", String.format("Drop item "));
			
			//  update the button list
			Service.UpdateButtonsList(mGridViewAdapter.getItems());

			//  end editing mode
			StopEditingMode(false);
			
			//  reset the grid view
			ResetGridView();
		}
	};
	
	
	//  Grid View Item Click
	//
	public void GridViewItemClick(int position)
	{
		//  check our current UI mode
		if ( GridEditMode )
		{	
			if ( ! Service.IsConnectedToServer() )
			{
				Toast.makeText(this,  "You must be connected to the server to create buttons.", Toast.LENGTH_LONG).show();
				StopEditingMode(true);
				return;
			}
			//  edit this button - launch the edit activity
			Intent intent = new Intent(SendButtonsActivity.this, EditButtonActivity.class);
			intent.putExtra("EditIndex", position);
			startActivityForResult(intent, ACTIVITYREQUEST_EDITBUTTON);
		}
		else if ( GridDeleteMode )
		{
			//  delete this button
			PyLauncherButton thisButton = (PyLauncherButton)mGridViewButtons.getItemAtPosition(position);
			Service.RemoveButton(thisButton);
			
			//  update the grid view
			ResetGridView();
			
			//  if we have deleted our last button, exit edit mode
			if ( Service.ButtonsList.size() == 0 )
				StopEditingMode(false);
		}
		else if ( GridDragMode )
		{
			//  click during drag mode is only handled by android 2.x
			if ( ! isUsingDynamicGrid() )
			{
				//  get this button, and the dragging button
				PyLauncherButton dragButton = mVisibleButtonsList.get(mDragIndex);

				// drop onto same spot?
				if ( position == mDragIndex )
				{
					StopEditingMode(true);
					return;
				}
				
				//  add this button at the drop position
				mVisibleButtonsList.add(position, dragButton);
				
				//  remove the button from where it used to be
				if ( position > mDragIndex )
				{
					//  drop is past the drag, remove at the drag position
					mVisibleButtonsList.remove(mDragIndex);
					//  roll over the last button
					PyLauncherButton lastButton = mVisibleButtonsList.remove(mVisibleButtonsList.size()-1);
					mVisibleButtonsList.add(0,lastButton);
				}
				else if ( position < mDragIndex )
				{
					//  drop is before the drag, remove at drag position plus one
					mVisibleButtonsList.remove(mDragIndex+1);
				}
				
				//  update buttons
				Service.UpdateButtonsList(mVisibleButtonsList);
				
				//  end editing mode
				StopEditingMode(false);
				
				//  reset the view
				ResetGridView();
			}
		}
		else
		{
			if ( ! Service.IsConnectedToServer() )
			{
				Toast.makeText(this,  "You must be connected to the server to create buttons.", Toast.LENGTH_LONG).show();
				return;
			}
			
			//  run this function
			PyLauncherButton thisButton = (PyLauncherButton)mGridViewButtons.getItemAtPosition(position);
			
			Service.RunPyFile(thisButton.getPyFile(),  thisButton.getCommandLineArgs() );
		}
	}
	
	
	//  Grid View Item Long Click
	//
	public void GridViewItemLongClick(int position)
	{
		//  what mode are we in
		if ( GridEditMode || GridDeleteMode || GridDragMode )
		{
			//  ignore long clicks when we are in edit mode
			return;
		}
		else
		{
			//  start drag mode
			StartDragMode(position);
		}
	}
	
	//  Start Edit Mode
	//
	protected void StartEditMode()
    {
    	StopEditingMode(true);
    	
    	GridEditMode = true;
    	mGridViewButtons.startWobbleAnimation();
    	
    	if ( ! isUsingDynamicGrid() )
    	{
    		mGridViewAdapter.notifyDataSetChanged();
    	}
    	else
    	{
    		mGridViewButtons.startEditMode();
    	}
    	
    	FormatStatus();
    }
    
	
	//  Start Delete Mode
	//
    protected void StartDeleteMode()
    {
    	StopEditingMode(true);
    	
    	GridDeleteMode = true;
    	mGridViewButtons.startWobbleAnimation();
    	
    	if ( ! isUsingDynamicGrid() )
    	{
    		mGridViewAdapter.notifyDataSetChanged();
    	}
    	else
    	{
    		mGridViewButtons.startEditMode();
    	}
    	
    	FormatStatus();
    }
    
    
    //  Start Drag Mode
    //
    protected void StartDragMode(int position)
    {
    	StopEditingMode(true);
    	
    	GridDragMode = true;
    	
    	if ( ! isUsingDynamicGrid() )
    	{
    		Vibrator myVib = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);
    		myVib.vibrate(100);
    		
    		mDragIndex = position;
    		mGridViewAdapter.notifyDataSetChanged();
    	}
    	else
    	{
    		mGridViewButtons.startEditMode(position);
    	}
    	
    	FormatStatus();
    }
    
	
    //  Stop Editing Mode
    //
	protected void StopEditingMode(boolean notifyAdapter)
	{
		GridDragMode = false;
		GridEditMode = false;
		GridDeleteMode = false;
		
		// handle grid view state
		mGridViewButtons.stopWobble(true);
		mGridViewButtons.stopEditMode();
		
		//  notify adapter
		if ( notifyAdapter )
			mGridViewAdapter.notifyDataSetChanged();
		
		FormatStatus();
	}

	
	//  Reset Grid View
	//
	private void ResetGridView()
	{
		Service.getVisibleButtonList(mVisibleButtonsList);
		mGridViewAdapter.set(mVisibleButtonsList);
		mGridViewAdapter.notifyDataSetChanged();
	}
	
//  Format Status String
	//
	public void FormatStatus()
	{
		mTextViewStatus.setVisibility(View.VISIBLE);
		
		if ( GridDeleteMode )
		{
			mTextViewStatus.setText("Tap a button to delete");
		}
		else if ( GridEditMode )
		{
			mTextViewStatus.setText("Tap a button to edit.");
		}
		else if ( GridDragMode )
		{
			if ( isUsingDynamicGrid() )
				mTextViewStatus.setText("Drag and drop the button to a new location.");
			else
				mTextViewStatus.setText("Select new location for the button.");
		}
		else
		{
			//  no special mode in play, format connection status string
			if ( Service != null && Service.IsConnectedToServer() )
			{
				mTextViewStatus.setVisibility(View.GONE);
				//mTextViewStatus.setText(String.format("Connected to " + Service.getConnectedToServerIp() + " : " + Service.getConnectedToServerOnPort()) );
			}
			else
			{
				mTextViewStatus.setText("Tap Settings to connect to pyLauncher on the remote computer.");
			}
		}
	}
	
	//  API Version required for grid view behavior
    private boolean isUsingDynamicGrid() {
        return false; 
        //  TODO - hook up use of dynamic grid for post honeycomb if the library gets fixed
        //return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
    
    
	
	
}
