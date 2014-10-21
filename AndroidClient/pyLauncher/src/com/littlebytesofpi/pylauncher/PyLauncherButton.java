package com.littlebytesofpi.pylauncher;

public class PyLauncherButton {

	public PyLauncherButton()
	{
		mPyFile = null;
		mCommandLineArgs = "";
		mTitle = "";
		mIcon = -1;
	}
	
	public PyLauncherButton(PyFile pyFile, String commandLineArgs, String title, int icon )
	{
		mPyFile = pyFile;
		mCommandLineArgs = commandLineArgs;
		mTitle = title;
		mIcon = icon;
	}
	
	public PyLauncherButton(String path, String commandLineArgs, String title, int icon )
	{
		mPyFile = new PyFile(path);
		mCommandLineArgs = commandLineArgs;
		mTitle = title;
		mIcon = icon;
	}
	
	protected PyFile mPyFile;
	public PyFile getPyFile() {
		return mPyFile;
	}

	protected String mCommandLineArgs;
	public String getCommandLineArgs() {
		return mCommandLineArgs;
	}

	protected String mTitle;
	public String getTitle() {
		return mTitle;
	}

	protected int mIcon;
	public int getIcon() {
		return mIcon;
	}
	
}
