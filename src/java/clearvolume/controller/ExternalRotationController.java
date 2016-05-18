package clearvolume.controller;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import com.jogamp.opengl.math.Quaternion;

import clearvolume.renderer.DisplayRequestInterface;

/**
 * Class ExternalRotationController
 * 
 * This class provides an implementation of RotationControllerInterface that
 * uses incoming data from a TCP port in he form:
 * 
 * [qw,qx,qy,qz,ax,ay,az,b1,b2,b3]
 * 
 * where (qw,qx,qy,qz) is a quaternion with qw,qx,qy,qz are floats. and
 * (ax,ay,az) is an acceleration vector (if available), and b1,b2, b3 are
 * variable size buttons.
 * 
 * typically, the data arrives from a Egg3D controller or similarly compatible
 * rotation controller. the default TCP port is 4444.
 *
 * @author Loic Royer 2014
 *
 */
public class ExternalRotationController	extends
										QuaternionRotationControllerBase implements
																		RotationControllerInterface,
																		Closeable,
																		Runnable
{

	/**
	 * Default Egg3D TCP port
	 */
	public static final int cDefaultEgg3DTCPport = 4444;

	// network related fields.
	private Socket mClientSocket;
	private final int mPortNumber;
	private volatile BufferedReader mBufferedInputStreamFromServer;

	// Quaternion and locking object.
	private final Quaternion mQuaternion = new Quaternion();
	private final Object mQuaternionUpdateLock = new Object();

	/**
	 * Thread responsible for receiving the data over TCP.
	 */
	private Thread mReceptionThread;

	/**
	 * DisplayRequest object that has to be called when requesting a display
	 * update.
	 */
	private final DisplayRequestInterface mDisplayRequest;

	/**
	 * Constructs an instance of the ExternalRotationController class
	 * 
	 * @param pPortNumber
	 *            port number
	 * @param pDisplayRequest
	 *            display request
	 * @throws UnknownHostException
	 *             thrown when host unknown
	 * @throws IOException
	 *             thrown when IO error
	 */
	public ExternalRotationController(	final int pPortNumber,
										DisplayRequestInterface pDisplayRequest) throws UnknownHostException,
																				IOException
	{
		super();
		mPortNumber = pPortNumber;
		mDisplayRequest = pDisplayRequest;

		mQuaternion.setX(1);
		mQuaternion.normalize();

	}

	/**
	 * Starts a thread that asynchronously attempts to connect to the TCP server
	 * using connect().
	 */
	public void connectAsynchronouslyOrWait()
	{
		final Runnable lConnectionRunnable = new Runnable()
		{
			@Override
			public void run()
			{
				while (!connect())
					try
					{
						Thread.sleep(500);
					}
					catch (final InterruptedException e)
					{
					}
			}
		};
		final Thread lConnectionThread = new Thread(lConnectionRunnable,
													"ClearVolume-ExternalControllerConnectionThread");
		lConnectionThread.setDaemon(true);
		lConnectionThread.start();
	}

	/**
	 * Makes one attempt at connecting to the TCP server and proceeds to start
	 * the reception thread.
	 * 
	 * @return true if connection succeeded.
	 */
	public boolean connect()
	{
		try
		{
			mClientSocket = new Socket("localhost", mPortNumber);
			mBufferedInputStreamFromServer = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream()));

			mReceptionThread = new Thread(	this,
											"ClearVolume-ExternalController");
			mReceptionThread.setDaemon(true);
			mReceptionThread.start();

			return true;
		}
		catch (final Throwable e)
		{
			return false;
		}
	}

	/**
	 * Interface method implementation
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException
	{
		if (mClientSocket != null)
			mClientSocket.close();
		mClientSocket = null;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		final BufferedReader lBufferedInputStreamFromServer = mBufferedInputStreamFromServer;
		while (mBufferedInputStreamFromServer != null && mBufferedInputStreamFromServer == lBufferedInputStreamFromServer)
		{
			String lReadLine;
			try
			{
				lReadLine = mBufferedInputStreamFromServer.readLine();
				parseMessage(lReadLine);
				mDisplayRequest.requestDisplay();
			}
			catch (final Throwable e)
			{
				System.out.println("Connection to external controller lost");
				System.err.println(e.getLocalizedMessage());
				mBufferedInputStreamFromServer = null;
			}
		}

		System.out.println("Trying to reconnect to external controller");
		connectAsynchronouslyOrWait();
	}

	/**
	 * Parses messages from the TCP server.
	 * 
	 * @param pReadLine
	 */
	private void parseMessage(String pReadLine)
	{
		// System.out.println(pReadLine);
		final String lSubString = pReadLine.substring(	1,
														pReadLine.length() - 2);

		final String[] lSplittedSubString = lSubString.split(",");

		final float lQuaternionW = Float.parseFloat(lSplittedSubString[0]);
		final float lQuaternionX = Float.parseFloat(lSplittedSubString[1]);
		final float lQuaternionY = Float.parseFloat(lSplittedSubString[2]);
		final float lQuaternionZ = Float.parseFloat(lSplittedSubString[3]);

		// TODO: find some use for these:
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
			mQuaternion.set(mQuaternion.invert());
		}/**/

	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.controller.RotationControllerInterface#isActive()
	 */
	@Override
	public boolean isActive()
	{
		return mClientSocket != null && mClientSocket.isConnected()
				&& !mClientSocket.isClosed()
				&& super.isActive();
	}

}
