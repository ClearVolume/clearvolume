package clearvolume.controller;

import java.io.Closeable;
import java.io.IOException;

import javax.media.opengl.GL2;

import com.jogamp.graph.math.Quaternion;

import egg3d.Egg3D;
import egg3d.Egg3DListener;

public class Egg3DController implements
														RotationControllerInterface,
														Closeable
{

	private final Egg3D mEgg3D;
	private final Quaternion mQuaternion = new Quaternion();
	private final Object mQuaternionUpdateLock = new Object();

	public Egg3DController()
	{
		super();

		mQuaternion.setX(1);
		mQuaternion.normalize();

		mEgg3D = new Egg3D();

		mEgg3D.addEgg3DListener(new Egg3DListener()
		{

			@Override
			public void update(	final float pQuatW,
													final float pQuatX,
													final float pQuatY,
													final float pQuatZ,
													final float pAccX,
													final float pAccY,
													final float pAccZ,
													final float pButton1,
													final float pButton2,
													final float pButton3)
			{
				synchronized (mQuaternionUpdateLock)
				{
					mQuaternion.setW(pQuatW);
					mQuaternion.setX(pQuatY);
					mQuaternion.setY(pQuatZ);
					mQuaternion.setZ(pQuatX);
					mQuaternion.normalize();
					mQuaternion.inverse();
				}
			}
		});
	}

	public boolean connect()
	{
		try
		{
			return mEgg3D.connect();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void putModelViewMatrixIn(final float[] pModelViewMatrix)
	{
		synchronized (mQuaternionUpdateLock)
		{
			mQuaternion.normalize();
			final float[] lQuaternionMatrix = mQuaternion.toMatrix();
			System.arraycopy(	lQuaternionMatrix,
												0,
												pModelViewMatrix,
												0,
												pModelViewMatrix.length);
		}
	}

	@Override
	public void rotateGL(final GL2 gl)
	{
		synchronized (mQuaternionUpdateLock)
		{
			final float[] lMatrix = mQuaternion.toMatrix();
			gl.glMultMatrixf(lMatrix, 0);
		}
	}

	@Override
	public void close() throws IOException
	{
		mEgg3D.close();
	}

}
