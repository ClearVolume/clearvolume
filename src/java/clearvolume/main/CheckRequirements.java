package clearvolume.main;

import clearvolume.utils.UnsupportedArchitectureException;
import org.apache.commons.lang.SystemUtils;

import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.swing.*;

public class CheckRequirements
{

	public static void check() throws UnsupportedArchitectureException
	{
		// check for correct system architecture and version
		if (!is64bit())
		{
			System.err.println("Sorry, but due to the large data handled, ClearVolume only supports 64bit architectures.");
			JOptionPane.showMessageDialog(null,
																		"Sorry, but due to the large data handled, ClearVolume only supports 64bit architectures.",
																		"Unsupported architecture",
																		JOptionPane.ERROR_MESSAGE);
			throw new UnsupportedArchitectureException("ClearVolume only supports 64bit architectures.");
		}

		if (SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_MAC)
		{
			if (Integer.parseInt(System.getProperty("os.version")
																	.split("\\.")[1]) < 9)
			{
				System.err.println("ClearVolume required Mac OS X 10.9 or higher. If possible, please upgrade.");
				JOptionPane.showMessageDialog(null,
																			"ClearVolume required Mac OS X 10.9 or higher. If possible, please upgrade.",
																			"Unsupported OS X version",
																			JOptionPane.ERROR_MESSAGE);
				throw new UnsupportedArchitectureException("ClearVolume only supports OS X 10.9 or higher.");
			}
		}

    try {
      GLProfile.get(GLProfile.GL4);
    } catch (GLException e) {
      System.err.println("Sorry, but for graphics rendering, ClearVolume requires your graphics device to support OpenGL 4.0 or higher.");
      System.err.println("On your device, only the following OpenGL versions are available:\n" + GLProfile.glAvailabilityToString());
      JOptionPane.showMessageDialog(null,
              "Sorry, but for graphics rendering, ClearVolume requires your graphics device to support OpenGL 4.0 or higher.\n\nOn your device, only the following OpenGL versions are available:\n" + GLProfile.glAvailabilityToString(),
              "OpenGL 4.0+ not supported",
              JOptionPane.ERROR_MESSAGE);
    }
  }


	public static boolean is64bit()
	{
		String lArchitectureDataModel = System.getProperty("sun.arch.data.model");
		/*System.out.format("Architecture detected: %s bit \n",
											lArchitectureDataModel);/**/
		final boolean is64bit = lArchitectureDataModel.contains("64");
		return is64bit;
	}

}
