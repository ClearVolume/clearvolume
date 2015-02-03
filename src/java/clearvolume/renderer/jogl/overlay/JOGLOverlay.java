package clearvolume.renderer.jogl.overlay;

import javax.media.opengl.GL4;

import cleargl.GLMatrix;

public abstract class JOGLOverlay implements Overlay
{
	private volatile boolean mDisplay = true;

	public boolean toggleDisplay()
	{
		mDisplay = !mDisplay;
		return mDisplay;
	}
	
	public boolean isDisplayed()
	{
		return mDisplay;
	}

	@Override
	public abstract String getName();

	@Override
	public abstract void init(GL4 pGL4);

	@Override
	public abstract void render(GL4 pGL4,
															GLMatrix pProjectionMatrix,
															GLMatrix pInvVolumeMatrix);

}
