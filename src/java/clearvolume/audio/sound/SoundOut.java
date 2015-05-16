package clearvolume.audio.sound;

import gnu.trove.list.array.TByteArrayList;

import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Simple single-class facade on top of the sound output JavaSound API.
 *
 * @author Loic Royer (2015)
 *
 */
public class SoundOut implements AutoCloseable
{

	private final AudioFormat mAudioFormat;

	private SourceDataLine mSourceDataLine;

	private final Object mLock = new Object();

	/**
	 * Default constructor with 44100f samples per second, 16 bit samples, and 1
	 * channel.
	 */
	public SoundOut()
	{
		this(44100f, 16, 1);
	}

	/**
	 * Constructor that can configure the sample rate, sample bit size, and number
	 * of channels.
	 * 
	 * @param pSampleRate
	 *          sample rate
	 * @param pSampleSizeInBits
	 *          sample size in bits
	 * @param pNumberOfChannels
	 *          numebr of channels
	 */
	public SoundOut(float pSampleRate,
									int pSampleSizeInBits,
									int pNumberOfChannels)
	{
		super();
		mAudioFormat = new AudioFormat(	pSampleRate,
																		pSampleSizeInBits,
																		pNumberOfChannels,
																		true,
																		false);
	}

	/**
	 * Starts to process incoming samples.
	 * 
	 * @throws LineUnavailableException
	 *           thrown if line unavailable
	 */
	public void start() throws LineUnavailableException
	{
		synchronized (mLock)
		{
			mSourceDataLine = AudioSystem.getSourceDataLine(mAudioFormat);
			mSourceDataLine.open(mAudioFormat);
			mSourceDataLine.start();
		}
	}

	/**
	 * Stops to process incoming samples.
	 */
	public void stop()
	{
		synchronized (mLock)
		{
			mSourceDataLine.flush();
			mSourceDataLine.stop();
		}
	}

	TByteArrayList mTemporaryBuffer = new TByteArrayList();
	byte[] mTemporaryArray;

	/**
	 * Plays a given double buffer of a given length.
	 * 
	 * @param pBuffer
	 *          double buffer
	 * @param pLength
	 *          length of buffer to use
	 */
	public void play(final double[] pBuffer, final int pLength)
	{
		play(pBuffer, pLength, null);
	}

	/**
	 * Plays a given double buffer of a given length - and releases a Lock just
	 * before sending the samples to the audio driver.
	 * 
	 * @param pBuffer
	 *          double buffer
	 * @param pLength
	 *          length of buffer to use
	 * @param pReentrantLock
	 *          lock to release
	 */
	public void play(	final double[] pBuffer,
										final int pLength,
										ReentrantLock pReentrantLock)
	{
		mTemporaryBuffer.reset();
		for (int i = 0; i < pBuffer.length; i++)
		{
			final double lDoubleValue = pBuffer[i];
			final int lIntValue = (int) (lDoubleValue * (1 << 15));
			final byte a = (byte) ((lIntValue) & 0xff);
			final byte b = (byte) ((lIntValue >> 8) & 0xff);

			mTemporaryBuffer.add(a);
			mTemporaryBuffer.add(b);
		}

		if (mTemporaryArray == null || mTemporaryArray.length < mTemporaryBuffer.size())
			mTemporaryArray = new byte[mTemporaryBuffer.size()];
		mTemporaryArray = mTemporaryBuffer.toArray(mTemporaryArray);

		play(mTemporaryArray, 2 * pLength, pReentrantLock);
	}

	/**
	 * Plays a given float buffer of a given length.
	 * 
	 * @param pBuffer
	 *          float buffer
	 * @param pLength
	 *          length of buffer to use
	 */
	public void play(final float[] pBuffer, final int pLength)
	{
		play(pBuffer, pLength, null);
	}

	/**
	 * Plays a given float buffer of a given length - and releases a Lock just
	 * before sending the samples to the audio driver.
	 * 
	 * @param pBuffer
	 *          float buffer
	 * @param pLength
	 *          length of buffer to use
	 * @param pReentrantLock
	 *          lock to release
	 */
	public void play(	final float[] pBuffer,
										final int pLength,
										ReentrantLock pReentrantLock)
	{
		mTemporaryBuffer.reset();
		for (int i = 0; i < pBuffer.length; i++)
		{
			final double lDoubleValue = pBuffer[i];
			final int lIntValue = (int) (lDoubleValue * (1 << 15));
			final byte a = (byte) ((lIntValue) & 0xff);
			final byte b = (byte) ((lIntValue >> 8) & 0xff);

			mTemporaryBuffer.add(a);
			mTemporaryBuffer.add(b);
		}

		if (mTemporaryArray == null || mTemporaryArray.length < mTemporaryBuffer.size())
			mTemporaryArray = new byte[mTemporaryBuffer.size()];
		mTemporaryArray = mTemporaryBuffer.toArray(mTemporaryArray);

		play(mTemporaryArray, 2 * pLength, pReentrantLock);
	}

	/**
	 * Plays a given byte buffer of a given length.
	 * 
	 * @param pBuffer
	 *          byte buffer
	 * @param pLength
	 *          length of buffer to use
	 */
	public void play(final byte[] pBuffer, final int pLength)
	{
		play(pBuffer, pLength, null);
	}

	public void play(	final byte[] pBuffer,
										final int pLength,
										ReentrantLock pReentrantLock)
	{
		int lLength;
		if (pLength > pBuffer.length)
		{
			lLength = pBuffer.length;
		}
		else
		{
			lLength = pLength;
		}

		if (pReentrantLock != null && pReentrantLock.isHeldByCurrentThread())
			pReentrantLock.unlock();
		synchronized (mLock)
		{
			mSourceDataLine.write(pBuffer, 0, lLength);
		}
	}

	public static byte[] intArrayToByte(final int[] pIntArray,
																			final byte[] pByteArray)
	{
		if (2 * pIntArray.length > pByteArray.length)
		{
			return null;
		}
		for (int i = 0; i < pIntArray.length; ++i)
		{
			pByteArray[2 * i] = (byte) (pIntArray[i] % 0xFF);
			pByteArray[2 * i + 1] = (byte) ((pIntArray[i] >> 8) % 0xFF);
		}

		return pByteArray;
	}

	public float getSampleRate()
	{
		return mAudioFormat.getSampleRate();
	}

	@Override
	public void close()
	{
		mSourceDataLine.close();
	}

}