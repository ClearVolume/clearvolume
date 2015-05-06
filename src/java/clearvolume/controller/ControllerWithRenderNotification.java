package clearvolume.controller;

import clearvolume.renderer.ClearVolumeRendererInterface;

/**
 * Interface ControllerWithRenderNotification
 *
 * @author Loic Royer (2015)
 *
 */
public interface ControllerWithRenderNotification
{
	/**
	 * Receives calls to notify rendering events
	 * 
	 * @param pClearVolumeRendererInterface
	 */
	public void notifyRender(ClearVolumeRendererInterface pClearVolumeRendererInterface);
}
