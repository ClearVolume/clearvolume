package clearvolume.renderer.panels;

import javax.swing.JPanel;

/**
 * Interface HasGUIPanel
 * 
 * Processors that implement this interface provide a Swing Panel for
 * configuring parameters.
 *
 * @author Loic Royer 2014
 *
 */
public interface HasGUIPanel
{

	/**
	 * Returns a Panel for this processor. Null if no panel is available
	 * 
	 * @return GUI panel
	 */
	JPanel getPanel();

}
