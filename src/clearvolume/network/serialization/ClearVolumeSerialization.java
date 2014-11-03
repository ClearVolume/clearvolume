package clearvolume.network.serialization;

import static java.lang.Math.toIntExact;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.Map;

import clearvolume.network.serialization.keyvalue.KeyValueMaps;
import clearvolume.volume.Volume;

public class ClearVolumeSerialization
{
	// first 4 digits of the CRC32 of 'ClearVolume' string :-)
	public static final int cStandardTCPPort = 9140;
	private static final int cLongSizeInBytes = Long.BYTES;

	public static final ByteBuffer serialize(	Volume<?> pVolume,
																						ByteBuffer pByteBuffer)
	{
		StringBuilder lStringBuilder = new StringBuilder();
		writeVolumeHeader(pVolume, lStringBuilder);

		final int lHeaderLength = lStringBuilder.length();

		final long lDataLength = pVolume.getVolumeDataSizeInBytes();
		int lNeededBufferLength = toIntExact(3 * cLongSizeInBytes
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

		return pByteBuffer;
	};

	private static void writeVolumeHeader(Volume<?> pVolume,
																				StringBuilder pStringBuilder)
	{
		LinkedHashMap<String, String> lHeaderMap = new LinkedHashMap<String, String>();
		lHeaderMap.put("index", "" + pVolume.getIndex());
		lHeaderMap.put("time", "" + pVolume.getTime());
		lHeaderMap.put("channel", "" + pVolume.getVolumeChannelID());
		lHeaderMap.put("dim", "" + pVolume.getDimension());
		lHeaderMap.put("type", "" + pVolume.getTypeName());
		lHeaderMap.put("bytespervoxel",
										"" + pVolume.getBytesPerVoxel());
		lHeaderMap.put("elementsize", "" + pVolume.getElementSize());
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
																Volume<?> pVolume)
	{

		Map<String, String> lHeaderMap = KeyValueMaps.readMapFromBuffer(pByteBuffer,
																																		pHeaderLength,
																																		null);
		final long lIndex = Long.parseLong(lHeaderMap.get("index"));
		final double lTime = Double.parseDouble(lHeaderMap.get("time"));
		final int lVolumeChannelID = Integer.parseInt(lHeaderMap.get("channel"));
		final int lDim = Integer.parseInt(lHeaderMap.get("dim"));
		final String lType = lHeaderMap.get("type");
		final long lElementSize = Long.parseLong(lHeaderMap.get("elementsize"));
		final long lWidthVoxels = Long.parseLong(lHeaderMap.get("widthvoxels"));
		final long lHeightVoxels = Long.parseLong(lHeaderMap.get("heightvoxels"));
		final long lDepthVoxels = Long.parseLong(lHeaderMap.get("depthvoxels"));

		final String lRealUnitName = lHeaderMap.get("realunit");
		final double lWidthReal = Double.parseDouble(lHeaderMap.get("widthreal"));
		final double lHeightReal = Double.parseDouble(lHeaderMap.get("heightreal"));
		final double lDepthReal = Double.parseDouble(lHeaderMap.get("depthreal"));

		pVolume.setIndex(lIndex);
		pVolume.setTime(lTime);
		pVolume.setType(lType);
		pVolume.setVolumeChannelID(lVolumeChannelID);
		pVolume.setDimension(lDim);
		pVolume.setVolumeDimensionsInVoxels(lElementSize,
																				lWidthVoxels,
																				lHeightVoxels,
																				lDepthVoxels);

		pVolume.setVolumeDimensionsInRealUnits(	lRealUnitName,
																						lWidthReal,
																						lHeightReal,
																						lDepthReal);

	};

	private static ThreadLocal<ByteBuffer> sScratchBufferThreadLocal = new ThreadLocal<ByteBuffer>();

	public static final <T> Volume<T> deserialize(SocketChannel pSocketChannel,
																								Volume<T> pVolume) throws IOException
	{
		if (pVolume == null)
		{
			pVolume = new Volume<T>();
		}

		ByteBuffer pScratchBuffer = sScratchBufferThreadLocal.get();
		if (pScratchBuffer == null || pScratchBuffer.capacity() == 0)
		{
			pScratchBuffer = ByteBuffer.allocateDirect(cLongSizeInBytes);
			pScratchBuffer.order(ByteOrder.nativeOrder());
		}

		readPartLength(pSocketChannel, pScratchBuffer);

		final int lHeaderLength = readPartLength(	pSocketChannel,
																							pScratchBuffer);

		pScratchBuffer = ensureScratchBufferLengthIsEnough(	pScratchBuffer,
																												lHeaderLength);

		readIntoScratchBuffer(pSocketChannel,
													pScratchBuffer,
													lHeaderLength);
		readVolumeHeader(pScratchBuffer, lHeaderLength, pVolume);

		final int lDataLength = readPartLength(	pSocketChannel,
																						pScratchBuffer);

		if (pScratchBuffer.capacity() < lDataLength)
		{
			pScratchBuffer = ByteBuffer.allocateDirect(lDataLength);
			pScratchBuffer.order(ByteOrder.nativeOrder());
		}

		readIntoScratchBuffer(pSocketChannel, pScratchBuffer, lDataLength);
		readVolumeData(pScratchBuffer, lDataLength, pVolume);

		sScratchBufferThreadLocal.set(pScratchBuffer);

		return pVolume;
	}

	private static void readIntoScratchBuffer(SocketChannel pSocketChannel,
																						ByteBuffer pScratchBuffer,
																						final int lHeaderLength) throws IOException
	{
		pScratchBuffer.clear();
		pScratchBuffer.limit(lHeaderLength);
		while (pScratchBuffer.hasRemaining())
		{
			pSocketChannel.read(pScratchBuffer);
			sleep();
		}
		pScratchBuffer.rewind();
	}


	private static ByteBuffer ensureScratchBufferLengthIsEnough(ByteBuffer pScratchBuffer,
																															final int lHeaderLength)
	{
		if (pScratchBuffer == null || pScratchBuffer.capacity() < lHeaderLength)
		{
			pScratchBuffer = ByteBuffer.allocateDirect(lHeaderLength);
			pScratchBuffer.order(ByteOrder.nativeOrder());
		}
		return pScratchBuffer;
	}

	private static int readPartLength(SocketChannel pSocketChannel,
																		ByteBuffer pScratchBuffer) throws IOException
	{
		pScratchBuffer.clear();
		pScratchBuffer.limit(Long.BYTES);
		while (pScratchBuffer.hasRemaining())
		{
			pSocketChannel.read(pScratchBuffer);
			sleep();
		}
		pScratchBuffer.rewind();
		final int lHeaderLength = toIntExact(pScratchBuffer.getLong());
		return lHeaderLength;
	};

	public static final <T> Volume<T> deserialize(ByteBuffer pByteBuffer,
																								Volume<T> pVolume)
	{
		pByteBuffer.rewind();
		final int lWholeLength = toIntExact(pByteBuffer.getLong());
		final int lHeaderLength = toIntExact(pByteBuffer.getLong());
		readVolumeHeader(pByteBuffer, lHeaderLength, pVolume);
		final long lDataLength = pByteBuffer.getLong();
		readVolumeData(pByteBuffer, lDataLength, pVolume);
		return pVolume;
	}

	static void readVolumeData(	ByteBuffer pByteBuffer,
															long pDataLength,
															Volume<?> pVolume)
	{

		if (pVolume.getVolumeData() == null || pVolume.getVolumeData()
																									.capacity() != pDataLength)
		{
			ByteBuffer lByteBuffer = ByteBuffer.allocateDirect(toIntExact(pDataLength));
			lByteBuffer.order(ByteOrder.nativeOrder());
			lByteBuffer.clear();
			pVolume.setVolumeData(lByteBuffer);
		}

		pVolume.readFromByteBuffer(pByteBuffer);
	}

	private static void sleep()
	{
		try
		{
			Thread.sleep(1);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

}
