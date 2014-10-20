package com.littlebytesofpi.pylauncher;

import java.util.ArrayList;

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
	Context mContext;
	
	public GridViewButtonsAdapter(Context context, ArrayList<PyLauncherButton> buttonList)
	{
		mContext = context;
		
		mButtonList = buttonList;
	}
	
	@Override
	public int getCount() {

		return mButtonList.size();

	}

	@Override
	public Object getItem(int arg0) {
		
		return mButtonList.get(arg0);
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
        if(convertView==null){
        	LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.gridviewbutton, null);
            
          
        }
        else
        {
            v = convertView;
        }
        
      
		
        
        PyLauncherButton thisButton = mButtonList.get(position);
        
        TextView tv = (TextView)v.findViewById(R.id.textView);
        tv.setText(thisButton.getTitle());
        ImageButton imageButton = (ImageButton)v.findViewById(R.id.imageButton);
        imageButton.setImageResource(R.drawable.ic_launcher);
        
        final int indexPosition = position;
    	


    		//  create the click listener for this item
    		imageButton.setOnClickListener(new View.OnClickListener() {

    			public void onClick(View v) {

    				 Toast.makeText(mContext, "" + indexPosition, Toast.LENGTH_SHORT).show();
    			}
    		});
    		
    		
        
        return v;
    }

}
