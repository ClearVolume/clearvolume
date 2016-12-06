package clearvolume.network.serialization.test;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.volume.Volume;
import coremem.enums.NativeTypeEnum;

public class ClearVolumeSerializationTests
{

	@Test
	public void test()
	{
		final Volume lVolume = new Volume(	NativeTypeEnum.UnsignedByte,
											1,
											128,
											128,
											128);
		lVolume.setTimeIndex(2);
		lVolume.setTimeInSeconds(3.3);
		lVolume.setChannelID(4);
		lVolume.setChannelName("channel 1");
		lVolume.setColor(1f, 0.5f, 0.3f, 0.2f);
		lVolume.setViewMatrix(new float[]
		{ 1 });
		lVolume.setVoxelSizeInRealUnits("um", 10, 11, 12);

		final ByteBuffer lSerializedVolumeData = lVolume.getDataBuffer();

		lSerializedVolumeData.rewind();
		lSerializedVolumeData.put((byte) 123);

		final ByteBuffer lBuffer = ClearVolumeSerialization.serialize(	lVolume,
																		null);

		final Volume lDeserializedVolume = new Volume();
		ClearVolumeSerialization.deserialize(	lBuffer,
												lDeserializedVolume);

		System.out.println(lDeserializedVolume);

		assertEquals(2, lDeserializedVolume.getTimeIndex());
		assertEquals(3.3, lDeserializedVolume.getTimeInSeconds(), 0);
		assertEquals(4, lDeserializedVolume.getChannelID());
		assertEquals(	"channel 1",
						lDeserializedVolume.getChannelName());
		assertEquals(1, lDeserializedVolume.getColor()[0], 0.001);
		assertEquals(0.5, lDeserializedVolume.getColor()[1], 0.001);
		assertEquals(0.3, lDeserializedVolume.getColor()[2], 0.001);
		assertEquals(0.2, lDeserializedVolume.getColor()[3], 0.001);
		assertEquals(1, lDeserializedVolume.getViewMatrix()[0], 0.001);
		assertEquals("um", lDeserializedVolume.getRealUnitName());
		assertEquals(	10,
						lDeserializedVolume.getVoxelWidthInRealUnits(),
						0);
		assertEquals(	11,
						lDeserializedVolume.getVoxelHeightInRealUnits(),
						0);
		assertEquals(	12,
						lDeserializedVolume.getVoxelDepthInRealUnits(),
						0);

		final ByteBuffer lDeserializedVolumeData = lDeserializedVolume.getDataBuffer();
		lDeserializedVolumeData.rewind();
		final byte lByte = lDeserializedVolumeData.get();

		assertEquals(lByte, 123);

	}

}
