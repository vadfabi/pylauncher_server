package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;

import android.content.Context;
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
	
	ViewHolder mViewHolder;
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		//  Get the UI for this line of the list
		if (convertView  == null)
		{
			//  first time through, map it into a new view holder
			LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.listadapter_event, null);

			//  create our ViewHolder to cache the views for this row
			mViewHolder = new ViewHolder();

			//   cache the views for this line
			mViewHolder.textViewDetails = (TextView)convertView.findViewById(R.id.textViewEventTitle);
			mViewHolder.textViewDetails.setTypeface(null, Typeface.BOLD);
			mViewHolder.textViewResults = (TextView)convertView.findViewById(R.id.textViewEventDescription);
			
			//  set the viewHolder as the tag of this object
			convertView.setTag(mViewHolder);
		}
		else
		{
			//  subsequent pass, recall it from the view holder
			mViewHolder = (ViewHolder)convertView.getTag();
		}

		//  get the directory for this row
		PyLaunchResult result = (PyLaunchResult) mDataList.get(position);
		
		String formatTitle = result.GetFileName();
		mViewHolder.textViewDetails.setText(formatTitle);
		
		
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
			formatResult += "\n - Launched By: " + result.mIpOfRequest;
			formatResult += "\n - Time Requested: " + result.mTimeRequest;
			formatResult += "\n - Time Launched: " + result.mTimeLaunch;
			formatResult += "\n - Time Completed: " + result.mTimeComplete;
		}
		
		
		mViewHolder.textViewResults.setText(formatResult);
		
		
	
		//  set the check box current state from persistent data
		return convertView;
	}
	

}
