package clearvolume.renderer;

/**
 * Enum ProjectionAlgorithm
 * 
 * Different kinds of mProjectionMatrix algorithms available in ClearVolume.
 *
 * @author Loic Royer 2014
 *
 */
public enum ProjectionAlgorithm
{
	/**
	 * Standard max projections.
	 */
	MaxProjection,
	/**
	 * Projects closest local maximum to eye.
	 */
	LocalMaxProjection,
	/**
	 * Blending front to back.
	 */
	BlendFrontToBack,
	/**
	 * Blending back to front.
	 */
	BlendBackToFront
}
