package clearvolume.renderer.cleargl.overlay;

import javax.media.opengl.GL;

import cleargl.GLMatrix;

/**
 * Overlay2D interface - methods specific for 2D overlays.
 *
 * @author Loic Royer (2015)
 *
 */
public interface Overlay2D
{
	/**
	 * Returns true when the overlay has changed.
	 * 
	 * @return true if the overlay contents have changed and must be redrawn.
	 */
	public boolean hasChanged2D();

	/**
	 * OpenGl code to render the 2D overlay.
	 * 
	 * @param pGL
	 * @param pProjectionMatrix
	 */
	public void render2D(	GL pGL,
												int pWidth,
												int pHeight,
												GLMatrix pProjectionMatrix);

}