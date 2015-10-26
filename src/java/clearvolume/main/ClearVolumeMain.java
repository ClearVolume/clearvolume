package clearvolume.main;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import clearvolume.network.client.main.ClearVolumeClientMain;
import clearvolume.network.server.main.ClearVolumeDemoServerMain;
import clearvolume.renderer.cleargl.ClearVolumeIcon;

public class ClearVolumeMain
{

	/**
	 * Launch the application.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args)
	{
		try
		{
			CheckRequirements.check();

			final CommandLine lCommandLineValues = new CommandLine();
			final CmdLineParser lCmdLineParser = new CmdLineParser(lCommandLineValues);
			lCmdLineParser.getProperties().withUsageWidth(80);

			System.err.println("[ClearVolume command line utility]");
			try
			{
				lCmdLineParser.parseArgument(args);
			}
			catch (final CmdLineException e)
			{
				System.err.println(e.getMessage());
				lCmdLineParser.printUsage(System.err);
				System.err.println();
				System.exit(1);
			}

			ClearVolumeIcon.setIcon();

			if (lCommandLineValues.isDemoServer())
				ClearVolumeDemoServerMain.main(args);
			else if (lCommandLineValues.isClient())
				ClearVolumeClientMain.connect(	lCommandLineValues.mConnectHostName,
												lCommandLineValues.mConnectPort,
												lCommandLineValues.mWindowSize,
												lCommandLineValues.mBytesPerVoxel,
												lCommandLineValues.mNumberOfLayers,
												lCommandLineValues.mTimeShiftAndMultiChannel,
												lCommandLineValues.mMultiColor);
			else
				ClearVolumeClientMain.main(args);
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}

	}
}
