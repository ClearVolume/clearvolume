package clearvolume.interfaces;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bridj.Pointer;
import org.bridj.Pointer.Releaser;
import org.bridj.PointerIO;

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

	private static ConcurrentHashMap<Integer, ClearVolumeRendererInterface> sNameToRendererMap = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, VolumeSinkInterface> sNameToVolumeSink = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer, VolumeManager> sNameToVolumeManager = new ConcurrentHashMap<>();

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
		List<Integer> lRendererList = Collections.list(sNameToRendererMap.keys());
		int[] lRendererArray = new int[lRendererList.size()];
		int i = 0;
		for (Integer lId : lRendererList)
			lRendererArray[i++] = lId;

		return lRendererArray;
	}

	public static void createRenderer(final int pRendererId,
																		final int pWindowWidth,
																		final int pWindowHeight,
																		final int pBytesPerVoxel,
																		final int pMaxTextureWidth,
																		final int pMaxTextureHeight)
	{
		try
		{
			ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolume",
																																																			pWindowWidth,
																																																			pWindowHeight,
																																																			pBytesPerVoxel,
																																																			pMaxTextureWidth,
																																																			pMaxTextureHeight);

			VolumeManager lVolumeManager = lClearVolumeRenderer.createCompatibleVolumeManager(sMaxQueueLength);
			sNameToVolumeManager.put(pRendererId, lVolumeManager);

			lClearVolumeRenderer.setVisible(true);
			sNameToRendererMap.put(pRendererId, lClearVolumeRenderer);

			ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
																																											lClearVolumeRenderer.createCompatibleVolumeManager(sMaxAvailableVolumes),
																																											sMaxMillisecondsToWaitForCopy,
																																											TimeUnit.MILLISECONDS);

			AsynchronousVolumeSinkAdapter lAsynchronousVolumeSinkAdapter = new AsynchronousVolumeSinkAdapter(	lClearVolumeRendererSink,
																																																				sMaxQueueLength,
																																																				sMaxMillisecondsToWait,
																																																				TimeUnit.MILLISECONDS);
			lAsynchronousVolumeSinkAdapter.start();

			sNameToVolumeSink.put(pRendererId, lAsynchronousVolumeSinkAdapter);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
		}

	}

	public static void send8bitUINTVolumeDataToSink(final int pSinkId,
																									final long pBufferAddress,
																									final long pBufferLength,
																									final long pWidth,
																									final long pHeight,
																									final long pDepth)
	{
		System.out.println("pSinkId=" + pSinkId);
		System.out.println("pBufferAddress=" + pBufferAddress);
		System.out.println("pBufferLength=" + pBufferLength);
		System.out.println("pWidth=" + pWidth);
		System.out.println("pHeight=" + pHeight);
		System.out.println("pDepth=" + pDepth);

		Pointer<Byte> lBridJPointer = getBridJPointer(pBufferAddress,
																									pBufferLength,
																									Byte.class);

		ByteBuffer lByteBuffer = lBridJPointer.getByteBuffer();

		send8bitUINTVolumeDataToSink(	pSinkId,
																	lByteBuffer,
																	1,
																	pWidth,
																	pHeight,
																	pDepth);
	}

	public static void send8bitUINTVolumeDataToSink(final int pSinkId,
																									ByteBuffer pByteBuffer,
																									long... pDimensions)
	{
		try
		{
			VolumeManager lVolumeManager = sNameToVolumeManager.get(pSinkId);

			Volume<Byte> lRequestedVolume = lVolumeManager.requestAndWaitForVolume(	sMaxMillisecondsToWait,
																																							TimeUnit.MILLISECONDS,
																																							Byte.class,
																																							pDimensions);

			ByteBuffer lVolumeData = lRequestedVolume.getVolumeData();
			lVolumeData.clear();
			pByteBuffer.rewind();
			lVolumeData.put(pByteBuffer);

			VolumeSinkInterface lVolumeSinkInterface = sNameToVolumeSink.get(pSinkId);
			lVolumeSinkInterface.sendVolume(lRequestedVolume);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
		}
	}

	public static void send16bitUINTVolumeDataToSink(	final int pSinkId,
																										final long pBufferAddress,
																										final long pBufferLength,
																										final long pWidth,
																										final long pHeight,
																										final long pDepth)
	{
		Pointer<Byte> lBridJPointer = getBridJPointer(pBufferAddress,
																									pBufferLength,
																									Byte.class);

		ByteBuffer lByteBuffer = lBridJPointer.getByteBuffer();

		send16bitUINTVolumeDataToSink(pSinkId,
																	lByteBuffer,
																	1,
																	pWidth,
																	pHeight,
																	pDepth);
	}

	public static void send16bitUINTVolumeDataToSink(	final int pSinkId,
																										final ByteBuffer pByteBuffer,
																										final long... pDimensions)
	{
		try
		{
			VolumeManager lVolumeManager = sNameToVolumeManager.get(pSinkId);

			Volume<Character> lRequestedVolume = lVolumeManager.requestAndWaitForVolume(sMaxMillisecondsToWait,
																																									TimeUnit.MILLISECONDS,
																																									Character.class,
																																									pDimensions);

			ByteBuffer lVolumeData = lRequestedVolume.getVolumeData();
			lVolumeData.clear();
			pByteBuffer.rewind();
			lVolumeData.put(pByteBuffer);

			VolumeSinkInterface lVolumeSinkInterface = sNameToVolumeSink.get(pSinkId);
			lVolumeSinkInterface.sendVolume(lRequestedVolume);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			sLastThrowableException = e;
		}
	}

	public static void destroyRenderer(final int pRendererId)
	{
		try
		{
			ClearVolumeRendererInterface lClearVolumeRenderer = sNameToRendererMap.get(pRendererId);
			if (lClearVolumeRenderer != null)
			{
				sNameToRendererMap.remove(lClearVolumeRenderer);
				lClearVolumeRenderer.close();
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
		}

		try
		{
			VolumeSinkInterface lVolumeSink = sNameToVolumeSink.get(pRendererId);
			if (lVolumeSink != null)
			{
				sNameToVolumeSink.remove(lVolumeSink);
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
		}

		try
		{
			VolumeManager lVolumeManager = sNameToVolumeManager.get(pRendererId);
			if (lVolumeManager != null)
			{
				sNameToVolumeManager.remove(lVolumeManager);
			}
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
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
}
