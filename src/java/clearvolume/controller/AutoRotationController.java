package clearvolume.controller;

import com.jogamp.opengl.math.Quaternion;

import clearvolume.renderer.ClearVolumeRendererInterface;

public class AutoRotationController	extends
									QuaternionRotationControllerBase implements
																	RotationControllerInterface,
																	RotationControllerWithRenderNotification
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

	public void stop()
	{
		getQuaternion().setIdentity();

	}

	public boolean isRotating()
	{
		return !getQuaternion().isIdentity();
	}

}
