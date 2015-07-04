package clearvolume.volume.sink.renderer;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import clearvolume.ClearVolumeCloseable;
import clearvolume.exceptions.ClearVolumeException;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.transferf.TransferFunction;
import clearvolume.transferf.TransferFunctions;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.NullVolumeSink;
import clearvolume.volume.sink.relay.RelaySinkAdapter;
import clearvolume.volume.sink.relay.RelaySinkInterface;
import coremem.types.NativeTypeEnum;
import coremem.util.Size;

public class ClearVolumeRendererSink extends RelaySinkAdapter	implements
																															RelaySinkInterface,
																															ClearVolumeCloseable
{

	private volatile ClearVolumeRendererInterface mClearVolumeRendererInterface;
	private volatile boolean mSwitchingRenderers;
	private volatile VolumeManager mVolumeManager;
	private final long mWaitForCopyTimeout;
	private final TimeUnit mTimeUnit;

	private String mRequestedWindowTitle;
	private int mRequestedWindowWidth, mRequestedWindowHeight;
	private int mMaxNumberOfAvailableVolumes;
	private int mNumberOfLayers;

	private final TreeMap<Integer, String> mSeenChannelIdToNameMap = new TreeMap<Integer, String>();

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

	public ClearVolumeRendererSink(	final String pRequestedWindowTitle,
																	final int pRequestedWindowWidth,
																	final int pRequestedWindowHeight,
																	final NativeTypeEnum pNativeTypeEnum,
																	final int pNumberOfLayers,
																	final long pWaitForCopyTimeout,
																	final TimeUnit pTimeUnit,
																	final int pMaxNumberOfAvailableVolumes)
	{
		super();
		mRequestedWindowTitle = pRequestedWindowTitle;
		mRequestedWindowWidth = pRequestedWindowWidth;
		mRequestedWindowHeight = pRequestedWindowHeight;
		mWaitForCopyTimeout = pWaitForCopyTimeout;
		mTimeUnit = pTimeUnit;
		mNumberOfLayers = pNumberOfLayers;
		mMaxNumberOfAvailableVolumes = pMaxNumberOfAvailableVolumes;

		createRenderer(pNativeTypeEnum, pNumberOfLayers);
	}

	@Override
	public void sendVolume(Volume pVolume)
	{
		final long lTimePointIndex = pVolume.getTimeIndex();
		final int lChannelID = pVolume.getChannelID();
		final String lChannelName = pVolume.getChannelName();
		mSeenChannelIdToNameMap.put(lChannelID, lChannelName);

		final NativeTypeEnum lNativeType = pVolume.getNativeType();
		final int lNumberOfChannelsSeen = mSeenChannelIdToNameMap.keySet()
																															.size();
		final int lNumberOfLayersNeeded = lNumberOfChannelsSeen;

		if (mClearVolumeRendererInterface == null || mClearVolumeRendererInterface.getNumberOfRenderLayers() < lNumberOfLayersNeeded
				|| mClearVolumeRendererInterface.getNativeType() != lNativeType)
		{
			System.out.println("Creating new Renderer!");
			System.out.format("Volume: nb channels seen: %d \n",
												lNumberOfChannelsSeen);
			System.out.format("Volume: type: %s \n", lNativeType);
			System.out.format("Volume: bytes per voxel: %d \n",
												Size.of(lNativeType));
			System.out.format("Renderer: nb of layers: %d \n",
												mClearVolumeRendererInterface.getNumberOfRenderLayers());
			System.out.format("Renderer: type: %s \n",
												mClearVolumeRendererInterface.getNativeType());
			createRenderer(lNativeType, lNumberOfLayersNeeded);
		}

		final int lNumberOfRenderLayers = mClearVolumeRendererInterface.getNumberOfRenderLayers();
		final int lRenderLayer = lChannelID % lNumberOfRenderLayers;

		mClearVolumeRendererInterface.setCurrentRenderLayer(lRenderLayer);

		TransferFunction lTransferFunction;
		final float[] lColor = pVolume.getColor();
		if (lColor != null)
		{
			lTransferFunction = TransferFunctions.getGradientForColor(lColor);
			mClearVolumeRendererInterface.setTransferFunction(lTransferFunction);
		}

		mClearVolumeRendererInterface.setVolumeDataBuffer(lRenderLayer,
																											pVolume);

		mClearVolumeRendererInterface.requestDisplay();

		mClearVolumeRendererInterface.waitToFinishAllDataBufferCopy(mWaitForCopyTimeout,
																																mTimeUnit);

		if (getRelaySink() != null)
			getRelaySink().sendVolume(pVolume);
		else
			pVolume.makeAvailableToManager();/**/

	}

	private void createRenderer(NativeTypeEnum pNativeTypeEnum,
															int pNumberOfLayers)
	{
		mSwitchingRenderers = true;
		try
		{
			if (mClearVolumeRendererInterface != null)
				mClearVolumeRendererInterface.close();
		}
		catch (final Throwable e)
		{
		}

		mClearVolumeRendererInterface = ClearVolumeRendererFactory.newBestRenderer(	mRequestedWindowTitle,
																																								mRequestedWindowWidth,
																																								mRequestedWindowHeight,
																																								pNativeTypeEnum,
																																								mRequestedWindowWidth,
																																								mRequestedWindowHeight,
																																								pNumberOfLayers,
																																								false);
		mClearVolumeRendererInterface.setVisible(true);

		try
		{
			if (mVolumeManager != null)
				mVolumeManager.close();
		}
		catch (final Throwable e)
		{
		}

		mVolumeManager = mClearVolumeRendererInterface.createCompatibleVolumeManager(mMaxNumberOfAvailableVolumes);

		mSwitchingRenderers = false;
	}

	@Override
	public VolumeManager getManager()
	{
		if (!(getRelaySink() instanceof NullVolumeSink))
			if (getRelaySink() != null)
				return getRelaySink().getManager();
		return mVolumeManager;
	}

	public void setVisible(boolean pIsVisible)
	{
		if (mClearVolumeRendererInterface != null)
			mClearVolumeRendererInterface.setVisible(pIsVisible);
	}

	public boolean isShowing()
	{
		try
		{
			while (mSwitchingRenderers)
				Thread.sleep(1);
		}
		catch (final InterruptedException e)
		{
		}
		if (mClearVolumeRendererInterface != null)
			return mClearVolumeRendererInterface.isShowing();
		return false;
	}

	public boolean isRendererCreated()
	{
		return mClearVolumeRendererInterface != null;
	}

	@Override
	public void close() throws ClearVolumeException
	{
		if (mClearVolumeRendererInterface != null)
			mClearVolumeRendererInterface.close();
	}

	public ClearVolumeRendererInterface getClearVolumeRenderer()
	{
		return mClearVolumeRendererInterface;
	}

}
