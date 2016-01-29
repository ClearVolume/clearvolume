package clearvolume.renderer.cleargl;

import static java.lang.Math.PI;

import java.util.Collection;

import cleargl.GLMatrix;
import cleargl.GLVector;
import scenery.Camera;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;

import clearvolume.controller.AutoRotationController;
import clearvolume.renderer.ClearVolumeRendererBase;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.SingleKeyToggable;
import clearvolume.renderer.cleargl.overlay.Overlay;
import clearvolume.renderer.processors.ProcessorInterface;
import clearvolume.transferf.CyclableTransferFunction;
import clearvolume.transferf.CyclingTransferFunction;
import clearvolume.transferf.TransferFunction1D;

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
	 *            renderer
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
					mClearVolumeRenderer.rotateByAngleX(-lRotationSpeed);

			}

			else
				mClearVolumeRenderer.addTranslationY(-lTranslationSpeed);

			break;

		case KeyEvent.VK_UP:
			if (mToggleRotationTranslation)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedX(+lAutoRotationSpeed);
				else
					mClearVolumeRenderer.rotateByAngleX(+lRotationSpeed);

			}
			else
				mClearVolumeRenderer.addTranslationY(+lTranslationSpeed);

			break;

		case KeyEvent.VK_LEFT:
			if (mToggleRotationTranslation)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedY(-lAutoRotationSpeed);
				else
					mClearVolumeRenderer.rotateByAngleY(+lRotationSpeed);

			}
			else
				mClearVolumeRenderer.addTranslationX(-lTranslationSpeed);

			break;

		case KeyEvent.VK_RIGHT:
			if (mToggleRotationTranslation)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedY(+lAutoRotationSpeed);
				else
					mClearVolumeRenderer.rotateByAngleY(-lRotationSpeed);

			}
			else
				mClearVolumeRenderer.addTranslationX(+lTranslationSpeed);

			break;

		case KeyEvent.VK_PAGE_DOWN:
			if (mToggleRotationTranslation)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedZ(-lAutoRotationSpeed);
				else
					mClearVolumeRenderer.rotateByAngleZ(+lRotationSpeed);

			}
			else
				mClearVolumeRenderer.addTranslationZ(-lTranslationSpeed / mClearVolumeRenderer.getFOV());

			break;

		case KeyEvent.VK_PAGE_UP:
			if (mToggleRotationTranslation)
			{
				if (lAutoRotateController.isActive())
					lAutoRotateController.addRotationSpeedZ(+lAutoRotationSpeed);
				else
					mClearVolumeRenderer.rotateByAngleZ(-lRotationSpeed);

			}
			else
				mClearVolumeRenderer.addTranslationZ(+lTranslationSpeed / mClearVolumeRenderer.getFOV());

			break;

		case KeyEvent.VK_ESCAPE:
			if (mClearVolumeRenderer.isFullScreen())
				mClearVolumeRenderer.toggleFullScreen();
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

		case KeyEvent.VK_D: {
			Camera cam = mClearVolumeRenderer.getScene().findObserver();
			if (cam.getTargeted() == false) {
				mClearVolumeRenderer.getScene().findObserver().getRotation().invert().rotateByAngleY(-lRotationSpeed).invert();
			}
		}
			break;

		case KeyEvent.VK_W: {
			Camera cam = mClearVolumeRenderer.getScene().findObserver();
			if (cam.getTargeted() == false) {
				/*GLMatrix r = GLMatrix.fromQuaternion(mClearVolumeRenderer.getScene().findObserver().getRotation());
				GLVector pos = mClearVolumeRenderer.getScene().findObserver().getPosition();

				float[] lookDirection = new float[4];
				GLMatrix.mulColMat4Vec4(lookDirection, r.getFloatArray(), new float[]{0.0f, 0.0f, 1.0f, 1.0f});
				lookDirection[0] = lookDirection[0] - pos.x();
				lookDirection[1] = lookDirection[1] - pos.y();
				lookDirection[2] = lookDirection[2] - pos.z();

				GLVector newPos = new GLVector(lookDirection);
				//mClearVolumeRenderer.getScene().findObserver().getView().translate(lookDirection[0], lookDirection[1], lookDirection[2]);
				*/

				GLVector newPos = cam.getPosition();
				GLVector orientation = cam.getWorldOrientation();
				newPos = newPos.plus(new GLVector(orientation.x(), orientation.y(), orientation.z()));

				cam.setPosition(newPos);
				System.err.println("Positin is now " + cam.getPosition());
			}
		}
			break;

		case KeyEvent.VK_S: {
			Camera cam = mClearVolumeRenderer.getScene().findObserver();
			if(cam.getTargeted() == false) {
				/*r = GLMatrix.fromQuaternion(mClearVolumeRenderer.getScene().findObserver().getRotation());
				pos = mClearVolumeRenderer.getScene().findObserver().getPosition();

				lookDirection = new float[4];
				GLMatrix.mulColMat4Vec4(lookDirection, r.getFloatArray(), new float[]{0.0f, 0.0f, 1.0f, 1.0f});
				lookDirection[0] = lookDirection[0] - pos.x();
				lookDirection[1] = lookDirection[1] - pos.y();
				lookDirection[2] = lookDirection[2] - pos.z();
				mClearVolumeRenderer.getScene().findObserver().getView().translate(-lookDirection[0], -lookDirection[1], -lookDirection[2]);*/
			}}
			break;

			case KeyEvent.VK_A: {
				Camera cam = mClearVolumeRenderer.getScene().findObserver();
				if(cam.getTargeted() == false) {
					//lAutoRotateController.setActive(!lAutoRotateController.isActive());
					mClearVolumeRenderer.getScene().findObserver().getRotation().invert().rotateByAngleY(+lRotationSpeed).invert();
				}}
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

		case KeyEvent.VK_T:
			if (mClearVolumeRenderer.getTransferFunction() instanceof CyclableTransferFunction)
			{
				final CyclableTransferFunction lCyclableTransferFunction = (CyclableTransferFunction) mClearVolumeRenderer.getTransferFunction();
				lCyclableTransferFunction.next();
			}
			else if (mClearVolumeRenderer.getTransferFunction() instanceof TransferFunction1D)
			{
				final CyclingTransferFunction lCyclingTransferFunction = CyclingTransferFunction.getDefault();
				lCyclingTransferFunction.addTransferFunction((TransferFunction1D) mClearVolumeRenderer.getTransferFunction());
				mClearVolumeRenderer.setTransferFunction(lCyclingTransferFunction);
			}
			break;

		case KeyEvent.VK_P:
			mClearVolumeRenderer.toggleParametersListFrame();
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
		final Collection<ProcessorInterface<?>> lProcessors = mClearVolumeRenderer.getProcessors();

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
				// (pE.getModifiers() &
				// lSingleKeyToggable.toggleKeyModifierMask()) ==
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