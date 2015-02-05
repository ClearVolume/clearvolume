package clearvolume.audio;

import gnu.trove.list.array.TByteArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class SoundOut
{

	private AudioFormat mAudioFormat;

	private SourceDataLine mSourceDataLine;

	public SoundOut()
	{
		super();
	}

	public void start()
	{
		try
		{
			mAudioFormat = getDefaultAudioFormat();
			/*************************************************************************
			 * DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class,
			 * mAudioFormat); mSourceDataLine = (SourceDataLine)
			 * AudioSystem.getLine(dataLineInfo);/
			 ************************************************************************/

			mSourceDataLine = AudioSystem.getSourceDataLine(getDefaultAudioFormat());

			mSourceDataLine.open(mAudioFormat);
			mSourceDataLine.start();

		}
		catch (final Exception e)
		{
			e.printStackTrace();
			System.out.println(e);
			System.exit(0);
		}
	}

	public void stop()
	{
		try
		{
			mSourceDataLine.flush();
			mSourceDataLine.stop();
			mSourceDataLine.close();
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			System.out.println(e);
			System.exit(0);
		}
	}

	TByteArrayList mTemporaryBuffer = new TByteArrayList();
	byte[] mTemporaryArray;

	public void play(final double[] pBuffer, final int pLength)
	{
		mTemporaryBuffer.reset();
		for (int i = 0; i < pBuffer.length; i++)
		{
			final double lDoubleValue = pBuffer[i];
			final int lIntValue = (int) (lDoubleValue * (1 << 15));
			byte a = (byte) ((lIntValue) & 0xff);
			byte b = (byte) ((lIntValue >> 8) & 0xff);
			// byte c = (byte) ((lIntValue >> 24) & 0xff);
			// System.out.format("a=%d, b=%d \n", a, b);

			mTemporaryBuffer.add(a);
			mTemporaryBuffer.add(b);
			// mTemporaryBuffer.add(c);
		}

		if (mTemporaryArray == null || mTemporaryArray.length < mTemporaryBuffer.size())
			mTemporaryArray = new byte[mTemporaryBuffer.size()];
		mTemporaryArray = mTemporaryBuffer.toArray(mTemporaryArray);

		play(mTemporaryArray, 2 * pLength);
	}

	public void play(final byte[] pBuffer, final int pLength)
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
		mSourceDataLine.write(pBuffer, 0, lLength);
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

	public AudioFormat getDefaultAudioFormat()
	{
		final float sampleRate = 44100f;
		// 8000,11025,16000,22050,44100
		final int sampleSizeInBits = 16;
		// 8,16
		final int channels = 1;
		// 1,2
		final boolean signed = true;
		// true,false
		final boolean bigEndian = false;
		// true,false
		return new AudioFormat(	sampleRate,
														sampleSizeInBits,
														channels,
														signed,
														bigEndian);
	}

}