package com.littlebytesofpi.pylauncher;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class PyLauncher extends TabActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_py_launcher);
        
        //  setup the tabs
        Resources ressources = getResources(); 
        TabHost tabHost = getTabHost();
         
        // Connect Tab
        Intent intentConnect = new Intent().setClass(this, ConnectTab.class);
        TabSpec tabSpecConnect = tabHost.newTabSpec("Connect")
        		.setIndicator("Connect", ressources.getDrawable(R.drawable.ic_connect))
                .setContent(intentConnect);
        
        // Directory Tab
        Intent intentDirectory = new Intent().setClass(this, DirectoryTab.class);
        TabSpec tabSpecDirectory = tabHost.newTabSpec("Directory")
                .setIndicator("Directory", ressources.getDrawable(R.drawable.ic_directory))
                .setContent(intentDirectory);
        
        // Launch Tab
        Intent intentSend = new Intent().setClass(this, SendTab.class);
        TabSpec tabSpecSend = tabHost.newTabSpec("Launch")
                .setIndicator("Launch", ressources.getDrawable(R.drawable.ic_send))
                .setContent(intentSend);
        
        tabHost.addTab(tabSpecConnect);
        tabHost.addTab(tabSpecDirectory);
        tabHost.addTab(tabSpecSend);
        
        
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.py_launcher, menu);
        return true;
    }
    
}
