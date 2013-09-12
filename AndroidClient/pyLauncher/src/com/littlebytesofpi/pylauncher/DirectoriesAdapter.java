package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

public class DirectoriesAdapter extends ListViewAdapter {

	public DirectoriesAdapter(ArrayList<PyFile> dataList, Context c)
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

		final int indexPosition = position;
		//  get the directory for this row
		PyFile directory = (PyFile) mDataList.get(indexPosition);

		mViewHolder.checkBoxSelect.setChecked(directory.mSet);


		//  create the click listener for this item
		mViewHolder.checkBoxSelect.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				PyFile dir = (PyFile)mDataList.get(indexPosition);

				if(((CheckBox)v).isChecked())
				{
					dir.mSet = true;
				}
				else
				{
					dir.mSet = false;
				}
			}
		});




		//  format the data in the text views
		mViewHolder.textViewName.setText(directory.getPath());
		
		//  set the check box current state from persistent data
		return convertView;
	}
	

}
