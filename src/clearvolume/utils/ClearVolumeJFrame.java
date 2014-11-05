package clearvolume.utils;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class ClearVolumeJFrame extends JFrame
{

	private static final String cClearVolumeWindowIconRessourcePath = "/clearvolume/icon/ClearVolumeIcon32.png";
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
		/*ImageIcon lImageIcon = SwingUtilities.loadIcon(	getClass(),
																										cClearVolumeWindowIconRessourcePath);
		if (lImageIcon == null)
		{
			System.err.println("Cannot find ClearVolume window icon at" + cClearVolumeWindowIconRessourcePath);
			return;
		}
		setIconImage(lImageIcon.getImage());/**/

		try
		{
			InputStream lResourceAsStream = this.getClass()
																					.getResourceAsStream(cClearVolumeWindowIconRessourcePath);
			setIconImage(ImageIO.read(lResourceAsStream));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

}
