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
		this(true);
	}

	public ClearVolumeJFrame(final boolean pSetClearVolumeIcon)
	{
		super();
		if (pSetClearVolumeIcon)
		{
			setClearVolumeIcon();
		}
	}

	public ClearVolumeJFrame(final GraphicsConfiguration pGc)
	{
		super(pGc);
		setClearVolumeIcon();
	}

	public ClearVolumeJFrame(	final String pTitle,
								final GraphicsConfiguration pGc)
	{
		super(pTitle, pGc);
		setClearVolumeIcon();
	}

	public ClearVolumeJFrame(final String pTitle) throws HeadlessException
	{
		super(pTitle);
		setClearVolumeIcon();
	}

	private void setClearVolumeIcon()
	{
		try
		{
			final URL lImageURL = getClass().getResource(cClearVolumeWindowIconRessourcePath);
			final ImageIcon lImageIcon = new ImageIcon(lImageURL);

			if (AppleMac.isMac())
			{
				AppleMac.setApplicationIcon(lImageIcon.getImage());
				AppleMac.setApplicationName("ClearVolume");
			}

			this.setIconImage(lImageIcon.getImage());
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}

	}

}
