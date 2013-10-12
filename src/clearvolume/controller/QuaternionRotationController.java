package clearvolume.controller;

import javax.media.opengl.GL2;

import com.jogamp.graph.math.Quaternion;

public class QuaternionRotationController	implements
																					RotationControllerInterface
{

	private final Quaternion mQuaternion = new Quaternion();
	private final Object mQuaternionUpdateLock = new Object();

	public QuaternionRotationController()
	{
		super();

		mQuaternion.setX(1);
		mQuaternion.normalize();
	}

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
		}
		gl.glMultMatrixf(lMatrix, 0);
	}

}
