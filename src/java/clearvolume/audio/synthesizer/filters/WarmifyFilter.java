package clearvolume.audio.synthesizer.filters;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.signum;
import clearvolume.audio.synthesizer.sources.Source;

public class WarmifyFilter extends FilterBase
{

	private volatile float mPower;

	public WarmifyFilter()
	{
		this(2);
	}

	public WarmifyFilter(float pPower)
	{
		super();
		mPower = pPower;
	}

	@Override
	public float next()
	{
		Source lSource = getSource();
		float lInSample = lSource.next();
		float lOutSample = lInSample + (float) (signum(lInSample) * pow(abs(lInSample),
																												mPower));
		return lOutSample;
	}

	public double getPower()
	{
		return mPower;
	}

	public void setPower(float pPower)
	{
		mPower = pPower;
	}

}
