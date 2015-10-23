package clearvolume.network.serialization.keyvalue.test;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import clearvolume.network.serialization.keyvalue.KeyValueMaps;

public class KeyValueMapsTests
{

	@Test
	public void test()
	{
		String lStringMap = "[a:ok,b:2,cd:0.1]";
		Map<String, String> lMap = KeyValueMaps.readMapFromString(	lStringMap,
																	null);
		assertEquals("ok", lMap.get("a"));
		assertEquals("2", lMap.get("b"));
		assertEquals("0.1", lMap.get("cd"));

		StringBuilder lStringBuilder = KeyValueMaps.writeStringFromMap(	lMap,
																		null);
		assertEquals(lStringMap, lStringBuilder.toString());
	}

}
