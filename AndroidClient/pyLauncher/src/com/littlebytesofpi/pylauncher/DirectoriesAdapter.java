package com.littlebytesofpi.pylauncher;

import java.util.List;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

public class DirectoriesAdapter extends ListViewAdapter {

	public DirectoriesAdapter(List<String> dataList, Context c)
	{
		super(dataList, c);
	}
	

	//  Declare the view holder class
	private class ViewHolder
	{
		TextView textViewName;
		CheckBox checkBoxSelect;
	}
	
	ViewHolder mViewHolder;
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		//  Get the UI for this line of the list
		if (convertView  == null)
		{
			//  first time through, map it into a new view holder
			LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.listadapter_directory, null);

			//  create our ViewHolder to cache the views for this row
			mViewHolder = new ViewHolder();

			//   cache the views for this line
			mViewHolder.textViewName = (TextView)convertView.findViewById(R.id.textViewFileName);
			mViewHolder.checkBoxSelect = (CheckBox)convertView.findViewById(R.id.checkBoxFileSelect);
			
			//  set the viewHolder as the tag of this object
			convertView.setTag(mViewHolder);
		}
		else
		{
			//  subsequent pass, recall it from the view holder
			mViewHolder = (ViewHolder)convertView.getTag();
		}

		//  get the sensor for this row
		final String event = (String) mDataList.get(position);

		//  format the data in the text views
		mViewHolder.textViewName.setText(event.toString());
		
				
		//  set the check box current state from persistent data
		return convertView;
	}
	

}
