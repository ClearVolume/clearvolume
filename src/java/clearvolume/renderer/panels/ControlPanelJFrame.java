package clearvolume.renderer.panels;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import clearvolume.renderer.ClearVolumeRendererInterface;

public class ControlPanelJFrame extends JFrame
{

	private static final long serialVersionUID = 1L;

	private final ControlJPanel mControlJPanel;

	/**
	 * Constructs a ControlPanel
	 * 
	 * @param pRenderLayerToBeControlled
	 *            render layer to be controlled
	 * @param pClearVolumeRendererInterface
	 *            render interface
	 */
	public ControlPanelJFrame(	final int pRenderLayerToBeControlled,
								final ClearVolumeRendererInterface pClearVolumeRendererInterface)
	{
		super();
		setSize(448, 149);
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		mControlJPanel = new ControlJPanel(	pRenderLayerToBeControlled,
											pClearVolumeRendererInterface);
		setContentPane(mControlJPanel);

	}

	public ClearVolumeRendererInterface getClearVolumeRendererInterface()
	{
		return mControlJPanel.getClearVolumeRendererInterface();
	}

	public void setClearVolumeRendererInterface(final ClearVolumeRendererInterface pClearVolumeRendererInterface)
	{
		mControlJPanel.setClearVolumeRendererInterface(pClearVolumeRendererInterface);
	}

}
