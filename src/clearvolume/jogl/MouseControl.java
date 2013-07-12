package clearvolume.jogl;

import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;

/**
 * Inner class encapsulating the MouseMotionListener and MouseWheelListener for
 * the interaction
 */
class MouseControl extends MouseAdapter implements MouseListener
{
	/**
	 * 
	 */
	private final JoglPBOVolumeRenderer mRenderer;

	/**
	 * @param pJoglVolumeRenderer
	 */
	MouseControl(final JoglPBOVolumeRenderer pJoglVolumeRenderer)
	{
		mRenderer = pJoglVolumeRenderer;
	}

	private int mPreviousMouseX, mPreviousMouseY;

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
			mRenderer.mTranslationX += dx / 100.0f;
			mRenderer.mTranslationY -= dy / 100.0f;
			mRenderer.notifyUpdateOfVolumeParameters();
		}

		// If the right button is held down, rotate the object
		else if (!pMouseEvent.isControlDown() && (pMouseEvent.isButtonDown(3)))
		{
			mRenderer.mRotationX += dy;
			mRenderer.mRotationY += dx;
			mRenderer.notifyUpdateOfVolumeParameters();
		}
		mPreviousMouseX = pMouseEvent.getX();
		mPreviousMouseY = pMouseEvent.getY();

		setTransfertFunctionRange(pMouseEvent);

		mRenderer.requestDisplay();

	}

	public void setTransfertFunctionRange(final MouseEvent pMouseEvent)
	{
		if (!pMouseEvent.isShiftDown() && pMouseEvent.isControlDown()
				&& pMouseEvent.isButtonDown(1))
		{

			final double nx = ((double) pMouseEvent.getX()) / mRenderer.getWindowWidth();
			final double ny = ((double) mRenderer.getWindowHeight() - (double) pMouseEvent.getY()) / mRenderer.getWindowHeight();

			mRenderer.setTransferRange(	Math.abs(Math.pow(nx, 3)),
																	Math.abs(Math.pow(ny, 3)));

		}

		if (pMouseEvent.isShiftDown() && !pMouseEvent.isControlDown()
				&& pMouseEvent.isButtonDown(1))
		{

			final double nx = ((double) pMouseEvent.getX()) / mRenderer.getWindowWidth();

			mRenderer.setGamma(Math.tan(Math.PI * nx / 2));

		}
	}

	@Override
	public void mouseMoved(final MouseEvent pMouseEvent)
	{
		mPreviousMouseX = pMouseEvent.getX();
		mPreviousMouseY = pMouseEvent.getY();
	}

	@Override
	public void mouseWheelMoved(final MouseEvent pMouseEvent)
	{
		final double lBytePerVoxelFactor = mRenderer.getBytesPerVoxel() == 1 ? 1
																																				: 16;

		double lWheelRotation = pMouseEvent.getWheelRotation();

		final double lZoomWheelFactor = 0.125f;

		mRenderer.mTranslationZ += lWheelRotation * lZoomWheelFactor;
		mPreviousMouseX = pMouseEvent.getX();
		mPreviousMouseY = pMouseEvent.getY();

		mRenderer.notifyUpdateOfVolumeParameters();
		mRenderer.requestDisplay();
	}

	private boolean isRightMouseButton(MouseEvent pMouseEvent)
	{
		return ((pMouseEvent.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK);
	}

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