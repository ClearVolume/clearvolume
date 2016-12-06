package clearvolume.interfaces.tests;

import java.nio.ByteBuffer;

import org.junit.Test;


import clearvolume.interfaces.ClearVolumeC;
import clearvolume.renderer.opencl.OpenCLAvailability;

public class ClearVolumeCTests
{

	@Test
	public void test8bitStreaming() throws InterruptedException
	{
		if (!OpenCLAvailability.isOpenCLAvailable())
			return;

		final int lRendererId = 1;
		final int lChannelId = 1;
		ClearVolumeC.createRenderer(lRendererId,
									256,
									256,
									1,
									512,
									512);

		ClearVolumeC.setChannelColor(lChannelId, new float[]
		{ 1, 1, 1, 1 });
		ClearVolumeC.setChannelName(lChannelId, "channel 1");

		final int lResolutionX = 128;
		final int lResolutionY = lResolutionX + 1;
		final int lResolutionZ = lResolutionX + 3;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
													* lResolutionZ];

		for (int i = 0; i < 512; i++)
		{
			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = x + lResolutionX
											* y
											+ lResolutionX
											* lResolutionY
											* z;
						int lValue = (((byte) i ^ (byte) x ^ (byte) y ^ (byte) z));
						if (lValue < 12)
							lValue = 0;
						lVolumeDataArray[lIndex] = (byte) lValue;
					}

			ClearVolumeC.setVolumeIndexAndTime(	lRendererId,
												i,
												0.1 * i);
			ClearVolumeC.send8bitUINTVolumeDataToSink(	lRendererId,
														lChannelId,
														ByteBuffer.wrap(lVolumeDataArray),
														lResolutionX,
														lResolutionY,
														lResolutionZ);
		}

		ClearVolumeC.destroyRenderer(lRendererId);

	}

	@Test
	public void test16bitStreaming() throws InterruptedException
	{
		if ( !OpenCLAvailability.isOpenCLAvailable())
			return;

		final int lRendererId = 1;
		final int lChannelId = 1;
		ClearVolumeC.createRenderer(lRendererId,
									256,
									256,
									2,
									512,
									512);

		ClearVolumeC.setChannelColor(lChannelId, new float[]
		{ 1, 1, 1, 1 });
		ClearVolumeC.setChannelName(lChannelId, "channel 1");

		final int lResolutionX = 128;
		final int lResolutionY = lResolutionX + 1;
		final int lResolutionZ = lResolutionX + 3;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
													* lResolutionZ
													* 2];

		for (int i = 0; i < 512; i++)
		{
			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = x + lResolutionX
											* y
											+ lResolutionX
											* lResolutionY
											* z;
						int lCharValue = (((char) i ^ (char) x
											^ (char) y ^ (char) z));
						if (lCharValue < 12)
							lCharValue = 0;
						lVolumeDataArray[2 * lIndex] = (byte) (lCharValue >> 8);
						lVolumeDataArray[2 * lIndex + 1] = (byte) lCharValue;
					}

			ClearVolumeC.setVolumeIndexAndTime(	lRendererId,
												i,
												0.1 * i);
			ClearVolumeC.send16bitUINTVolumeDataToSink(	lRendererId,
														lChannelId,
														ByteBuffer.wrap(lVolumeDataArray),
														lResolutionX,
														lResolutionY,
														lResolutionZ);
		}

		ClearVolumeC.destroyRenderer(lRendererId);

	}

	@Test
	public void testCreateServer16bit() throws InterruptedException
	{
		if (!OpenCLAvailability.isOpenCLAvailable())
			return;

		final int lServerId = 1;
		final int lChannelId = 1;
		ClearVolumeC.createServer(lServerId);

		ClearVolumeC.setChannelColor(lChannelId, new float[]
		{ 1, 1, 1, 1 });
		ClearVolumeC.setChannelName(lChannelId, "channel 1");

		final int lResolutionX = 128;
		final int lResolutionY = lResolutionX + 1;
		final int lResolutionZ = lResolutionX + 3;

		final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
													* lResolutionZ
													* 2];

		for (int i = 0; i < 32; i++)
		{
			System.out.println("i=" + i);
			for (int z = 0; z < lResolutionZ; z++)
				for (int y = 0; y < lResolutionY; y++)
					for (int x = 0; x < lResolutionX; x++)
					{
						final int lIndex = x + lResolutionX
											* y
											+ lResolutionX
											* lResolutionY
											* z;
						int lCharValue = (((char) i ^ (char) x
											^ (char) y ^ (char) z));
						if (lCharValue < 12)
							lCharValue = 0;
						lVolumeDataArray[2 * lIndex] = (byte) (lCharValue >> 8);
						lVolumeDataArray[2 * lIndex + 1] = (byte) lCharValue;
					}

			ClearVolumeC.setVolumeIndexAndTime(lServerId, i, 0.1 * i);
			ClearVolumeC.send16bitUINTVolumeDataToSink(	lServerId,
														lChannelId,
														ByteBuffer.wrap(lVolumeDataArray),
														lResolutionX,
														lResolutionY,
														lResolutionZ);
		}

		ClearVolumeC.destroyServer(lServerId);

	}
}
