package clearvolume.renderer.cleargl;

import static java.lang.Math.PI;

import java.util.Collection;

import clearvolume.controller.AutoRotationController;
import clearvolume.controller.AutoTranslateController;
import clearvolume.renderer.ClearVolumeRendererBase;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.cleargl.overlay.Overlay;
import clearvolume.renderer.cleargl.overlay.SingleKeyToggable;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;

/**
 * Class MouseControl
 * 
 * This class implements interface KeyListener and provides mouse controls for
 * the JoglPBOVolumeRender.
 *
 * @author Loic Royer 2014
 *
 */
class KeyboardControl extends KeyAdapter implements KeyListener
{

	volatile boolean mRotationMode = true;
	volatile boolean mSpaceShipMode = false;

	/**
	 * Reference to renderer.
	 */
	private final ClearVolumeRendererInterface mClearVolumeRenderer;

	/**
	 * Constructs a Keyboard control listener given a renderer.
	 * 
	 * @param pJoglVolumeRenderer
	 *          renderer
	 */
	KeyboardControl(final ClearVolumeRendererInterface pClearVolumeRenderer)
	{
		mClearVolumeRenderer = pClearVolumeRenderer;
	}

	/**
	 * Interface method implementation
	 * 
	 * @see com.jogamp.newt.event.KeyAdapter#keyPressed(com.jogamp.newt.event.KeyEvent)
	 */
	@Override
	public void keyPressed(final KeyEvent pE)
	{
		final AutoRotationController lAutoRotateController = mClearVolumeRenderer.getAutoRotateController();
		final AutoTranslateController lAutoTranslateController = mClearVolumeRenderer.getAutoTranslateController();

		final boolean lIsShiftPressed = pE.isShiftDown();
		final boolean lIsCtrlPressed = pE.isControlDown();
		final boolean lIsMetaPressed = pE.isMetaDown();
		final float lTranslationSpeed = lIsShiftPressed	? 0.1f
																										: (lIsMetaPressed	? 0.001f
																																			: 0.01f);
		final float lRotationSpeed = (float) (2 * PI * (lIsShiftPressed	? 0.025f
																																		: (lIsMetaPressed	? 0.0005f
																																											: 0.005f)));
		final float lAutoRotationSpeed = 0.01f * lRotationSpeed;

		switch (pE.getKeyCode())
		{
		case KeyEvent.VK_SPACE:
			mRotationMode = !mRotationMode;
			break;
		case KeyEvent.VK_DOWN:
			if (mRotationMode)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedX(-lAutoRotationSpeed);
				else
					mClearVolumeRenderer.getQuaternion()
															.invert()
															.rotateByAngleX(-lRotationSpeed)
															.invert();
			}
			else
			{
				if (lAutoTranslateController.isActive())
					if (mSpaceShipMode)
						lAutoTranslateController.addTranslationSpeedY(mClearVolumeRenderer.getQuaternion(),
																													+lTranslationSpeed);
					else
						lAutoTranslateController.addTranslationSpeedY(+lTranslationSpeed);
				else
					mClearVolumeRenderer.addTranslationY(+lTranslationSpeed);
			}
			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();
			break;
		case KeyEvent.VK_UP:
			if (mRotationMode)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedX(+lAutoRotationSpeed);
				else
					mClearVolumeRenderer.getQuaternion()
															.invert()
															.rotateByAngleX(+lRotationSpeed)
															.invert();
			}
			else
			{
				if (lAutoTranslateController.isActive())
					if (mSpaceShipMode)
						lAutoTranslateController.addTranslationSpeedY(mClearVolumeRenderer.getQuaternion(),
																													-lTranslationSpeed);
					else
						lAutoTranslateController.addTranslationSpeedY(-lTranslationSpeed);
				else
					mClearVolumeRenderer.addTranslationY(-lTranslationSpeed);
			}

			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();

			break;

		case KeyEvent.VK_LEFT:
			if (mRotationMode)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedY(-lAutoRotationSpeed);
				else
					mClearVolumeRenderer.getQuaternion()
															.invert()
															.rotateByAngleY(+lRotationSpeed)
															.invert();

			}
			else
			{
				if (lAutoTranslateController.isActive())
					if (mSpaceShipMode)
						lAutoTranslateController.addTranslationSpeedX(mClearVolumeRenderer.getQuaternion(),
																													+lTranslationSpeed);
					else
						lAutoTranslateController.addTranslationSpeedX(+lTranslationSpeed);
				else
					mClearVolumeRenderer.addTranslationX(+lTranslationSpeed);
			}
			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();

			break;
		case KeyEvent.VK_RIGHT:
			if (mRotationMode)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedY(+lAutoRotationSpeed);
				else
					mClearVolumeRenderer.getQuaternion()
															.invert()
															.rotateByAngleY(-lRotationSpeed)
															.invert();

			}
			else
			{
				if (lAutoTranslateController.isActive())
					if (mSpaceShipMode)
						lAutoTranslateController.addTranslationSpeedX(mClearVolumeRenderer.getQuaternion(),
																													-lTranslationSpeed);
					else
						lAutoTranslateController.addTranslationSpeedX(-lTranslationSpeed);
				else
					mClearVolumeRenderer.addTranslationX(-lTranslationSpeed);
			}

			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();

			break;

		case KeyEvent.VK_PAGE_DOWN:
			if (mRotationMode)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedZ(-lAutoRotationSpeed);
				else
					mClearVolumeRenderer.getQuaternion()
															.invert()
															.rotateByAngleZ(+lRotationSpeed)
															.invert();

			}
			else
			{
				if (lAutoTranslateController.isActive())
					if (mSpaceShipMode)
						lAutoTranslateController.addTranslationSpeedZ(mClearVolumeRenderer.getQuaternion(),
																													-lTranslationSpeed / mClearVolumeRenderer.getFOV());
					else
						lAutoTranslateController.addTranslationSpeedZ(-lTranslationSpeed / mClearVolumeRenderer.getFOV());
				else
					mClearVolumeRenderer.addTranslationZ(-lTranslationSpeed / mClearVolumeRenderer.getFOV());
			}

			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();

			break;
		case KeyEvent.VK_PAGE_UP:
			if (mRotationMode)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedZ(+lAutoRotationSpeed);
				else
					mClearVolumeRenderer.getQuaternion()
															.invert()
															.rotateByAngleZ(-lRotationSpeed)
															.invert();

			}
			else
			{
				if (lAutoTranslateController.isActive())
					if (mSpaceShipMode)
						lAutoTranslateController.addTranslationSpeedZ(mClearVolumeRenderer.getQuaternion(),
																													+lTranslationSpeed / mClearVolumeRenderer.getFOV());
					else
						lAutoTranslateController.addTranslationSpeedZ(+lTranslationSpeed / mClearVolumeRenderer.getFOV());
				else
					mClearVolumeRenderer.addTranslationZ(+lTranslationSpeed / mClearVolumeRenderer.getFOV());
			}

			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();

			break;
		case KeyEvent.VK_ESCAPE:
			if (mClearVolumeRenderer.isFullScreen())
				mClearVolumeRenderer.toggleFullScreen();
			break;

		case KeyEvent.VK_S:
			mClearVolumeRenderer.toggleRecording();

		case KeyEvent.VK_R:
			if (lAutoRotateController.isActive() && !lAutoRotateController.isRotating())
			{
				lAutoRotateController.setActive(false);
			}
			if (lAutoTranslateController.isActive() && !lAutoTranslateController.isMoving())
			{
				lAutoTranslateController.setActive(false);
			}
			if (lAutoRotateController.isActive() || lAutoTranslateController.isActive())
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.stopRotation();
				if (lAutoTranslateController.isActive())
					lAutoTranslateController.stopTranslation();
			}
			else
			{
				if (lAutoRotateController.isActive() || lAutoTranslateController.isActive())
				{
					if (lAutoRotateController.isActive())
						lAutoRotateController.stopRotation();
					if (lAutoTranslateController.isActive())
						lAutoTranslateController.stopTranslation();
				}
				else
				{
					mClearVolumeRenderer.resetBrightnessAndGammaAndTransferFunctionRanges();
					mClearVolumeRenderer.resetRotationTranslation();
				}
			}
			break;

		case KeyEvent.VK_A:
			if (lIsCtrlPressed)
			{
				lAutoTranslateController.setActive(!lAutoTranslateController.isActive());
				if (lAutoTranslateController.isActive() && !lAutoTranslateController.isMoving())
				{
					lAutoTranslateController.setDefaultTranslation();
				}
				mRotationMode = false;
			}
			else
			{
				lAutoRotateController.setActive(!lAutoRotateController.isActive());
				if (lAutoRotateController.isActive() && !lAutoRotateController.isRotating())
				{
					lAutoRotateController.setDefaultRotation();
				}
				mRotationMode = true;
			}
			break;

		case KeyEvent.VK_C:
			mClearVolumeRenderer.requestVolumeCapture();
			break;

		case KeyEvent.VK_M:
			mClearVolumeRenderer.toggleAdaptiveLOD();
			break;

		case KeyEvent.VK_O:
			if (mClearVolumeRenderer.getFOV() == ClearVolumeRendererBase.cDefaultFOV)
				mClearVolumeRenderer.setFOV(ClearVolumeRendererBase.cOrthoLikeFOV);
			else
				mClearVolumeRenderer.setFOV(ClearVolumeRendererBase.cDefaultFOV);
			break;

		case KeyEvent.VK_I:
			mClearVolumeRenderer.cycleRenderAlgorithm();
			break;

		case KeyEvent.VK_TAB:
			mSpaceShipMode = !mSpaceShipMode;
			mClearVolumeRenderer.setTranslateFirstRotateSecond(!mSpaceShipMode);
			System.out.println("mSpaceShipMode=" + mSpaceShipMode);
			break;
		}

		if (pE.getKeyCode() >= KeyEvent.VK_0 && pE.getKeyCode() <= KeyEvent.VK_9)
		{
			int lRenderLayerIndex = pE.getKeyCode() - KeyEvent.VK_0;

			if (lRenderLayerIndex == 0)
				lRenderLayerIndex = 10;
			else
				lRenderLayerIndex--;

			if (lRenderLayerIndex < mClearVolumeRenderer.getNumberOfRenderLayers())
			{
				if (lIsShiftPressed)
					mClearVolumeRenderer.setLayerVisible(	lRenderLayerIndex,
																								!mClearVolumeRenderer.isLayerVisible(lRenderLayerIndex));
				else
					mClearVolumeRenderer.setCurrentRenderLayer(lRenderLayerIndex);
			}
		}

		processOverlayRelatedEvents(pE);

	}

	private void processOverlayRelatedEvents(KeyEvent pE)
	{
		final Collection<Overlay> lOverlays = mClearVolumeRenderer.getOverlays();

		boolean lHasAnyOverlayBeenToggled = false;

		for (final Overlay lOverlay : lOverlays)
			if (lOverlay instanceof SingleKeyToggable)
			{
				final SingleKeyToggable lSingleKeyToggable = (SingleKeyToggable) lOverlay;

				final boolean lRightKey = pE.getKeyCode() == lSingleKeyToggable.toggleKeyCode();
				final boolean lRightModifiers = (pE.getModifiers() & lSingleKeyToggable.toggleKeyModifierMask()) == lSingleKeyToggable.toggleKeyModifierMask();

				if (lRightKey && lRightModifiers)
				{
					lOverlay.toggleDisplay();
					lHasAnyOverlayBeenToggled = true;
				}
			}

		if (lHasAnyOverlayBeenToggled)
			mClearVolumeRenderer.requestDisplay();
	}
}