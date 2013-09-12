package com.littlebytesofpi.pylauncher;

import java.io.File;

import android.R.bool;

public class PyFile {
	
	//  Full path to the file or directory
	String mFullPath;
	
	//  flag to keep track of state in list views
	boolean mSet;
	
	public PyFile(String fullPath){
			
		mFullPath = fullPath;
		mSet = false;
	}
	
	@Override
	public String toString(){
		
		return new File(mFullPath).getName();
	}
	
	public String getPath(){
		return mFullPath;
	}

}
