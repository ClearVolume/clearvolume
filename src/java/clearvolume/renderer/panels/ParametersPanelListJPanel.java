package clearvolume.renderer.panels;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class ParametersPanelListJPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	// private final JScrollPane mScrollPane;

	public ParametersPanelListJPanel()
	{
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		// setLayout(null);

		// mScrollPane = new JScrollPane();
		// add(mScrollPane);

	}

	public void addPanel(JPanel pPanel)
	{
		pPanel.setBorder(BorderFactory.createMatteBorder(	1,
															5,
															1,
															1,
															Color.black));
		pPanel.validate();
		add(pPanel);

	}

	public void removePanel(JPanel pPanel)
	{
		remove(pPanel);
	}

}
