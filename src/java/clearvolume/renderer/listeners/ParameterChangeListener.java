package clearvolume.renderer.listeners;

import clearvolume.renderer.ClearVolumeRendererInterface;


/**
 * ParameterChangeListener Interface
 *
 * @author Loic Royer (2015)
 *
 */
public interface ParameterChangeListener
{

	/**
	 * Called by the renderer when a
	 * 
	 * @param pClearVolumeRenderer
	 */
	void notifyParameterChange(ClearVolumeRendererInterface pClearVolumeRenderer);

}
