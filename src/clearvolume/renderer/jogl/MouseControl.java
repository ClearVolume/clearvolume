package clearvolume.renderer.jogl;

import clearvolume.renderer.ClearVolumeRendererInterface;

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
	private final ClearVolumeRendererInterface mRenderer;

	/**
	 * @param pJoglVolumeRenderer
	 */
	MouseControl(final ClearVolumeRendererInterface pClearVolumeRenderer)
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
		// System.out.println(pMouseEvent);

		final int dx = pMouseEvent.getX() - mPreviousMouseX;
		final int dy = pMouseEvent.getY() - mPreviousMouseY;

		// If the left button is held down, move the object
		if (!pMouseEvent.isShiftDown() && !pMouseEvent.isControlDown()
				&& pMouseEvent.isButtonDown(1))
		{
			mRenderer.addTranslationX(dx / 100.0f);
			mRenderer.addTranslationY(-dy / 100.0f);
			mRenderer.notifyUpdateOfVolumeRenderingParameters();
		}

		// If the right button is held down, rotate the object
		else if (!pMouseEvent.isControlDown() && (pMouseEvent.isButtonDown(3)))
		{
			mRenderer.addRotationX(dx);
			mRenderer.addRotationY(dy);
			mRenderer.notifyUpdateOfVolumeRenderingParameters();
		}
		mPreviousMouseX = pMouseEvent.getX();
		mPreviousMouseY = pMouseEvent.getY();

		setTransfertFunctionRange(pMouseEvent);

		mRenderer.requestDisplay();

	}

	/**
	 * Sets the transfer function range.
	 * 
	 * @param pMouseEvent
	 */
	public void setTransfertFunctionRange(final MouseEvent pMouseEvent)
	{
		if (!pMouseEvent.isShiftDown() && pMouseEvent.isControlDown()
				&& pMouseEvent.isButtonDown(1))
		{

			final double nx = ((double) pMouseEvent.getX()) / mRenderer.getWindowWidth();
			final double ny = ((double) mRenderer.getWindowHeight() - (double) pMouseEvent.getY()) / mRenderer.getWindowHeight();

			mRenderer.setTransferFunctionRange(	Math.abs(Math.pow(nx, 3)),
																	Math.abs(Math.pow(ny, 3)));

		}

		if (pMouseEvent.isShiftDown() && !pMouseEvent.isControlDown()
				&& pMouseEvent.isButtonDown(1))
		{

			final double nx = ((double) pMouseEvent.getX()) / mRenderer.getWindowWidth();

			mRenderer.setGamma(Math.tan(Math.PI * nx / 2));

		}
	}

	/**
	 * Interface method implementation
	 * 
	 * @see com.jogamp.newt.event.MouseAdapter#mouseMoved(com.jogamp.newt.event.MouseEvent)
	 */
	@Override
	public void mouseMoved(final MouseEvent pMouseEvent)
	{
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

		double lWheelRotation = pMouseEvent.getWheelRotation();

		final double lZoomWheelFactor = 0.125f;

		mRenderer.addTranslationZ(lWheelRotation * lZoomWheelFactor);
		mPreviousMouseX = pMouseEvent.getX();
		mPreviousMouseY = pMouseEvent.getY();

		mRenderer.notifyUpdateOfVolumeRenderingParameters();
		mRenderer.requestDisplay();
	}


	/**
	 * Interface method implementation
	 * 
	 * @see com.jogamp.newt.event.MouseAdapter#mouseClicked(com.jogamp.newt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked(final MouseEvent pMouseEvent)
	{
		if (pMouseEvent.getClickCount() == 1)
		{
			setTransfertFunctionRange(pMouseEvent);
		}
		else if (pMouseEvent.getClickCount() == 2)
		{
			mRenderer.toggleFullScreen();
			mRenderer.requestDisplay();
		}

	}
}