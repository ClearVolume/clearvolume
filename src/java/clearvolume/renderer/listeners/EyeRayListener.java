package clearvolume.renderer.listeners;

import com.jogamp.newt.event.MouseEvent;

import clearvolume.renderer.cleargl.ClearGLVolumeRenderer;
import clearvolume.renderer.cleargl.utils.ScreenToEyeRay.EyeRay;

public interface EyeRayListener
{

	/**
	 * @param pRenderer
	 * @param pMouseEvent
	 * @param pEyeRay
	 * @return true if a listener wants to prevent any other display parameter
	 *         change (rotation, translation, ...)
	 */
	boolean notifyEyeRay(	ClearGLVolumeRenderer pRenderer,
							MouseEvent pMouseEvent,
							EyeRay pEyeRay);

}
