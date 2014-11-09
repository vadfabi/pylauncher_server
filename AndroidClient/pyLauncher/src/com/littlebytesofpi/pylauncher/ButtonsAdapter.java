package com.littlebytesofpi.pylauncher;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ButtonsAdapter extends BaseAdapter {

	PyLauncherService Service;
	
	public ButtonsAdapter(PyLauncherService service)
	{
		Service = service;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return Service.GetButtonDrawableCount();
	}

	@Override
	public Object getItem(int arg0) {
		return Service.GetButtonDrawable(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(Service);
            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageDrawable(Service.GetButtonDrawable(position));
        
        return imageView;
    }

}
