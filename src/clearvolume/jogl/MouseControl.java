package clearvolume.jogl;

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
		if (!pMouseEvent.isShiftDown() && pMouseEvent.isButtonDown(1))
		{
			mRenderer.mTranslationX += dx / 100.0f;
			mRenderer.mTranslationY -= dy / 100.0f;
			mRenderer.notifyUpdateOfVolumeParameters();
		}

		// If the right button is held down, rotate the object
		else if (pMouseEvent.isShiftDown() && (pMouseEvent.isButtonDown(1)))
		{
			mRenderer.mRotationX += dy;
			mRenderer.mRotationY += dx;
			mRenderer.notifyUpdateOfVolumeParameters();
		}
		mPreviousMouseX = pMouseEvent.getX();
		mPreviousMouseY = pMouseEvent.getY();

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
																																				: 8;

		final double lWheelRotation = pMouseEvent.getWheelRotation();
		// System.out.println(lWheelRotation);
		// Translate along the Z-axis
		if (pMouseEvent.isAltDown())
		{
			mRenderer.addTransferOffset(lBytePerVoxelFactor * 0.001
																	* lWheelRotation);
		}
		else if (pMouseEvent.isShiftDown())
		{
			mRenderer.addTransferScale(lBytePerVoxelFactor * 0.001
																	* lWheelRotation);
		}
		else if (pMouseEvent.isMetaDown())
		{
			mRenderer.addDensity(lBytePerVoxelFactor * 0.001
														* lWheelRotation);
		}
		else if (pMouseEvent.isAltGraphDown())
		{
			mRenderer.addBrightness( 0.001
															* lWheelRotation);
		}
		else
		{

			mRenderer.mTranslationZ += 0.125f * lWheelRotation;
			mPreviousMouseX = pMouseEvent.getX();
			mPreviousMouseY = pMouseEvent.getY();
		}

		mRenderer.notifyUpdateOfVolumeParameters();
	}

	@Override
	public void mouseClicked(final MouseEvent pE)
	{
		if (pE.getClickCount() == 2)
		{
			mRenderer.toggleFullScreen();
		}
	}

}