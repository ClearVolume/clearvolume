package clearvolume.volume.sink.filter;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
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

	private final Object mLock = new Object();
	private final ConcurrentHashMap<Integer, String> mSeenChannelIdToNameMap = new ConcurrentHashMap<Integer, String>();
	private final CopyOnWriteArrayList<Integer> mSeenChannelList = new CopyOnWriteArrayList<Integer>();
	private final ConcurrentHashMap<Integer, Boolean> mActiveChannelMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, Volume> mChanneltoVolumeMap = new ConcurrentHashMap<>();

	private final VolumeManager mEmptyVolumeManager = new VolumeManager(2);
	private final VolumeSinkInterface mVolumeSinkForFilteredVolumes = new NullVolumeSink();;

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
				final Integer lChannelId = mSeenChannelList.get(pIndex);
				return lChannelId + " | "
						+ mSeenChannelIdToNameMap.get(lChannelId);
			}
			catch (final Exception e)
			{
				final int lSize = mSeenChannelIdToNameMap.size();
				if (pIndex >= lSize)
					pIndex = lSize;
				return getElementAt(pIndex);
			}

		}

	};

	public ChannelFilterSink()
	{

	}

	public ChannelFilterSink(VolumeSinkInterface pRelaySink)
	{
		super(pRelaySink);

	}

	public void setActiveChannels(final int[] pActiveChannels)
	{
		mSeekingExecutor.execute(new Runnable()
		{

			@Override
			public void run()
			{
				synchronized (mLock)
				{
					final Set<Integer> lOldActiveChannelsKeySet = mActiveChannelMap.keySet();
					final ArrayList<Integer> lOldActiveChannels = new ArrayList<Integer>(lOldActiveChannelsKeySet);
					mActiveChannelMap.clear();
					for (final int lActiveChannel : pActiveChannels)
					{
						mActiveChannelMap.put(lActiveChannel, true);
					}
					for (int i = 0; i < pActiveChannels.length; i++)
						if (!lOldActiveChannels.contains(pActiveChannels[i]))
							sendVolumeInternal(pActiveChannels[i]);

					for (final Integer lChannel : lOldActiveChannels)
						if (!contains(pActiveChannels, lChannel))
							sendVolumeInternal(lChannel);
				}

			}
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
	public void sendVolume(Volume pVolume)
	{
		synchronized (mLock)
		{
			final int lChannelID = pVolume.getChannelID();
			final String lChannelName = pVolume.getChannelName();

			if (mChanneltoVolumeMap.get(lChannelID) != null)
				if (mVolumeSinkForFilteredVolumes != null)
					mVolumeSinkForFilteredVolumes.sendVolume(mChanneltoVolumeMap.get(lChannelID));
			mChanneltoVolumeMap.put(lChannelID, pVolume);

			if (!mSeenChannelList.contains(lChannelID))
			{
				mSeenChannelList.add(lChannelID);
				mActiveChannelMap.put(lChannelID, true);
				final ListDataListener[] lListeners = mChannelListModel.getListeners(ListDataListener.class);
				for (final ListDataListener lListDataListener : lListeners)
				{
					final ListDataEvent lListDataEvent = new ListDataEvent(	lListDataListener,
																			ListDataEvent.CONTENTS_CHANGED,
																			0,
																			mSeenChannelList.size());
					SwingUtilities.invokeLater(new Runnable()
					{

						@Override
						public void run()
						{
							lListDataListener.contentsChanged(lListDataEvent);
						};
					});

				}
			}
			mSeenChannelIdToNameMap.put(lChannelID, lChannelName);

			sendVolumeInternal(lChannelID);
		}
	}

	private void sendVolumeInternal(final int lChannelID)
	{
		synchronized (mLock)
		{
			final Volume lVolume = mChanneltoVolumeMap.get(lChannelID);
			final Boolean lBoolean = mActiveChannelMap.get(lChannelID);
			if (lBoolean != null && lBoolean)
			{
				forward(lVolume);
			}
			else
			{
				final Volume lEmptyVolume = mEmptyVolumeManager.requestAndWaitForVolumeLike(1,
																							TimeUnit.MILLISECONDS,
																							lVolume);
				lEmptyVolume.copyMetaDataFrom(lVolume);
				forward(lEmptyVolume);
			}
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

	private void forward(Volume pVolume)
	{
		if (getRelaySink() != null)
			getRelaySink().sendVolume(pVolume);
	}

	public ListModel<String> getChannelListModel()
	{
		return mChannelListModel;
	}

	@Override
	public void close()
	{
		synchronized (mLock)
		{
			mSeenChannelIdToNameMap.clear();
			mSeenChannelList.clear();
			mActiveChannelMap.clear();
			mEmptyVolumeManager.close();
		}
	}

}
