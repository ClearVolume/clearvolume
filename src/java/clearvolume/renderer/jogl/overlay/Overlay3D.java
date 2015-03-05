package clearvolume.renderer.jogl.overlay;

import javax.media.opengl.GL4;

import cleargl.GLMatrix;

/**
 * Overlay2D interface - methods specific for 2D overlays.
 *
 * @author Loic Royer (2015)
 *
 */
public interface Overlay3D
{
	/**
	 * Returns true when the overlay has changed.
	 * 
	 * @return true if the overlay contents have changed and must be redrawn.
	 */
	public boolean hasChanged3D();

	/**
	 * OpenGl code to render the 3D overlay.
	 * 
	 * @param pGL4
	 * @param pProjectionMatrix
	 * @param pInvVolumeMatrix
	 */
	public void render3D(	GL4 pGL4,
												GLMatrix pProjectionMatrix,
												GLMatrix pInvVolumeMatrix);


}
