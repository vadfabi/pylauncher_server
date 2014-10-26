package com.littlebytesofpi.pylauncher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.askerov.dynamicgrid.BaseDynamicGridAdapter;

import android.R.color;
import android.content.Context;
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
	
	final WeakReference<SendButtonsActivity> mParentActivity;
	
	
	public GridViewButtonsAdapter(SendButtonsActivity parentActivity, ArrayList<PyLauncherButton> buttonList)
	{
		super(parentActivity, buttonList, 3);
		
		mParentActivity = new WeakReference<SendButtonsActivity>(parentActivity);
	}
	
	
	

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
		ViewHolder holder;
        
        if(convertView==null)
        {
        	LayoutInflater li = (LayoutInflater)mParentActivity.get().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

       		 mParentActivity.get().GridViewItemClick(indexPosition);
       	 }
        });
        
        //  create the long click listener for this item
        holder.imageButtonIcon.setOnLongClickListener( new OnLongClickListener() {
		
			public boolean onLongClick(View v) {
				mParentActivity.get().GridViewItemLongClick(indexPosition);
				return true;
			}
		});
        
        //  set the layout elements    
        holder.imageButtonIcon.setBackgroundColor(color.transparent);
      
        PyLauncherButton thisButton = (PyLauncherButton)getItem(position);
        holder.textViewTitle.setText(thisButton.getTitle());
        holder.imageButtonIcon.setImageDrawable(mParentActivity.get().mService.GetButtonDrawable(thisButton.getIcon()));
		 
        return convertView;
    }

}
