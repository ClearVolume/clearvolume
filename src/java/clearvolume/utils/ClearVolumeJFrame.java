package clearvolume.utils;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class ClearVolumeJFrame extends JFrame
{

	private static final String cClearVolumeWindowIconRessourcePath = "/clearvolume/icon/ClearVolumeIcon256.png";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ClearVolumeJFrame()
	{
		super();
		setClearVolumeIcon();
	}

	public ClearVolumeJFrame(GraphicsConfiguration pGc)
	{
		super(pGc);
		setClearVolumeIcon();
	}

	public ClearVolumeJFrame(String pTitle, GraphicsConfiguration pGc)
	{
		super(pTitle, pGc);
		setClearVolumeIcon();
	}

	public ClearVolumeJFrame(String pTitle) throws HeadlessException
	{
		super(pTitle);
		setClearVolumeIcon();
	}

	private void setClearVolumeIcon()
	{
		try
		{
			URL lImageURL = getClass().getResource(cClearVolumeWindowIconRessourcePath);
			ImageIcon lImageIcon = new ImageIcon(lImageURL);

			if(AppleMac.isMac())
			{
				AppleMac.setApplicationIcon(lImageIcon.getImage());
				AppleMac.setApplicationName("ClearVolume");
			}
			
			this.setIconImage(lImageIcon.getImage());
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}

	}

}
