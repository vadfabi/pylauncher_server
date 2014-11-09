package com.littlebytesofpi.pylauncher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;



public class Support extends Activity {

	TextView TextViewAbout1;
	TextView TextViewProjLink;
	TextView TextViewAbout2;
	TextView TextViewAppLink;

	final String appName = "com.littlebytesofpi.pylauncher";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_support);
		
		TextViewAbout1 = (TextView)findViewById(R.id.textViewAbout1);
		TextViewProjLink = (TextView)findViewById(R.id.textViewProjPageLink);
		TextViewAbout2 = (TextView)findViewById(R.id.textViewAbout2);
		TextViewAppLink = (TextView)findViewById(R.id.textViewAppPageLink);
		
		//  preamble
		TextViewAbout1.setText("Little Bytes of Pi is a Corvallis, Oregon company inspired by the Raspberry Pi. Our mission is to create useful products and resources for educators and innovators alike.\n\n" +
				"Please check out our projects page, to see details about our current activities:");
		
		
		//  link to our web page
		TextViewProjLink.setMovementMethod(LinkMovementMethod.getInstance());
		
		
		//  
		TextViewAbout2.setText("If you would like to support our efforts, please consider purchasing the paid version of this application:");
		
		
		//  link to google play
		TextViewAppLink.setText("pyLauncher on Google Play");
		TextViewAppLink.setTextColor(Color.parseColor("#00CCFF"));
		TextViewAppLink.setPaintFlags(TextViewAppLink.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);

		TextViewAppLink.setOnClickListener( new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				
				try {
				    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appName)));
				} catch (android.content.ActivityNotFoundException anfe) {
				    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="+appName)));
				}
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.support, menu);
		return true;
	}

}
