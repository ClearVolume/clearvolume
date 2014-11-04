package clearvolume.main;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import clearvolume.network.client.main.ClearVolumeClientMain;
import clearvolume.network.server.main.ClearVolumeDemoServerMain;

public class ClearVolumeMain
{

	/**
	 * Launch the application.
	 */
	public static void main(String[] args)
	{
		try
		{
			CommandLine lCommandLineValues = new CommandLine();
			CmdLineParser lCmdLineParser = new CmdLineParser(lCommandLineValues);
			lCmdLineParser.getProperties().withUsageWidth(80);

			System.err.println("[ClearVolume command line utility]");
			try
			{
				lCmdLineParser.parseArgument(args);
			}
			catch (CmdLineException e)
			{
				System.err.println(e.getMessage());
				lCmdLineParser.printUsage(System.err);
				System.err.println();
				System.exit(1);
			}

			if (lCommandLineValues.isDemoServer())
				ClearVolumeDemoServerMain.main(args);
			else if (lCommandLineValues.isClient())
				ClearVolumeClientMain.connect(lCommandLineValues.mConnectHostName,
																			lCommandLineValues.mConnectPort,
																			lCommandLineValues.mWindowSize,
																			lCommandLineValues.mBytesPerVoxel);
			else
				ClearVolumeClientMain.main(args);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}

	}
}
