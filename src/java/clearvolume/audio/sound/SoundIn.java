package clearvolume.audio.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;

/**
 * Simple single-class facade on top of the sound output JavaSound API.
 *
 * @author Loic Royer (2015)
 *
 */
public class SoundIn
{

	AudioFormat audioFormat;

	TargetDataLine targetDataLine;

	/**
	 * @throws java.awt.HeadlessException
	 */
	public SoundIn()
	{
		super();
	}

	public void start()
	{
		try
		{
			audioFormat = getAudioFormat();
			/*************************************************************************
			 * DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class,
			 * mAudioFormat); targetDataLine = (TargetDataLine)
			 * AudioSystem.getLine(dataLineInfo);/
			 ************************************************************************/

			targetDataLine = AudioSystem.getTargetDataLine(getAudioFormat());
			targetDataLine.open(audioFormat);
			targetDataLine.start();

		}
		catch (final Exception e)
		{
			System.out.println(e);
			System.exit(0);
		}
	}

	public void stop()
	{
		try
		{
			targetDataLine.flush();
			targetDataLine.stop();
			targetDataLine.close();
		}
		catch (final Exception e)
		{
			System.out.println(e);
			System.exit(0);
		}
	}

	public int record(final byte[] pBuffer)
	{
		return targetDataLine.read(pBuffer, 0, pBuffer.length);
	}

	private AudioFormat getAudioFormat()
	{
		final float sampleRate = 88200f;
		// 8000,11025,16000,22050, 44100, 88200f
		final int sampleSizeInBits = 24;
		// 8,16
		final int channels = 2;
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