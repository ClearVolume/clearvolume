package clearvolume.renderer.listeners;

import clearvolume.renderer.jogl.JOGLClearVolumeRenderer;
import clearvolume.renderer.jogl.utils.ScreenToEyeRay.EyeRay;

import com.jogamp.newt.event.MouseEvent;

public interface EyeRayListener
{

	/**
	 * @param pRenderer
	 * @param pMouseEvent
	 * @param pEyeRay
	 * @return true if a listener wants to prevent any other display parameter
	 *         change (rotation, translation, ...)
	 */
	boolean notifyEyeRay(	JOGLClearVolumeRenderer pRenderer,
										MouseEvent pMouseEvent,
										EyeRay pEyeRay);

}
