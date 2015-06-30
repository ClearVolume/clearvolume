package clearvolume.renderer.panels;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class ParametersListPanelJFrame extends JFrame
{

	private static final long serialVersionUID = 1L;

	private final ParametersPanelListJPanel mParametersPanelListJPanel;

	/**
	 * Constructs a ParametersListPanelJFrame
	 * 
	 */
	public ParametersListPanelJFrame()
	{
		super();
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		mParametersPanelListJPanel = new ParametersPanelListJPanel();
		setContentPane(mParametersPanelListJPanel);
		mParametersPanelListJPanel.validate();
	}

	public void addPanel(JPanel pPanel)
	{
		mParametersPanelListJPanel.addPanel(pPanel);
		pack();
	}

	public void removePanel(JPanel pPanel)
	{
		mParametersPanelListJPanel.removePanel(pPanel);
		pack();
	}

}
