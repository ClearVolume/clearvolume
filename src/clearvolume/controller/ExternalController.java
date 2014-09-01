package clearvolume.controller;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.media.opengl.GL2;

import clearvolume.DisplayRequest;

import com.jogamp.graph.math.Quaternion;

public class ExternalController	implements
																RotationControllerInterface,
																Closeable,
																Runnable
{

	public static final int Egg3DTCPport = 4444;

	private final Quaternion mQuaternion = new Quaternion();
	private final Object mQuaternionUpdateLock = new Object();
	private Socket mClientSocket;
	private volatile BufferedReader mBufferedInputStreamFromServer;
	private int mPortNumber;
	private Thread mThread;

	private DisplayRequest mDisplayRequest;

	public ExternalController(final int pPortNumber,
														DisplayRequest pDisplayRequest)	throws UnknownHostException,
																									IOException
	{
		super();
		mPortNumber = pPortNumber;
		mDisplayRequest = pDisplayRequest;

		mQuaternion.setX(1);
		mQuaternion.normalize();

	}

	public void connectAsynchronouslyOrWait()
	{
		Runnable lConnectionRunnable = new Runnable()
		{
			@Override
			public void run()
			{
				while (!connect())
					try
					{
						Thread.sleep(500);
					}
					catch (InterruptedException e)
					{
					}
			}
		};
		Thread lConnectionThread = new Thread(lConnectionRunnable,
																					"ClearVolume-ExternalControllerConnectionThread");
		lConnectionThread.setDaemon(true);
		lConnectionThread.start();
	}

	public boolean connect()
	{
		try
		{
			mClientSocket = new Socket("localhost", mPortNumber);
			mBufferedInputStreamFromServer = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream()));

			mThread = new Thread(this, "ClearVolume-ExternalController");
			mThread.setDaemon(true);
			mThread.start();

			return true;
		}
		catch (final Throwable e)
		{
			// e.printStackTrace();
			return false;
		}
	}

	@Override
	public void putModelViewMatrixIn(final float[] pModelViewMatrix)
	{
		float[] lQuaternionMatrix;
		synchronized (mQuaternionUpdateLock)
		{
			mQuaternion.normalize();
			lQuaternionMatrix = mQuaternion.toMatrix();
		}
		System.arraycopy(	lQuaternionMatrix,
											0,
											pModelViewMatrix,
											0,
											pModelViewMatrix.length);

	}

	@Override
	public void rotateGL(final GL2 gl)
	{
		float[] lMatrix;
		synchronized (mQuaternionUpdateLock)
		{
			lMatrix = mQuaternion.toMatrix();
			// System.out.println(lMatrix);
		}
		gl.glMultMatrixf(lMatrix, 0);
	}

	@Override
	public void close() throws IOException
	{
		mClientSocket.close();
		mClientSocket = null;
	}

	@Override
	public void run()
	{
		BufferedReader lBufferedInputStreamFromServer = mBufferedInputStreamFromServer;
		while (mBufferedInputStreamFromServer != null && mBufferedInputStreamFromServer == lBufferedInputStreamFromServer)
		{
			String lReadLine;
			try
			{
				lReadLine = mBufferedInputStreamFromServer.readLine();
				parseMessage(lReadLine);
				mDisplayRequest.requestDisplay();
			}
			catch (Throwable e)
			{
				System.out.println("Connection to external controller lost");
				System.err.println(e.getLocalizedMessage());
				mBufferedInputStreamFromServer = null;
			}
		}

		System.out.println("Trying to reconnect to external controller");
		connectAsynchronouslyOrWait();
	}

	private void parseMessage(String pReadLine)
	{
		// System.out.println(pReadLine);
		String lSubString = pReadLine.substring(1, pReadLine.length() - 2);
		
		String[] lSplittedSubString = lSubString.split(",");

		final float lQuaternionW = Float.parseFloat(lSplittedSubString[0]);
		final float lQuaternionX = Float.parseFloat(lSplittedSubString[1]);
		final float lQuaternionY = Float.parseFloat(lSplittedSubString[2]);
		final float lQuaternionZ = Float.parseFloat(lSplittedSubString[3]);
		final float lAccelerationX = Float.parseFloat(lSplittedSubString[4]);
		final float lAccelerationY = Float.parseFloat(lSplittedSubString[5]);
		final float lAccelerationZ = Float.parseFloat(lSplittedSubString[6]);
		final float lButton1 = Float.parseFloat(lSplittedSubString[7]);
		final float lButton2 = Float.parseFloat(lSplittedSubString[8]);
		final float lButton3 = Float.parseFloat(lSplittedSubString[9]);

		synchronized (mQuaternionUpdateLock)
		{
			mQuaternion.setW(lQuaternionW);
			mQuaternion.setX(lQuaternionX);
			mQuaternion.setY(lQuaternionY);
			mQuaternion.setZ(lQuaternionZ);
			mQuaternion.normalize();
			mQuaternion.inverse();
		}/**/

	}

	@Override
	public boolean isActive()
	{
		return mClientSocket != null && mClientSocket.isConnected()
						&& !mClientSocket.isClosed();
	}

}
