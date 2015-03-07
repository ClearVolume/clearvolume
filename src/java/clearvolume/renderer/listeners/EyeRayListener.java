package clearvolume.renderer.listeners;

import clearvolume.renderer.jogl.JOGLClearVolumeRenderer;
import clearvolume.renderer.jogl.utils.ScreenToEyeRay.EyeRay;

import com.jogamp.newt.event.MouseEvent;

public interface EyeRayListener
{

	void notifyEyeRay(JOGLClearVolumeRenderer pRenderer,
										MouseEvent pMouseEvent,
										EyeRay pEyeRay);

}
