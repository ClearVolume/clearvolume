package clearvolume.renderer.jogl;

public class AdaptiveLODController
{

	private final JOGLClearVolumeRenderer mJoglClearVolumeRenderer;

	private volatile boolean mMultiPassRenderingInProgress;
	private volatile boolean mDisplayRequestReceived;

	public AdaptiveLODController(JOGLClearVolumeRenderer pJoglClearVolumeRenderer)
	{
		mJoglClearVolumeRenderer = pJoglClearVolumeRenderer;

	}

	public void requestDisplay()
	{
		if (mMultiPassRenderingInProgress)
			mDisplayRequestReceived = true;
		else
			mJoglClearVolumeRenderer.requestDisplayInternal();
	}

	public boolean isKernelRunNeeded()
	{
		return false;
	}

	public float getPhase()
	{
		return 0;
	}

	public boolean isBufferClearingNeeded()
	{
		return true;
	}

	public boolean isRedrawNeeded()
	{
		return false;
	}

}
