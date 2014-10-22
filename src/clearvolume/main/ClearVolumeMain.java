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
		// parse the command line arguments and options
		CommandLine lCommandLineValues = new CommandLine();
		CmdLineParser lCmdLineParser = new CmdLineParser(lCommandLineValues);
		lCmdLineParser.getProperties().withUsageWidth(80);

		try
		{
			lCmdLineParser.parseArgument(args);
		}
		catch (CmdLineException e)
		{
			System.err.println(e.getMessage());
			System.err.println("java DotsMain [options...] arguments...");
			// print the list of available options
			lCmdLineParser.printUsage(System.err);
			System.err.println();
			System.exit(1);
		}

		if (lCommandLineValues.isDemoServer())
			ClearVolumeDemoServerMain.main(args);
		else
			ClearVolumeClientMain.main(args);


	}

}
