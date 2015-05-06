package clearvolume.controller;

import clearvolume.renderer.ClearVolumeRendererInterface;

import com.jogamp.opengl.math.Quaternion;

public class AutoRotationController	extends
																		RotationControllerBase implements
																																		RotationControllerInterface,
																																		ControllerWithRenderNotification
{

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
	}

	public void addRotationSpeedY(float pDelta)
	{
		getQuaternion().rotateByAngleY(pDelta);
	}

	public void addRotationSpeedZ(float pDelta)
	{
		getQuaternion().rotateByAngleZ(pDelta);
	}

	public void stopRotation()
	{
		getQuaternion().setIdentity();
	}

	public void setDefaultRotation()
	{
		addRotationSpeedY(0.01f);
	}

	public boolean isRotating()
	{
		return !getQuaternion().isIdentity();
	}

}
