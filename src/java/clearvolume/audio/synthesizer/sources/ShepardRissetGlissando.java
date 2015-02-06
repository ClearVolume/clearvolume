package clearvolume.audio.synthesizer.sources;

import static java.lang.Math.PI;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.sin;

import java.util.ArrayList;

public class ShepardRissetGlissando extends SourceBase implements
																											Source
{

	private float mBottomFrequency;
	private float mTopFrequency;
	private float mFrequencySpacing;
	private volatile float mAmplitude = 1;

	private ArrayList<Sinusoid> mSinusoidList = new ArrayList<>();

	public ShepardRissetGlissando()
	{
		this(440, 4 * 440, 220);
	}

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

	private float getAmplitudeEnvelopp(float pFrequency)
	{
		if (pFrequency < mBottomFrequency || pFrequency > mTopFrequency)
			return 0;
		float lNormalizedFrequency = (pFrequency - mBottomFrequency) / (mTopFrequency - mBottomFrequency);
		float lAmplitude = (float) (mAmplitude * pow(	sin(PI * lNormalizedFrequency),
																									2));
		return lAmplitude;
	}

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

	public float getAmplitude()
	{
		return mAmplitude;
	}

	public void setAmplitude(float pAmplitude)
	{
		mAmplitude = pAmplitude;
	}

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
