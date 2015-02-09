package clearvolume.renderer.processors.impl;

import clearvolume.renderer.processors.OpenCLProcessor;

import com.nativelibs4java.opencl.CLKernel;

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
		ensureOpenCLInitialized();
		getDevice().setArgs(mProcessorKernel, getArgs());
		getDevice().run(mProcessorKernel,
										(int) pWidthInVoxels,
										(int) pHeightInVoxels);
		
		double lThisValueShouldMakeSenseSomehow = 0;

		notifyListenersOfResult(lThisValueShouldMakeSenseSomehow);

	}
}
