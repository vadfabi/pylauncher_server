package com.littlebytesofpi.tcpconnect;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LogEventAdapter extends ListViewAdapter {

	public LogEventAdapter(List<LogEvent> dataList, Context c)
	{
		super(dataList, c);
	}
	
//  To implement this list view adapter
	//
	//  1)  Create a class to hold your specific UI view items
	//  This is necessary because getView recycles views, so your UI element click listeners will not respond properly if you do not persist the view
//  Declare the view holder class
	private class ViewHolder
	{
		TextView textView1;
	}
	
	//  2)  Declare a member variable of this class
	ViewHolder mViewHolder;
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		//  Get the UI for this line of the list
		if (convertView  == null)
		{
			//  first time through, map it into a new view holder
			LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.logeventadapteritem, null);

			//  create our ViewHolder to cache the views for this row
			mViewHolder = new ViewHolder();

			//   cache the views for this line
			mViewHolder.textView1 = (TextView)convertView.findViewById(R.id.textView1);
			
			//  set the viewHolder as the tag of this object
			convertView.setTag(mViewHolder);
		}
		else
		{
			//  subsequent pass, recall it from the view holder
			mViewHolder = (ViewHolder)convertView.getTag();
		}

		//  get the sensor for this row
		final LogEvent event = (LogEvent) mDataList.get(position);

		//  format the data in the text views
		mViewHolder.textView1.setText(event.toString());
		
				
		//  set the check box current state from persistent data
		return convertView;
	}
}
