package clearvolume.renderer.factory;

import java.util.Properties;

import clearcuda.CudaAvailability;
import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.opencl.OpenCLAvailability;
import coremem.types.NativeTypeEnum;

public class ClearVolumeRendererFactory
{



	/**
	 * Constructs an ClearVolumeRenderer class given a window name, width and
	 * height.
	 *
	 * @param pWindowName
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param useInCanvas
	 *          must be set true if you will use ClearVolume embedded in an AWT or
	 *          Swing container.
	 * @return best 8 bit renderer
	 */
	public static final ClearVolumeRendererInterface newBestRenderer8Bit(	final String pWindowName,
																																				final int pWindowWidth,
																																				final int pWindowHeight,
																																				final boolean useInCanvas)
	{
		return newBestRenderer(	pWindowName,
														pWindowWidth,
														pWindowHeight,
														NativeTypeEnum.UnsignedByte,
														useInCanvas);
	}

	/**
	 * Constructs an ClearVolumeRenderer class given a window name, width and
	 * height.
	 *
	 * @param pWindowName
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param useInCanvas
	 *          must be set true if you will use ClearVolume embedded in an AWT or
	 *          Swing container.
	 * @return best 8 bit renderer
	 */
	public static final ClearVolumeRendererInterface newBestRenderer16Bit(final String pWindowName,
																																				final int pWindowWidth,
																																				final int pWindowHeight,
																																				final boolean useInCanvas)
	{
		return newBestRenderer(	pWindowName,
														pWindowWidth,
														pWindowHeight,
														NativeTypeEnum.UnsignedShort,
														useInCanvas);
	}

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width, height, and bytes=per-voxel. The texture dimensions are set to
	 * a default of 768x768.
	 *
	 * @param pWindowName
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param pNativeTypeEnum
	 *          native type
	 * @param useInCanvas
	 *          must be set true if you will use ClearVolume embedded in an AWT or
	 *          Swing container.
	 * 
	 * @return best ClearVolume renderer
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final NativeTypeEnum pNativeTypeEnum,
																																		final boolean useInCanvas)
	{
		return newBestRenderer(	pWindowName,
														pWindowWidth,
														pWindowHeight,
														pNativeTypeEnum,
														768,
														768,
														useInCanvas);
	}

	/**
	 * Constructs an instance of the JCudaClearVolumeRenderer class given a window
	 * name, width, height, bytes=per-voxel, max window width and height.
	 *
	 * 
	 * @param pWindowName
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param pNativeTypeEnum
	 *          nativ etype
	 * @param pMaxTextureWidth
	 *          max render width
	 * @param pMaxTextureHeight
	 *          max render height
	 * @return best ClearVolume renderer
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final NativeTypeEnum pNativeTypeEnum,
																																		final int pMaxTextureWidth,
																																		final int pMaxTextureHeight)
	{
		return newBestRenderer(	pWindowName,
														pWindowWidth,
														pWindowHeight,
														pNativeTypeEnum,
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
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param pNativeTypeEnum
	 *          native typ
	 * @param pMaxTextureWidth
	 *          max render width
	 * @param pMaxTextureHeight
	 *          max render height
	 * @param useInCanvas
	 *          must be set true if you will use ClearVolume embedded in an AWT or
	 *          Swing container.
	 * @return best ClearVolume renderer
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final NativeTypeEnum pNativeTypeEnum,
																																		final int pMaxTextureWidth,
																																		final int pMaxTextureHeight,
																																		final boolean useInCanvas)
	{
		return newBestRenderer(	pWindowName,
														pWindowWidth,
														pWindowHeight,
														pNativeTypeEnum,
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
	 *          window name
	 * @param pWindowWidth
	 *          window width
	 * @param pWindowHeight
	 *          window height
	 * @param pNativeTypeEnum
	 *          native type
	 * @param pMaxTextureWidth
	 *          max render width
	 * @param pMaxTextureHeight
	 *          max render height
	 * @param pNumberOfRenderLayers
	 *          number of render layers
	 * @param pUseInCanvas
	 *          must be set true if you will use ClearVolume embedded in an AWT or
	 *          Swing container.
	 * @return best ClearVolume renderer
	 */
	public static final ClearVolumeRendererInterface newBestRenderer(	final String pWindowName,
																																		final int pWindowWidth,
																																		final int pWindowHeight,
																																		final NativeTypeEnum pNativeTypeEnum,
																																		final int pMaxTextureWidth,
																																		final int pMaxTextureHeight,
																																		final int pNumberOfRenderLayers,
																																		final boolean pUseInCanvas)
	{
		try
		{
			final Properties lProperties = new Properties(System.getProperties());

			if (lProperties.getProperty("ClearVolume.disableOpenCL") == null)
			{
				final ClearVolumeRendererInterface lNewOpenCLRenderer = internalCreateOpenCLRenderer(	pWindowName,
																																														pWindowWidth,
																																														pWindowHeight,
																																														pNativeTypeEnum,
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

			if (lProperties.getProperty("ClearVolume.disableCUDA") == null)
			{
				final ClearVolumeRendererInterface lNewCudaRenderer = internalCreateCudaRenderer(	pWindowName,
																																													pWindowWidth,
																																													pWindowHeight,
																																													pNativeTypeEnum,
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
																																		final NativeTypeEnum pNativeTypeEnum,
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

			return internalCreateCudaRenderer(pWindowName,
																				pWindowWidth,
																				pWindowHeight,
																				pNativeTypeEnum,
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
																																			final NativeTypeEnum pNativeTypeEnum,
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

			return internalCreateOpenCLRenderer(	pWindowName,
																					pWindowWidth,
																					pWindowHeight,
																					pNativeTypeEnum,
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

	private static ClearVolumeRendererInterface internalCreateCudaRenderer(	final String pWindowName,
																																					final int pWindowWidth,
																																					final int pWindowHeight,
																																					final NativeTypeEnum pNativeTypeEnum,
																																					final int pMaxTextureWidth,
																																					final int pMaxTextureHeight,
																																					final int pNumberOfRenderLayers,
																																					final boolean pUseInCanvas)
	{

			return new clearvolume.renderer.clearcuda.JCudaClearVolumeRenderer(	pWindowName,
																																					pWindowWidth,
																																					pWindowHeight,
																																					pNativeTypeEnum,
																																					pMaxTextureWidth,
																																					pMaxTextureHeight,
																																					pNumberOfRenderLayers,
																																					pUseInCanvas);
	}

	private static ClearVolumeRendererInterface internalCreateOpenCLRenderer(final String pWindowName,
																																					final int pWindowWidth,
																																					final int pWindowHeight,
																																					final NativeTypeEnum pNativeTypeEnum,
																																					final int pMaxTextureWidth,
																																					final int pMaxTextureHeight,
																																					final int pNumberOfRenderLayers,
																																					final boolean pUseInCanvas)
	{

			return new clearvolume.renderer.opencl.OpenCLVolumeRenderer(pWindowName,
																																	pWindowWidth,
																																	pWindowHeight,
																																	pNativeTypeEnum.toString(),
																																	pMaxTextureWidth,
																																	pMaxTextureHeight,
																																	pNumberOfRenderLayers,
																																	pUseInCanvas);
	}



}
