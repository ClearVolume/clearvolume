package clearvolume.controller;



/**
 * Class ControllerBase
 * 
 *
 * @author Loic Royer 2014
 *
 */
public abstract class ControllerBase implements ControllerInterface
{

	private volatile boolean mActive = true;

	@Override
	public void setActive(boolean pActive)
	{
		mActive = pActive;
	}

	@Override
	public boolean isActive()
	{
		return mActive;
	}


}
