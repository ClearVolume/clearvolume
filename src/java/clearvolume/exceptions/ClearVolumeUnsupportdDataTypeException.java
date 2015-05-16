package clearvolume.exceptions;

public class ClearVolumeUnsupportdDataTypeException	extends
																										ClearVolumeException
{

	public ClearVolumeUnsupportdDataTypeException(String pMessage,
																								Throwable pCause)
	{
		super(pMessage, pCause);
	}

	public ClearVolumeUnsupportdDataTypeException(String pString)
	{
		super(pString);
	}

}
