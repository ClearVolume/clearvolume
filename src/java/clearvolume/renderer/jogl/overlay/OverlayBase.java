package clearvolume.renderer.jogl.overlay;


public abstract class OverlayBase implements Overlay
{
	private volatile boolean mDisplay = true;

	@Override
	public boolean toggleDisplay()
	{
		mDisplay = !mDisplay;
		return mDisplay;
	}
	
	@Override
	public boolean isDisplayed()
	{
		return mDisplay;
	}

}
