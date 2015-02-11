package clearvolume.renderer.processors.impl;

import static java.lang.Math.random;

import java.nio.FloatBuffer;

import clearvolume.renderer.processors.OpenCLProcessor;

public class ThreeVectorGenerator	extends
																	OpenCLProcessor<FloatBuffer>
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

		final FloatBuffer lRandomVector = FloatBuffer.wrap(new float[]
		{ x, y, z });

		System.out.format("new vector: (%g,%g,%g) \n", x, y, z);

		notifyListenersOfResult(lRandomVector);
	}
}
