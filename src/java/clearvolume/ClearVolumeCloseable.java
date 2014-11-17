package clearvolume;

public interface ClearVolumeCloseable extends AutoCloseable
{
	@Override
	void close() throws ClearVolumeException;
}
