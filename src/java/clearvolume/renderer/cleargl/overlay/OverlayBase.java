package clearvolume.renderer.cleargl.overlay;

import cleargl.scenegraph.Node;
/**
 * OverlayBase - Class implementing basic functionality of classes implementing
 * the Overlay interface.
 *
 * @author Loic Royer (2015)
 *
 */
public abstract class OverlayBase extends Node implements Overlay
{
	private volatile boolean mDisplay = true;

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.Overlay#toggleDisplay()
	 */
	@Override
	public boolean toggle()
	{
		setDisplayed(!isDisplayed());
		return isDisplayed();
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.Overlay#setDisplay(boolean)
	 */
	@Override
	public void setDisplayed(boolean pDisplay)
	{
		mDisplay = pDisplay;
	}

	/* (non-Javadoc)
	 * @see clearvolume.renderer.cleargl.overlay.Overlay#isDisplayed()
	 */
	@Override
	public boolean isDisplayed()
	{
		return mDisplay;
	}

	public OverlayBase() {
		super("Overlay-" + getClass().getName());
	}

}
