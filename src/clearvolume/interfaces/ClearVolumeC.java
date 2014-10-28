package clearvolume.interfaces;

public class ClearVolumeC
{

	private static Throwable sLastThrowableException = null;

	public static final String getLastExceptionMessage()
	{
		if (sLastThrowableException == null)
			return "none";
		else
			return sLastThrowableException.getLocalizedMessage();
	}

	private static void clearLastExceptionMessage()
	{
		sLastThrowableException = null;
	}


}
