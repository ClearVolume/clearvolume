package clearvolume.network;

import static java.lang.Math.toIntExact;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.Map;

import clearvolume.network.keyvalue.KeyValueMaps;
import clearvolume.volume.Volume;

public class ClearVolumeSerialization
{
	// first 4 digits of the CRC32 of 'ClearVolume' string :-)
	public static final int cStandardTCPPort = 9140;
	private static final int cLongSizeInBytes = 8;

	public static final void serialize(	Volume pVolume,
																			ByteBuffer pByteBuffer)
	{
		StringBuilder lStringBuilder = new StringBuilder();
		writeVolumeHeader(pVolume, lStringBuilder);

		final int lHeaderLength = lStringBuilder.length();

		final long lDataLength = pVolume.getVolumeDataSizeInBytes();
		int lNeededBufferLength = toIntExact(2 * cLongSizeInBytes
																					+ lHeaderLength
																					+ lDataLength);
		if (pByteBuffer == null || pByteBuffer.capacity() != lNeededBufferLength)
		{
			pByteBuffer = ByteBuffer.allocateDirect(lNeededBufferLength);
			pByteBuffer.order(ByteOrder.nativeOrder());
		}
		pByteBuffer.clear();

		pByteBuffer.putLong(lNeededBufferLength);
		pByteBuffer.putLong(lHeaderLength);
		pByteBuffer.put(lStringBuilder.toString().getBytes());
		pByteBuffer.putLong(lDataLength);
		pVolume.writeToByteBuffer(pByteBuffer);

	};

	private static void writeVolumeHeader(Volume pVolume,
																				StringBuilder pStringBuilder)
	{
		LinkedHashMap<String, String> lHeaderMap = new LinkedHashMap<String, String>();
		lHeaderMap.put("index", "" + pVolume.getIndex());
		lHeaderMap.put("time", "" + pVolume.getTime());
		lHeaderMap.put("channel", "" + pVolume.getChannel());
		lHeaderMap.put("dim", "" + pVolume.getDimension());
		lHeaderMap.put("bytespervoxel", "" + pVolume.getBytesPerVoxels());
		lHeaderMap.put("widthvoxels", "" + pVolume.getWidthInVoxels());
		lHeaderMap.put("heightvoxels", "" + pVolume.getHeightInVoxels());
		lHeaderMap.put("depthvoxels", "" + pVolume.getDepthInVoxels());
		lHeaderMap.put("widthreal", "" + pVolume.getWidthInRealUnits());
		lHeaderMap.put("heightreal", "" + pVolume.getHeightInRealUnits());
		lHeaderMap.put("depthreal", "" + pVolume.getDepthInRealUnits());
		lHeaderMap.put("realunit", pVolume.getRealUnitName());

		KeyValueMaps.writeStringFromMap(lHeaderMap, pStringBuilder);
	}

	static void readVolumeHeader(	ByteBuffer pByteBuffer,
																int pHeaderLength,
																Volume pVolume)
	{

		Map<String, String> lHeaderMap = KeyValueMaps.readMapFromBuffer(pByteBuffer,
																																		pHeaderLength,
																																		null);
		final long lIndex = Long.parseLong(lHeaderMap.get("index"));
		final double lTime = Double.parseDouble(lHeaderMap.get("time"));
		final int lChannel = Integer.parseInt(lHeaderMap.get("channel"));
		final int lDim = Integer.parseInt(lHeaderMap.get("dim"));
		final long lBytesPerVoxel = Long.parseLong(lHeaderMap.get("bytespervoxel"));
		final long lWidthVoxels = Long.parseLong(lHeaderMap.get("widthvoxels"));
		final long lHeightVoxels = Long.parseLong(lHeaderMap.get("heightvoxels"));
		final long lDepthVoxels = Long.parseLong(lHeaderMap.get("depthvoxels"));

		final String lRealUnitName = lHeaderMap.get("realunit");
		final long lWidthReal = Long.parseLong(lHeaderMap.get("widthreal"));
		final long lHeightReal = Long.parseLong(lHeaderMap.get("heightreal"));
		final long lDepthReal = Long.parseLong(lHeaderMap.get("depthreal"));

		pVolume.setIndex(lIndex);
		pVolume.setTime(lTime);
		pVolume.setChannel(lChannel);
		pVolume.setDimension(lDim);
		pVolume.setVolumeDimensionsInVoxels(lBytesPerVoxel,
																				lWidthVoxels,
																				lHeightVoxels,
																				lDepthVoxels);

		pVolume.setVolumeDimensionsInRealUnits(	lRealUnitName,
																						lWidthReal,
																						lHeightReal,
																						lDepthReal);

	};

	public static final Volume deserialize(	SocketChannel pSocketChannel,
																					ByteBuffer pScratchBuffer,
																					Volume pVolume) throws IOException
	{

		if (pScratchBuffer == null || pScratchBuffer.capacity() == 0)
		{
			pScratchBuffer = ByteBuffer.allocateDirect(cLongSizeInBytes);
			pScratchBuffer.order(ByteOrder.nativeOrder());
		}

		pScratchBuffer.clear();
		pScratchBuffer.limit(1);
		pSocketChannel.read(pScratchBuffer);
		final int lNeededBufferLength = toIntExact(pScratchBuffer.getLong());

		if (pScratchBuffer == null || pScratchBuffer.capacity() != lNeededBufferLength)
		{
			pScratchBuffer = ByteBuffer.allocateDirect(lNeededBufferLength);
			pScratchBuffer.order(ByteOrder.nativeOrder());
		}

		pScratchBuffer.clear();
		pSocketChannel.read(pScratchBuffer);

		return deserialize(pScratchBuffer, pVolume);

	};

	public static final Volume deserialize(	ByteBuffer pByteBuffer,
																					Volume pVolume)
	{
		final int lHeaderLength = toIntExact(pByteBuffer.getLong());
		readVolumeHeader(pByteBuffer, lHeaderLength, pVolume);
		final long lDataLength = pByteBuffer.getLong();
		readVolumeData(pByteBuffer, lDataLength, pVolume);
		return pVolume;

	}

	static void readVolumeData(	ByteBuffer pByteBuffer,
															long pDataLength,
															Volume pVolume)
	{

		if (pVolume.getVolumeData().capacity() != pDataLength)
		{
			ByteBuffer lByteBuffer = ByteBuffer.allocateDirect(toIntExact(pDataLength));
			lByteBuffer.order(ByteOrder.nativeOrder());
			lByteBuffer.clear();
			pVolume.setVolumeData(lByteBuffer);
		}

		pVolume.readFromByteBuffer(pByteBuffer);
	}

}
