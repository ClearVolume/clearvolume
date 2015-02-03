package clearvolume.renderer.processors.impl;

import clearvolume.renderer.processors.OpenCLProcessor;

public class OpenCLTest extends OpenCLProcessor<Double>
{

	@Override
	public String getName()
	{
		return "openclsum";
	}

	@Override
	public void process(int pRenderLayerIndex,
											long pWidthInVoxels,
											long pHeightInVoxels,
											long pDepthInVoxels)
	{

	}



}
