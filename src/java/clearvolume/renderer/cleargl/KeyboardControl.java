package clearvolume.renderer.cleargl;

import static java.lang.Math.PI;

import java.util.Collection;

import clearvolume.controller.AutoRotationController;
import clearvolume.renderer.ClearVolumeRendererBase;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.SingleKeyToggable;
import clearvolume.renderer.cleargl.overlay.Overlay;
import clearvolume.renderer.processors.Processor;

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

	volatile boolean mToggleRotationTranslation = true;

	/**
	 * Reference to renderer.
	 */
	private final ClearVolumeRendererInterface mClearVolumeRenderer;

	/**
	 * Mouse control
	 */
	private final MouseControl mMouseControl;

	/**
	 * Constructs a Keyboard control listener given a renderer.
	 * 
	 * @param pMouseControl
	 * 
	 * @param pJoglVolumeRenderer
	 *          renderer
	 */
	KeyboardControl(final ClearVolumeRendererInterface pClearVolumeRenderer,
									MouseControl pMouseControl)
	{
		mClearVolumeRenderer = pClearVolumeRenderer;
		mMouseControl = pMouseControl;
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
			mToggleRotationTranslation = !mToggleRotationTranslation;
			break;

		case KeyEvent.VK_DOWN:
			if (mToggleRotationTranslation)
			{

				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedX(-lAutoRotationSpeed);
				else
					mClearVolumeRenderer.setQuaternion(mClearVolumeRenderer.getQuaternion()
																																	.invert()
																																	.rotateByAngleX(-lRotationSpeed)
																																	.invert());

			}

			else
				mClearVolumeRenderer.addTranslationY(-lTranslationSpeed);
			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();
			break;

		case KeyEvent.VK_UP:
			if (mToggleRotationTranslation)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedX(+lAutoRotationSpeed);
				else
					mClearVolumeRenderer.setQuaternion(mClearVolumeRenderer.getQuaternion()
																																	.invert()
																																	.rotateByAngleX(+lRotationSpeed)
																																	.invert());

			}
			else
				mClearVolumeRenderer.addTranslationY(+lTranslationSpeed);
			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();
			break;

		case KeyEvent.VK_LEFT:
			if (mToggleRotationTranslation)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedY(-lAutoRotationSpeed);
				else
					mClearVolumeRenderer.setQuaternion(mClearVolumeRenderer.getQuaternion()
																																	.invert()
																																	.rotateByAngleY(+lRotationSpeed)
																																	.invert());

			}
			else
				mClearVolumeRenderer.addTranslationX(-lTranslationSpeed);
			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();
			break;

		case KeyEvent.VK_RIGHT:
			if (mToggleRotationTranslation)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedY(+lAutoRotationSpeed);
				else
					mClearVolumeRenderer.setQuaternion(mClearVolumeRenderer.getQuaternion()
																																	.invert()
																																	.rotateByAngleY(-lRotationSpeed)
																																	.invert());

			}
			else
				mClearVolumeRenderer.addTranslationX(+lTranslationSpeed);

			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();
			break;

		case KeyEvent.VK_PAGE_DOWN:
			if (mToggleRotationTranslation)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedZ(-lAutoRotationSpeed);
				else
					mClearVolumeRenderer.setQuaternion(mClearVolumeRenderer.getQuaternion()
																																	.invert()
																																	.rotateByAngleZ(+lRotationSpeed)
																																	.invert());

			}
			else
				mClearVolumeRenderer.addTranslationZ(-lTranslationSpeed / mClearVolumeRenderer.getFOV());

			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();
			break;

		case KeyEvent.VK_PAGE_UP:
			if (mToggleRotationTranslation)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedZ(+lAutoRotationSpeed);
				else
					mClearVolumeRenderer.setQuaternion(mClearVolumeRenderer.getQuaternion()
																																	.invert()
																																	.rotateByAngleZ(-lRotationSpeed)
																																	.invert());

			}
			else
				mClearVolumeRenderer.addTranslationZ(+lTranslationSpeed / mClearVolumeRenderer.getFOV());

			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();
			break;

		case KeyEvent.VK_ESCAPE:
			if (mClearVolumeRenderer.isFullScreen())
				mClearVolumeRenderer.toggleFullScreen();
			break;

		case KeyEvent.VK_S:
			mClearVolumeRenderer.toggleRecording();
			break;

		case KeyEvent.VK_R:
			if (lAutoRotateController.isActive() && !lAutoRotateController.isRotating())
			{
				lAutoRotateController.setActive(false);
			}
			if (lAutoRotateController.isActive())
			{
				lAutoRotateController.stop();
			}
			else
			{
				mClearVolumeRenderer.resetBrightnessAndGammaAndTransferFunctionRanges();
				mClearVolumeRenderer.resetRotationTranslation();
			}
			break;

		case KeyEvent.VK_A:
			lAutoRotateController.setActive(!lAutoRotateController.isActive());
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
			mClearVolumeRenderer.cycleRenderAlgorithm(mClearVolumeRenderer.getCurrentRenderLayerIndex());
			break;

		case KeyEvent.VK_L:
			mMouseControl.toggleMoveLightMode();
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

		handleOverlayRelatedEvents(pE);
		handleProcessorsRelatedEvents(pE);

	}

	private void handleOverlayRelatedEvents(KeyEvent pE)
	{
		final Collection<Overlay> lOverlays = mClearVolumeRenderer.getOverlays();

		processSingleKeyToggableEvents(pE, lOverlays);
	}

	private void handleProcessorsRelatedEvents(KeyEvent pE)
	{
		final Collection<Processor<?>> lProcessors = mClearVolumeRenderer.getProcessors();

		processSingleKeyToggableEvents(pE, lProcessors);
	}

	private void processSingleKeyToggableEvents(KeyEvent pE,
																							final Collection<?> lOverlays)
	{
		boolean lHasAnyOverlayBeenToggled = false;

		for (final Object lOverlay : lOverlays)
			if (lOverlay instanceof SingleKeyToggable)
			{
				final SingleKeyToggable lSingleKeyToggable = (SingleKeyToggable) lOverlay;

				final boolean lRightKey = pE.getKeyCode() == lSingleKeyToggable.toggleKeyCode();
				final boolean lRightModifiers = pE.getModifiers() == lSingleKeyToggable.toggleKeyModifierMask();
				// (pE.getModifiers() & lSingleKeyToggable.toggleKeyModifierMask()) ==
				// lSingleKeyToggable.toggleKeyModifierMask();

				if (lRightKey && lRightModifiers)
				{
					lSingleKeyToggable.toggle();

					lHasAnyOverlayBeenToggled = true;
				}
			}

		if (lHasAnyOverlayBeenToggled)
			mClearVolumeRenderer.notifyChangeOfVolumeRenderingParameters();
	}

}