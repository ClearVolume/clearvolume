package clearvolume.utils;

import java.net.URL;

import javax.swing.ImageIcon;

public class SwingUtilities
{
	public static final ImageIcon loadIcon(	Class pRootClass,
											String strPath)
	{
		URL imgURL = pRootClass.getResource(strPath);
		if (imgURL != null)
			return new ImageIcon(imgURL);
		else
			return null;
	}
}
