package clearvolume.renderer.cleargl.overlay;


/**
 * Overlays that implement this interface can declare a key binding that will be
 * used to toggle it's display on/off
 *
 * @author Loic Royer (2015)
 *
 */
public interface SingleKeyToggable
{


		/**
	 * Returns key code of toggle key combination.
	 * 
	 * @return toggle key as short code.
	 */
	public short toggleKeyCode();

	/**
	 * Returns modifier of toggle key combination.
	 * 
	 * @return toggle key as short code.
	 */
	public int toggleKeyModifierMask();



}
