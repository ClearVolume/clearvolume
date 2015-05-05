package clearvolume.renderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import clearvolume.ClearVolumeCloseable;
import clearvolume.controller.AutoRotationController;
import clearvolume.controller.RotationControllerInterface;
import clearvolume.renderer.cleargl.overlay.Overlay;
import clearvolume.renderer.listeners.EyeRayListener;
import clearvolume.renderer.listeners.VolumeCaptureListener;
import clearvolume.renderer.processors.Processor;
import clearvolume.transferf.TransferFunction;
import clearvolume.volume.Volume;
import clearvolume.volume.VolumeManager;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.opengl.math.Quaternion;

import coremem.ContiguousMemoryInterface;
import coremem.fragmented.FragmentedMemoryInterface;
import coremem.types.NativeTypeEnum;

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
	 * Returns the native type of this renderer.
	 *
	 * @return native type.
	 */
	public NativeTypeEnum getNativeType();

	/**
	 * Sets the native type for this renderer.
	 *
	 * @return native type
	 */
	public void setNativeType(NativeTypeEnum pNativeType);

	/**
	 * Returns the number of bytes per voxel for this renderer.
	 * 
	 * @return bytes per voxel
	 */
	public long getBytesPerVoxel();

	/**
	 * Sets the display used by the renderer visible.
	 *
	 * @param pVisible
	 */
	public void setVisible(boolean pVisible);

	/**
	 * Rturns the window name.
	 *
	 * @return window name.
	 */
	public String getWindowName();

	/**
	 * Returns window width.
	 *
	 * @return window width
	 */
	public int getWindowWidth();

	/**
	 * Returns
	 *
	 * @return window height.
	 */
	public int getWindowHeight();

	/**
	 * Returns true if the display is in full-screen mode.
	 *
	 * @return true if full-screen
	 */
	public boolean isFullScreen();

	/**
	 * Toggles fullscreen mode on/off
	 */
	public void toggleFullScreen();

	/**
	 * Toggles box display.
	 */
	public void toggleBoxDisplay();

	/**
	 * Returns true if the current layer is visible.
	 *
	 * @return
	 */
	public boolean isLayerVisible();

	/**
	 * returns true if the given layer is visible.
	 *
	 * @param pRenderLayerIndex
	 * @return
	 */
	public boolean isLayerVisible(int pRenderLayerIndex);

	/**
	 * Sets visiblility of the current layer.
	 *
	 * @param pVisble
	 */
	public void setLayerVisible(boolean pVisble);

	/**
	 * Sets visibility of a given layer.
	 *
	 * @param pRenderLayerIndex
	 * @param pVisble
	 */
	public void setLayerVisible(int pRenderLayerIndex, boolean pVisble);

	/**
	 * Returns the brightness of the current render layer index.
	 *
	 * @return brightness
	 */
	public double getBrightness();

	/**
	 * Returns the brightness of the current render layer index.
	 *
	 * @param pRenderLayerIndex
	 * @return
	 */
	public double getBrightness(int pRenderLayerIndex);

	/**
	 * @return
	 */
	public double getGamma();

	/**
	 * @param pRenderLayerIndex
	 * @return
	 */
	public double getGamma(int pRenderLayerIndex);

	/**
	 * @return
	 */
	public double getTransferRangeMin();

	/**
	 * @param pRenderLayerIndex
	 * @return
	 */
	public double getTransferRangeMin(int pRenderLayerIndex);

	/**
	 * @return
	 */
	public double getTransferRangeMax();

	/**
	 * @param pRenderLayerIndex
	 * @return
	 */
	public double getTransferRangeMax(int pRenderLayerIndex);

	/**
	 * @param pRenderLayerIndex
	 * @param pTransferRangeMin
	 * @param pTransferRangeMax
	 */
	public void setTransferFunctionRange(	int pRenderLayerIndex,
																				double pTransferRangeMin,
																				double pTransferRangeMax);

	/**
	 * @param pRenderLayerIndex
	 * @param pTransferRangeMin
	 */
	public void setTransferFunctionRangeMin(int pRenderLayerIndex,
																					double pTransferRangeMin);

	/**
	 * @param pRenderLayerIndex
	 * @param pTransferRangeMax
	 */
	public void setTransferFunctionRangeMax(int pRenderLayerIndex,
																					double pTransferRangeMax);

	/**
	 * @param pDelta
	 */
	public void addTransferFunctionRangeMin(double pDelta);

	/**
	 * @param pRenderLayerIndex
	 * @param pDelta
	 */
	public void addTransferFunctionRangeMin(int pRenderLayerIndex,
																					double pDelta);

	/**
	 * @param pDelta
	 */
	public void addTransferFunctionRangeMax(double pDelta);

	/**
	 * @param pRenderLayerIndex
	 * @param pDelta
	 */
	public void addTransferFunctionRangeMax(int pRenderLayerIndex,
																					double pDelta);

	/**
	 * @param pTransferRangePositionDelta
	 */
	public void addTransferFunctionRangePosition(double pTransferRangePositionDelta);

	/**
	 * @param pTransferRangeWidthDelta
	 */
	public void addTransferFunctionRangeWidth(double pTransferRangeWidthDelta);

	/**
	 * Sets the transfer function used for rendering.
	 *
	 * @param pTransfertFunction
	 *          transfer function
	 */
	public void setTransferFunction(TransferFunction pTransfertFunction);

	/**
	 * Sets the transfer function used for rendering.
	 *
	 * @param pTransfertFunction
	 *          transfer function
	 */
	public void setTransferFunction(int pRenderLayerIndex,
																	TransferFunction pTransfertFunction);

	/**
	 * Returns the transfer function set for a given layer.
	 */
	public TransferFunction getTransferFunction();

	/**
	 * Returns the transfer function set for a given layer.
	 *
	 * @param pRenderLayerIndex
	 */
	public TransferFunction getTransferFunction(int pRenderLayerIndex);

	/**
	 * Sets the transfer function range. Both min and max values should be within
	 * [0,1].
	 *
	 * @param pMin
	 *          minimum
	 * @param pMax
	 *          maximum
	 */
	public void setTransferFunctionRange(double pMin, double pMax);

	/**
	 * Sets the transfer function range minimum.
	 *
	 * @param pMin
	 *          transfer function range minimum.
	 */
	public void setTransferFunctionRangeMin(double pMin);

	/**
	 * Sets the transfer function range maximum.
	 *
	 * @param pMax
	 *          transfer function range maximum.
	 */
	public void setTransferFunctionRangeMax(double pMax);

	/**
	 * Gamma size used for display.
	 *
	 * @param pGamma
	 */
	public void setGamma(double pGamma);

	/**
	 * @param pRenderLayerIndex
	 * @param pGamma
	 */
	public void setGamma(int pRenderLayerIndex, double pGamma);

	/**
	 * @param pRenderLayerIndex
	 * @param pBrightness
	 */
	public void setBrightness(int pRenderLayerIndex, double pBrightness);

	/**
	 * Sets the brightness for display
	 *
	 * @param pBrightness
	 */
	public void setBrightness(double pBrightness);

	/**
	 * @param pBrightnessDelta
	 */
	public void addBrightness(double pBrightnessDelta);

	/**
	 * @param pRenderLayerIndex
	 * @param pBrightnessDelta
	 */
	public void addBrightness(int pRenderLayerIndex,
														double pBrightnessDelta);

	/**
	 * Resets gamma, brightness, and transfer function range.
	 */
	public void resetBrightnessAndGammaAndTransferFunctionRanges();

	/**
	 * Sets the amount of dithering [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 * @param pDithering
	 *          new dithering level for render layer
	 */
	public void setDithering(int pRenderLayerIndex, double pDithering);

	/**
	 * Returns samount of dithering [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 * @return dithering
	 */
	public float getDithering(int pRenderLayerIndex);

	/**
	 * Sets the quality level [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 * @param pQuality
	 *          new quality level for render layer
	 */
	public void setQuality(int pRenderLayerIndex, double pQuality);

	/**
	 * Returns the quality level [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 * @return quality level
	 */
	public float getQuality(int pRenderLayerIndex);

	/**
	 * Sets the currently render algorithm used.
	 *
	 * @param pRenderAlgorithm
	 */
	public void setRenderAlgorithm(RenderAlgorithm pRenderAlgorithm);

	/**
	 * Gets the currently used render algorithm used.
	 *
	 * @return currently used render algorithm
	 */
	public RenderAlgorithm getRenderAlgorithm();

	/**
	 * Cycles through rendering algorithms
	 */
	public void cycleRenderAlgorithm();

	/**
	 * Sets the current render layer.
	 *
	 * @param pLayerIndex
	 *          Layer to render the volume to.
	 */
	public void setCurrentRenderLayer(int pLayerIndex);

	/**
	 * Gets the current render layer.
	 *
	 */
	public int getCurrentRenderLayerIndex();

	/**
	 * Sets number of render layers.
	 *
	 * @param pNumberOfRenderLayers
	 *          Number of render layers
	 */
	public void setNumberOfRenderLayers(int pNumberOfRenderLayers);

	/**
	 * Gets number of render layers.
	 *
	 */
	public int getNumberOfRenderLayers();

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
	 * dimensions (pSizeX,pSizeY,pSizeZ). This data is uploaded to a given render
	 * layer.
	 *
	 * @param pRenderLayerIndex
	 * @param pByteBuffer
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			ByteBuffer pByteBuffer,
																			long pSizeX,
																			long pSizeY,
																			long pSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). This data is uploaded to a given render
	 * layer.
	 *
	 * @param pRenderLayerIndex
	 * @param pFragmentedMemoryInterface
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			FragmentedMemoryInterface pFragmentedMemoryInterface,
																			long pSizeX,
																			long pSizeY,
																			long pSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). This data is uploaded to a given render
	 * layer.
	 * 
	 * @param pRenderLayerIndex
	 * @param pContiguousMemoryInterface
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			ContiguousMemoryInterface pContiguousMemoryInterface,
																			long pSizeX,
																			long pSizeY,
																			long pSizeZ);

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
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			ByteBuffer pByteBuffer,
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
	 * @param pFragmentedMemoryInterface
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 * @param pVoxelSizeX
	 * @param pVoxelSizeY
	 * @param pVoxelSizeZ
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			FragmentedMemoryInterface pFragmentedMemoryInterface,
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
	 * @param pContiguousMemoryInterface
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 * @param pVoxelSizeX
	 * @param pVoxelSizeY
	 * @param pVoxelSizeZ
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			ContiguousMemoryInterface pContiguousMemoryInterface,
																			long pSizeX,
																			long pSizeY,
																			long pSizeZ,
																			double pVoxelSizeX,
																			double pVoxelSizeY,
																			double pVoxelSizeZ);

	/**
	 * * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). In addition the real units are provided.
	 * The amount of time to wait for the data to be fully copied can be provided,
	 * if a timeout of zero is given then this call will return immediately.
	 * 
	 * @param pTimeOut
	 * @param pTimeUnit
	 * @param pRenderLayerIndex
	 * @param pFragmentedMemoryInterface
	 * @param pSizeX
	 * @param pSizeY
	 * @param pSizeZ
	 * @param pVoxelSizeX
	 * @param pVoxelSizeY
	 * @param pVoxelSizeZ
	 * @return
	 */
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			int pRenderLayerIndex,
																			FragmentedMemoryInterface pFragmentedMemoryInterface,
																			long pSizeX,
																			long pSizeY,
																			long pSizeZ,
																			double pVoxelSizeX,
																			double pVoxelSizeY,
																			double pVoxelSizeZ);

	/**
	 * Updates the displayed volume with the provided Volume for a given layer.
	 *
	 * @param pVolume
	 *          Volume data to use for updating display.
	 * @return
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			Volume pVolume);

	/**
	 * Creates a compatible VolumeManager - possibly capable of allocating pinned
	 * memory or memory optimised in other ways. pMaxAvailableVolumes is the
	 * maximal number of volumes to be kept allocated and available so as to avoid
	 * memory trashing.
	 *
	 * @param pMaxAvailableVolumes
	 * @return
	 */
	public VolumeManager createCompatibleVolumeManager(int pMaxAvailableVolumes);

	/**
	 * Waits until volume data copy completes for all layers.
	 *
	 * @return true is completed, false if it timed-out.
	 */
	public boolean waitToFinishAllDataBufferCopy(	long pTimeOut,
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
	public void resetRotationTranslation();

	/**
	 * Translates along x axis by pDX.
	 *
	 * @param pDX
	 *          amount of translation
	 */
	public void addTranslationX(double pDX);

	/**
	 * Translates along y axis by pDY.
	 *
	 * @param pDY
	 *          amount of translation
	 */
	public void addTranslationY(double pDY);

	/**
	 * Translates along z axis by pDZ.
	 *
	 * @param pDZ
	 *          amount of translation
	 */
	public void addTranslationZ(double pDZ);

	/**
	 * Sets the translation vector x component.
	 *
	 * @param pTranslationX
	 *          x component
	 */
	public void setTranslationX(double pTranslationX);

	/**
	 * Sets the translation vector y component.
	 *
	 * @param pTranslationY
	 *          y component
	 */
	public void setTranslationY(double pTranslationY);

	/**
	 * Sets the translation vector z component.
	 *
	 * @param pTranslationZ
	 *          z component
	 */
	public void setTranslationZ(double pTranslationZ);

	/**
	 * Sets default translation z component. (so that you can see all of the
	 * volume)
	 */
	void setDefaultTranslationZ();

	/**
	 * Returns the translation vector x component.
	 *
	 * @return x component
	 */
	public float getTranslationX();

	/**
	 * Returns the translation vector y component.
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
	 * Sets the FOV
	 * 
	 * @param pFOV
	 */
	void setFOV(double pFOV);

	/**
	 * Returns FOV
	 * 
	 * @return
	 */
	float getFOV();

	/**
	 * Adds FOV.
	 * 
	 * @param pDelta
	 */
	void addFOV(double pDelta);

	/**
	 * Returns the Quaternion.
	 *
	 * @return Quaternion
	 */
	public Quaternion getQuaternion();

	/**
	 * Adds a rotation controller.
	 *
	 * @param pRotationControllerInterface
	 *          rotation controller
	 */
	public void addRotationController(RotationControllerInterface pRotationControllerInterface);

	/**
	 * Removes a rotation controller.
	 *
	 * @param pRotationControllerInterface
	 *          rotation controller
	 */
	public void removeRotationController(RotationControllerInterface pRotationControllerInterface);

	/**
	 * Returns the current list of rotation controllers.
	 *
	 * @return currently used rotation controller.
	 */
	public ArrayList<RotationControllerInterface> getRotationControllers();


	/**
	 * Returns the auto rotation controller.
	 *
	 * @return auto rotation controller
	 */
	AutoRotationController getAutoRotateController();

	/**
	 * Notifies renderer that display/volume parameters have changed and a display
	 * update is needed.
	 */
	public void notifyChangeOfVolumeRenderingParameters();

	/**
	 * Toggles the display of the Control Frame;
	 */
	public void toggleControlPanelDisplay();

	/**
	 * Toggles recording of rendered window frames.
	 */
	public void toggleRecording();

	/**
	 * Returns whether the renderer's display is showing.
	 *
	 * @return true if renderer's display is showing/running.
	 */
	public boolean isShowing();

	/**
	 * Prevents the closing of the window.
	 */
	public void disableClose();

	/**
	 * Adds overlay module to draw with 3D primitives within the rendering volume
	 * and 2D primitives on top of the whole image.
	 *
	 * @param pOverlay3D
	 *          Overlay3D to add.
	 */
	public void addOverlay(Overlay pOverlay);

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
	public void requestVolumeCapture();

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
	 * Sets the Multi-pass algorithm active or inactive.
	 * 
	 * @param pMultiPassOn
	 *          true iof on, false if off
	 */
	public void setMultiPass(boolean pMultiPassOn);

	/**
	 * Returns the adaptive level-of-detail (LOD) controller.
	 * 
	 * @return
	 */
	public AdaptiveLODController getAdaptiveLODController();

	/**
	 * Toggle on/off the adaptive Level-Of-Detail engine
	 */
	public void toggleAdaptiveLOD();

	/**
	 * Adds a eye ray listener to this renderer.
	 *
	 * @param pEyeRayListener
	 *          eye ray listener
	 */
	public void addEyeRayListener(EyeRayListener pEyeRayListener);

	/**
	 * Removes a eye ray listener to this renderer.
	 *
	 * @param pEyeRayListener
	 *          eye ray listener
	 */
	public void removeEyeRayListener(EyeRayListener pEyeRayListener);


	/**
	 * Returns display lock;
	 * 
	 * @return reentrant display lock
	 */
	ReentrantLock getDisplayLock();

	/**
	 * Interface method implementation
	 *
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close();








}
