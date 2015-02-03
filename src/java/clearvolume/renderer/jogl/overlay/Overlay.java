package clearvolume.renderer.jogl.overlay;

import javax.media.opengl.GL4;

import cleargl.GLMatrix;

public interface Overlay
{
	public String getName();

	public void init(GL4 pGL4);

	public void render(	GL4 pGL4,
											GLMatrix pProjectionMatrix,
											GLMatrix pInvVolumeMatrix);

}
