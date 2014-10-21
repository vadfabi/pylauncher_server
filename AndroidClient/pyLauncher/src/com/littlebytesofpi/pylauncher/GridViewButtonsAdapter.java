package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;

import android.R.color;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class GridViewButtonsAdapter extends BaseAdapter {

	ArrayList<PyLauncherButton> mButtonList;
	SendButtonsActivity mParentActivity;
	
	public GridViewButtonsAdapter(SendButtonsActivity parentActivity, ArrayList<PyLauncherButton> buttonList)
	{
		mParentActivity = parentActivity;
		
		mButtonList = buttonList;
	}
	
	@Override
	public int getCount() 
	{
		//  the number of buttons is the size of the array plus one for the add new button
		return mButtonList.size() + 1;

	}

	@Override
	public Object getItem(int arg0) 
	{
		if ( arg0 > mButtonList.size()-1)
		{
			return null;
		}
		else
		{
			return mButtonList.get(arg0);
		}
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View v;
        if(convertView==null)
        {
        	LayoutInflater li = (LayoutInflater)mParentActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.gridviewbutton, null);
        }
        else
        {
            v = convertView;
        }
        
        //  get the layout elements
        TextView tv = (TextView)v.findViewById(R.id.textView);
        ImageButton imageButton = (ImageButton)v.findViewById(R.id.imageButton);
        imageButton.setBackgroundColor(color.transparent);
      
        //  if this is an action button, process it
        if ( mButtonList.size() > position )
        {
        	 PyLauncherButton thisButton = mButtonList.get(position);
             tv.setText(thisButton.getTitle());
             imageButton.setImageDrawable(mParentActivity.mService.GetButtonDrawable(thisButton.getIcon()));
             
             final int indexPosition = position;

             //  create the click listener for this item
             imageButton.setOnClickListener(new View.OnClickListener() 
             {
            	 public void onClick(View v) {

            		 Toast.makeText(mParentActivity, "" + indexPosition, Toast.LENGTH_SHORT).show();
            	 }
             });
        }
        else
        {
        	//  this is the extra button for add function
        	tv.setText("New Button");
            imageButton.setImageResource(R.drawable.ic_addbutton);
            
            //  create the click listener for this item
            imageButton.setOnClickListener(new View.OnClickListener() 
            {
           	 public void onClick(View v) {
           		 GridViewButtonsAdapter.this.mParentActivity.AddButton();
           	 }
            });
        }
		 
        return v;
    }

}
