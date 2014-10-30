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
		
		if ( mFullPath.contains(".py") )
			return new File(mFullPath).getName();
		else
			return mFullPath;
	}
	
	public String getPath(){
		return mFullPath;
	}
	
	public String getDirectoryPath()
	{
		File file = new File(mFullPath);
		
		String test = file.getParentFile().getPath();
		String path = file.getPath();
		if ( path.contains(".py") )
			return file.getParentFile().getPath();
		else
			return path;
	}
}
