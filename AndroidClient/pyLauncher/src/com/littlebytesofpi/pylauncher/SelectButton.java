package com.littlebytesofpi.pylauncher;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.littlebytesofpi.pylauncher.PyLauncherService.LocalBinder;

public class SelectButton extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_button);
		
	}
	
	
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
	}

	
	//  onDestroy
	@Override
	public void onDestroy(){
		
		UnbindFromService();
		
		super.onDestroy();
	}
	

	int selectedIcon = -1;
	

	/*
	 * Service Handling
	 * 
	 */
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
			
			GridView gridView = (GridView)findViewById(R.id.gridView);
			gridView.setAdapter(new ButtonsAdapter(Service));
			
			gridView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					
					//  finish the activity with the selected result
					selectedIcon = position;
					
					Intent intent = new Intent();
					intent.putExtra("select", position);
					setResult(RESULT_OK, intent);
					finish();
				}
			});
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
		Intent startIntent = new Intent(SelectButton.this, PyLauncherService.class);
		getApplicationContext().bindService(startIntent, Connection, Context.BIND_AUTO_CREATE);
	}


	//  UnbindFromService
	void UnbindFromService() {
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
			switch (msg.what) 
			{

			default:
				break;
			}
		}
	};  

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		
		return super.onOptionsItemSelected(item);
	}
}
