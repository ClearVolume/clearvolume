package clearvolume.renderer.factory;

import clearcuda.CudaAvailability;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.renderer.opencl.OpenCLVolumeRenderer;

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
		boolean lCUDAOperational = false;

		try
		{
			lCUDAOperational = CudaAvailability.isClearCudaOperational();
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}

		if (lCUDAOperational)
			return new JCudaClearVolumeRenderer(pWindowName,
																					pWindowWidth,
																					pWindowHeight,
																					pBytesPerVoxel,
																					pMaxTextureWidth,
																					pMaxTextureHeight,
																					pNumberOfRenderLayers,
																					pUseInCanvas);
		else
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
