package com.littlebytesofpi.pylauncher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.askerov.dynamicgrid.BaseDynamicGridAdapter;

import android.R.color;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

public class GridViewButtonsAdapter extends BaseDynamicGridAdapter {

	
	private class ViewHolder
	{
		public TextView textViewTitle;
		public ImageButton imageButtonIcon;
		
		private ViewHolder(View view)
		{
			textViewTitle = (TextView)view.findViewById(R.id.textView);
			imageButtonIcon = (ImageButton)view.findViewById(R.id.imageButton);
		}
	}
	
	final WeakReference<SendButtonsActivity> ParentActivity;
	
	
	public GridViewButtonsAdapter(SendButtonsActivity parentActivity, ArrayList<PyLauncherButton> buttonList)
	{
		super(parentActivity, buttonList, 3);
		
		ParentActivity = new WeakReference<SendButtonsActivity>(parentActivity);
	}
	
	
	

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
		ViewHolder holder;
        
        if(convertView==null)
        {
        	LayoutInflater li = (LayoutInflater)ParentActivity.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = li.inflate(R.layout.gridviewbutton, null);
            holder = new ViewHolder(convertView);
            
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }
 
        //  create the click listeners for this item
        final int indexPosition = position;
 
        //  create the click listener for this item
        holder.imageButtonIcon.setOnClickListener(new View.OnClickListener() 
        {
       	 public void onClick(View v) {

       		 ParentActivity.get().GridViewItemClick(indexPosition);
       	 }
        });
        
        //  create the long click listener for this item
        holder.imageButtonIcon.setOnLongClickListener( new OnLongClickListener() {
		
			public boolean onLongClick(View v) {
				ParentActivity.get().GridViewItemLongClick(indexPosition);
				return true;
			}
		});
       
        holder.imageButtonIcon.setBackgroundColor(color.transparent);
        
     
      	if ( ParentActivity.get().GridEditMode )
      		holder.imageButtonIcon.setImageResource(R.drawable.ic_edit);//(color.holo_blue_dark);
      	else if ( ParentActivity.get().GridDeleteMode )
      		holder.imageButtonIcon.setImageResource(R.drawable.ic_delete);//(color.holo_blue_dark);
      	else if ( ! isUsingDynamicGrid() && ParentActivity.get().GridDragMode && position != ParentActivity.get().mDragIndex )
      		holder.imageButtonIcon.setImageResource(R.drawable.ic_moveleft);//(color.holo_blue_dark);
      	else
      		holder.imageButtonIcon.setImageResource(R.drawable.ic_blank);
      	
        PyLauncherButton thisButton = (PyLauncherButton)getItem(position);
        holder.textViewTitle.setText(thisButton.getTitle());
        holder.imageButtonIcon.setBackgroundDrawable(ParentActivity.get().Service.GetButtonDrawable(thisButton.getIcon()));
        		 
        return convertView;
    }
	
//  API Version required for grid view behavior
    private boolean isUsingDynamicGrid() {
        return false; 
        //  TODO - hook up use of dynamic grid for post honeycomb if the library gets fixed
        //return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

}
