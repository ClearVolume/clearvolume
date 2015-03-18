package clearvolume.renderer.cleargl.overlay;

import java.util.ArrayList;

import javax.media.opengl.GL4;

import cleargl.GLMatrix;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.processors.Processor;

public class OverlayForProcessors extends OverlayBase	implements
																											Overlay2D,
																											Overlay3D
{

	private final ArrayList<Processor<?>> mProcessors = new ArrayList<Processor<?>>();

	private final Overlay mOverlay;

	public OverlayForProcessors(Overlay Overlay)
	{
		mOverlay = Overlay;
	}

	public Overlay getDelegatedOverlay()
	{
		return mOverlay;
	}

	public void addProcessor(Processor<?> pProcessor)
	{
		mProcessors.add(pProcessor);
	}

	public void removeProcessor(Processor<?> pProcessor)
	{
		mProcessors.remove(pProcessor);
	}

	public ArrayList<Processor<?>> getProcessors()
	{
		return mProcessors;
	}

	@Override
	public boolean toggleDisplay()
	{
		final boolean lNewState = getDelegatedOverlay().toggleDisplay();
		for (final Processor<?> lProcessor : mProcessors)
		{
			lProcessor.toggleActive();
		}
		return lNewState;
	}

	@Override
	public boolean isDisplayed()
	{
		return getDelegatedOverlay().isDisplayed();
	}

	@Override
	public String getName()
	{
		return mOverlay.getName();
	}

	@Override
	public void init(	GL4 pGL4,
										DisplayRequestInterface pDisplayRequestInterface)
	{
		mOverlay.init(pGL4, pDisplayRequestInterface);
	}

	@Override
	public boolean hasChanged3D()
	{
		if (mOverlay instanceof Overlay3D)
		{
			final Overlay3D lOverlay3D = (Overlay3D) mOverlay;
			return lOverlay3D.hasChanged3D();
		}
		return false;
	}

	@Override
	public void render3D(	GL4 pGL4,
												GLMatrix pProjectionMatrix,
												GLMatrix pInvVolumeMatrix)
	{
		if (mOverlay instanceof Overlay3D)
		{
			final Overlay3D lOverlay3D = (Overlay3D) mOverlay;
			lOverlay3D.render3D(pGL4, pProjectionMatrix, pInvVolumeMatrix);
		}
	}

	@Override
	public boolean hasChanged2D()
	{
		if (mOverlay instanceof Overlay2D)
		{
			final Overlay2D lOverlay2D = (Overlay2D) mOverlay;
			return lOverlay2D.hasChanged2D();
		}
		return false;
	}

	@Override
	public void render2D(GL4 pGL4, GLMatrix pProjectionMatrix)
	{
		if (mOverlay instanceof Overlay2D)
		{
			final Overlay2D lOverlay2D = (Overlay2D) mOverlay;
			lOverlay2D.render2D(pGL4, pProjectionMatrix);
		}
	}

}
