package clearvolume.network.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.Test;

import clearvolume.network.ClearVolumeSerialization;
import clearvolume.network.client.ClearVolumeTCPClient;
import clearvolume.network.server.ClearVolumeTCPServer;
import clearvolume.volume.Volume;
import clearvolume.volume.sink.VolumeSinkInterface;

public class ClearVolumeNetworkTests
{

	@Test
	public void test() throws IOException
	{
		ClearVolumeTCPServer lClearVolumeTCPServer = new ClearVolumeTCPServer(10);

		SocketAddress lServerSocketAddress = new InetSocketAddress(ClearVolumeSerialization.cStandardTCPPort);
		lClearVolumeTCPServer.open(lServerSocketAddress);


		VolumeSinkInterface lVolumeSink = new VolumeSinkInterface()
		{

			@Override
			public void sendVolume(Volume pVolume)
			{
				System.out.println("Received volume:" + pVolume);

			}
		};
		ClearVolumeTCPClient lClearVolumeTCPClient = new ClearVolumeTCPClient(lVolumeSink);

		SocketAddress lClientSocketAddress = new InetSocketAddress(	"localhost",
																																ClearVolumeSerialization.cStandardTCPPort);
		lClearVolumeTCPClient.open(lClientSocketAddress);

		lClearVolumeTCPClient.start();

		Volume lVolume = new Volume(128, 127, 126);
		lClearVolumeTCPServer.sendVolume(lVolume);

		lClearVolumeTCPClient.stop();

		lClearVolumeTCPClient.close();

		lClearVolumeTCPServer.close();

	}
}
