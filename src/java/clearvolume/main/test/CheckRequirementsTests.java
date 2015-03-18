package clearvolume.main.test;

import org.junit.Test;

import clearvolume.exceptions.UnsupportedArchitectureException;
import clearvolume.main.CheckRequirements;

public class CheckRequirementsTests
{

	@Test
	public void test() throws UnsupportedArchitectureException
	{
		CheckRequirements.check();
	}

}
