package clearvolume.renderer.jogl;

import static java.lang.Math.max;
import static java.lang.Math.min;

import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

/**
 * Class MouseControl
 * 
 * This class implements interface MouseListener and provides mouse controls for
 * the JoglPBOVolumeRender.
 *
 * @author Loic Royer 2014
 *
 */
class MouseControl extends MouseAdapter implements MouseListener
{
	/**
	 * Reference of the renderer
	 */
	private final JOGLClearVolumeRenderer mRenderer;

	/**
	 * @param pJoglVolumeRenderer
	 */
	MouseControl(final JOGLClearVolumeRenderer pClearVolumeRenderer)
	{
		mRenderer = pClearVolumeRenderer;
	}

	/**
	 * Previous mouse positions.
	 */
	private int mPreviousMouseX, mPreviousMouseY;

	/**
	 * Interface method implementation
	 * 
	 * @see com.jogamp.newt.event.MouseAdapter#mouseDragged(com.jogamp.newt.event.MouseEvent)
	 */
	@Override
	public void mouseDragged(final MouseEvent pMouseEvent)
	{
		if (mRenderer.notifyEyeRayListeners(mRenderer, pMouseEvent))
			return;

		handleRotationAndTranslation(pMouseEvent);
		handleGammaMinMax(pMouseEvent);
		mRenderer.notifyChangeOfVolumeRenderingParameters();
		mRenderer.getAdaptiveLODController()
							.notifyUserInteractionInProgress();
	}


	/**
	 * Interface method implementation
	 * 
	 * @see com.jogamp.newt.event.MouseAdapter#mouseMoved(com.jogamp.newt.event.MouseEvent)
	 */
	@Override
	public void mouseMoved(final MouseEvent pMouseEvent)
	{
		if (mRenderer.notifyEyeRayListeners(mRenderer, pMouseEvent))
			return;

		mPreviousMouseX = pMouseEvent.getX();
		mPreviousMouseY = pMouseEvent.getY();
	}

	/**
	 * Interface method implementation
	 * 
	 * @see com.jogamp.newt.event.MouseAdapter#mouseWheelMoved(com.jogamp.newt.event.MouseEvent)
	 */
	@Override
	public void mouseWheelMoved(final MouseEvent pMouseEvent)
	{
		if (mRenderer.notifyEyeRayListeners(mRenderer, pMouseEvent))
			return;

		final float[] lWheelRotation = pMouseEvent.getRotation();

		final double lZoomWheelFactor = 0.0125f;

		// mRenderer.addTranslationX(lWheelRotation[2] * lZoomWheelFactor);
		// mRenderer.addTranslationY(lWheelRotation[0] * lZoomWheelFactor);
		mRenderer.addTranslationZ(lWheelRotation[1] * lZoomWheelFactor);
		mPreviousMouseX = pMouseEvent.getX();
		mPreviousMouseY = pMouseEvent.getY();

		mRenderer.notifyChangeOfVolumeRenderingParameters();
		mRenderer.getAdaptiveLODController()
							.notifyUserInteractionInProgress();



	}

	/**
	 * Interface method implementation
	 * 
	 * @see com.jogamp.newt.event.MouseAdapter#mouseClicked(com.jogamp.newt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked(final MouseEvent pMouseEvent)
	{
		if (mRenderer.notifyEyeRayListeners(mRenderer, pMouseEvent))
			return;

		if (pMouseEvent.getClickCount() == 1)
		{
			handleGammaMinMax(pMouseEvent);
		}
		else if (pMouseEvent.getClickCount() == 2)
		{
			mRenderer.toggleFullScreen();
			mRenderer.notifyChangeOfVolumeRenderingParameters();
		}

	}

	@Override
	public void mousePressed(MouseEvent pMouseEvent)
	{
		if (mRenderer.notifyEyeRayListeners(mRenderer, pMouseEvent))
			return;

		mRenderer.getAdaptiveLODController()
							.notifyUserInteractionInProgress();

	}

	@Override
	public void mouseReleased(MouseEvent pMouseEvent)
	{
		if (mRenderer.notifyEyeRayListeners(mRenderer, pMouseEvent))
			return;

		mRenderer.getAdaptiveLODController().notifyUserInteractionEnded();
		super.mouseReleased(pMouseEvent);

	}

	private void handleRotationAndTranslation(final MouseEvent pMouseEvent)
	{
		final int dx = pMouseEvent.getX() - mPreviousMouseX;
		final int dy = pMouseEvent.getY() - mPreviousMouseY;

		// If the left button is held down, rotate the object
		if (!pMouseEvent.isMetaDown() && !pMouseEvent.isShiftDown()
				&& !pMouseEvent.isControlDown()
				&& pMouseEvent.isButtonDown(1))
		{
			mRenderer.rotate(dx, dy);
		}

		// If the right button is held down, translate the object
		else if (!pMouseEvent.isMetaDown() && !pMouseEvent.isControlDown()
							&& (pMouseEvent.isButtonDown(3)))
		{

			mRenderer.addTranslationX(dx / 100.0f);
			mRenderer.addTranslationY(-dy / 100.0f);
		}
		mPreviousMouseX = pMouseEvent.getX();
		mPreviousMouseY = pMouseEvent.getY();
	}

	/**
	 * Sets the transfer function range.
	 * 
	 * @param pMouseEvent
	 */
	public void handleGammaMinMax(final MouseEvent pMouseEvent)
	{
		if (!pMouseEvent.isMetaDown() && !pMouseEvent.isShiftDown()
				&& pMouseEvent.isControlDown()
				&& pMouseEvent.isButtonDown(1))
		{

			final double nx = ((double) pMouseEvent.getX()) / mRenderer.getWindowWidth();
			final double ny = ((double) mRenderer.getWindowHeight() - (double) pMouseEvent.getY()) / mRenderer.getWindowHeight();

			mRenderer.setTransferFunctionRange(	Math.abs(Math.pow(nx, 3)),
																					Math.abs(Math.pow(ny, 3)));

		}

		if (!pMouseEvent.isMetaDown() && pMouseEvent.isShiftDown()
				&& !pMouseEvent.isControlDown()
				&& pMouseEvent.isButtonDown(1))
		{

			final double nx = ((double) pMouseEvent.getX()) / mRenderer.getWindowWidth();

			mRenderer.setGamma(Math.tan(Math.PI * nx / 2));

		}

		if (!pMouseEvent.isMetaDown() && pMouseEvent.isShiftDown()
				&& pMouseEvent.isControlDown()
				&& pMouseEvent.isButtonDown(1))
		{

			final double nx = ((double) pMouseEvent.getX()) / mRenderer.getWindowWidth();

			mRenderer.setBrightness(Math.tan(Math.PI * nx / 2));

		}

		if (pMouseEvent.isMetaDown() && pMouseEvent.isButtonDown(1))
		{
			double nx = ((double) pMouseEvent.getX()) / mRenderer.getWindowWidth();

			nx = (max(min(nx, 1), 0));
			nx = nx * nx;

			mRenderer.setQuality(mRenderer.getCurrentRenderLayerIndex(), nx);
		}

		/*
		System.out.println("isAltDown" + pMouseEvent.isAltDown());
		System.out.println("isAltGraphDown" + pMouseEvent.isAltGraphDown());
		System.out.println("isControlDown" + pMouseEvent.isControlDown());
		System.out.println("isMetaDown" + pMouseEvent.isMetaDown());
		System.out.println("isShiftDown" + pMouseEvent.isShiftDown());/**/

	}

}