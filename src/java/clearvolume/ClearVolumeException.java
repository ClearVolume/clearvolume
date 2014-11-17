package clearvolume;

public class ClearVolumeException extends Exception
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ClearVolumeException()
	{
		super();
	}

	public ClearVolumeException(String pMessage,
															Throwable pCause,
															boolean pEnableSuppression,
															boolean pWritableStackTrace)
	{
		super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
	}

	public ClearVolumeException(String pMessage, Throwable pCause)
	{
		super(pMessage, pCause);
	}

	public ClearVolumeException(String pMessage)
	{
		super(pMessage);
	}

	public ClearVolumeException(Throwable pCause)
	{
		super(pCause);
	}

}
