package clearvolume.renderer.jogl.overlay.o2d;

import javax.media.opengl.GL4;

import cleargl.GLMatrix;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.jogl.overlay.Overlay2DBase;

public class TextOverlay extends Overlay2DBase
{



	@Override
	public String getName()
	{
		return "text";
	}

	@Override
	public boolean hasChanged()
	{
		return false;
	}

	@Override
	public void init(	GL4 pGL4,
										DisplayRequestInterface pDisplayRequestInterface)
	{
		try
		{


		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void render(	GL4 pGL4,
											GLMatrix pProjectionMatrix,
											GLMatrix pInvVolumeMatrix)
	{
		if (isDisplayed())
		{
			// TODO: do something!

		}
	}


}
