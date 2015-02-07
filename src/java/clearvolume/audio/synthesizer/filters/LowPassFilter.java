package clearvolume.audio.synthesizer.filters;

import clearvolume.audio.synthesizer.sources.Source;

public class LowPassFilter extends FilterBase
{

	private volatile double mValue;

	private volatile double mAlpha;

	public LowPassFilter()
	{
		this(0.1f);
	}

	public LowPassFilter(double pAlpha)
	{
		super();
		mAlpha = pAlpha;
	}

	@Override
	public float next()
	{
		Source lSource = getSource();
		float lSample = lSource.next();

		mValue = getAlpha() * lSample + (1 - getAlpha()) * mValue;

		return (float) mValue;
	}

	public double getAlpha()
	{
		return mAlpha;
	}

	public void setAlpha(double pAlpha)
	{
		mAlpha = pAlpha;
	}



}
