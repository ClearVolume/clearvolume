package clearvolume.renderer.jogl.overlay.o2d;

import javax.media.opengl.GL4;

import cleargl.GLMatrix;
import clearvolume.renderer.DisplayRequestInterface;
import clearvolume.renderer.jogl.overlay.OverlayBase;

public class TextOverlay extends OverlayBase
{



	@Override
	public String getName()
	{
		return "text";
	}

	@Override
	public boolean hasChanged2D()
	{
		return false;
	}

	@Override
	public boolean hasChanged3D()
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
	public void render2D(	GL4 pGL4,
											GLMatrix pProjectionMatrix,
											GLMatrix pInvVolumeMatrix)
	{
		if (isDisplayed())
		{
			// TODO: do something!

		}
	}

	@Override
	public void render3D(	GL4 pGL4,
												GLMatrix pProjectionMatrix,
												GLMatrix pInvVolumeMatrix)
	{
		if (isDisplayed())
		{
			// TODO: do something!

		}
	}

}
