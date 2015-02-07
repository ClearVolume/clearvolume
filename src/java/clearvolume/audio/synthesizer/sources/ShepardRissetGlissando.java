package clearvolume.audio.synthesizer.sources;

import static java.lang.Math.PI;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.sin;

import java.util.ArrayList;

/**
 * Source that generates a Shepard-Risset glissando sound illusion. This is a
 * sort of auditive devil's staircase - you have the impression that the sound
 * can go up o down indefinitely. It is an example of
 * 'locally-consistent-globally-inconsistent' family of illusions.
 * 
 * In practice this is implemented with a translating frequency comb multiplied
 * by a stationary amplitude envelop of finite support. When Diracs of the comb
 * fall out on one side of the envelop support they reappear on the other side.
 *
 * @author Loic Royer (2015)
 *
 */
public class ShepardRissetGlissando extends SourceBase implements
																											Source
{

	private float mBottomFrequency;
	private float mTopFrequency;
	private float mFrequencySpacing;

	private ArrayList<Sinusoid> mSinusoidList = new ArrayList<>();

	/**
	 * Default constructor with reasonable example parameters.
	 */
	public ShepardRissetGlissando()
	{
		this(440, 4 * 440, 220);
	}

	/**
	 * Constructs a Shepard-Risset glissando source for a given support
	 * [pBottomFrequency,pTopFrequency] and frequency spacing.
	 * 
	 * @param pBottomFrequency
	 *          bottom frequency of the support.
	 * @param pTopFrequency
	 *          top frequency of the support.
	 * @param pFrequencySpacing
	 *          frequency spacing.
	 */
	public ShepardRissetGlissando(float pBottomFrequency,
																float pTopFrequency,
																float pFrequencySpacing)
	{
		super();
		mBottomFrequency = pBottomFrequency;
		mTopFrequency = pTopFrequency;
		mFrequencySpacing = pFrequencySpacing;

		for (float lFrequency = mBottomFrequency; lFrequency < mTopFrequency; lFrequency += mFrequencySpacing)
		{
			Sinusoid lSinusoid = new Sinusoid(lFrequency);
			mSinusoidList.add(lSinusoid);
		}
	}

	/**
	 * Translates by a given delta frequency the frequency comb. This can be done
	 * to an arbitrary extend since the frequency comb translations have the
	 * topology of a circle. Hence the notion of 'virtual-frequency'.
	 * 
	 * @param pDeltaFrequencyHertz
	 *          change in frequency.
	 */
	public void changeVirtualFrequency(float pDeltaFrequencyHertz)
	{
		for (Sinusoid lSinusoid : mSinusoidList)
		{
			float lFrequency = lSinusoid.getFrequencyInHertz();
			lFrequency += pDeltaFrequencyHertz;
			float lFrequencyBand = mTopFrequency - mBottomFrequency;
			if (lFrequency > mTopFrequency)
			{
				lFrequency -= lFrequencyBand;
			}
			else if (lFrequency < mBottomFrequency)
			{
				lFrequency += lFrequencyBand;
			}
			lSinusoid.setFrequencyInHertz(lFrequency);

			float lAmplitude = getAmplitudeEnvelopp(lFrequency);
			lSinusoid.setAmplitude(lAmplitude);
		}

	}

	/**
	 * Computes the value of the amplitude envelop for a given frequency.
	 * 
	 * @param pFrequency
	 *          frequency
	 * @return amplitude
	 */
	private float getAmplitudeEnvelopp(float pFrequency)
	{
		if (pFrequency < mBottomFrequency || pFrequency > mTopFrequency)
			return 0;
		float lNormalizedFrequency = (pFrequency - mBottomFrequency) / (mTopFrequency - mBottomFrequency);
		float lAmplitude = (float) (getAmplitude() * pow(	sin(PI * lNormalizedFrequency),
																									2));
		return lAmplitude;
	}

	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#next()
	 */
	@Override
	public float next()
	{
		float lSample = 0;
		for (Sinusoid lSinusoid : mSinusoidList)
		{
			float lSinusoidSample = lSinusoid.next();
			lSample += lSinusoidSample;
		}
		return lSample;
	}


	/* (non-Javadoc)
	 * @see clearvolume.audio.synthesizer.sources.Source#getPeriodInSamples()
	 */
	@Override
	public int getPeriodInSamples()
	{
		int lMaxPeriodInSamples = 0;
		for (Sinusoid lSinusoid : mSinusoidList)
		{
			int lPeriodInSamples = lSinusoid.getPeriodInSamples();
			lMaxPeriodInSamples = max(lMaxPeriodInSamples, lPeriodInSamples);
		}
		return lMaxPeriodInSamples;
	}

}
