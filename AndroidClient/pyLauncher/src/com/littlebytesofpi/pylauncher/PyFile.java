package com.littlebytesofpi.pylauncher;

import java.io.File;

import android.R.bool;

public class PyFile {
	
	//  Full path to the file or directory
	String FullPath;
	
	//  flag to keep track of state in list views
	boolean mSet;
	  
	public PyFile(String fullPath){
			
		FullPath = fullPath;
		mSet = false;
	}
	
	@Override
	public String toString(){
		
		if ( FullPath.contains(".py") )
			return new File(FullPath).getName();
		else
			return FullPath;
	}
	
	public String GetPath(){
		return FullPath;
	}
	
	public String GetDirectoryPath()
	{
		File file = new File(FullPath);
		
		String path = file.getPath();
		if ( path.contains(".py") )
			return file.getParentFile().getPath();
		else
			return path;
	}
}
