package clearvolume.renderer.factory;

import java.util.Properties;

import clearcuda.CudaAvailability;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.opencl.OpenCLAvailability;

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
	public static final ClearVolumeRendererInterface newBestRenderer8Bit(	final String pWindowName,
																																				final int pWindowWidth,
																																				final int pWindowHeight,
																																				final boolean useInCanvas)
	{
		return newBestRenderer(	pWindowName,
														pWindowWidth,
														pWindowHeight,
														1,
														useInCanvas);
	}

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width, height, and bytes=per-voxel. The texture dimensions are set to
	 * a default of 768x768.
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
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final int pBytesPerVoxel,
																																		final int pMaxTextureWidth,
																																		final int pMaxTextureHeight)
	{
		return newBestRenderer(	pWindowName,
														pWindowWidth,
														pWindowHeight,
														pBytesPerVoxel,
														pMaxTextureWidth,
														pMaxTextureHeight,
														1,
														false);
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
		try
		{
			final Properties lProperties = new Properties(System.getProperties());

			if (lProperties.getProperty("ClearVolume.disableCUDA") == null)
			{
				final ClearVolumeRendererInterface lNewCudaRenderer = newCudaRenderer(pWindowName,
																																							pWindowWidth,
																																							pWindowHeight,
																																							pBytesPerVoxel,
																																							pMaxTextureWidth,
																																							pMaxTextureHeight,
																																							pNumberOfRenderLayers,
																																							pUseInCanvas);

				if (lNewCudaRenderer != null)
					return lNewCudaRenderer;
			}
			else
			{
				System.err.println("Caution: Use of CUDA has been explicitly disabled!");
			}

			if (lProperties.getProperty("ClearVolume.disableOpenCL") == null)
			{
				final ClearVolumeRendererInterface lNewOpenCLRenderer = newOpenCLRenderer(pWindowName,
																																									pWindowWidth,
																																									pWindowHeight,
																																									pBytesPerVoxel,
																																									pMaxTextureWidth,
																																									pMaxTextureHeight,
																																									pNumberOfRenderLayers,
																																									pUseInCanvas);
				return lNewOpenCLRenderer;
			}
			else
			{
				System.err.println("Caution: Use of OpenCL has been explicitly disabled!");
			}

			System.err.println("Your system cannot run ClearVolume because it does not support CUDA or OpenCL.");
			return null;
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			return null;
		}
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
		try
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

			return new clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer(	pWindowName,
																																					pWindowWidth,
																																					pWindowHeight,
																																					pBytesPerVoxel,
																																					pMaxTextureWidth,
																																					pMaxTextureHeight,
																																					pNumberOfRenderLayers,
																																					pUseInCanvas);
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			return null;
		}
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
		try
		{
			boolean lOpenCLOperational = false;
			try
			{
				lOpenCLOperational = OpenCLAvailability.isOpenCLAvailable();
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
			}

			if (!lOpenCLOperational)
				return null;

			return new clearvolume.renderer.opencl.OpenCLVolumeRenderer(pWindowName,
																																	pWindowWidth,
																																	pWindowHeight,
																																	pBytesPerVoxel,
																																	pMaxTextureWidth,
																																	pMaxTextureHeight,
																																	pNumberOfRenderLayers,
																																	pUseInCanvas);
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
