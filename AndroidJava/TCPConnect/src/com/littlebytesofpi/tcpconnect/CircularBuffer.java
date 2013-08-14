package com.littlebytesofpi.tcpconnect;

import android.util.Log;

public class CircularBuffer {
	
	private byte[] 		mBuffer;
	private final int 	mBufferSize;
	private int 		mHeadPosition;
	private int 		mTailPosition;
	
	

	public CircularBuffer(int size, boolean logD ){

		D = logD;

		mBufferSize = size;
	
		mHeadPosition = 0;
		mTailPosition = 0;

		mBuffer = new byte[mBufferSize];
	}

	public int getBufferSize(){
		return mBufferSize;
	}

	public int getDataLength(){

		if ( mHeadPosition < mTailPosition )
		{
			//  wrap around
			return ( (mBufferSize - mTailPosition) + mHeadPosition );
		}
		else
			return mHeadPosition - mTailPosition;

	}

	public int getFreeBufferSize(){

		if ( mHeadPosition < mTailPosition )
		{
			return (mTailPosition - mHeadPosition);
		}
		else if ( mHeadPosition > mTailPosition )
		{
			return (mBufferSize - mHeadPosition + mTailPosition);

		}
		else
			return mBufferSize;

	}

	public void insertData(byte[] data, int length){

		//  check for buffer overflow
		if ( D ) { if (length > getFreeBufferSize() ) Log.e(TAG, "Buffer overflow"); }


		//  copy the first part to the array end
		int lengthFirstPart = Math.min(length , (mBufferSize - mHeadPosition));
		System.arraycopy(data,  0, mBuffer, mHeadPosition, lengthFirstPart);
		if ( D ) Log.d(TAG, "insertData " + lengthFirstPart + " bytes at " + mHeadPosition + "to new head " + (mHeadPosition+lengthFirstPart));

		//  increment new head position
		mHeadPosition = mHeadPosition + lengthFirstPart;


		//  see if we wrapped
		if ( lengthFirstPart < length )
		{
			int lengthSecondPart = length - lengthFirstPart;
			System.arraycopy(data,  lengthFirstPart, mBuffer, 0, lengthSecondPart);
			if ( D ) Log.d(TAG, "insertData wrap buffer " + lengthSecondPart + " bytes to new head " + lengthSecondPart);
			mHeadPosition = lengthSecondPart;
		}
	}

	public int getData(byte[] data, int length){

		//  check for buffer underflow
		if ( D ) { if (length > getDataLength() ) Log.e(TAG, "Buffer underflow"); }

		int lengthCopied = Math.min(length,  getDataLength());

		//  copy depends on if head is behind the tail
		if ( mHeadPosition < mTailPosition )
		{
			//  wrap around
			int lengthFirstPart = mBufferSize - mTailPosition;
			if ( lengthCopied <= lengthFirstPart )
			{
				System.arraycopy(mBuffer, mTailPosition, data, 0, lengthCopied);
				if ( D ) Log.d(TAG, "getData copy " + lengthCopied + " bytes from tail " + mTailPosition + " to new tail " + (mTailPosition+lengthCopied));
				mTailPosition += lengthCopied;
				return lengthCopied;
			}

			int lengthSecondPart = lengthCopied - lengthFirstPart;

			System.arraycopy(mBuffer,  mTailPosition, data, 0, lengthFirstPart);
			if ( D ) Log.d(TAG, "getData wrap copy " + lengthFirstPart + " bytes from tail " + mTailPosition + " to new tail " + (mTailPosition+lengthFirstPart));
			System.arraycopy(mBuffer,  0, data, lengthFirstPart, lengthSecondPart);
			if ( D ) Log.d(TAG, "getData wrap copy " + lengthSecondPart + " bytes from tail " + 0 + " to new tail " + lengthSecondPart);
			mTailPosition = lengthSecondPart;

		}
		else
		{
			System.arraycopy(mBuffer,  mTailPosition, data, 0, lengthCopied);
			if ( D ) Log.d(TAG, "getData copy " + lengthCopied + " bytes from tail " + mTailPosition + " to new tail " + (mTailPosition+lengthCopied));
			mTailPosition += lengthCopied;
		}

		return lengthCopied;
	}



	boolean D ; //= false;
	String TAG = "CircularBuffer";
	

}
