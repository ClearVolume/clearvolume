package clearvolume.controller;

import com.jogamp.opengl.math.Quaternion;

/**
 * Class QuaternionRotationControllerBase
 * 
 * Example implementation of a RotationControllerInterface using quaternions.
 *
 * @author Loic Royer 2014
 *
 */
public class QuaternionRotationControllerBase	implements
																							RotationControllerInterface
{

	private volatile boolean mActive = true;
	private final Quaternion mQuaternion = new Quaternion();
	protected final Object mQuaternionUpdateLock = new Object();

	/**
	 * Constructs an instance of the QuaternionRotationControllerBase class
	 */
	public QuaternionRotationControllerBase()
	{
		super();
		mQuaternion.setIdentity();
	}

	@Override
	public void setActive(boolean pActive)
	{
		mActive = pActive;
	}

	@Override
	public boolean isActive()
	{
		return mActive;
	}

	/**
	 * Sets the quaternion.
	 * 
	 * @param pQuaternion
	 *          quaternion
	 */
	@Override
	public void setQuaternion(final Quaternion pQuaternion)
	{
		synchronized (mQuaternionUpdateLock)
		{
			mQuaternion.setX(pQuaternion.getX());
			mQuaternion.setY(pQuaternion.getY());
			mQuaternion.setZ(pQuaternion.getZ());
			mQuaternion.setW(pQuaternion.getW());
		}
	}

	/**
	 * Returns a copy of the currently used quaternion.
	 * 
	 * @return quaternion
	 */
	@Override
	public Quaternion getQuaternion()
	{
		return mQuaternion;
	}

}
