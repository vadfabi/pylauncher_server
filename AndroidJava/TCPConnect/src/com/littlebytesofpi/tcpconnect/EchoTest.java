package com.littlebytesofpi.tcpconnect;


public class EchoTest extends Thread {

	private boolean mThreadRunning = false;
	private boolean mThreadExit = false;
	
	TCPConnectService mService;
	
	public EchoTest(TCPConnectService service){
		mService = service;
	}

	public void run() {

		mThreadRunning = true;

		// Keep listening to the InputStream while connected
		while (mThreadRunning) {
			
			long startTime = System.currentTimeMillis();
			String echoString = "$TCP_ECHOTEST," + startTime;

			String readResponse = IpFunctions.SendStringToPort(mService.getConnectedToServerIp(), mService.getServerPort(), echoString + mService.getClientListeningOnPort());
		}

		mThreadRunning = false;
		mThreadExit = true;
	}

	public void cancel() {

		mThreadRunning = false;


		//  now wait for thread run to exit
		while ( ! mThreadExit )
		{
			try{
				Thread.sleep(100);
			} catch (InterruptedException e){}
		}


	}
}
