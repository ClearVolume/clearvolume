package clearvolume.network.serialization.keyvalue;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class KeyValueMaps
{

	public static final Map<String, String> readMapFromBuffer(	ByteBuffer pByteBuffer,
																int pHeaderLength,
																HashMap<String, String> pDestMap)
	{

		byte[] lByteArray = new byte[pHeaderLength];
		pByteBuffer.get(lByteArray);

		String lMapString = new String(lByteArray);

		return readMapFromString(lMapString, pDestMap);
	}

	public static final Map<String, String> readMapFromString(	String pMapString,
																Map<String, String> pDestMap)
	{
		if (pDestMap == null)
			pDestMap = new LinkedHashMap<String, String>();
		pDestMap.clear();

		pMapString = pMapString.substring(1, pMapString.length() - 1);

		String[] lSplittedHeader = pMapString.split(",");

		for (String lKeyValueEntry : lSplittedHeader)
		{
			int lKeyValueSeparatorIndex = lKeyValueEntry.indexOf(':');
			final String lKey = lKeyValueEntry.substring(	0,
															lKeyValueSeparatorIndex);
			final String lValue = lKeyValueEntry.substring(	lKeyValueSeparatorIndex + 1,
															lKeyValueEntry.length());
			pDestMap.put(lKey, lValue);
		}

		return pDestMap;
	}

	public static final StringBuilder writeStringFromMap(	Map<String, String> pDestMap,
															StringBuilder pStringBuilder)
	{
		if (pStringBuilder == null)
			pStringBuilder = new StringBuilder();
		pStringBuilder.setLength(0);

		pStringBuilder.append('[');

		boolean isFirst = true;
		for (Map.Entry<String, String> lKeyValueEntry : pDestMap.entrySet())
		{
			final String lKey = lKeyValueEntry.getKey();
			final String lValue = lKeyValueEntry.getValue();
			if (!isFirst)
				pStringBuilder.append(',');
			pStringBuilder.append(lKey);
			pStringBuilder.append(':');
			pStringBuilder.append(lValue);
			isFirst = false;
		}

		pStringBuilder.append(']');

		return pStringBuilder;
	}

	public static final ByteBuffer writeBufferFromMap(	Map<String, String> pDestMap,
														ByteBuffer pByteBuffer)
	{
		pByteBuffer.putChar('[');

		boolean isFirst = true;
		for (Map.Entry<String, String> lKeyValueEntry : pDestMap.entrySet())
		{
			final String lKey = lKeyValueEntry.getKey();
			final String lValue = lKeyValueEntry.getValue();
			if (!isFirst)
				pByteBuffer.putChar(',');
			pByteBuffer.put(lKey.getBytes());
			pByteBuffer.putChar(':');
			pByteBuffer.put(lValue.getBytes());
			isFirst = false;
		}

		pByteBuffer.putChar(']');

		return pByteBuffer;
	}
}
