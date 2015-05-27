package clearvolume.utils.math.lowdiscrepancy.test;

import org.junit.Test;

import clearvolume.utils.math.lowdiscrepancy.ModularSequence;

public class ModularSequenceTests
{

	@Test
	public void test()
	{

		System.out.println(ModularSequence.computeGapList(13, 8, 12));
		System.out.println(ModularSequence.computeGapScore(13, 8, 12));
		System.out.println(ModularSequence.computeGapScore(13, 8));

		System.out.println("----------");
		for (int k = 2; k < 13; k++)
			System.out.format("n=%d, k=%d, s=%d \n",
												13,
												k,
												ModularSequence.computeGapScore(13, k));

		System.out.println("----------");
		System.out.println(ModularSequence.findKWithBestGapScore(13));

		System.out.println("----------");
		for (int n = 2; n < 30; n++)
			System.out.format("n=%d, best k=%d \n",
												n,
												ModularSequence.findKWithBestGapScore(n));

	}
}
