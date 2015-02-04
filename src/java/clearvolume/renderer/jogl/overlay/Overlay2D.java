package clearvolume.renderer.jogl.overlay;

import javax.media.opengl.GL4;

import cleargl.GLMatrix;
import clearvolume.renderer.DisplayRequestInterface;

public interface Overlay2D
{
	public String getName();

	public boolean toggleDisplay();

	public boolean isDisplayed();

	public void init(	GL4 pGL4,
										DisplayRequestInterface pDisplayRequestInterface);

	public void render(	GL4 pGL4,
											GLMatrix pProjectionMatrix,
											GLMatrix pInvVolumeMatrix);

	public boolean hasChanged();

}
