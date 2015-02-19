package clearvolume.main;

import org.kohsuke.args4j.Option;

import clearvolume.network.serialization.ClearVolumeSerialization;

public class CommandLine
{

	/*
	// Max value for the random seed (minimum is 0). 
	public static final int MAX_RANDOM_SEED = 1000;

	// Valid options for the experiment type 
	public static enum ExptOption
	{
		DEGREES, FEATURES
	};

	@Argument(required = true, index = 0, usage = "type of experiment")
	public ExptOption exptOption;

	@Argument(required = true, index = 1, metaVar = "FILE", usage = "the config file for the experiment")
	public String configFileName;

	@Option(name = "-mFauxscopeRandomizer", aliases =
	{ "--resultsDir" }, metaVar = "DIR", usage = "directory to place the results file")
	public String resultsDir = "results/";

	@Option(name = "-s", aliases =
	{ "--seed" }, metaVar = "VALUE", usage = "seed to use for the program's random number generator")
	public int seed = (new Random()).nextInt(MAX_RANDOM_SEED + 1);
	/**/
	// Examples above! REAL STUFF BELOW:

	@Option(name = "-s", aliases =
	{ "--demo-server" }, usage = "starts a demo ClearVolume network server.")
	public boolean mDemoServer = false;

	@Option(name = "-c", aliases =
	{ "--connect" }, usage = "ClearVolume server address to connect to.")
	public String mConnectHostName = null;

	@Option(name = "-p", aliases =
	{ "--port" }, usage = "server port nuber to connect to.")
	public int mConnectPort = ClearVolumeSerialization.cStandardTCPPort;

	@Option(name = "-w", aliases =
	{ "--window-size" }, usage = "renderer window size.")
	public int mWindowSize = 512;

	@Option(name = "-b", aliases =
	{ "--bytes-per-voxel" }, usage = "bytes per voxel.")
	public int mBytesPerVoxel = 1;

	@Option(name = "-t", aliases =
	{ "--tsmc" }, usage = "Time-shift & Multi-Channel mode.")
	public boolean mTimeShiftAndMultiChannel;

	@Option(name = "-mc", aliases =
	{ "--multi-color" }, usage = "Multi-Color rendering.")
	public boolean mMultiColor;

	@Option(name = "-l", aliases =
	{ "--nb-colors" }, usage = "Number of colors (layers) to render simultaneously.")
	public int mNumberOfLayers;

	public boolean isDemoServer()
	{
		return mDemoServer;
	}

	public boolean isClient()
	{
		return mConnectHostName != null;
	}

}
