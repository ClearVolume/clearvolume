package clearvolume.renderer.factory;

import clearcuda.CudaAvailability;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.renderer.opencl.OpenCLVolumeRenderer;

import java.util.Properties;

public class ClearVolumeRendererFactory
{

	/**
	 * Constructs an ClearVolumeRenderer class given a window name, width and
	 * height.
	 * 
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param useInCanvas
	 *          must be set true if you will use ClearVolume embedded in an AWT or
	 *          Swing container.
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final boolean useInCanvas)
	{
		return newBestRenderer(pWindowName, 1024, 1024, 1, useInCanvas);
	}

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width, height, and bytes=per-voxel.
	 * 
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param useInCanvas
	 *          must be set true if you will use ClearVolume embedded in an AWT or
	 *          Swing container.
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final int pBytesPerVoxel,
																																		final boolean useInCanvas)
	{
		return newBestRenderer(	pWindowName,
														pWindowWidth,
														pWindowHeight,
														pBytesPerVoxel,
														768,
														768,
														useInCanvas);
	}

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width, height, bytes=per-voxel, max window width and height.
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param pMaxTextureWidth
	 * @param pMaxTextureHeight
	 * @param useInCanvas
	 *          must be set true if you will use ClearVolume embedded in an AWT or
	 *          Swing container.
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final int pBytesPerVoxel,
																																		final int pMaxTextureWidth,
																																		final int pMaxTextureHeight,
																																		final boolean useInCanvas)
	{
		return newBestRenderer(	pWindowName,
														pWindowWidth,
														pWindowHeight,
														pBytesPerVoxel,
														pMaxTextureWidth,
														pMaxTextureHeight,
														1,
														useInCanvas);
	}

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width, height, and bytes=per-voxel, max window width and height, and
	 *
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 * @param pMaxTextureWidth
	 * @param pMaxTextureHeight
	 * @param pNumberOfRenderLayers
	 * @param pUseInCanvas
	 *          must be set true if you will use ClearVolume embedded in an AWT or
	 *          Swing container.
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final int pBytesPerVoxel,
																																		final int pMaxTextureWidth,
																																		final int pMaxTextureHeight,
																																		final int pNumberOfRenderLayers,
																																		final boolean pUseInCanvas)
	{
    Properties p = new Properties(System.getProperties());

    if(p.getProperty("ClearVolume.disableCUDA") == null) {
      ClearVolumeRendererInterface lNewCudaRenderer = newCudaRenderer(pWindowName,
              pWindowWidth,
              pWindowHeight,
              pBytesPerVoxel,
              pMaxTextureWidth,
              pMaxTextureHeight,
              pNumberOfRenderLayers,
              pUseInCanvas);

      if (lNewCudaRenderer != null)
        return lNewCudaRenderer;
    } else {
      System.err.println("Caution: Use of CUDA has been explicitly disabled!");
    }

    if(p.getProperty("ClearVolume.disableOpenCL") == null) {
      return new OpenCLVolumeRenderer(pWindowName,
              pWindowWidth,
              pWindowHeight,
              pBytesPerVoxel,
              pMaxTextureWidth,
              pMaxTextureHeight,
              pNumberOfRenderLayers,
              pUseInCanvas);
    } else {
      System.err.println("Caution: Use of OpenCL has been explicitly disabled!");
    }

    System.err.println("Your system cannot run ClearVolume because it does not support CUDA or OpenCL.");
    return null;
	}

	public static final ClearVolumeRendererInterface newCudaRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final int pBytesPerVoxel,
																																		final int pMaxTextureWidth,
																																		final int pMaxTextureHeight,
																																		final int pNumberOfRenderLayers,
																																		final boolean pUseInCanvas)
	{
		boolean lCUDAOperational = false;
		try
		{
			lCUDAOperational = CudaAvailability.isClearCudaOperational();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}

		if (!lCUDAOperational)
			return null;

		return new JCudaClearVolumeRenderer(pWindowName,
																				pWindowWidth,
																				pWindowHeight,
																				pBytesPerVoxel,
																				pMaxTextureWidth,
																				pMaxTextureHeight,
																				pNumberOfRenderLayers,
																				pUseInCanvas);
	}

	public static final ClearVolumeRendererInterface newOpenCLRenderer(	final String pWindowName,
																																			final int pWindowWidth,
																																			final int pWindowHeight,
																																			final int pBytesPerVoxel,
																																			final int pMaxTextureWidth,
																																			final int pMaxTextureHeight,
																																			final int pNumberOfRenderLayers,
																																			final boolean pUseInCanvas)
	{
		return new OpenCLVolumeRenderer(pWindowName,
																		pWindowWidth,
																		pWindowHeight,
																		pBytesPerVoxel,
																		pMaxTextureWidth,
																		pMaxTextureHeight,
																		pNumberOfRenderLayers,
																		pUseInCanvas);
	}
}
