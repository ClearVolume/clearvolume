package clearvolume;

/**
 * Enum ProjectionAlgorithm
 * 
 * Different kinds of projection algorythms available in ClearVolume.
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
	 * Blending bacjk to front.
	 */
	BlendBackToFront
}
