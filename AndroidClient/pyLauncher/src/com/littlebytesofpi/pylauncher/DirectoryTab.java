package com.littlebytesofpi.pylauncher;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class DirectoryTab extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_directory_tab);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.directory_tab, menu);
		return true;
	}

}
