package clearvolume.interfaces;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

	private static ConcurrentHashMap<String, ClearVolumeRendererInterface> sNameToRendererMap = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, VolumeSinkInterface> sNameToVolumeSink = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, VolumeManager> sNameToVolumeManager = new ConcurrentHashMap<>();

	private static volatile long sMaxMillisecondsToWaitForCopy = 10;
	private static volatile int sMaxQueueLength = 10;
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

	public static String[] getRendererList()
	{
		List<String> lRendererList = Collections.list(sNameToRendererMap.keys());
		return (String[]) lRendererList.toArray();
	}

	public static void createRenderer(final String pRendererName,
																		final int pWindowWidth,
																		final int pWindowHeight,
																		final int pBytesPerVoxel,
																		final int pMaxTextureWidth,
																		final int pMaxTextureHeight)
	{
		ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	pRendererName,
																																																		pWindowWidth,
																																																		pWindowHeight,
																																																		pBytesPerVoxel,
																																																		pMaxTextureWidth,
																																																		pMaxTextureHeight);
		lClearVolumeRenderer.setVisible(true);
		sNameToRendererMap.put(pRendererName, lClearVolumeRenderer);

		ClearVolumeRendererSink lClearVolumeRendererSink = new ClearVolumeRendererSink(	lClearVolumeRenderer,
																																										sMaxMillisecondsToWaitForCopy,
																																										TimeUnit.MILLISECONDS);

		AsynchronousVolumeSinkAdapter lAsynchronousVolumeSinkAdapter = new AsynchronousVolumeSinkAdapter(	lClearVolumeRendererSink,
																																																			sMaxQueueLength,
																																																			sMaxMillisecondsToWait,
																																																			TimeUnit.MILLISECONDS);
		lAsynchronousVolumeSinkAdapter.start();

		sNameToVolumeSink.put(pRendererName,
													lAsynchronousVolumeSinkAdapter);

		VolumeManager lVolumeManager = new VolumeManager(sMaxQueueLength);

		sNameToVolumeManager.put(pRendererName, lVolumeManager);
	}

	public static void send8bitUINTVolumeDataToSink(String pSinkName,
																							ByteBuffer pByteBuffer,
																							long... pDimensions)
	{
		VolumeManager lVolumeManager = sNameToVolumeManager.get(pSinkName);

		Volume<Byte> lRequestedVolume = lVolumeManager.requestAndWaitForVolume(	sMaxMillisecondsToWait,
																																						TimeUnit.MILLISECONDS,
																																						Byte.class,
																																						pDimensions);

		ByteBuffer lVolumeData = lRequestedVolume.getVolumeData();
		lVolumeData.clear();
		pByteBuffer.rewind();
		lVolumeData.put(pByteBuffer);

		VolumeSinkInterface lVolumeSinkInterface = sNameToVolumeSink.get(pSinkName);
		lVolumeSinkInterface.sendVolume(lRequestedVolume);
	}

	public static void send16bitUINTVolumeDataToSink(	String pSinkName,
																								ByteBuffer pByteBuffer,
																								long... pDimensions)
	{
		VolumeManager lVolumeManager = sNameToVolumeManager.get(pSinkName);

		Volume<Character> lRequestedVolume = lVolumeManager.requestAndWaitForVolume(sMaxMillisecondsToWait,
																																								TimeUnit.MILLISECONDS,
																																								Character.class,
																																								pDimensions);

		ByteBuffer lVolumeData = lRequestedVolume.getVolumeData();
		lVolumeData.clear();
		pByteBuffer.rewind();
		lVolumeData.put(pByteBuffer);

		VolumeSinkInterface lVolumeSinkInterface = sNameToVolumeSink.get(pSinkName);
		lVolumeSinkInterface.sendVolume(lRequestedVolume);
	}

	public static void destroyRenderer(String pRendererName)
	{
		try
		{
			ClearVolumeRendererInterface lClearVolumeRenderer = sNameToRendererMap.get(pRendererName);
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
			VolumeSinkInterface lVolumeSink = sNameToVolumeSink.get(pRendererName);
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
			VolumeManager lVolumeManager = sNameToVolumeManager.get(pRendererName);
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

}
