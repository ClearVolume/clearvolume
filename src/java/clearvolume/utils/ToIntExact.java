package clearvolume.utils;

public class ToIntExact {
	public static int toIntExact(long pValue) {
		if (pValue > Integer.MAX_VALUE)
			throw new IllegalArgumentException(
					"Long too large to be exactly converted to int!");
		return (int) pValue;
	}
}
