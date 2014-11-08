package clearvolume.volume.sink.renderer;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.transferf.TransferFunction;
import clearvolume.transferf.TransferFunctions;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.NullVolumeSink;
import clearvolume.volume.sink.relay.RelaySinkAdapter;
import clearvolume.volume.sink.relay.RelaySinkInterface;

public class ClearVolumeRendererSink extends RelaySinkAdapter	implements
																															RelaySinkInterface
{

	private ClearVolumeRendererInterface mClearVolumeRendererInterface;
	private VolumeManager mVolumeManager;
	private long mWaitForCopyTimeout;
	private TimeUnit mTimeUnit;

	private TreeMap<Integer, String> mSeenChannelIdToNameMap = new TreeMap<Integer, String>();
	private ArrayList<Integer> mSeenChannelList = new ArrayList<Integer>();

	private volatile long mLastTimePointDisplayed = Long.MIN_VALUE;

	public ClearVolumeRendererSink(	ClearVolumeRendererInterface pClearVolumeRendererInterface,
																	VolumeManager pVolumeManager,
																	long pWaitForCopyTimeout,
																	TimeUnit pTimeUnit)
	{
		super();
		mClearVolumeRendererInterface = pClearVolumeRendererInterface;
		mVolumeManager = pVolumeManager;
		mWaitForCopyTimeout = pWaitForCopyTimeout;
		mTimeUnit = pTimeUnit;

	}

	@Override
	public void sendVolume(Volume<?> pVolume)
	{

		final long lTimePointIndex = pVolume.getTimeIndex();
		final int lChannelID = pVolume.getChannelID();
		final String lChannelName = pVolume.getChannelName();
		mSeenChannelIdToNameMap.put(lChannelID, lChannelName);
		final int lNumberOfRenderLayers = mClearVolumeRendererInterface.getNumberOfRenderLayers();
		final int lRenderLayer = lChannelID % lNumberOfRenderLayers;

		mClearVolumeRendererInterface.setCurrentRenderLayer(lRenderLayer);

		TransferFunction lTransferFunction;
		final float[] lColor = pVolume.getColor();
		if (lColor != null)
			lTransferFunction = TransferFunctions.getGradientForColor(lColor);
		else
			lTransferFunction = TransferFunctions.getGradientForColor(lRenderLayer);

		mClearVolumeRendererInterface.setTransfertFunction(lTransferFunction);
		mClearVolumeRendererInterface.setVolumeDataBuffer(pVolume);

		// if (lTimePointIndex > mLastTimePointDisplayed)
		{
			mClearVolumeRendererInterface.requestDisplay();

			mClearVolumeRendererInterface.waitToFinishDataBufferCopy(	mWaitForCopyTimeout,
																																mTimeUnit);
			mLastTimePointDisplayed = lTimePointIndex;
		}

		if (getRelaySink() != null)
			getRelaySink().sendVolume(pVolume);
		else
			pVolume.makeAvailableToManager();/**/

	}

	@Override
	public VolumeManager getManager()
	{
		if (!(getRelaySink() instanceof NullVolumeSink))
			if (getRelaySink() != null)
				return getRelaySink().getManager();
		return mVolumeManager;
	}

	public ListModel<String> getChannelListModel()
	{
		return new AbstractListModel<String>()
		{

			@Override
			public int getSize()
			{

				return mSeenChannelIdToNameMap.size();
			}

			@Override
			public String getElementAt(int pIndex)
			{
				return mSeenChannelIdToNameMap.get(mSeenChannelList.get(pIndex));
			}

		};
	}

}
