package clearvolume.network.serialization;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.Map;

import clearvolume.network.serialization.keyvalue.KeyValueMaps;
import clearvolume.utils.ToIntExact;
import clearvolume.volume.Volume;

public class ClearVolumeSerialization
{
	// first 4 digits of the CRC32 of 'ClearVolume' string :-)
	public static final int cStandardTCPPort = 9140;
	private static final int cLongSizeInBytes = 8;

	public static final ByteBuffer serialize(	Volume pVolume,
																						ByteBuffer pByteBuffer)
	{
		final StringBuilder lStringBuilder = new StringBuilder();
		writeVolumeHeader(pVolume, lStringBuilder);

		final int lHeaderLength = lStringBuilder.length();

		final long lDataLength = pVolume.getDataSizeInBytes();
		final int lNeededBufferLength = ToIntExact.toIntExact(3 * cLongSizeInBytes
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

	private static void writeVolumeHeader(Volume pVolume,
																				StringBuilder pStringBuilder)
	{

		final LinkedHashMap<String, String> lHeaderMap = new LinkedHashMap<String, String>();
		lHeaderMap.put("index", "" + pVolume.getTimeIndex());
		lHeaderMap.put("time", "" + pVolume.getTimeInSeconds());
		lHeaderMap.put("channel", "" + pVolume.getChannelID());
		lHeaderMap.put("channelname", pVolume.getChannelName());
		lHeaderMap.put("color", serializeFloatArray(pVolume.getColor()));
		lHeaderMap.put(	"viewmatrix",
										serializeFloatArray(pVolume.getViewMatrix()));
		lHeaderMap.put("dim", "" + pVolume.getDimension());
		lHeaderMap.put("type", "" + pVolume.getTypeName());
		lHeaderMap.put("bytespervoxel", "" + pVolume.getBytesPerVoxel());
		lHeaderMap.put("elementsize", "" + pVolume.getElementSize());
		lHeaderMap.put("width", "" + pVolume.getWidthInVoxels());
		lHeaderMap.put("height", "" + pVolume.getHeightInVoxels());
		lHeaderMap.put("depth", "" + pVolume.getDepthInVoxels());
		lHeaderMap.put(	"voxelwidth",
										"" + pVolume.getVoxelWidthInRealUnits());
		lHeaderMap.put(	"voxelheight",
										"" + pVolume.getVoxelHeightInRealUnits());
		lHeaderMap.put(	"voxeldepth",
										"" + pVolume.getVoxelDepthInRealUnits());
		lHeaderMap.put("realunit", pVolume.getRealUnitName());

		KeyValueMaps.writeStringFromMap(lHeaderMap, pStringBuilder);
	}

	static void readVolumeHeader(	ByteBuffer pByteBuffer,
																int pHeaderLength,
																Volume pVolume)
	{

		final Map<String, String> lHeaderMap = KeyValueMaps.readMapFromBuffer(pByteBuffer,
																																		pHeaderLength,
																																		null);
		final long lIndex = parseLong(lHeaderMap.get("index"), 0);
		final double lTime = parseDouble(lHeaderMap.get("time"), 0);
		final int lVolumeChannelID = parseInt(lHeaderMap.get("channel"),
																					0);
		final String lVolumeChannelName = parseString(lHeaderMap.get("channelname"),
																									"noname");

		final float[] lColor = parseFloatArray(	lHeaderMap.get("color"),
																						new float[]
																						{ 1.f, 1.f, 1.f, 1.f });

		final float[] lViewMatrix = parseFloatArray(lHeaderMap.get("viewmatrix"),
																								new float[]
																								{ 1.f,
																									0.f,
																									0.f,
																									0.f,
																									0.f,
																									1.f,
																									0.f,
																									0.f,
																									0.f,
																									0.f,
																									1.f,
																									0.f,
																									0.f,
																									0.f,
																									0.f,
																									1.f });

		final int lDim = parseInt(lHeaderMap.get("dim"), 3);

		final String lType = lHeaderMap.get("type");

		final long lElementSize = parseLong(lHeaderMap.get("elementsize"),
																				1);

		final long lWidth = parseLong(lHeaderMap.get("width"));
		final long lHeight = parseLong(lHeaderMap.get("height"));
		final long lDepth = parseLong(lHeaderMap.get("depth"));

		final String lRealUnitName = parseString(	lHeaderMap.get("realunit"),
																							"1");
		final double lVoxelWidth = parseDouble(	lHeaderMap.get("voxelwidth"),
																						1.);
		final double lVoxelHeight = parseDouble(lHeaderMap.get("voxelheight"),
																						1.);
		final double lVoxelDepth = parseDouble(	lHeaderMap.get("voxeldepth"),
																						1.);

		pVolume.setTimeIndex(lIndex);
		pVolume.setTimeInSeconds(lTime);
		pVolume.setType(lType);
		pVolume.setChannelID(lVolumeChannelID);
		pVolume.setChannelName(lVolumeChannelName);
		pVolume.setColor(lColor);
		pVolume.setViewMatrix(lViewMatrix);
		pVolume.setDimension(lDim);
		pVolume.setDimensionsInVoxels(lElementSize,
																	lWidth,
																	lHeight,
																	lDepth);

		pVolume.setVoxelSizeInRealUnits(lRealUnitName,
																		lVoxelWidth,
																		lVoxelHeight,
																		lVoxelDepth);

	};

	private static float[] parseFloatArray(	String pString,
																					final float[] defaultValue)
	{
		return (pString == null) ? defaultValue
														: deserializeFloatArray(pString);

	}

	private static double parseDouble(String pString)
	{
		if (pString == null)
			return 0;
		return Double.parseDouble(pString);
	}

	private static String parseString(String pString)
	{
		if (pString == null)
			return "";
		return pString;
	}

	private static int parseInt(String pString)
	{
		if (pString == null)
			return 0;
		return Integer.parseInt(pString);
	}

	private static long parseLong(String pString)
	{
		if (pString == null)
			return 0;
		return Long.parseLong(pString);
	}

	private static double parseDouble(String pString,
																		double defaultValue)
	{
		return (pString == null) ? defaultValue
														: Double.parseDouble(pString);
	}

	private static String parseString(String pString,
																		String defaultValue)
	{
		return (pString == null) ? defaultValue : pString;
	}

	private static int parseInt(String pString, int defaultValue)
	{
		return (pString == null) ? defaultValue
														: Integer.parseInt(pString);
	}

	private static long parseLong(String pString, long defaultValue)
	{
		return (pString == null) ? defaultValue : Long.parseLong(pString);
	}

	private static String serializeFloatArray(float[] pFloatArray)
	{
		if (pFloatArray == null)
			return "";
		final StringBuilder lStringBuilder = new StringBuilder();
		for (int i = 0; i < pFloatArray.length; i++)
		{
			final float lValue = pFloatArray[i];
			lStringBuilder.append(lValue);
			if (i != pFloatArray.length - 1)
				lStringBuilder.append(" ");
		}
		return lStringBuilder.toString();
	}

	private static float[] deserializeFloatArray(String pString)
	{
		if (pString == null || pString.isEmpty())
			return null;
		float[] lFloatArray;
		try
		{
			pString = pString.trim();
			final String[] lSplittedString = pString.split(" ", -1);
			lFloatArray = new float[lSplittedString.length];
			for (int i = 0; i < lFloatArray.length; i++)
				lFloatArray[i] = Float.parseFloat(lSplittedString[i]);
			return lFloatArray;
		}
		catch (final NumberFormatException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private static ThreadLocal<ByteBuffer> sScratchBufferThreadLocal = new ThreadLocal<ByteBuffer>();

	public static final Volume deserialize(	SocketChannel pSocketChannel,
																					Volume pVolume) throws IOException
	{
		if (pVolume == null)
		{
			pVolume = new Volume();
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
		pScratchBuffer.limit(cLongSizeInBytes);
		while (pScratchBuffer.hasRemaining())
		{
			pSocketChannel.read(pScratchBuffer);
			sleep();
		}
		pScratchBuffer.rewind();
		final int lHeaderLength = ToIntExact.toIntExact(pScratchBuffer.getLong());
		return lHeaderLength;
	};



	public static final Volume deserialize(	ByteBuffer pByteBuffer,
																					Volume pVolume)
	{
		pByteBuffer.rewind();
		final int lWholeLength = ToIntExact.toIntExact(pByteBuffer.getLong());
		final int lHeaderLength = ToIntExact.toIntExact(pByteBuffer.getLong());
		readVolumeHeader(pByteBuffer, lHeaderLength, pVolume);
		final long lDataLength = pByteBuffer.getLong();
		readVolumeData(pByteBuffer, lDataLength, pVolume);
		return pVolume;
	}

	static void readVolumeData(	ByteBuffer pByteBuffer,
															long pDataLength,
															Volume pVolume)
	{

		if (pVolume.getDataBuffer() == null || pVolume.getDataBuffer()
																									.capacity() != pDataLength)
		{
			final ByteBuffer lByteBuffer = ByteBuffer.allocateDirect(ToIntExact.toIntExact(pDataLength));
			lByteBuffer.order(ByteOrder.nativeOrder());
			lByteBuffer.clear();
			pVolume.setDataBuffer(lByteBuffer);
		}

		pVolume.readFromByteBuffer(pByteBuffer);
	}

	
	
	private static void sleep()
	{
		try
		{
			Thread.sleep(1);
		}
		catch (final InterruptedException e)
		{
			e.printStackTrace();
		}
	}

}
