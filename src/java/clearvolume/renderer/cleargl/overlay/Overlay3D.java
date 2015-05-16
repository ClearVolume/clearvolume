package clearvolume.renderer.cleargl.overlay;

import cleargl.GLMatrix;

import com.jogamp.opengl.GL;

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
	 * @param pGL
	 *          GL object
	 * @param pWidth
	 *          viewport width
	 * @param pHeight
	 *          viewport height
	 * @param pProjectionMatrix
	 *          projection matrix
	 * @param pModelViewMatrix
	 *          model view matrix
	 */
	public void render3D(	GL pGL,
												int pWidth,
												int pHeight,
												GLMatrix pProjectionMatrix,
												GLMatrix pModelViewMatrix);

}
