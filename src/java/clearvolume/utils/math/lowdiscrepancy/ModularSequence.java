package clearvolume.utils.math.lowdiscrepancy;

import gnu.trove.list.array.TIntArrayList;

import java.util.concurrent.ConcurrentHashMap;

public class ModularSequence
{

	public static int get(int n, int k, int i)
	{
		return (i * k) % n;
	}

	public static boolean[] generateBinaryMap(int n, int k, int m)
	{
		final boolean[] lBinaryMap = new boolean[n];
		for (int i = 0; i < m; i++)
			lBinaryMap[get(n, k, i)] = true;

		return lBinaryMap;
	}

	public static boolean isBinaryMapFilled(int n, int k)
	{
		final boolean[] lBinaryMap = generateBinaryMap(n, k, n);
		for (final boolean b : lBinaryMap)
			if (!b)
				return false;

		return true;
	}

	public static TIntArrayList computeGapList(int n, int k, int m)
	{
		final boolean[] lBinaryMap = generateBinaryMap(n, k, m);

		final TIntArrayList lGapList = new TIntArrayList();

		int last = 0;
		for (int i = 1; i < n; i++)
		{
			if (lBinaryMap[i])
			{
				lGapList.add(i - last);
				last = i;
			}
		}

		lGapList.add(n - last);

		return lGapList;
	}

	public static int computeGapScore(int n, int k, int m)
	{
		final TIntArrayList lGapList = computeGapList(n, k, m);
		final int[] lArray = lGapList.toArray();

		int lScore = 1;
		for (final int gaplength : lArray)
			lScore *= gaplength;

		return lScore;
	}

	public static int computeGapScore(int n, int k)
	{
		int lScore = 1;
		for (int m = 1; m < n; m++)
			lScore += computeGapScore(n, k, m);

		return lScore;
	}

	public static int findKWithBestGapScore(int n)
	{
		if (n == 1 || n == 2)
			return 1;

		int lScore = Integer.MIN_VALUE;
		int lBestK = 1;
		for (int k = 2; k < n; k++)
			if (isBinaryMapFilled(n, k))
			{
				final int lNewScore = computeGapScore(n, k);
				if (lNewScore >= lScore)
				{
					lBestK = k;
					lScore = lNewScore;
				}
			}

		return lBestK;
	}

	private static ConcurrentHashMap<Integer, Integer> sCache = new ConcurrentHashMap<Integer, Integer>();

	public static int findKWithBestGapScoreCached(int n)
	{

		Integer lBestK = sCache.get(n);

		if (lBestK == null)
		{
			lBestK = findKWithBestGapScore(n);
			sCache.put(n, lBestK);
		}

		return lBestK;
	}
}
