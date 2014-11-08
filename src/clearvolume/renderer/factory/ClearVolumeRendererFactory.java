package clearvolume.renderer.factory;

import clearcuda.CudaAvailability;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer;
import clearvolume.renderer.clearopencl.OpenCLVolumeRenderer;

public class ClearVolumeRendererFactory
{

	/**
	 * Constructs an ClearVolumeRenderer class given a window name, width and
	 * height.
	 * 
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight)
	{
		return new JCudaClearVolumeRenderer(pWindowName, 1024, 1024);
	}

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width, height, and bytes=per-voxel.
	 * 
	 * @param pWindowName
	 * @param pWindowWidth
	 * @param pWindowHeight
	 * @param pBytesPerVoxel
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final int pBytesPerVoxel)
	{
		return new JCudaClearVolumeRenderer(pWindowName,
																				pWindowWidth,
																				pWindowHeight,
																				pBytesPerVoxel);
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
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final int pBytesPerVoxel,
																																		final int pMaxTextureWidth,
																																		final int pMaxTextureHeight)
	{
		return new JCudaClearVolumeRenderer(pWindowName,
																				pWindowWidth,
																				pWindowHeight,
																				pBytesPerVoxel,
																				pMaxTextureWidth,
																				pMaxTextureHeight);
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
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final int pBytesPerVoxel,
																																		final int pMaxTextureWidth,
																																		final int pMaxTextureHeight,
																																		final int pNumberOfRenderLayers)
	{
		if (CudaAvailability.isClearCudaOperational())
			return new JCudaClearVolumeRenderer(pWindowName,
																					pWindowWidth,
																					pWindowHeight,
																					pBytesPerVoxel,
																					pMaxTextureWidth,
																					pMaxTextureHeight,
																					pNumberOfRenderLayers);
		else
			return new OpenCLVolumeRenderer(pWindowName,
																			pWindowWidth,
																			pWindowHeight,
																			pBytesPerVoxel,
																			pMaxTextureWidth,
																			pMaxTextureHeight,
																			pNumberOfRenderLayers);

	}
}
