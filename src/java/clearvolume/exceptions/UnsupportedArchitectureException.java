package clearvolume.exceptions;

public class UnsupportedArchitectureException	extends
												ClearVolumeException
{

	private static final long serialVersionUID = 1L;

	public UnsupportedArchitectureException(String message)
	{
		super(message, null);
	}

}