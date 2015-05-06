package clearvolume.controller;

import clearvolume.renderer.ClearVolumeRendererInterface;

import com.jogamp.opengl.math.Quaternion;



public class AutoTranslateController extends ControllerBase	implements
																														TranslationControllerInterface,
																														ControllerWithRenderNotification
{

	private volatile float[] mVector = new float[3];

	public AutoTranslateController()
	{
		super();
		setActive(false);
	}

	@Override
	public void setVector(float[] pVector)
	{
		mVector = pVector;
	}

	@Override
	public float[] getVector()
	{
		// TODO Auto-generated method stub
		return mVector;
	}

	@Override
	public void notifyRender(ClearVolumeRendererInterface pClearVolumeRendererInterface)
	{

	}

	public boolean isMoving()
	{
		return mVector[0] != 0 || mVector[1] != 0 || mVector[2] != 0;
	}

	public void setDefaultTranslation()
	{
		stopTranslation();
	}

	public void stopTranslation()
	{
		mVector[0] = 0;
		mVector[1] = 0;
		mVector[2] = 0;
	}

	public void addTranslationSpeedX(float pDeltaX)
	{
		mVector[0] += pDeltaX;
	}

	public void addTranslationSpeedY(float pDeltaY)
	{
		mVector[1] += pDeltaY;
	}

	public void addTranslationSpeedZ(float pDeltaZ)
	{
		mVector[2] += pDeltaZ;
	}

	public void addTranslation(float[] pDelta)
	{
		mVector[0] += pDelta[0];
		mVector[1] += pDelta[1];
		mVector[2] += pDelta[2];
	}

	public void addTranslationSpeedX(	Quaternion pQuaternion,
																		float pDeltaX)
	{
		final float[] lRotatedVector = new float[3];
		lRotatedVector[0] = pDeltaX;
		
		pQuaternion.rotateVector(lRotatedVector, 0,  lRotatedVector, 0);

		addTranslation(lRotatedVector);
	}

	public void addTranslationSpeedY(	Quaternion pQuaternion,
																		float pDeltaY)
	{
		final float[] lRotatedVector = new float[3];
		lRotatedVector[1] = pDeltaY;

		pQuaternion.rotateVector(lRotatedVector, 0, lRotatedVector, 0);

		addTranslation(lRotatedVector);
	}

	public void addTranslationSpeedZ(	Quaternion pQuaternion,
																		float pDeltaZ)
	{
		final float[] lRotatedVector = new float[3];
		lRotatedVector[2] = pDeltaZ;

		pQuaternion.rotateVector(lRotatedVector, 0, lRotatedVector, 0);

		addTranslation(lRotatedVector);
	}



}
