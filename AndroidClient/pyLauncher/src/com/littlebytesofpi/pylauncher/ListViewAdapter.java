package com.littlebytesofpi.pylauncher;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;



public abstract class ListViewAdapter extends BaseAdapter {

	//  save the context and the list of objects
	public Context Context;
	public List<?> DataList;
	
	//  constructor
	public ListViewAdapter(List<?> dataList, Context c){

		DataList = dataList;
		Context = c;
	}
	
	
	//  Adapter functions
	public int getCount() {
		
		return DataList.size();
	}

	public Object getItem(int position) {
		
		return DataList.get(position);
	}

	public long getItemId(int position) {
		
		return position;
	}

	//  To implement this list view adapter
	//
	//  1)  Create a class to hold your specific UI view items
	//  This is necessary because getView recycles views, so your UI element click listeners will not respond properly if you do not persist the view
	//class ViewHolder
	//{
		//  map your UI elements here
		//  ImageView image;
		//  TextView textView1;
		// ...

	//}
	
	//  2)  Declare a member variable of this class
	//  ViewHolder mViewHolder;
	
	//  3)  Override getView and manipulate the list view item at position
	public abstract View getView(int position, View convertView, ViewGroup parent);
	
	
//	//  Your getView class will look something like this
//  public View getView(int position, View convertView, ViewGroup parent){
//		
//		
//		//  Get the UI for this line of the list
//		if (convertView  == null)
//		{
//			//  first time through, map it into a new view holder
//			LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//			convertView = vi.inflate(R.layout.text_view, null);
//
//			mViewHolder = new ViewHolder();
//
//			// cache the views
//			viewHolder.image = (ImageView) convertView.findViewById(R.id.icon);
//			viewHolder.textView1 = (TextView)convertView.findViewById(R.id.mainTitle);
//			// ...
//			
//			//  set this viewholder object as a tag to the convert view
//			convertView.setTag(viewHolder);
//		}
//		else
//		{
//			//  subsequent pass, recall it from the view holder
//			viewHolder = (ViewHolder)convertView.getTag();
//		}
//
//		//  You can access mDataList here and do what you want with the views in the viewHolder
//
//		//  return the convertVIew
//		return convertView;
//}

}
