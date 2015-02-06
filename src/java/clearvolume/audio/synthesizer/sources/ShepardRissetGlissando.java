package clearvolume.audio.synthesizer.sources;

import static java.lang.Math.PI;
import static java.lang.Math.pow;
import static java.lang.Math.sin;

import java.util.ArrayList;

public class ShepardRissetGlissando extends SourceBase implements
																											Source
{

	private float mBottomFrequency;
	private float mTopFrequency;
	private float mFrequencySpacing;

	private ArrayList<Sinusoid> mSinusoidList = new ArrayList<>();

	public ShepardRissetGlissando()
	{
		this(440, 4 * 440, 440);
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
			if (lFrequency > mTopFrequency)
			{
				lFrequency -= mTopFrequency - mBottomFrequency;
			}
			else if (lFrequency < mBottomFrequency)
			{
				lFrequency += mTopFrequency - mBottomFrequency;
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
		float lAmplitude = (float) pow(sin(PI * lNormalizedFrequency), 2);
		return lAmplitude;
	}

	@Override
	public float next()
	{
		float lSample = 0;
		for (Sinusoid lSinusoid : mSinusoidList)
		{
			float lSinusoidSample = lSinusoid.next();
			// System.out.println(lSinusoidSample);
			lSample += lSinusoidSample;
		}
		return lSample;
	}

}
