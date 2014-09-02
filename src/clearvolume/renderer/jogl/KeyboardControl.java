package clearvolume.renderer.jogl;

import clearvolume.renderer.ClearVolumeRenderer;

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
	/**
	 * Reference to renderer.
	 */
	private final ClearVolumeRenderer mClearVolumeRenderer;

	/**
	 * Constructs a Keyboard control listener given a renderer.
	 * 
	 * @param pJoglVolumeRenderer
	 *          renderer
	 */
	KeyboardControl(final ClearVolumeRenderer pClearVolumeRenderer)
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
		final boolean lIsShiftPressed = pE.isShiftDown();
		final float speed = lIsShiftPressed ? 0.1f : 0.001f;

		switch (pE.getKeyCode())
		{
		case KeyEvent.VK_DOWN:
			mClearVolumeRenderer.addTranslationZ(-speed);
			break;
		case KeyEvent.VK_UP:
			mClearVolumeRenderer.addTranslationZ(+speed);
			break;

		case KeyEvent.VK_LEFT:
			mClearVolumeRenderer.addTranslationX(-speed);
			break;
		case KeyEvent.VK_RIGHT:
			mClearVolumeRenderer.addTranslationX(+speed);
			break;

		case KeyEvent.VK_PAGE_DOWN:
			mClearVolumeRenderer.addTranslationY(-speed);
			break;
		case KeyEvent.VK_PAGE_UP:
			mClearVolumeRenderer.addTranslationY(+speed);
			break;
		case KeyEvent.VK_ESCAPE:
			if (mClearVolumeRenderer.isFullScreen())
				mClearVolumeRenderer.toggleFullScreen();
			break;
		case KeyEvent.VK_R:
			mClearVolumeRenderer.resetBrightnessAndGammaAndTransferFunctionRanges();
			mClearVolumeRenderer.resetRotationTranslation();
			break;
		}

		mClearVolumeRenderer.requestDisplay();
	}

}