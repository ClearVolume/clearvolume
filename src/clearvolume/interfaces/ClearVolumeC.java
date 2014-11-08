package clearvolume.interfaces;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bridj.Pointer;
import org.bridj.Pointer.Releaser;
import org.bridj.PointerIO;

import clearvolume.network.serialization.ClearVolumeSerialization;
import clearvolume.network.server.ClearVolumeTCPServerSink;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;
import clearvolume.volume.sink.AsynchronousVolumeSinkAdapter;
import clearvolume.volume.sink.NullVolumeSink;
import clearvolume.volume.sink.VolumeSinkInterface;
import clearvolume.volume.sink.filter.ChannelFilterSink;
import clearvolume.volume.sink.filter.gui.ChannelFilterSinkJFrame;
import clearvolume.volume.sink.renderer.ClearVolumeRendererSink;
import clearvolume.volume.sink.timeshift.TimeShiftingSink;
import clearvolume.volume.sink.timeshift.gui.TimeShiftingSinkJFrame;

public class ClearVolumeC
{
	private static final long cTimeShiftSoftHoryzon = 50;
	private static final long cTimeShiftHardHoryzon = 100;

	private static Throwable sLastThrowableException = null;

	private static ConcurrentHashMap<Integer, ClearVolumeRendererInterface> sIDToRendererMap = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, ClearVolumeTCPServerSink> sIDToServerMap = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, VolumeSinkInterface> sIDToVolumeSink = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, AsynchronousVolumeSinkAdapter> sIDToVolumeAsyncSink = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, TimeShiftingSink> sIDToTimeShiftingSink = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, TimeShiftingSinkJFrame> sIDToTimeShiftingSinkJFrame = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, ChannelFilterSink> sIDToChannelFilterSink = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, ChannelFilterSinkJFrame> sIDToChannelFilterSinkJFrame = new ConcurrentHashMap<>();

	private static ConcurrentHashMap<Integer, VolumeManager> sIDToVolumeManager = new ConcurrentHashMap<>();

	private static ConcurrentHashMap<Integer, double[]> sIDToVolumeDimensionsInRealUnit = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, Integer> sIDToVolumeTimeIndex = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, Double> sIDToVolumeTimeInSeconds = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, String> sChannelIDToChannelName = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, float[]> sChannelIDToChannelColor = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, float[]> sChannelIDToChannelViewMatrix = new ConcurrentHashMap<>();

	private static volatile int sMaxAvailableVolumes = 10;
	private static volatile int sMaxQueueLength = 10;
	private static volatile long sMaxMillisecondsToWaitForCopy = 10;
	private static volatile long sMaxMillisecondsToWait = 10;

	public static final String getLastExceptionMessage()
	{
		if (sLastThrowableException == null)
			return "none";
		else
			return sLastThrowableException.getLocalizedMessage();
	}

	public static void clearLastExceptionMessage()
	{
		sLastThrowableException = null;
	}

	public static int[] getRendererList()
	{
		List<Integer> lRendererList = Collections.list(sIDToRendererMap.keys());
		int[] lRendererArray = new int[lRendererList.size()];
		int i = 0;
		for (Integer lId : lRendererList)
			lRendererArray[i++] = lId;

		return lRendererArray;
	}

	public static int createRenderer(	final int pRendererId,
																		final int pWindowWidth,
																		final int pWindowHeight,
																		final int pBytesPerVoxel,
																		final int pMaxTextureWidth,
																		final int pMaxTextureHeight)
	{
		return createRenderer(pRendererId,
													pWindowWidth,
													pWindowHeight,
													pBytesPerVoxel,
													pMaxTextureWidth,
													pMaxTextureHeight,
													true,
													true);
	}

	public static int createRenderer(	final int pRendererId,
																		final int pWindowWidth,
																		final int pWindowHeight,
																		final int pBytesPerVoxel,
																		final int pMaxTextureWidth,
																		final int pMaxTextureHeight,
																		final boolean pTimeShift,
																		final boolean pChannelSelector)
	{
		try
		{
			ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolume[ID=" + pRendererId
																																																					+ "]",
																																																			pWindowWidth,
																																																			pWindowHeight,
																																																			pBytesPerVoxel,
																																																			pMaxTextureWidth,
																																																			pMaxTextureHeight);

			VolumeManager lVolumeManager = lClearVolumeRenderer.createCompatibleVolumeManager(sMaxQueueLength);
			sIDToVolumeManager.put(pRendererId, lVolumeManager);

			lClearVolumeRenderer.setVisible(true);
			sIDToRendererMap.put(pRendererId, lClearVolumeRenderer);

			ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
																																											lClearVolumeRenderer.createCompatibleVolumeManager(sMaxAvailableVolumes),
																																											sMaxMillisecondsToWaitForCopy,
																																											TimeUnit.MILLISECONDS);
			VolumeSinkInterface lSinkAfterAsynchronousVolumeSinkAdapter = lClearVolumeRendererSink;

			TimeShiftingSink lTimeShiftingSink = null;
			TimeShiftingSinkJFrame lTimeShiftingSinkJFrame = null;
			if (pTimeShift)
			{
				lTimeShiftingSink = new TimeShiftingSink(	cTimeShiftSoftHoryzon,
																									cTimeShiftHardHoryzon);
				sIDToTimeShiftingSink.put(pRendererId, lTimeShiftingSink);

				lTimeShiftingSinkJFrame = new TimeShiftingSinkJFrame(lTimeShiftingSink);
				lTimeShiftingSinkJFrame.setVisible(true);
				sIDToTimeShiftingSinkJFrame.put(pRendererId,
																				lTimeShiftingSinkJFrame);

				lTimeShiftingSink.setRelaySink(lSinkAfterAsynchronousVolumeSinkAdapter);

				lClearVolumeRendererSink.setRelaySink(new NullVolumeSink());

				lSinkAfterAsynchronousVolumeSinkAdapter = lTimeShiftingSink;
			}

			ChannelFilterSink lChannelFilterSink = null;
			ChannelFilterSinkJFrame lChannelFilterSinkJFrame = null;
			if (pChannelSelector)
			{
				lChannelFilterSink = new ChannelFilterSink();
				sIDToChannelFilterSink.put(pRendererId, lChannelFilterSink);

				lChannelFilterSinkJFrame = new ChannelFilterSinkJFrame(lChannelFilterSink);
				lChannelFilterSinkJFrame.setVisible(true);
				sIDToChannelFilterSinkJFrame.put(	pRendererId,
																					lChannelFilterSinkJFrame);

				lChannelFilterSink.setRelaySink(lSinkAfterAsynchronousVolumeSinkAdapter);

				lSinkAfterAsynchronousVolumeSinkAdapter = lChannelFilterSink;
			}

			AsynchronousVolumeSinkAdapter lAsynchronousVolumeSinkAdapter = new AsynchronousVolumeSinkAdapter(	lSinkAfterAsynchronousVolumeSinkAdapter,
																																																				sMaxQueueLength,
																																																				sMaxMillisecondsToWait,
																																																				TimeUnit.MILLISECONDS);
			lAsynchronousVolumeSinkAdapter.start();

			sIDToVolumeAsyncSink.put(	pRendererId,
																lAsynchronousVolumeSinkAdapter);

			sIDToVolumeSink.put(pRendererId, lAsynchronousVolumeSinkAdapter);

			return 0;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
			return 1;
		}

	}

	public static int destroyRenderer(final int pRendererId)
	{
		try
		{
			AsynchronousVolumeSinkAdapter lAsynchronousVolumeSinkAdapter = sIDToVolumeAsyncSink.get(pRendererId);
			if (lAsynchronousVolumeSinkAdapter != null)
			{
				lAsynchronousVolumeSinkAdapter.stop();
				lAsynchronousVolumeSinkAdapter.waitForStop();
				sIDToVolumeAsyncSink.remove(pRendererId);
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 1;
		}

		try
		{
			TimeShiftingSink lTimeShiftingSink = sIDToTimeShiftingSink.get(pRendererId);
			if (lTimeShiftingSink != null)
			{
				TimeShiftingSinkJFrame lTimeShiftingSinkJFrame = sIDToTimeShiftingSinkJFrame.get(pRendererId);
				lTimeShiftingSinkJFrame.setVisible(false);
				lTimeShiftingSinkJFrame.dispose();
				lTimeShiftingSink.close();
				sIDToTimeShiftingSinkJFrame.remove(pRendererId);
				sIDToTimeShiftingSink.remove(pRendererId);
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 2;
		}

		try
		{
			ChannelFilterSink lChannelFilterSink = sIDToChannelFilterSink.get(pRendererId);
			if (lChannelFilterSink != null)
			{
				ChannelFilterSinkJFrame lChannelFilterSinkJFrame = sIDToChannelFilterSinkJFrame.get(pRendererId);
				lChannelFilterSinkJFrame.setVisible(false);
				lChannelFilterSinkJFrame.dispose();
				lChannelFilterSink.close();
				sIDToChannelFilterSinkJFrame.remove(pRendererId);
				sIDToChannelFilterSink.remove(pRendererId);
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 3;
		}

		try
		{
			VolumeManager lVolumeManager = sIDToVolumeManager.get(pRendererId);
			if (lVolumeManager != null)
			{
				sIDToVolumeManager.remove(pRendererId);
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 4;
		}

		try
		{
			ClearVolumeRendererInterface lClearVolumeRenderer = sIDToRendererMap.get(pRendererId);
			if (lClearVolumeRenderer != null)
			{
				sIDToRendererMap.remove(pRendererId);
				lClearVolumeRenderer.waitToFinishDataBufferCopy(1,
																												TimeUnit.SECONDS);

				lClearVolumeRenderer.close();
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 5;
		}

		return 0;
	}

	public static int createServer(final int pServerId)
	{
		try
		{
			VolumeManager lVolumeManager = new VolumeManager(sMaxAvailableVolumes);
			sIDToVolumeManager.put(pServerId, lVolumeManager);

			ClearVolumeTCPServerSink lClearVolumeTCPServerSink = new ClearVolumeTCPServerSink(new NullVolumeSink(),
																																												sMaxAvailableVolumes);

			SocketAddress lServerSocketAddress = new InetSocketAddress(ClearVolumeSerialization.cStandardTCPPort);
			if (!lClearVolumeTCPServerSink.open(lServerSocketAddress))
			{
				return 3;
			}
			if (!lClearVolumeTCPServerSink.start())
			{
				return 4;
			}

			sIDToServerMap.put(pServerId, lClearVolumeTCPServerSink);
			sIDToVolumeSink.put(pServerId, lClearVolumeTCPServerSink);

			return 0;

		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
			return 1;
		}

	}

	public static int destroyServer(final int pServerId)
	{
		try
		{
			ClearVolumeTCPServerSink lClearVolumeTCPServerSink = sIDToServerMap.get(pServerId);
			if (lClearVolumeTCPServerSink != null)
			{
				sIDToServerMap.remove(pServerId);
				lClearVolumeTCPServerSink.stop();
				lClearVolumeTCPServerSink.close();
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 1;
		}

		try
		{
			VolumeSinkInterface lVolumeSink = sIDToVolumeSink.get(pServerId);
			if (lVolumeSink != null)
			{
				sIDToVolumeSink.remove(pServerId);
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 2;
		}

		try
		{
			VolumeManager lVolumeManager = sIDToVolumeManager.get(pServerId);
			if (lVolumeManager != null)
			{
				sIDToVolumeManager.remove(pServerId);
			}
			return 0;
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 3;
		}

	}

	public static int setVoxelDimensionsInRealUnits(final int pSinkId,
																									final double pWidthInRealUnits,
																									final double pHeightInRealUnits,
																									final double pDepthInRealUnits)
	{
		try
		{
			sIDToVolumeDimensionsInRealUnit.put(pSinkId, new double[]
			{ pWidthInRealUnits, pHeightInRealUnits, pDepthInRealUnits });
			return 0;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
			return 1;
		}
	}

	public static int setVolumeIndexAndTime(final int pSinkId,
																					final int pVolumeIndex,
																					final double pVolumeTimeInSeconds)
	{
		try
		{
			sIDToVolumeTimeIndex.put(pSinkId, pVolumeIndex);
			sIDToVolumeTimeInSeconds.put(pSinkId, pVolumeTimeInSeconds);
			return 0;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
			return 1;
		}
	}

	public static int setChannelName(	final int pChannelID,
																		final String pChanelName)
	{
		try
		{
			sChannelIDToChannelName.put(pChannelID,
																	new String(pChanelName).intern());
			return 0;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
			return 1;
		}
	}

	public static int setChannelColor(final int pChannelID,
																		final float[] pChanelColor)
	{
		try
		{
			sChannelIDToChannelColor.put(	pChannelID,
																		Arrays.copyOf(pChanelColor,
																									pChanelColor.length));
			return 0;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
			return 1;
		}
	}

	public static int setChannelViewMatrix(	final int pChannelID,
																					final float[] pViewMatrix)
	{
		try
		{
			sChannelIDToChannelViewMatrix.put(pChannelID,
																				Arrays.copyOf(pViewMatrix,
																											pViewMatrix.length));
			return 0;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
			return 1;
		}
	}

	public static int send8bitUINTVolumeDataToSink(	final int pSinkId,
																									final int pChannelId,
																									final long pBufferAddress,
																									final long pBufferLength,
																									final int pWidthInVoxels,
																									final int pHeightInVoxels,
																									final int pDepthInVoxels)
	{
		Pointer<Byte> lBridJPointer = getBridJPointer(pBufferAddress,
																									pBufferLength,
																									Byte.class);

		ByteBuffer lByteBuffer = lBridJPointer.getByteBuffer();

		return send8bitUINTVolumeDataToSink(pSinkId,
																				pChannelId,
																				lByteBuffer,
																				pWidthInVoxels,
																				pHeightInVoxels,
																				pDepthInVoxels);
	}

	public static int send8bitUINTVolumeDataToSink(	final int pSinkId,
																									final int pChannelId,
																									ByteBuffer pByteBuffer,
																									final int pWidthInVoxels,
																									final int pHeightInVoxels,
																									final int pDepthInVoxels)
	{
		try
		{
			VolumeManager lVolumeManager = sIDToVolumeManager.get(pSinkId);

			Volume<Byte> lRequestedVolume = lVolumeManager.requestAndWaitForVolume(	sMaxMillisecondsToWait,
																																							TimeUnit.MILLISECONDS,
																																							Byte.class,
																																							1,
																																							pWidthInVoxels,
																																							pHeightInVoxels,
																																							pDepthInVoxels);

			String lChannelName = sChannelIDToChannelName.get(pChannelId);
			if (lChannelName != null)
				lRequestedVolume.setChannelName(lChannelName);

			float[] lChannelColor = sChannelIDToChannelColor.get(pChannelId);
			if (lChannelColor != null)
				lRequestedVolume.setColor(lChannelColor);

			float[] lViewMatrix = sChannelIDToChannelViewMatrix.get(pChannelId);
			if (lViewMatrix != null)
				lRequestedVolume.setViewMatrix(lViewMatrix);

			Integer lTimeIndex = sIDToVolumeTimeIndex.get(pSinkId);
			if (lTimeIndex != null)
				lRequestedVolume.setTimeIndex(lTimeIndex);

			Double lTimeInSeconds = sIDToVolumeTimeInSeconds.get(pSinkId);
			if (lTimeInSeconds != null)
				lRequestedVolume.setTimeInSeconds(lTimeInSeconds);

			double[] lDimensionsInRealUnits = sIDToVolumeDimensionsInRealUnit.get(pSinkId);
			if (lDimensionsInRealUnits != null)
				lRequestedVolume.setVoxelSizeInRealUnits(	"um",
																									lDimensionsInRealUnits);

			ByteBuffer lVolumeData = lRequestedVolume.getDataBuffer();
			lVolumeData.clear();
			pByteBuffer.rewind();
			lVolumeData.put(pByteBuffer);

			VolumeSinkInterface lVolumeSinkInterface = sIDToVolumeSink.get(pSinkId);
			lVolumeSinkInterface.sendVolume(lRequestedVolume);

			return 0;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
			return 1;
		}
	}

	public static int send16bitUINTVolumeDataToSink(final int pSinkId,
																									final int pChannelId,
																									final long pBufferAddress,
																									final long pBufferLength,
																									final int pWidthInVoxels,
																									final int pHeightInVoxels,
																									final int pDepthInVoxels)
	{
		Pointer<Byte> lBridJPointer = getBridJPointer(pBufferAddress,
																									pBufferLength,
																									Byte.class);

		ByteBuffer lByteBuffer = lBridJPointer.getByteBuffer();

		return send16bitUINTVolumeDataToSink(	pSinkId,
																					pChannelId,
																					lByteBuffer,
																					pWidthInVoxels,
																					pHeightInVoxels,
																					pDepthInVoxels);
	}

	public static int send16bitUINTVolumeDataToSink(final int pSinkId,
																									final int pChannelId,
																									final ByteBuffer pByteBuffer,
																									final int pWidthInVoxels,
																									final int pHeightInVoxels,
																									final int pDepthInVoxels)
	{
		try
		{
			VolumeManager lVolumeManager = sIDToVolumeManager.get(pSinkId);

			Volume<Character> lRequestedVolume = lVolumeManager.requestAndWaitForVolume(sMaxMillisecondsToWait,
																																									TimeUnit.MILLISECONDS,
																																									Character.class,
																																									1,
																																									pWidthInVoxels,
																																									pHeightInVoxels,
																																									pDepthInVoxels);

			setCurrentVolumeMetadata(pSinkId, pChannelId, lRequestedVolume);

			ByteBuffer lVolumeData = lRequestedVolume.getDataBuffer();
			lVolumeData.clear();
			pByteBuffer.rewind();
			lVolumeData.put(pByteBuffer);

			VolumeSinkInterface lVolumeSinkInterface = sIDToVolumeSink.get(pSinkId);
			lVolumeSinkInterface.sendVolume(lRequestedVolume);

			return 0;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
			return 1;
		}
	}

	private static void setCurrentVolumeMetadata(	final int pSinkId,
																								final int pChannelId,
																								Volume<Character> lRequestedVolume)
	{
		String lChannelName = sChannelIDToChannelName.get(pChannelId);
		if (lChannelName != null)
			lRequestedVolume.setChannelName(lChannelName);

		float[] lChannelColor = sChannelIDToChannelColor.get(pChannelId);
		if (lChannelColor != null)
			lRequestedVolume.setColor(lChannelColor);

		float[] lViewMatrix = sChannelIDToChannelViewMatrix.get(pChannelId);
		if (lViewMatrix != null)
			lRequestedVolume.setViewMatrix(lViewMatrix);

		Integer lTimeIndex = sIDToVolumeTimeIndex.get(pSinkId);
		if (lTimeIndex != null)
			lRequestedVolume.setTimeIndex(lTimeIndex);

		Double lTimeInSeconds = sIDToVolumeTimeInSeconds.get(pSinkId);
		if (lTimeInSeconds != null)
			lRequestedVolume.setTimeInSeconds(lTimeInSeconds);

		double[] lDimensionsInRealUnits = sIDToVolumeDimensionsInRealUnit.get(pSinkId);
		if (lDimensionsInRealUnits != null)
			lRequestedVolume.setVoxelSizeInRealUnits(	"um",
																								lDimensionsInRealUnits);
	}

	// jbyte* bbuf_in; jbyte* bbuf_out;
	// bbuf_in = (*env)->GetDirectBufferAddress(env, buf1);
	// bbuf_out= (*env)->GetDirectBufferAddress(env, buf2);
	// The return type of GetDirectBufferAddress is void*, you need to cast it to
	// a jbyte*: bbuf_in = (jbyte*)(env*)->GetDirectBufferAddress(env, buf1); //C
	// bbuf_in = (jbyte*)env->GetDirectBufferAddress(buf1); //c++

	private static final <T> Pointer<T> getBridJPointer(final long pBufferAddress,
																											final long pBufferLength,
																											final Class<T> pTargetClass)
	{

		PointerIO<?> lPointerIO = PointerIO.getInstance(pTargetClass);
		Releaser lReleaser = new Releaser()
		{

			@Override
			public void release(Pointer<?> pP)
			{
				// can't control life-cycle of
			}
		};

		@SuppressWarnings("unchecked")
		Pointer<T> lPointerToAddress = (Pointer<T>) Pointer.pointerToAddress(	pBufferAddress,
																																					pBufferLength,
																																					lPointerIO,
																																					lReleaser);

		return lPointerToAddress;

	}

	/**********************************/

	public static int requestVolumeBuffer(final int pSinkId,
																				final int pChannelId,
																				ByteBuffer pByteBuffer,
																				final int pWidthInVoxels,
																				final int pHeightInVoxels,
																				final int pDepthInVoxels)
	{
		try
		{
			VolumeManager lVolumeManager = sIDToVolumeManager.get(pSinkId);

			Volume<Byte> lRequestedVolume = lVolumeManager.requestAndWaitForVolume(	sMaxMillisecondsToWait,
																																							TimeUnit.MILLISECONDS,
																																							Byte.class,
																																							1,
																																							pWidthInVoxels,
																																							pHeightInVoxels,
																																							pDepthInVoxels);

			lRequestedVolume.setChannelID(pChannelId);

			String lChannelName = sChannelIDToChannelName.get(pChannelId);
			if (lChannelName != null)
				lRequestedVolume.setChannelName(lChannelName);

			float[] lChannelColor = sChannelIDToChannelColor.get(pChannelId);
			if (lChannelColor != null)
				lRequestedVolume.setColor(lChannelColor);

			float[] lViewMatrix = sChannelIDToChannelViewMatrix.get(pChannelId);
			if (lViewMatrix != null)
				lRequestedVolume.setViewMatrix(lViewMatrix);

			Integer lTimeIndex = sIDToVolumeTimeIndex.get(pSinkId);
			if (lTimeIndex != null)
				lRequestedVolume.setTimeIndex(lTimeIndex);

			Double lTimeInSeconds = sIDToVolumeTimeInSeconds.get(pSinkId);
			if (lTimeInSeconds != null)
				lRequestedVolume.setTimeInSeconds(lTimeInSeconds);

			double[] lDimensionsInRealUnits = sIDToVolumeDimensionsInRealUnit.get(pSinkId);
			if (lDimensionsInRealUnits != null)
				lRequestedVolume.setVoxelSizeInRealUnits(	"um",
																									lDimensionsInRealUnits);

			ByteBuffer lVolumeData = lRequestedVolume.getDataBuffer();
			lVolumeData.clear();
			pByteBuffer.rewind();
			lVolumeData.put(pByteBuffer);

			VolumeSinkInterface lVolumeSinkInterface = sIDToVolumeSink.get(pSinkId);
			lVolumeSinkInterface.sendVolume(lRequestedVolume);

			return 0;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
			return 1;
		}
	}
}
