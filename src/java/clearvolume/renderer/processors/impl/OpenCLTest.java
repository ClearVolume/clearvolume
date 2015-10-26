package clearvolume.renderer.processors.impl;

import com.nativelibs4java.opencl.CLKernel;

import clearvolume.renderer.processors.OpenCLProcessor;

public class OpenCLTest extends OpenCLProcessor<Double>
{

	private CLKernel mProcessorKernel;

	@Override
	public String getName()
	{
		return "openclsum";
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

		final double lThisValueShouldMakeSenseSomehow = 0.f;

		notifyListenersOfResult(lThisValueShouldMakeSenseSomehow);

	}
}
