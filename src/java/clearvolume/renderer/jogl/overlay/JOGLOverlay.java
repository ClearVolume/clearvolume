package clearvolume.renderer.jogl.overlay;

import javax.media.opengl.GL4;

import cleargl.GLMatrix;
import clearvolume.renderer.DisplayRequestInterface;

public abstract class JOGLOverlay implements Overlay
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

	@Override
	public abstract String getName();

	@Override
	public abstract void init(GL4 pGL4,
														DisplayRequestInterface pDisplayRequestInterface);

	@Override
	public abstract void render(GL4 pGL4,
															GLMatrix pProjectionMatrix,
															GLMatrix pInvVolumeMatrix);

}