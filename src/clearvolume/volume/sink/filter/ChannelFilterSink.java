package clearvolume.volume.sink.filter;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import clearvolume.ClearVolumeCloseable;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.NullVolumeSink;
import clearvolume.volume.sink.VolumeSinkInterface;
import clearvolume.volume.sink.relay.RelaySinkAdapter;
import clearvolume.volume.sink.relay.RelaySinkInterface;

public class ChannelFilterSink extends RelaySinkAdapter	implements
																												RelaySinkInterface,
																												ClearVolumeCloseable
{
	private static final ExecutorService mSeekingExecutor = Executors.newSingleThreadExecutor();

	private ConcurrentHashMap<Integer, String> mSeenChannelIdToNameMap = new ConcurrentHashMap<Integer, String>();
	private CopyOnWriteArrayList<Integer> mSeenChannelList = new CopyOnWriteArrayList<Integer>();
	private ConcurrentHashMap<Integer, Boolean> mActiveChannelMap = new ConcurrentHashMap<>();

	private ConcurrentHashMap<Integer, Volume> mChanneltoVolumeMap = new ConcurrentHashMap<>();

	private VolumeManager mEmptyVolumeManager = new VolumeManager(2);

	AbstractListModel<String> mChannelListModel = new AbstractListModel<String>()
	{
		private static final long serialVersionUID = 1L;

		@Override
		public int getSize()
		{
			return mSeenChannelList.size();
		}

		@Override
		public String getElementAt(int pIndex)
		{
			try
			{
				Integer lChannelId = mSeenChannelList.get(pIndex);
				return lChannelId + " | "
								+ mSeenChannelIdToNameMap.get(lChannelId);
			}
			catch (Exception e)
			{
				int lSize = mSeenChannelIdToNameMap.size();
				if (pIndex >= lSize)
					pIndex = lSize;
				return getElementAt(pIndex);
			}
		}

	};

	public ChannelFilterSink()
	{
		super();
	}

	public ChannelFilterSink(VolumeSinkInterface pVolumeSinkInterface)
	{
		setRelaySink(pVolumeSinkInterface);
	}

	public void setActiveChannels(int[] pActiveChannels)
	{
		ArrayList<Integer> lOldActiveChannels = new ArrayList<>(mActiveChannelMap.keySet());
		mActiveChannelMap.clear();
		for (int lActiveChannel : pActiveChannels)
		{
			mActiveChannelMap.put(lActiveChannel, true);
		}
		mSeekingExecutor.execute(() -> {
			for (int i = 0; i < pActiveChannels.length; i++)
				if (!lOldActiveChannels.contains(pActiveChannels[i]))
					sendVolumeInternal(pActiveChannels[i]);

			for (Integer lChannel : lOldActiveChannels)
				if (!contains(pActiveChannels, lChannel))
					sendVolumeInternal(lChannel);
		});
	}

	private static boolean contains(final int[] array, final int v)
	{
		for (final int e : array)
			if (e == v)
				return true;
		return false;
	}

	@Override
	public void sendVolume(Volume<?> pVolume)
	{
		final int lChannelID = pVolume.getChannelID();
		final String lChannelName = pVolume.getChannelName();

		if (!mSeenChannelList.contains(lChannelID))
		{
			mSeenChannelList.add(lChannelID);
			mActiveChannelMap.put(lChannelID, true);
			ListDataListener[] lListeners = mChannelListModel.getListeners(ListDataListener.class);
			for (ListDataListener lListDataListener : lListeners)
			{
				ListDataEvent lListDataEvent = new ListDataEvent(	lListDataListener,
																													ListDataEvent.CONTENTS_CHANGED,
																													0,
																													mSeenChannelList.size());
				lListDataListener.contentsChanged(lListDataEvent);
			}
		}
		mSeenChannelIdToNameMap.put(lChannelID, lChannelName);

		if (mChanneltoVolumeMap.get(lChannelID) != null)
			mChanneltoVolumeMap.get(lChannelID).makeAvailableToManager();
		mChanneltoVolumeMap.put(lChannelID, pVolume);

		sendVolumeInternal(lChannelID);
	}

	private void sendVolumeInternal(final int lChannelID)
	{
		Volume<?> lVolume = mChanneltoVolumeMap.get(lChannelID);
		Boolean lBoolean = mActiveChannelMap.get(lChannelID);
		if (lBoolean != null && lBoolean)
		{
			if (getRelaySink() != null)
				getRelaySink().sendVolume(lVolume);
		}
		else
		{
			Volume<?> lEmptyVolume = mEmptyVolumeManager.requestAndWaitForVolumeLike(	1,
																																								TimeUnit.MILLISECONDS,
																																								lVolume);
			lEmptyVolume.copyMetaDataFrom(lVolume);

			if (getRelaySink() != null)
				getRelaySink().sendVolume(lEmptyVolume);
		}
	}

	@Override
	public VolumeManager getManager()
	{
		if (!(getRelaySink() instanceof NullVolumeSink))
			if (getRelaySink() != null)
				return getRelaySink().getManager();
		return null;
	}

	public ListModel<String> getChannelListModel()
	{
		return mChannelListModel;
	}

	@Override
	public void close()
	{
		mSeenChannelIdToNameMap.clear();
		mSeenChannelList.clear();
		mActiveChannelMap.clear();
		mEmptyVolumeManager.close();
	}

}
