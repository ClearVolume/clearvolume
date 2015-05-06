package clearvolume.controller;

import com.jogamp.opengl.math.Quaternion;

/**
 * Class RotationControllerBase
 * 
 *
 * @author Loic Royer 2014
 *
 */
public class RotationControllerBase extends ControllerBase implements
																																		RotationControllerInterface
{

	private volatile boolean mActive = true;
	private final Quaternion mQuaternion = new Quaternion();
	protected final Object mQuaternionUpdateLock = new Object();

	/**
	 * Constructs an instance of the RotationControllerBase class
	 */
	public RotationControllerBase()
	{
		super();
		mQuaternion.setIdentity();
	}

	/**
	 * Sets the quaternion.
	 * 
	 * @param pQuaternion
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
