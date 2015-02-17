package clearvolume.renderer;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import clearvolume.ClearVolumeCloseable;
import clearvolume.controller.RotationControllerInterface;
import clearvolume.projections.ProjectionAlgorithm;
import clearvolume.renderer.jogl.overlay.Overlay;
import clearvolume.renderer.processors.Processor;
import clearvolume.transferf.TransferFunction;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;

import com.jogamp.newt.awt.NewtCanvasAWT;

/**
 * Interface ClearVolumeRenderer
 *
 * Classes that implement this interface provide the basic functionality of
 * ClearVolume renderer.
 *
 * @author Loic Royer 2014
 *
 */
/**
 *
 *
 * @author Loic Royer (2015)
 *
 */
public interface ClearVolumeRendererInterface	extends
																							DisplayRequestInterface,
																							ClearVolumeCloseable
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
	 * Returns true if the current layer is visible.
	 *
	 * @return
	 */
	boolean isLayerVisible();

	/**
	 * returns true if the given layer is visible.
	 *
	 * @param pRenderLayerIndex
	 * @return
	 */
	boolean isLayerVisible(int pRenderLayerIndex);

	/**
	 * Sets visiblility of the current layer.
	 *
	 * @param pVisble
	 */
	void setLayerVisible(boolean pVisble);

	/**
	 * Sets visibility of a given layer.
	 *
	 * @param pRenderLayerIndex
	 * @param pVisble
	 */
	void setLayerVisible(int pRenderLayerIndex, boolean pVisble);

	/**
	 * Returns the brightness of the current render layer index.
	 *
	 * @return brightness
	 */
	double getBrightness();

	/**
	 * Returns the brightness of the current render layer index.
	 *
	 * @param pRenderLayerIndex
	 * @return
	 */
	double getBrightness(int pRenderLayerIndex);

	/**
	 * @return
	 */
	double getGamma();

	/**
	 * @param pRenderLayerIndex
	 * @return
	 */
	double getGamma(int pRenderLayerIndex);

	/**
	 * @return
	 */
	double getTransferRangeMin();

	/**
	 * @param pRenderLayerIndex
	 * @return
	 */
	double getTransferRangeMin(int pRenderLayerIndex);

	/**
	 * @return
	 */
	double getTransferRangeMax();

	/**
	 * @param pRenderLayerIndex
	 * @return
	 */
	double getTransferRangeMax(int pRenderLayerIndex);

	/**
	 * @param pRenderLayerIndex
	 * @param pTransferRangeMin
	 * @param pTransferRangeMax
	 */
	void setTransferFunctionRange(int pRenderLayerIndex,

	double pTransferRangeMin, double pTransferRangeMax);

	/**
	 * @param pRenderLayerIndex
	 * @param pTransferRangeMin
	 */
	void setTransferFunctionRangeMin(	int pRenderLayerIndex,
																		double pTransferRangeMin);

	/**
	 * @param pRenderLayerIndex
	 * @param pTransferRangeMax
	 */
	void setTransferFunctionRangeMax(	int pRenderLayerIndex,
																		double pTransferRangeMax);

	/**
	 * @param pDelta
	 */
	void addTransferFunctionRangeMin(double pDelta);

	/**
	 * @param pRenderLayerIndex
	 * @param pDelta
	 */
	void addTransferFunctionRangeMin(	int pRenderLayerIndex,
																		double pDelta);

	/**
	 * @param pDelta
	 */
	void addTransferFunctionRangeMax(double pDelta);

	/**
	 * @param pRenderLayerIndex
	 * @param pDelta
	 */
	void addTransferFunctionRangeMax(	int pRenderLayerIndex,
																		double pDelta);

	/**
	 * @param pTransferRangePositionDelta
	 */
	void addTransferFunctionRangePosition(double pTransferRangePositionDelta);

	/**
	 * @param pTransferRangeWidthDelta
	 */
	void addTransferFunctionRangeWidth(double pTransferRangeWidthDelta);

	/**
	 * Sets the transfer function used for rendering.
	 *
	 * @param pTransfertFunction
	 *          transfer function
	 */
	void setTransferFunction(TransferFunction pTransfertFunction);

	/**
	 * Sets the transfer function used for rendering.
	 *
	 * @param pTransfertFunction
	 *          transfer function
	 */
	void setTransferFunction(	int pRenderLayerIndex,
														TransferFunction pTransfertFunction);

	/**
	 * Returns the transfer function set for a given layer.
	 */
	TransferFunction getTransferFunction();

	/**
	 * Returns the transfer function set for a given layer.
	 *
	 * @param pRenderLayerIndex
	 */
	TransferFunction getTransferFunction(int pRenderLayerIndex);

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
	 * @param pRenderLayerIndex
	 * @param pGamma
	 */
	void setGamma(int pRenderLayerIndex, double pGamma);

	/**
	 * @param pRenderLayerIndex
	 * @param pBrightness
	 */
	void setBrightness(int pRenderLayerIndex, double pBrightness);

	/**
	 * Sets the brightness for display
	 *
	 * @param pBrightness
	 */
	void setBrightness(double pBrightness);

	/**
	 * @param pBrightnessDelta
	 */
	void addBrightness(double pBrightnessDelta);

	/**
	 * @param pRenderLayerIndex
	 * @param pBrightnessDelta
	 */
	void addBrightness(int pRenderLayerIndex, double pBrightnessDelta);

	/**
	 * Resets gamma, brightness, and transfer function range.
	 */
	void resetBrightnessAndGammaAndTransferFunctionRanges();

	/**
	 * Sets the amount of dithering [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 * @param pDithering
	 *          new dithering level for render layer
	 */
	void setDithering(int pRenderLayerIndex, double pDithering);

	/**
	 * Returns samount of dithering [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 * @return dithering
	 */
	float getDithering(int pRenderLayerIndex);

	/**
	 * Sets the quality level [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 * @param pQuality
	 *          new quality level for render layer
	 */
	void setQuality(int pRenderLayerIndex, double pQuality);

	/**
	 * Returns the quality level [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 * @return quality level
	 */
	float getQuality(int pRenderLayerIndex);

	/**
	 * Sets the mProjectionMatrix algorithm used.
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
	int getCurrentRenderLayerIndex();

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
	 * dimensions (pSizeX,pSizeY,pSizeZ). This data is uploaded to a given render
	 * layer.
	 *
	 * @param pByteBuffer
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 */
	void setVolumeDataBuffer(	int pRenderLayerIndex,
														ByteBuffer pByteBuffer,
														long pSizeX,
														long pSizeY,
														long pSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ).
	 *
	 * @param pByteBuffer
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 */
	@Deprecated
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
	public void setVoxelSize(	double pVoxelSizeX,
														double pVoxelSizeY,
														double pVoxelSizeZ);

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
	@Deprecated
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
	 * @param pRenderLayerIndex
	 * @param pByteBuffer
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 * @param pVoxelSizeX
	 * @param pVoxelSizeY
	 * @param pVoxelSizeZ
	 */
	void setVolumeDataBuffer(	final int pRenderLayerIndex,
														ByteBuffer pByteBuffer,
														long pSizeX,
														long pSizeY,
														long pSizeZ,
														double pVoxelSizeX,
														double pVoxelSizeY,
														double pVoxelSizeZ);

	/**
	 * Updates the displayed volume with the provided volume.
	 *
	 * @param pVolume
	 *          Volume data to use for updating display.
	 */
	@Deprecated
	void setVolumeDataBuffer(Volume<?> pVolume);

	/**
	 * Updates the displayed volume with the provided Volume for a given layer.
	 *
	 * @param pVolume
	 *          Volume data to use for updating display.
	 */
	void setVolumeDataBuffer(int pRenderLayerIndex, Volume<?> pVolume);

	/**
	 * Creates a compatible VolumeManager - possibly capable of allocating pinned
	 * memory or memory optimised in other ways. pMaxAvailableVolumes is the
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
	void notifyChangeOfVolumeRenderingParameters();

	/**
	 * Toggles the display of the Control Frame;
	 */
	void toggleControlPanelDisplay();

	/**
	 * Toggles recording of rendered window frames.
	 */
	void toggleRecording();

	/**
	 * Returns whether the renderer's display is showing.
	 *
	 * @return true if renderer's display is showing/running.
	 */
	boolean isShowing();

	/**
	 * Prevents the closing of the window.
	 */
	void disableClose();

	/**
	 * Adds overlay module to draw with 3D primitives within the rendering volume
	 * and 2D primitives on top of the whole image.
	 *
	 * @param pOverlay3D
	 *          Overlay3D to add.
	 */
	void addOverlay(Overlay pOverlay);

	/**
	 * Adds a processor to this renderer.
	 *
	 * @param pProcessor
	 *          Processor to add.
	 */
	public void addProcessor(Processor<?> pProcessor);

	/**
	 * Adds these processors to this renderer.
	 *
	 * @param pProcessors
	 *          Processors to add.
	 */
	public void addProcessors(Collection<Processor<?>> pProcessors);

	/**
	 * Adds a capture listener to this renderer.
	 *
	 * @param pVolumeCaptureListener
	 *          capture listener
	 */
	public void addVolumeCaptureListener(VolumeCaptureListener pVolumeCaptureListener);

	/**
	 * Requests capture of the current displayed volume (Preferably of all layers
	 * but possibly just of the current layer.)
	 */
	void requestVolumeCapture();

	/**
	 * Returns the list of overlays in this renderer.
	 *
	 * @return
	 */
	public Collection<Overlay> getOverlays();

	/**
	 * Returns a Canvas that can be used to embed this renderer.
	 *
	 * @return A NewtCanvasAWT object or null if the renderer cannot be embedded.
	 */
	public NewtCanvasAWT getNewtCanvasAWT();

	/**
	 * Returns the adaptive level-of-detail (LOD) controller.
	 * 
	 * @return
	 */
	AdaptiveLODController getAdaptiveLODController();

	/**
	 * Interface method implementation
	 *
	 * @see java.io.Closeable#close()
	 */
	@Override
	void close();


}
