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
	}

}
