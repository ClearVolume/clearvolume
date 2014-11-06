package clearvolume.network.serialization.test;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.volume.Volume;

public class ClearVolumeSerializationTests
{

	@Test
	public void test()
	{
		Volume<Byte> lVolume = new Volume<Byte>(Byte.class,
																						1,
																						128,
																						128,
																						128);
		lVolume.setTimeIndex(2);
		lVolume.setTimeInSeconds(3.3);
		lVolume.setChannelID(4);
		lVolume.setDimensionsInRealUnits("um", 10, 11, 12);
		
		ByteBuffer lSerializedVolumeData = lVolume.getDataBuffer();
		
		lSerializedVolumeData.rewind();
		lSerializedVolumeData.put((byte) 123);

		ByteBuffer lBuffer = ClearVolumeSerialization.serialize(	lVolume,
																																null);
		
		Volume<Byte> lDeserializedVolume = new Volume<Byte>();
		ClearVolumeSerialization.deserialize(lBuffer, lDeserializedVolume);
		
		assertEquals(2, lDeserializedVolume.getTimeIndex());
		assertEquals(3.3, lDeserializedVolume.getTimeInSeconds(), 0);
		assertEquals(4, lDeserializedVolume.getChannelID());
		assertEquals("um", lDeserializedVolume.getRealUnitName());
		assertEquals(10, lDeserializedVolume.getWidthInRealUnits(), 0);
		assertEquals(11, lDeserializedVolume.getHeightInRealUnits(), 0);
		assertEquals(12, lDeserializedVolume.getDepthInRealUnits(), 0);

		ByteBuffer lDeserializedVolumeData = lDeserializedVolume.getDataBuffer();
		lDeserializedVolumeData.rewind();
		byte lByte = lDeserializedVolumeData.get();

		assertEquals(lByte, 123);

		System.out.println(lDeserializedVolume);

	}

}
