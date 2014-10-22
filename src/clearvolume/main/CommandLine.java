package clearvolume.main;

import org.kohsuke.args4j.Option;

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

	@Option(name = "-r", aliases =
	{ "--resultsDir" }, metaVar = "DIR", usage = "directory to place the results file")
	public String resultsDir = "results/";

	@Option(name = "-s", aliases =
	{ "--seed" }, metaVar = "VALUE", usage = "seed to use for the program's random number generator")
	public int seed = (new Random()).nextInt(MAX_RANDOM_SEED + 1);
	/**/
	// Examples above! REAL STUFF BELOW:

	@Option(name = "-demoserver", aliases =
	{ "--demo-server" }, usage = "starts a demo ClearVolume network server")
	public boolean mDemoServer = false;

	public boolean isDemoServer()
	{
		return mDemoServer;
	}

}
