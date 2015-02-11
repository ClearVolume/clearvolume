package clearvolume.renderer.processors.impl;

import java.nio.FloatBuffer;

import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLKernel;

public class ThreeVectorGenerator extends OpenCLProcessor<FloatBuffer>
{

	private CLKernel mProcessorKernel;

	@Override
	public String getName()
	{
		return "threevectorgenerator";
	}

	public void ensureOpenCLInitialized()
	{
		if (mProcessorKernel == null)
		{
			mProcessorKernel = getDevice().compileKernel(	OpenCLTest.class.getResource("kernels/test.cl"),
																										"test");
		}
	}

	@Override
	public void process(int pRenderLayerIndex,
											long pWidthInVoxels,
											long pHeightInVoxels,
											long pDepthInVoxels)
	{
		if (!isActive())
			return;

		ensureOpenCLInitialized();
		getDevice().setArgs(mProcessorKernel, getVolumeBuffers());
		getDevice().run(mProcessorKernel,
										(int) pWidthInVoxels,
										(int) pHeightInVoxels);

		final FloatBuffer randomVector = FloatBuffer.wrap(new float[]{
                    -0.4f + (float)Math.random() * ((0.4f - (-0.4f)) + 0.8f),
                    -0.4f + (float)Math.random() * ((0.4f - (-0.4f)) + 0.8f),
                    -0.4f + (float)Math.random() * ((0.4f - (-0.4f)) + 0.8f)
    });

		notifyListenersOfResult(randomVector);
	}
}
