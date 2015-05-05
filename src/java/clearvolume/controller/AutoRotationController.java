package clearvolume.controller;

import clearvolume.renderer.ClearVolumeRendererInterface;

import com.jogamp.opengl.math.Quaternion;

public class AutoRotationController	extends
																		QuaternionRotationControllerBase implements
																																		RotationControllerInterface,
																																		RotationControllerWithRenderNotification
{

	private volatile boolean mModified;

	public AutoRotationController()
	{
		super();
		setActive(false);
	}

	/**
	 * Returns a copy of the currently used quaternion.
	 * 
	 * @return quaternion
	 */
	@Override
	public Quaternion getQuaternion()
	{
		return super.getQuaternion();
	}

	@Override
	public void notifyRender(ClearVolumeRendererInterface pClearVolumeRendererInterface)
	{
		// in case we need to be notified at render time...
	}


	public void addRotationSpeedX(float pDelta)
	{
		getQuaternion().rotateByAngleX(pDelta);
		mModified = true;
	}

	public void addRotationSpeedY(float pDelta)
	{
		getQuaternion().rotateByAngleY(pDelta);
		mModified = true;
	}

	public void addRotationSpeedZ(float pDelta)
	{
		getQuaternion().rotateByAngleZ(pDelta);
		mModified = true;
	}

	public void stopRotation()
	{
		getQuaternion().setIdentity();
		mModified = false;
	}

	public void setDefaultRotation()
	{
		addRotationSpeedY(0.01f);
	}

	public boolean isModified()
	{
		return mModified;
	}


}
