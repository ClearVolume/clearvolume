package clearvolume.jogl;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;

/**
 * Inner class encapsulating the MouseMotionListener and MouseWheelListener for
 * the interaction
 */
class KeyboardControl extends KeyAdapter implements KeyListener
{
	/**
	 * 
	 */
	private final JoglPBOVolumeRenderer mRenderer;

	/**
	 * @param pJoglVolumeRenderer
	 */
	KeyboardControl(final JoglPBOVolumeRenderer pJoglVolumeRenderer)
	{
		mRenderer = pJoglVolumeRenderer;
	}

	@Override
	public void keyPressed(final KeyEvent pE)
	{
		final boolean lIsShiftPressed = pE.isShiftDown();
		final double speed = lIsShiftPressed ? 0.1 : 0.001;
		switch (pE.getKeyCode())
		{
		case KeyEvent.VK_DOWN:
			mRenderer.mTranslationZ -= speed;
			break;
		case KeyEvent.VK_UP:
			mRenderer.mTranslationZ += speed;
			break;

		case KeyEvent.VK_LEFT:
			mRenderer.mTranslationX -= speed;
			break;
		case KeyEvent.VK_RIGHT:
			mRenderer.mTranslationX += speed;
			break;

		case KeyEvent.VK_PAGE_DOWN:
			mRenderer.mTranslationY -= speed;
			break;
		case KeyEvent.VK_PAGE_UP:
			mRenderer.mTranslationY += speed;
			break;
		case KeyEvent.VK_ESCAPE:
			if (mRenderer.isFullScreen())
				mRenderer.toggleFullScreen();
			break;
		case KeyEvent.VK_R:
			mRenderer.resetDensityBrightnessOffsetScale();
			mRenderer.resetRotationTranslation();
			break;
		}
	}

}