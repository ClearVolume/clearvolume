package clearvolume.volume.sink.timeshift;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import clearvolume.ClearVolumeCloseable;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.relay.RelaySinkAdapter;
import clearvolume.volume.sink.relay.RelaySinkInterface;

public class MultiChannelTimeShiftingSink extends RelaySinkAdapter implements
																																	RelaySinkInterface,
																																	ClearVolumeCloseable
{
	private static final float cCleanUpRatio = 0.25f;

	private SwitchableSoftReferenceManager<Volume<?>> mSwitchableSoftReferenceManager;

	private ReentrantLock lLock = new ReentrantLock();
	private HashMap<Integer, TreeMap<Long, SwitchableSoftReference<Volume<?>>>> mChannelToVolumeListsMap = new HashMap<>();

	private TreeSet<Integer> mAvailableChannels = new TreeSet<>();

	private volatile long mSoftMemoryHoryzonInTimePointIndices;
	private volatile long mHardMemoryHoryzonInTimePointIndices;
	private volatile long mCleanUpPeriodInTimePoints;
	private volatile long mHighestTimePointIndexSeen = 0;
	private volatile long mTimeShift = 0;
	private volatile int mCurrentChannelID = 0;

	public MultiChannelTimeShiftingSink(long pSoftMemoryHoryzonInTimePointIndices,
																			long pHardMemoryHoryzonInTimePointIndices)
	{
		super();
		mSwitchableSoftReferenceManager = new SwitchableSoftReferenceManager<>();
		mSoftMemoryHoryzonInTimePointIndices = Math.min(pSoftMemoryHoryzonInTimePointIndices,
																										pHardMemoryHoryzonInTimePointIndices);
		mHardMemoryHoryzonInTimePointIndices = Math.max(pHardMemoryHoryzonInTimePointIndices,
																										pSoftMemoryHoryzonInTimePointIndices);
		mCleanUpPeriodInTimePoints = (long) (mSoftMemoryHoryzonInTimePointIndices * cCleanUpRatio);
	}

	public void setTimeShiftNormalized(double pTimeShiftNormalized)
	{
		mTimeShift = -Math.round(mHardMemoryHoryzonInTimePointIndices * pTimeShiftNormalized);
	}

	public void setTimeShift(long pTimeShift)
	{
		mTimeShift = pTimeShift;
	}

	public long getTimeShift()
	{
		return mTimeShift;
	}

	public int getNumberOfAvailableChannels()
	{
		return mAvailableChannels.size();
	}

	public int getAvailableChannels()
	{
		return mAvailableChannels.size();
	}

	public Integer nextChannel()
	{
		final int lPreviousChannelID = mCurrentChannelID;

		if (mAvailableChannels.isEmpty())
			return null;
		Integer lHigher = mAvailableChannels.higher(mCurrentChannelID);
		if (lHigher == null)
			mCurrentChannelID = mAvailableChannels.first();
		else
			mCurrentChannelID = lHigher;

		if (mCurrentChannelID != lPreviousChannelID)
			sendVolumeInternal(mCurrentChannelID);

		return mCurrentChannelID;
	}

	public Integer previousChannel()
	{
		final int lPreviousChannelID = mCurrentChannelID;

		if (mAvailableChannels.isEmpty())
			return null;
		Integer lLower = mAvailableChannels.lower(mCurrentChannelID);
		if (lLower == null)
			mCurrentChannelID = mAvailableChannels.last();
		else
			mCurrentChannelID = lLower;

		if (mCurrentChannelID != lPreviousChannelID)
			sendVolumeInternal(mCurrentChannelID);

		return mCurrentChannelID;
	}

	@Override
	public void sendVolume(Volume<?> pVolume)
	{

		int lVolumeChannelID = pVolume.getChannelID();
		mAvailableChannels.add(lVolumeChannelID);
		TreeMap<Long, SwitchableSoftReference<Volume<?>>> lTimePointIndexToVolumeMapReference = mChannelToVolumeListsMap.get(lVolumeChannelID);

		if (lTimePointIndexToVolumeMapReference == null)
		{
			lTimePointIndexToVolumeMapReference = new TreeMap<>();
			mChannelToVolumeListsMap.put(	lVolumeChannelID,
																		lTimePointIndexToVolumeMapReference);
		}

		lTimePointIndexToVolumeMapReference.put(pVolume.getIndex(),
																						wrapWithReference(pVolume));

		mHighestTimePointIndexSeen = Math.max(mHighestTimePointIndexSeen,
																					pVolume.getIndex());

		sendVolumeInternal(lVolumeChannelID);
	}

	private void sendVolumeInternal(int lVolumeChannelID)
	{
		Volume<?> lVolumeToSend = getVolumeToSend(lVolumeChannelID);

		if (lVolumeToSend != null)
			getRelaySink().sendVolume(lVolumeToSend);

		cleanUpOldVolumes(mHighestTimePointIndexSeen, lVolumeChannelID);
	}

	private SwitchableSoftReference<Volume<?>> wrapWithReference(Volume<?> pVolume)
	{
		return mSwitchableSoftReferenceManager.wrapReference(	pVolume,
																													() -> {
																														System.out.println("CLEANING!");
																														pVolume.makeAvailableToManager();
																													});
	}

	private Volume<?> getVolumeToSend(int pVolumeChannelID)
	{
		if (!mAvailableChannels.contains(mCurrentChannelID))
		{
			Integer lNewCurrentChannelID = mAvailableChannels.floor(mCurrentChannelID);
			if (lNewCurrentChannelID == null)
			{
				lNewCurrentChannelID = mAvailableChannels.ceiling(mCurrentChannelID);
			}
			mCurrentChannelID = lNewCurrentChannelID;
		}

		if (pVolumeChannelID != mCurrentChannelID)
			return null;

		TreeMap<Long, SwitchableSoftReference<Volume<?>>> lTimePointIndexToVolumeMap = mChannelToVolumeListsMap.get(mCurrentChannelID);

		if (lTimePointIndexToVolumeMap.isEmpty())
			return null;

		Entry<Long, SwitchableSoftReference<Volume<?>>> lIndexVolumeEntry = lTimePointIndexToVolumeMap.floorEntry(mHighestTimePointIndexSeen + mTimeShift);

		if (lIndexVolumeEntry == null)
			return null;

		Volume<?> lVolume = lIndexVolumeEntry.getValue().get();
		if (lVolume == null)
		{
			lTimePointIndexToVolumeMap.remove(lIndexVolumeEntry.getKey());
			return getVolumeToSend(pVolumeChannelID);
		}

		return lVolume;
	}

	private void cleanUpOldVolumes(long pTimePointIndex, int pChannelID)
	{
		if (pTimePointIndex % mCleanUpPeriodInTimePoints != 0)
			return;

		TreeMap<Long, SwitchableSoftReference<Volume<?>>> lTimePointIndexToVolumeMap = mChannelToVolumeListsMap.get(pChannelID);
		if (lTimePointIndexToVolumeMap.isEmpty())
		{
			mChannelToVolumeListsMap.remove(pChannelID);
			return;
		}
		Long lLastTimePoint = lTimePointIndexToVolumeMap.lastKey();

		Long lTimePoint = lLastTimePoint;
		while (lTimePoint != null && lTimePoint > lLastTimePoint - mSoftMemoryHoryzonInTimePointIndices)
		{
			lTimePoint = lTimePointIndexToVolumeMap.lowerKey(lTimePoint);
		}
		while (lTimePoint != null && lTimePoint > lLastTimePoint - mHardMemoryHoryzonInTimePointIndices)
		{
			SwitchableSoftReference<Volume<?>> lSwitchableSoftReference = lTimePointIndexToVolumeMap.get(lTimePoint);
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
			SwitchableSoftReference<Volume<?>> lSwitchableSoftReference = lTimePointIndexToVolumeMap.get(lTimePoint);
			Volume<?> lVolume = lSwitchableSoftReference.get();
			if (lVolume != null)
				lVolume.makeAvailableToManager();
			lTimePointIndexToVolumeMap.remove(lTimePoint);
			lTimePoint = lTimePointIndexToVolumeMap.lowerKey(lTimePoint);
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
		for (Map.Entry<Integer, TreeMap<Long, SwitchableSoftReference<Volume<?>>>> lTimeLineForChannelEntry : mChannelToVolumeListsMap.entrySet())
		{
			TreeMap<Long, SwitchableSoftReference<Volume<?>>> lTimeLineTreeMap = lTimeLineForChannelEntry.getValue();

			for (Map.Entry<Long, SwitchableSoftReference<Volume<?>>> lTimePointEntry : lTimeLineTreeMap.entrySet())
			{
				SwitchableSoftReference<Volume<?>> lVolumeSoftReference = lTimePointEntry.getValue();
				Volume<?> lVolume = lVolumeSoftReference.get();
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
