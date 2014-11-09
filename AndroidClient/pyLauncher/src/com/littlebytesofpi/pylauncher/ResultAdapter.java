package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.opengl.Visibility;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

public class ResultAdapter extends ListViewAdapter {

	public ResultAdapter(ArrayList<PyLaunchResult> dataList, Context c)
	{
		super(dataList, c);
	}
	

	//  Declare the view holder class
	private class ViewHolder
	{
		TextView textViewDetails;
		TextView textViewResults;
	}
	
	ViewHolder ViewHolder;
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		//  Get the UI for this line of the list
		if (convertView  == null)
		{
			//  first time through, map it into a new view holder
			LayoutInflater vi = (LayoutInflater)Context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.listadapter_event, null);

			//  create our ViewHolder to cache the views for this row
			ViewHolder = new ViewHolder();

			//   cache the views for this line
			ViewHolder.textViewDetails = (TextView)convertView.findViewById(R.id.textViewEventTitle);
			ViewHolder.textViewDetails.setTypeface(null, Typeface.BOLD);
			ViewHolder.textViewResults = (TextView)convertView.findViewById(R.id.textViewEventDescription);
			
			//  set the viewHolder as the tag of this object
			convertView.setTag(ViewHolder);
		}
		else
		{
			//  subsequent pass, recall it from the view holder
			ViewHolder = (ViewHolder)convertView.getTag();
		}

		//  get the directory for this row
		PyLaunchResult result = (PyLaunchResult) DataList.get(position);
		
		String formatTitle = result.GetFileName();
		ViewHolder.textViewDetails.setText(formatTitle);
		
		
		String formatResult = "";
		//
		for (String nextResult : result.mResults)
		{
			if ( formatResult.length() != 0 )
				formatResult += "\n";
			formatResult += "> " + nextResult;
		}
		
		if ( result.mExpanded )
		{	
			formatResult += "\n\nDetails:";
			formatResult += "\n - Launched By: " + result.IpOfRequest;
			formatResult += "\n - Time Requested: " + result.TimeRequest;
			formatResult += "\n - Time Launched: " + result.TimeLaunch;
			formatResult += "\n - Time Completed: " + result.TimeComplete;
		}
		
		
		ViewHolder.textViewResults.setText(formatResult);
		
		if ( position % 2 == 0 )
		{
			convertView.setBackgroundColor(Color.parseColor("#383838"));
		}
		else
		{
			convertView.setBackgroundColor(Color.parseColor("#282828"));
		}
		
		
	
		//  set the check box current state from persistent data
		return convertView;
	}
	

}
