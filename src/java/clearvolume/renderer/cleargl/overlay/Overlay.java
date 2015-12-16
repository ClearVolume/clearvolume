package clearvolume.renderer.cleargl.overlay;

import com.jogamp.opengl.GL;
import cleargl.RendererInterface;

import clearvolume.renderer.DisplayRequestInterface;

/**
 * Overlays enable rendering OpenGl primitives within the 3D volume (3D overlay)
 * but also on top of the whole window (2D overlay)
 *
 * @author Loic Royer (2015)
 *
 */
public interface Overlay
{
	RendererInterface renderer = null;
	/**
	 * Name of overlay.
	 * 
	 * @return Name.
	 */
	public String getName();

	/**
	 * Toggle the display of the overlay.
	 * 
	 * @return state after toggle.
	 */
	public boolean toggle();

	/**
	 * Sets display flag.
	 * 
	 * @param pDisplay
	 *            true if displayed, false if not
	 */
	public void setDisplayed(boolean pDisplay);

	/**
	 * Returns true if the overlay is displayed.
	 * 
	 * @return true if displayed.
	 */
	public boolean isDisplayed();

	/**
	 * OPenGL Intialization code. In addition to a GL object, a
	 * DisplayRequestInterface object is passed that can be used to request
	 * redrawing.
	 * 
	 * 
	 * @param pGL
	 *            a GL object.
	 * @param pDisplayRequestInterface
	 *            to request display.
	 */
	public void init(	GL pGL,
						DisplayRequestInterface pDisplayRequestInterface);

}
