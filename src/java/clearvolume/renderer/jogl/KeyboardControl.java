package clearvolume.renderer.jogl;

import clearvolume.renderer.ClearVolumeRendererInterface;

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
		final boolean lIsShiftPressed = pE.isShiftDown();
		final boolean lIsCtrlPressed = pE.isControlDown();
		final float speed = lIsShiftPressed ? 0.1f : 0.001f;

		switch (pE.getKeyCode())
		{
		case KeyEvent.VK_DOWN:
			mClearVolumeRenderer.addTranslationZ(-speed);
			mClearVolumeRenderer.requestDisplay();
			break;
		case KeyEvent.VK_UP:
			mClearVolumeRenderer.addTranslationZ(+speed);
			mClearVolumeRenderer.requestDisplay();
			break;

		case KeyEvent.VK_LEFT:
			mClearVolumeRenderer.addTranslationX(-speed);
			mClearVolumeRenderer.requestDisplay();
			break;
		case KeyEvent.VK_RIGHT:
			mClearVolumeRenderer.addTranslationX(+speed);
			mClearVolumeRenderer.requestDisplay();
			break;

		case KeyEvent.VK_PAGE_DOWN:
			mClearVolumeRenderer.addTranslationY(-speed);
			mClearVolumeRenderer.requestDisplay();
			break;
		case KeyEvent.VK_PAGE_UP:
			mClearVolumeRenderer.addTranslationY(+speed);
			mClearVolumeRenderer.requestDisplay();
			break;
		case KeyEvent.VK_ESCAPE:
			if (mClearVolumeRenderer.isFullScreen())
				mClearVolumeRenderer.toggleFullScreen();
			mClearVolumeRenderer.requestDisplay();
			break;
		case KeyEvent.VK_R:
			if (lIsCtrlPressed)
			{
				mClearVolumeRenderer.toggleRecording();
			}
			else
			{
				mClearVolumeRenderer.resetBrightnessAndGammaAndTransferFunctionRanges();
				mClearVolumeRenderer.resetRotationTranslation();
				mClearVolumeRenderer.requestDisplay();
			}
			break;
		case KeyEvent.VK_B:
			mClearVolumeRenderer.toggleBoxDisplay();
			mClearVolumeRenderer.requestDisplay();
			break;
		case KeyEvent.VK_C:
			mClearVolumeRenderer.toggleControlPanelDisplay();
			mClearVolumeRenderer.requestDisplay();
			break;

		}

	}
}