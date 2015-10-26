package clearvolume.network.server.main;

import clearvolume.network.demo.ClearVolumeNetworkDemo;

public class ClearVolumeDemoServerMain
{

	/**
	 * Launch the application.
	 *
	 * @param args
	 *            command line parameters
	 */
	public static void main(String[] args)
	{
		final ClearVolumeNetworkDemo lClearVolumeNetworkDemo = new ClearVolumeNetworkDemo();
		lClearVolumeNetworkDemo.startServerOneChannel();
	}

}
