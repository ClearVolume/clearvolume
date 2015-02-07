package clearvolume.renderer;

import javax.swing.JFrame;

public class ControlPanelJFrame extends JFrame
{

	private static final long serialVersionUID = 1L;

	private ControlJPanel mControlJPanel;

	/**
	 * Create the frame.
	 */
	public ControlPanelJFrame()
	{
		super();
		setSize(448, 149);
		setResizable(false);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		mControlJPanel = new ControlJPanel();
		setContentPane(mControlJPanel);

	}

	public ClearVolumeRendererInterface getClearVolumeRendererInterface()
	{
		return mControlJPanel.getClearVolumeRendererInterface();
	}

	public void setClearVolumeRendererInterface(ClearVolumeRendererInterface pClearVolumeRendererInterface)
	{
		mControlJPanel.setClearVolumeRendererInterface(pClearVolumeRendererInterface);
	}

}
