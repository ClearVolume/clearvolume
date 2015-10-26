package clearvolume.demo;

import static java.lang.Math.random;

import clearvolume.renderer.processors.ProcessorBase;

public class RandomWalk extends ProcessorBase<float[]>
{

	private final float cDelta = 0.1f;

	private volatile float x, y, z;

	@Override
	public String getName()
	{
		return "threevectorgenerator";
	}

	@Override
	public void process(int pRenderLayerIndex,
						long pWidthInVoxels,
						long pHeightInVoxels,
						long pDepthInVoxels)
	{
		if (!isActive())
			return;

		x += cDelta * (random() - 0.5);
		y += cDelta * (random() - 0.5);
		z += cDelta * (random() - 0.5);

		final float[] lRandomVector = new float[]
		{ x, y, z };

		notifyListenersOfResult(lRandomVector);
	}

	@Override
	public boolean isCompatibleProcessor(Class<?> pRendererClass)
	{
		return true;
	}
}
