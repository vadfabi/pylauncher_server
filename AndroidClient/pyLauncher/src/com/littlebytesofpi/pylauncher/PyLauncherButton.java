package com.littlebytesofpi.pylauncher;

public class PyLauncherButton {

	public PyLauncherButton()
	{
		PyFile = null;
		CommandLineArgs = "";
		Title = "";
		Icon = -1;
	}
	
	public PyLauncherButton(PyFile pyFile, String commandLineArgs, String title, int icon )
	{
		PyFile = pyFile;
		CommandLineArgs = commandLineArgs;
		Title = title;
		Icon = icon;
	}
	
	public PyLauncherButton(String path, String commandLineArgs, String title, int icon )
	{
		PyFile = new PyFile(path);
		CommandLineArgs = commandLineArgs;
		Title = title;
		Icon = icon;
	}
	
	protected PyFile PyFile;
	public PyFile getPyFile() {
		return PyFile;
	}

	protected String CommandLineArgs;
	public String getCommandLineArgs() {
		return CommandLineArgs;
	}

	protected String Title;
	public String getTitle() {
		return Title;
	}

	protected int Icon;
	public int getIcon() {
		return Icon;
	}
	
}
