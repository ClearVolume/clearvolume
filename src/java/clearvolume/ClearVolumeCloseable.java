package clearvolume;

import clearvolume.exceptions.ClearVolumeException;

public interface ClearVolumeCloseable extends AutoCloseable
{
	@Override
	void close() throws ClearVolumeException;
}
