package clearvolume.renderer.jogl.overlay;


/**
 * OverlayBase - Class implementing basic functionality of classes implementing
 * the Overlay interface.
 *
 * @author Loic Royer (2015)
 *
 */
public abstract class OverlayBase implements Overlay
{
	private volatile boolean mDisplay = true;

	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay#toggleDisplay()
	 */
	@Override
	public boolean toggleDisplay()
	{
		mDisplay = !mDisplay;
		return mDisplay;
	}
	
	/* (non-Javadoc)
	 * @see clearvolume.renderer.jogl.overlay.Overlay#isDisplayed()
	 */
	@Override
	public boolean isDisplayed()
	{
		return mDisplay;
	}

}
