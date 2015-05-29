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
import clearvolume.renderer.listeners.ParameterChangeListener;
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
	 * @param pNativeType
	 *          native type
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
	 *          true to set visible, false to set invisible
	 */
	public void setVisible(boolean pVisible);

	/**
	 * Returns the window name.
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
	 * @return true if current layer visible
	 */
	public boolean isLayerVisible();

	/**
	 * Returns true if the given layer is visible.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return true if layer visible, false otherwise
	 */
	public boolean isLayerVisible(int pRenderLayerIndex);

	/**
	 * Sets visibility of the current layer.
	 *
	 * @param pVisible
	 *          true to set it visible, false to set it invisible
	 */
	public void setLayerVisible(boolean pVisible);

	/**
	 * Sets visibility of a given layer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pVisible
	 *          true to set visible, false to set invisible
	 */
	public void setLayerVisible(int pRenderLayerIndex, boolean pVisible);

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
	 *          render layer index
	 * @return brightness
	 */
	public double getBrightness(int pRenderLayerIndex);

	/**
	 * Returns gamma value for current layer.
	 * 
	 * @return gamma
	 */
	public double getGamma();

	/**
	 * Returns gamma for given render layer
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return gamma
	 */
	public double getGamma(int pRenderLayerIndex);

	/**
	 * Returns transfer range min for current render layer
	 * 
	 * @return transfer range min
	 */
	public double getTransferRangeMin();

	/**
	 * Returns transfer range min
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return transfer range min
	 */
	public double getTransferRangeMin(int pRenderLayerIndex);

	/**
	 * Returns transfer range max for current render layer
	 * 
	 * @return transfer range max
	 */
	public double getTransferRangeMax();

	/**
	 * Returns transfer range min
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return transfer range min
	 */
	public double getTransferRangeMax(int pRenderLayerIndex);

	/**
	 * Sets transfer function range [min,max] for given render layer
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pTransferRangeMin
	 *          transfer range min
	 * @param pTransferRangeMax
	 *          transfer range max
	 */
	public void setTransferFunctionRange(	int pRenderLayerIndex,
																				double pTransferRangeMin,
																				double pTransferRangeMax);

	/**
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pTransferRangeMin
	 *          transfer range min
	 */
	public void setTransferFunctionRangeMin(int pRenderLayerIndex,
																					double pTransferRangeMin);

	/**
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pTransferRangeMax
	 *          tarnsfer range max
	 */
	public void setTransferFunctionRangeMax(int pRenderLayerIndex,
																					double pTransferRangeMax);

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
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pTransfertFunction
	 *          transfer function
	 */
	public void setTransferFunction(int pRenderLayerIndex,
																	TransferFunction pTransfertFunction);

	/**
	 * Returns the transfer function set for the current layer.
	 * 
	 * @return transfer function
	 */
	public TransferFunction getTransferFunction();

	/**
	 * Returns the transfer function set for a given layer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return transfer function
	 */
	public TransferFunction getTransferFunction(int pRenderLayerIndex);
	
	/**
	 * Returns the transfer function array
	 * 
	 * @return transfer function array
	 */
	public float[] getTransferFunctionArray();

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
	 *          gamma value
	 */
	public void setGamma(double pGamma);

	/**
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pGamma
	 *          gamma value
	 */
	public void setGamma(int pRenderLayerIndex, double pGamma);

	/**
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pBrightness
	 *          brightness
	 */
	public void setBrightness(int pRenderLayerIndex, double pBrightness);

	/**
	 * Sets the brightness for display
	 *
	 * @param pBrightness
	 *          brightness
	 */
	public void setBrightness(double pBrightness);

	/**
	 * Resets gamma, brightness, and transfer function range.
	 */
	public void resetBrightnessAndGammaAndTransferFunctionRanges();

	/**
	 * Sets the amount of dithering [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pDithering
	 *          new dithering level for render layer
	 */
	public void setDithering(int pRenderLayerIndex, double pDithering);

	/**
	 * Returns samount of dithering [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return dithering
	 */
	public float getDithering(int pRenderLayerIndex);

	/**
	 * Sets the quality level [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pQuality
	 *          new quality level for render layer
	 */
	public void setQuality(int pRenderLayerIndex, double pQuality);

	/**
	 * Returns the quality level [0,1] for a given render layer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return quality level
	 */
	public float getQuality(int pRenderLayerIndex);

	/**
	 * Sets the render algorithm for the given render layer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pRenderAlgorithm
	 *          render algorithm
	 */
	public void setRenderAlgorithm(	final int pRenderLayerIndex,
																	RenderAlgorithm pRenderAlgorithm);

	/**
	 * Sets the current render algorithm for all render layers.
	 *
	 * @param pRenderAlgorithm
	 *          render algorithm
	 */
	public void setRenderAlgorithm(RenderAlgorithm pRenderAlgorithm);

	/**
	 * Gets the currently used render algorithm used.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @return currently used render algorithm
	 */
	public RenderAlgorithm getRenderAlgorithm(final int pRenderLayerIndex);

	/**
	 * Cycles through rendering algorithms for all render layers
	 */
	public void cycleRenderAlgorithm();

	/**
	 * Cycles through rendering algorithms for current layer index
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 */
	public void cycleRenderAlgorithm(int pRenderLayerIndex);

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
	 * @return current render layer index
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
	 * @return number of render layers
	 */
	public int getNumberOfRenderLayers();

	/**
	 * Updates the voxel size of subsequently rendered volumes
	 *
	 * @param pVoxelSizeX
	 *          voxel size along X
	 * @param pVoxelSizeY
	 *          voxel size along Y
	 * @param pVoxelSizeZ
	 *          voxel size along Z
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
	 *          render layer index
	 * @param pByteBuffer
	 *          NIO byte buffer
	 * @param pVolumeSizeX
	 *          volume size along X
	 * @param pVolumeSizeY
	 *          volume size along Y
	 * @param pVolumeSizeZ
	 *          volume size along Z
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			ByteBuffer pByteBuffer,
																			long pVolumeSizeX,
																			long pVolumeSizeY,
																			long pVolumeSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). This data is uploaded to a given render
	 * layer.
	 *
	 * @param pTimeOut
	 *          time out delay
	 * @param pTimeUnit
	 *          time unit for time out delay
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pByteBuffer
	 *          NIO byte buffer
	 * @param pVolumeSizeX
	 *          volume size along X
	 * @param pVolumeSizeY
	 *          volume size along Y
	 * @param pVolumeSizeZ
	 *          volume suze along Z
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			int pRenderLayerIndex,
																			ByteBuffer pByteBuffer,
																			long pVolumeSizeX,
																			long pVolumeSizeY,
																			long pVolumeSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). This data is uploaded to a given render
	 * layer.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pFragmentedMemoryInterface
	 *          fragmented buffer
	 * @param pVolumeSizeX
	 *          volume size along X
	 * @param pVolumeSizeY
	 *          volume size along Y
	 * @param pVolumeSizeZ
	 *          volume size along Z
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			FragmentedMemoryInterface pFragmentedMemoryInterface,
																			long pVolumeSizeX,
																			long pVolumeSizeY,
																			long pVolumeSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). This data is uploaded to a given render
	 * layer.
	 * 
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pContiguousMemoryInterface
	 *          contiguous buffer
	 * @param pVolumeSizeX
	 *          volume size along X
	 * @param pVolumeSizeY
	 *          volume size along Y
	 * @param pVolumeSizeZ
	 *          volume size along Z
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			ContiguousMemoryInterface pContiguousMemoryInterface,
																			long pVolumeSizeX,
																			long pVolumeSizeY,
																			long pVolumeSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). In addition the real units are provided.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pByteBuffer
	 *          NIO byte buffer
	 * @param pVolumeSizeX
	 *          volume size along X
	 * @param pVolumeSizeY
	 *          volume size along Y
	 * @param pVolumeSizeZ
	 *          volume size along Z
	 * @param pVoxelSizeX
	 *          voxel size along X
	 * @param pVoxelSizeY
	 *          voxel size along Y
	 * @param pVoxelSizeZ
	 *          voxel size along Z
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	final int pRenderLayerIndex,
																			ByteBuffer pByteBuffer,
																			long pVolumeSizeX,
																			long pVolumeSizeY,
																			long pVolumeSizeZ,
																			double pVoxelSizeX,
																			double pVoxelSizeY,
																			double pVoxelSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). In addition the voxel dimensions in
	 * arbitrary units must also be provided.
	 * 
	 * @param pTimeOut
	 *          time out delay
	 * @param pTimeUnit
	 *          time unit for time out delay
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pByteBuffer
	 *          NIO byte buffer
	 * @param pVolumeSizeX
	 *          volume size along X
	 * @param pVolumeSizeY
	 *          volume size along Y
	 * @param pVolumeSizeZ
	 *          volume size along Z
	 * @param pVoxelSizeX
	 *          voxel size along X
	 * @param pVoxelSizeY
	 *          voxel size along Y
	 * @param pVoxelSizeZ
	 *          voxel size along Z
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			final int pRenderLayerIndex,
																			final ByteBuffer pByteBuffer,
																			final long pVolumeSizeX,
																			final long pVolumeSizeY,
																			final long pVolumeSizeZ,
																			final double pVoxelSizeX,
																			final double pVoxelSizeY,
																			final double pVoxelSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). In addition the voxel dimensions in
	 * arbitrary units must also be provided.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pFragmentedMemoryInterface
	 *          fragmented buffer
	 * @param pVolumeSizeX
	 *          volume size along X
	 * @param pVolumeSizeY
	 *          volume size along Y
	 * @param pVolumeSizeZ
	 *          volume size along Z
	 * @param pVoxelSizeX
	 *          voxel size along X
	 * @param pVoxelSizeY
	 *          voxel size along Y
	 * @param pVoxelSizeZ
	 *          voxel size along Z
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			FragmentedMemoryInterface pFragmentedMemoryInterface,
																			long pVolumeSizeX,
																			long pVolumeSizeY,
																			long pVolumeSizeZ,
																			double pVoxelSizeX,
																			double pVoxelSizeY,
																			double pVoxelSizeZ);

	/**
	 * Updates the displayed volume with the provided volume data of voxel
	 * dimensions (pSizeX,pSizeY,pSizeZ). In addition the voxel dimensions in
	 * arbitrary units must also be provided.
	 *
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pContiguousMemoryInterface
	 *          contiguous buffer
	 * @param pVolumeSizeX
	 *          volume size along X
	 * @param pVolumeSizeY
	 *          volume size along Y
	 * @param pVolumeSizeZ
	 *          volume size along Z
	 * @param pVoxelSizeX
	 *          voxel size along X
	 * @param pVoxelSizeY
	 *          voxel size along Y
	 * @param pVoxelSizeZ
	 *          voxel size along Z
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	int pRenderLayerIndex,
																			ContiguousMemoryInterface pContiguousMemoryInterface,
																			long pVolumeSizeX,
																			long pVolumeSizeY,
																			long pVolumeSizeZ,
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
	 *          time out delay
	 * @param pTimeUnit
	 *          time unit for time out delay
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pFragmentedMemoryInterface
	 *          fragmented buffer
	 * @param pVolumeSizeX
	 *          volume size along X
	 * @param pVolumeSizeY
	 *          volume size along Y
	 * @param pVolumeSizeZ
	 *          volume size along Z
	 * @param pVoxelSizeX
	 *          voxel size along X
	 * @param pVoxelSizeY
	 *          voxel size along Y
	 * @param pVoxelSizeZ
	 *          voxel size along Z
	 * @return true if buffer fully copied
	 */
	public boolean setVolumeDataBuffer(	long pTimeOut,
																			TimeUnit pTimeUnit,
																			int pRenderLayerIndex,
																			FragmentedMemoryInterface pFragmentedMemoryInterface,
																			long pVolumeSizeX,
																			long pVolumeSizeY,
																			long pVolumeSizeZ,
																			double pVoxelSizeX,
																			double pVoxelSizeY,
																			double pVoxelSizeZ);

	/**
	 * Updates the given render layer with a volume.
	 *
	 * @param pRenderLayerIndex
	 *          rende rlayer index
	 * 
	 * @param pVolume
	 *          Volume data to use for updating display.
	 * @return true if buffer fully copied
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
	 *          max available volumes
	 * @return volume manager
	 */
	public VolumeManager createCompatibleVolumeManager(int pMaxAvailableVolumes);

	/**
	 * Waits until volume data copy completes for all layers.
	 *
	 *
	 * @param pTimeOut
	 *          timeout delay
	 * @param pTimeUnit
	 *          time unit
	 * @return true is completed, false if it timed-out.
	 */
	public boolean waitToFinishAllDataBufferCopy(	long pTimeOut,
																								TimeUnit pTimeUnit);

	/**
	 * Waits until volume data copy completes for a given layer.
	 *
	 * @return true is completed, false if it timed-out.
	 */
	/**
	 * @param pRenderLayerIndex
	 *          render layer index
	 * @param pTimeOut
	 *          timeout delay
	 * @param pTimeUnit
	 *          time unit
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
	 * @return FOV
	 */
	float getFOV();

	/**
	 * Adds FOV.
	 * 
	 * @param pDelta
	 */
	void addFOV(double pDelta);

	/**
	 * Copies the given Quaternion to the current Quaternion values.
	 * 
	 * @param pQuaternion
	 */
	void setQuaternion(Quaternion pQuaternion);

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
	 * Adds a parameter change listener
	 * 
	 * @param pParameterChangeListener
	 */
	void addParameterChangeListener(ParameterChangeListener pParameterChangeListener);

	/**
	 * Removes a parameter change listener
	 * 
	 * @param pParameterChangeListener
	 */
	void removeParameterChangeListener(ParameterChangeListener pParameterChangeListener);

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
	 * @param pOverlay
	 *          Overlay to add.
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
	 * @return Overlay collection
	 */
	public Collection<Overlay> getOverlays();

	/**
	 * Returns the list of processors in this renderer.
	 *
	 * @return Psrocessors collection
	 */
	public Collection<Processor<?>> getProcessors();

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
	public void setAdaptiveLODActive(boolean pMultiPassOn);

	/**
	 * Sets the adaptive LOD flag
	 * 
	 * @return true if adaptive LOD is active
	 */
	boolean getAdaptiveLODActive();

	/**
	 * Returns the adaptive level-of-detail (LOD) controller.
	 * 
	 * @return adaptive LOD controller
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
