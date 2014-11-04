package clearvolume.interfaces;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
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
import clearvolume.volume.sink.ClearVolumeRendererSink;
import clearvolume.volume.sink.VolumeSinkInterface;

public class ClearVolumeC
{
	private static Throwable sLastThrowableException = null;

	private static ConcurrentHashMap<Integer, ClearVolumeRendererInterface> sIDToRendererMap = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, ClearVolumeTCPServerSink> sIDToServerMap = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, VolumeSinkInterface> sIDToVolumeSink = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, AsynchronousVolumeSinkAdapter> sIDToVolumeAsyncSink = new ConcurrentHashMap<>();

	private static ConcurrentHashMap<Integer, VolumeManager> sIDToVolumeManager = new ConcurrentHashMap<>();

	private static ConcurrentHashMap<Integer, double[]> sIDToVolumeDimensionsInRealUnit = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, Integer> sIDToVolumeIndex = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, Double> sIDToVolumeTimeInSeconds = new ConcurrentHashMap<>();

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

			AsynchronousVolumeSinkAdapter lAsynchronousVolumeSinkAdapter = new AsynchronousVolumeSinkAdapter(	lClearVolumeRendererSink,
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
				sIDToVolumeAsyncSink.remove(lAsynchronousVolumeSinkAdapter);
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 1;
		}

		try
		{
			VolumeManager lVolumeManager = sIDToVolumeManager.get(pRendererId);
			if (lVolumeManager != null)
			{
				sIDToVolumeManager.remove(lVolumeManager);
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 2;
		}

		try
		{
			ClearVolumeRendererInterface lClearVolumeRenderer = sIDToRendererMap.get(pRendererId);
			if (lClearVolumeRenderer != null)
			{
				sIDToRendererMap.remove(lClearVolumeRenderer);
				lClearVolumeRenderer.waitToFinishDataBufferCopy(1,
																												TimeUnit.SECONDS);

				lClearVolumeRenderer.close();
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return 3;
		}

		return 0;
	}

	public static int createServer(final int pServerId)
	{
		try
		{
			VolumeManager lVolumeManager = new VolumeManager(sMaxAvailableVolumes);
			sIDToVolumeManager.put(pServerId, lVolumeManager);

			ClearVolumeTCPServerSink lClearVolumeTCPServerSink = new ClearVolumeTCPServerSink(lVolumeManager,
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
				sIDToServerMap.remove(lClearVolumeTCPServerSink);
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
				sIDToVolumeSink.remove(lVolumeSink);
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
				sIDToVolumeManager.remove(lVolumeManager);
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
			sIDToVolumeIndex.put(pSinkId, pVolumeIndex);
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

			lRequestedVolume.setVolumeChannelID(pChannelId);
			final Integer lIndex = sIDToVolumeIndex.get(pSinkId);
			if (lIndex != null)
				lRequestedVolume.setIndex(lIndex);
			final Double lTimeInSeconds = sIDToVolumeTimeInSeconds.get(pSinkId);
			if (lTimeInSeconds != null)
				lRequestedVolume.setTime(lTimeInSeconds);

			double[] lDimensionsInRealUnit = sIDToVolumeDimensionsInRealUnit.get(pSinkId);
			if (lDimensionsInRealUnit != null)
				lRequestedVolume.setVolumeDimensionsInRealUnits("um",
																												lDimensionsInRealUnit);

			ByteBuffer lVolumeData = lRequestedVolume.getVolumeData();
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

			lRequestedVolume.setVolumeChannelID(pChannelId);
			final Integer lIndex = sIDToVolumeIndex.get(pSinkId);
			if (lIndex != null)
				lRequestedVolume.setIndex(lIndex);
			final Double lTimeInSeconds = sIDToVolumeTimeInSeconds.get(pSinkId);
			if (lTimeInSeconds != null)
				lRequestedVolume.setTime(lTimeInSeconds);

			double[] lDimensionsInRealUnit = sIDToVolumeDimensionsInRealUnit.get(pSinkId);
			if (lDimensionsInRealUnit != null)
				lRequestedVolume.setVolumeDimensionsInRealUnits("um",
																												lDimensionsInRealUnit);

			ByteBuffer lVolumeData = lRequestedVolume.getVolumeData();
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

	// jbyte* bbuf_in; jbyte* bbuf_out;
	// bbuf_in = (*env)->GetDirectBufferAddress(env, buf1);
	// bbuf_out= (*env)->GetDirectBufferAddress(env, buf2);
	// The return type of GetDirectBufferAddress is void*, you need to cast it to
	// a jbyte*: bbuf_in = (jbyte*)(env*)->GetDirectBufferAddress(env, buf1); //C
	// bbuf_in = (jbyte*)env->GetDirectBufferAddress(buf1); //c++ –

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

			lRequestedVolume.setVolumeChannelID(pChannelId);
			lRequestedVolume.setIndex(sIDToVolumeIndex.get(pSinkId));
			lRequestedVolume.setTime(sIDToVolumeTimeInSeconds.get(pSinkId));
			lRequestedVolume.setVolumeDimensionsInRealUnits("um",
																											sIDToVolumeDimensionsInRealUnit.get(pSinkId));

			ByteBuffer lVolumeData = lRequestedVolume.getVolumeData();
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
