package clearvolume.renderer;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import clearvolume.controller.RotationControllerInterface;
import clearvolume.projections.ProjectionAlgorithm;
import clearvolume.transferf.TransferFunction;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;

/**
 * Interface ClearVolumeRenderer
 * 
 * Classes that implement this interface provide the basic functionality of
 * ClearVolume renderer.
 *
 * @author Loic Royer 2014
 *
 */
public interface ClearVolumeRendererInterface	extends
																							DisplayRequestInterface,
																							Closeable
{

	/**
	 * Returns the number of bytes per voxels for the volume data.
	 * 
	 * @return bytes-per-voxel
	 */
	int getBytesPerVoxel();

	/**
	 * Sets the display used by the renderer visible.
	 * 
	 * @param pVisible
	 */
	void setVisible(boolean pVisible);

	/**
	 * Rturns the window name.
	 * 
	 * @return window name.
	 */
	String getWindowName();

	/**
	 * Returns window width.
	 * 
	 * @return window width
	 */
	int getWindowWidth();

	/**
	 * Returns
	 * 
	 * @return window height.
	 */
	int getWindowHeight();

	/**
	 * Returns true if the display is in full-screen mode.
	 * 
	 * @return true if full-screen
	 */
	boolean isFullScreen();

	/**
	 * Toggles fullscreen mode on/off
	 */
	void toggleFullScreen();

	/**
	 * Toggles box display.
	 */
	void toggleBoxDisplay();

	/**
	 * Sets the transfer function used for rendering.
	 * 
	 * @param pTransfertFunction
	 *          transfer function
	 */
	void setTransfertFunction(TransferFunction pTransfertFunction);

	/**
	 * Sets the transfer function range. Both min and max values should be within
	 * [0,1].
	 * 
	 * @param pMin
	 *          minimum
	 * @param pMax
	 *          maximum
	 */
	void setTransferFunctionRange(double pMin, double pMax);

	/**
	 * Sets the transfer function range minimum.
	 * 
	 * @param pMin
	 *          transfer function range minimum.
	 */
	void setTransferFunctionRangeMin(double pMin);

	/**
	 * Sets the transfer function range maximum.
	 * 
	 * @param pMax
	 *          transfer function range maximum.
	 */
	void setTransferFunctionRangeMax(double pMax);

	/**
	 * Gamma value used for display.
	 * 
	 * @param pGamma
	 */
	void setGamma(double pGamma);

	/**
	 * Sets the brightness for display
	 * 
	 * @param pBrightness
	 */
	void setBrightness(double pBrightness);

	/**
	 * Resets gamma, brightness, and transfer function range.
	 */
	void resetBrightnessAndGammaAndTransferFunctionRanges();

	/**
	 * Sets the projection algorithm used.
	 * 
	 * @param pProjectionAlgorithm
	 */
	void setProjectionAlgorithm(ProjectionAlgorithm pProjectionAlgorithm);

	/**
	 * Sets the rotation controller used (in addition to the mouse).
	 * 
	 * @param pRotationControllerInterface
	 */
	void setQuaternionController(RotationControllerInterface pRotationControllerInterface);

	/**
	 * Sets the current render layer.
	 * 
	 * @param pLayerIndex
	 *          Layer to render the volume to.
	 */
	void setCurrentRenderLayer(int pLayerIndex);

	/**
	 * Gets the current render layer.
	 *
	 */
	int getCurrentRenderLayer();

	/**
	 * Sets number of render layers.
	 * 
	 * @param pNumberOfRenderLayers
	 *          Number of render layers
	 */
	void setNumberOfRenderLayers(int pNumberOfRenderLayers);

	/**
	 * Gets number of render layers.
	 * 
	 */
	int getNumberOfRenderLayers();

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ).
	 * 
	 * @param pByteBuffer
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 */
	void setVolumeDataBuffer(	ByteBuffer pByteBuffer,
														long pSizeX,
														long pSizeY,
														long pSizeZ);
	
	/**
	 * Updates the voxel size of subsequently rendered volumes
	 * 
	 * @param pVoxelSizeX
	 * @param pVoxelSizeY
	 * @param pVoxelSizeZ
	 */
	public void setVoxelSize(double pVoxelSizeX, double pVoxelSizeY, double pVoxelSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). In addition the real units are provided.
	 * 
	 * @param pByteBuffer
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 * @param pVoxelSizeX
	 * @param pVoxelSizeY
	 * @param pVoxelSizeZ
	 */
	void setVolumeDataBuffer(	ByteBuffer pByteBuffer,
														long pSizeX,
														long pSizeY,
														long pSizeZ,
														double pVoxelSizeX,
														double pVoxelSizeY,
														double pVoxelSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). In addition the real units are provided.
	 * 
	 * @param pVolume
	 *          Volume data to use for updating display.
	 */
	void setVolumeDataBuffer(Volume<?> pVolume);

	/**
	 * Creates a compatible VolumeManager - possibly capable of allocating pinned
	 * memory or memory optimized in other ways. pMaxAvailableVolumes is the
	 * maximal number of volumes to be kept allocated and available so as to avoid
	 * memory trashing.
	 * 
	 * @param pMaxAvailableVolumes
	 * @return
	 */
	VolumeManager createCompatibleVolumeManager(int pMaxAvailableVolumes);

	/**
	 * Waits until volume data copy completes for all layers.
	 * 
	 * @return true is completed, false if it timed-out.
	 */
	public boolean waitToFinishAllDataBufferCopy(	long pTimeOut,
																								TimeUnit pTimeUnit);

	/**
	 * Waits until volume data copy completes for current layer.
	 * 
	 * @return true is completed, false if it timed-out.
	 */
	public boolean waitToFinishDataBufferCopy(long pTimeOut,
																						TimeUnit pTimeUnit);

	/**
	 * Waits until volume data copy completes for a given layer.
	 * 
	 * @return true is completed, false if it timed-out.
	 */
	public boolean waitToFinishDataBufferCopy(final int pRenderLayerIndex,
																						long pTimeOut,
																						TimeUnit pTimeUnit);

	/**
	 * Resets rotation and translation parameters.
	 */
	void resetRotationTranslation();

	/**
	 * Translates along x axis by pDX.
	 * 
	 * @param pDX
	 *          amount of translation
	 */
	void addTranslationX(double pDX);

	/**
	 * Translates along y axis by pDY.
	 * 
	 * @param pDY
	 *          amount of translation
	 */
	void addTranslationY(double pDY);

	/**
	 * Translates along z axis by pDZ.
	 * 
	 * @param pDZ
	 *          amount of translation
	 */
	void addTranslationZ(double pDZ);

	/**
	 * Rotates along x axis by pDRX.
	 * 
	 * @param pDRX
	 *          amount of rotation
	 */
	void addRotationX(int pDRX);

	/**
	 * Rotates along y axis by pDRY.
	 * 
	 * @param pDRY
	 *          amount of rotation
	 */
	void addRotationY(int pDRY);

	/**
	 * Returns the translation vector x component.
	 * 
	 * @return x component
	 */
	public float getTranslationX();

	/**
	 * Returns the translation vector x component.
	 * 
	 * @return y component
	 */
	public float getTranslationY();

	/**
	 * Returns the translation vector z component.
	 * 
	 * @return z component
	 */
	public float getTranslationZ();

	/**
	 * Returns the rotation vector x component.
	 * 
	 * @return x component
	 */
	public float getRotationY();

	/**
	 * Returns the rotation vector y component.
	 * 
	 * @return y component
	 */
	public float getRotationX();

	/**
	 * Notifies renderer that display/volume parameters have changed and a display
	 * update is needed.
	 */
	void notifyUpdateOfVolumeRenderingParameters();

	/**
	 * Returns whether the renderer's display is showing.
	 * 
	 * @return true if renderer's display is showing/running.
	 */
	boolean isShowing();

	/**
	 * Interface method implementation
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	void close();

}
