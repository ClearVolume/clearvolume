package clearvolume.controller;

import javax.media.opengl.GL2;

import com.jogamp.opengl.math.Quaternion;

/**
 * Class QuaternionRotationController
 * 
 * Exmaple implementation of a RotationControllerInterface using quaternions.
 *
 * @author Loic Royer 2014
 *
 */
public class QuaternionRotationController	implements
																					RotationControllerInterface
{

	// Quaternin and locking object
	private final Quaternion mQuaternion = new Quaternion();
	private final Object mQuaternionUpdateLock = new Object();

	/**
	 * Constructs an instance of the QuaternionRotationController class
	 */
	public QuaternionRotationController()
	{
		super();

		mQuaternion.setX(1);
		mQuaternion.normalize();
	}

	/**
	 * Sets the quaternion.
	 * 
	 * @param pQuaternion
	 */
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
	public Quaternion getQuaternion()
	{
		return new Quaternion(mQuaternion.getW(),
													mQuaternion.getX(),
													mQuaternion.getY(),
													mQuaternion.getZ());
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.controller.RotationControllerInterface#putModelViewMatrixIn(float[])
	 */
	@Override
	public void putModelViewMatrixIn(final float[] pModelViewMatrix)
	{
		float[] lQuaternionMatrix;
		synchronized (mQuaternionUpdateLock)
		{
			mQuaternion.normalize();
			lQuaternionMatrix = mQuaternion.toMatrix(new float[16], 0);
		}
		System.arraycopy(	lQuaternionMatrix,
											0,
											pModelViewMatrix,
											0,
											pModelViewMatrix.length);

	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.controller.RotationControllerInterface#rotateGL(javax.media.opengl.GL2)
	 */
	@Override
	public void rotateGL(final GL2 gl)
	{
		float[] lMatrix;
		synchronized (mQuaternionUpdateLock)
		{
			lMatrix = mQuaternion.toMatrix(new float[16], 0);
		}
		gl.glMultMatrixf(lMatrix, 0);
	}

	/**
	 * Interface method implementation
	 * 
	 * @see clearvolume.controller.RotationControllerInterface#isActive()
	 */
	@Override
	public boolean isActive()
	{
		return true;
	}

}
