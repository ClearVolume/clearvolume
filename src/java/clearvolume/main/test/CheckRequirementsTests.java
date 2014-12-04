package clearvolume.main.test;

import org.junit.Test;

import clearvolume.main.CheckRequirements;
import clearvolume.utils.UnsupportedArchitectureException;

public class CheckRequirementsTests
{

	@Test
	public void test() throws UnsupportedArchitectureException
	{
		CheckRequirements.check();
	}

}
