package clearvolume.demo.fauxscope;

/**
 * Created by ulrik on 13/02/15.
 */
public interface FauxscopeRandomizer
{
	void init();

	void reinitialize();

	float[] getNextPoint();
}
