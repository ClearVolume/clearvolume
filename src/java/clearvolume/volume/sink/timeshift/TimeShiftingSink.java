package clearvolume.volume.sink.timeshift;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import clearvolume.ClearVolumeCloseable;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.relay.RelaySinkAdapter;
import clearvolume.volume.sink.relay.RelaySinkInterface;

public class TimeShiftingSink extends RelaySinkAdapter implements
																											RelaySinkInterface,
																											ClearVolumeCloseable
{
	private static final float cCleanUpRatio = 0.25f;

	private static final ExecutorService mSeekingExecutor = Executors.newSingleThreadExecutor();
	private final SwitchableSoftReferenceManager<Volume> mSwitchableSoftReferenceManager;

	private final Object mLock = new Object();
	private HashMap<Integer, TreeMap<Long, SwitchableSoftReference<Volume>>> mChannelToVolumeListsMap = new HashMap<>();
	private final TreeSet<Integer> mAvailableChannels = new TreeSet<>();

	private volatile long mSoftMemoryHorizonInTimePointIndices;
	private volatile long mHardMemoryHorizonInTimePointIndices;
	private volatile long mCleanUpPeriodInTimePoints;
	private volatile long mHighestTimePointIndexSeen = 0;
	private volatile long mTimeShift = 0;
	private volatile boolean mIsPlaying = true;

	public TimeShiftingSink(long pSoftMemoryHoryzonInTimePointIndices,
													long pHardMemoryHoryzonInTimePointIndices)
	{
		super();
		mSwitchableSoftReferenceManager = new SwitchableSoftReferenceManager<>();
		mSoftMemoryHorizonInTimePointIndices = Math.min(pSoftMemoryHoryzonInTimePointIndices,
																										pHardMemoryHoryzonInTimePointIndices);
		mHardMemoryHorizonInTimePointIndices = Math.max(pHardMemoryHoryzonInTimePointIndices,
																										pSoftMemoryHoryzonInTimePointIndices);
		mCleanUpPeriodInTimePoints = (long) (mSoftMemoryHorizonInTimePointIndices * cCleanUpRatio);
	}

	public void setTimeShiftNormalized(final double pTimeShiftNormalized)
	{
		final Runnable lRunnable = new Runnable()
		{

			@Override
			public void run()
			{
				synchronized (mLock)
				{
					final long lPreviousTimeShift = mTimeShift;

					// find the available data interval to evade invalid indices
					final long startPos = Math.max(	0,
																					mHighestTimePointIndexSeen - mHardMemoryHorizonInTimePointIndices);
					final long interval = mHighestTimePointIndexSeen - startPos;

					// System.err.println("interval=[" + startPos +"," +interval+"]");
					mTimeShift = -Math.round(interval * pTimeShiftNormalized);
					if (lPreviousTimeShift != mTimeShift)
						for (final int lChannel : mAvailableChannels)
							sendVolumeInternal(lChannel);
				}
			}
		};

		mSeekingExecutor.execute(lRunnable);
	}

	public void setTimeShift(long pTimeShift)
	{
		mTimeShift = pTimeShift;
	}

	public long getTimeShift()
	{
		return mTimeShift;
	}

	public long getHardMemoryHorizon()
	{
		return mHardMemoryHorizonInTimePointIndices;
	}

	public long getSoftMemoryHorizon()
	{
		return mSoftMemoryHorizonInTimePointIndices;
	}

	public long getNumberOfTimepoints()
	{
		return mHighestTimePointIndexSeen;
	}

	public int getNumberOfAvailableChannels()
	{
		return mAvailableChannels.size();
	}

	public int getAvailableChannels()
	{
		return mAvailableChannels.size();
	}

	@Override
	public void sendVolume(Volume pVolume)
	{
		synchronized (mLock)
		{
			final int lVolumeChannelID = pVolume.getChannelID();
			mAvailableChannels.add(lVolumeChannelID);
			TreeMap<Long, SwitchableSoftReference<Volume>> lTimePointIndexToVolumeMapReference = mChannelToVolumeListsMap.get(lVolumeChannelID);

			if (lTimePointIndexToVolumeMapReference == null)
			{
				lTimePointIndexToVolumeMapReference = new TreeMap<>();
				mChannelToVolumeListsMap.put(	lVolumeChannelID,
																			lTimePointIndexToVolumeMapReference);
			}

			lTimePointIndexToVolumeMapReference.put(pVolume.getTimeIndex(),
																							wrapWithReference(pVolume));

			mHighestTimePointIndexSeen = Math.max(mHighestTimePointIndexSeen,
																						pVolume.getTimeIndex());

			if (mIsPlaying)
				sendVolumeInternal(lVolumeChannelID);

			cleanUpOldVolumes(mHighestTimePointIndexSeen, lVolumeChannelID);
		}
	}

	private void sendVolumeInternal(int lVolumeChannelID)
	{
		synchronized (mLock)
		{
			final Volume lVolumeToSend = getVolumeToSend(lVolumeChannelID);

			if (lVolumeToSend != null)
			{
				getRelaySink().sendVolume(lVolumeToSend);
			}
			else
			{
				System.err.println("Did not have any volume to send :(");
			}

			cleanUpOldVolumes(mHighestTimePointIndexSeen, lVolumeChannelID);
		}
	}

	private SwitchableSoftReference<Volume> wrapWithReference(final Volume pVolume)
	{
		final Runnable lCleaningRunnable = new Runnable()
		{
			@Override
			public void run()
			{
				System.out.println("CLEANING!");
				pVolume.makeAvailableToManager();
			}
		};
		return mSwitchableSoftReferenceManager.wrapReference(	pVolume,
																													lCleaningRunnable);
	}

	private Volume getVolumeToSend(int pVolumeChannelID)
	{
		synchronized (mLock)
		{
			final TreeMap<Long, SwitchableSoftReference<Volume>> lTimePointIndexToVolumeMap = mChannelToVolumeListsMap.get(pVolumeChannelID);

			if (lTimePointIndexToVolumeMap.isEmpty())
				return null;

			Entry<Long, SwitchableSoftReference<Volume>> lIndexVolumeEntry = lTimePointIndexToVolumeMap.floorEntry(mHighestTimePointIndexSeen + mTimeShift);
			if (lIndexVolumeEntry == null)
				lIndexVolumeEntry = lTimePointIndexToVolumeMap.ceilingEntry(mHighestTimePointIndexSeen + mTimeShift);

			if (lIndexVolumeEntry == null)
				return null;

			final Volume lVolume = lIndexVolumeEntry.getValue().get();
			if (lVolume == null)
			{
				lTimePointIndexToVolumeMap.remove(lIndexVolumeEntry.getKey());
				return getVolumeToSend(pVolumeChannelID);
			}
			return lVolume;
		}
	}

	private void cleanUpOldVolumes(long pTimePointIndex, int pChannelID)
	{
		synchronized (mLock)
		{
			if (pTimePointIndex % mCleanUpPeriodInTimePoints != 0)
				return;

			final TreeMap<Long, SwitchableSoftReference<Volume>> lTimePointIndexToVolumeMap = mChannelToVolumeListsMap.get(pChannelID);
			if (lTimePointIndexToVolumeMap.isEmpty())
			{
				mChannelToVolumeListsMap.remove(pChannelID);
				return;
			}
			final Long lLastTimePoint = lTimePointIndexToVolumeMap.lastKey();

			Long lTimePoint = lLastTimePoint;
			while (lTimePoint != null && lTimePoint > lLastTimePoint - mSoftMemoryHorizonInTimePointIndices)
			{
				lTimePoint = lTimePointIndexToVolumeMap.lowerKey(lTimePoint);
			}
			while (lTimePoint != null && lTimePoint > lLastTimePoint - mHardMemoryHorizonInTimePointIndices)
			{
				final SwitchableSoftReference<Volume> lSwitchableSoftReference = lTimePointIndexToVolumeMap.get(lTimePoint);
				if (lSwitchableSoftReference.isGone())
				{
					lTimePointIndexToVolumeMap.remove(lTimePoint);
					continue;
				}
				lSwitchableSoftReference.soften();
				lTimePoint = lTimePointIndexToVolumeMap.lowerKey(lTimePoint);
			}
			while (lTimePoint != null)
			{
				final SwitchableSoftReference<Volume> lSwitchableSoftReference = lTimePointIndexToVolumeMap.get(lTimePoint);
				final Volume lVolume = lSwitchableSoftReference.get();
				if (lVolume != null)
					lVolume.makeAvailableToManager();
				lTimePointIndexToVolumeMap.remove(lTimePoint);
				lTimePoint = lTimePointIndexToVolumeMap.lowerKey(lTimePoint);
			}
		}
	}

	@Override
	public VolumeManager getManager()
	{
		return getRelaySink().getManager();
	}

	@Override
	public void close()
	{
		synchronized (mLock)
		{
			for (final Map.Entry<Integer, TreeMap<Long, SwitchableSoftReference<Volume>>> lTimeLineForChannelEntry : mChannelToVolumeListsMap.entrySet())
			{
				final TreeMap<Long, SwitchableSoftReference<Volume>> lTimeLineTreeMap = lTimeLineForChannelEntry.getValue();

				for (final Map.Entry<Long, SwitchableSoftReference<Volume>> lTimePointEntry : lTimeLineTreeMap.entrySet())
				{
					final SwitchableSoftReference<Volume> lVolumeSoftReference = lTimePointEntry.getValue();
					final Volume lVolume = lVolumeSoftReference.get();
					if (lVolume != null)
						lVolume.close();
					lVolumeSoftReference.soften();
				}

				lTimeLineTreeMap.clear();
			}
			mChannelToVolumeListsMap.clear();
			mChannelToVolumeListsMap = null;

			mAvailableChannels.clear();
		}

	}

	public void pause()
	{
		mIsPlaying = false;
	}

	public void play()
	{
		mIsPlaying = true;
	}

}
