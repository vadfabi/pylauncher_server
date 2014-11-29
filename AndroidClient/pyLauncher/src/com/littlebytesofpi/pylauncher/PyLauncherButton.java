package com.littlebytesofpi.pylauncher;

public class PyLauncherButton {

	public PyLauncherButton()
	{
		PyFile = null;
		Environment = "python";
		CommandLineArgs = "";
		Title = "";
		Icon = -1;
	}
	
	public PyLauncherButton(String environment, PyFile pyFile, String commandLineArgs, String title, int icon )
	{
		Environment = environment;
		PyFile = pyFile;
		CommandLineArgs = commandLineArgs;
		Title = title;
		Icon = icon;
	}
	
	public PyLauncherButton(String environment, String path, String commandLineArgs, String title, int icon )
	{
		Environment = environment;
		PyFile = new PyFile(path);
		CommandLineArgs = commandLineArgs;
		Title = title;
		Icon = icon;
	}
	
	protected String Environment;
	public String getEnvironment() {
		return Environment;
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
