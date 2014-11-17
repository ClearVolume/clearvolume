package clearvolume.projections;

/**
 * Enum ProjectionAlgorithm
 * 
 * Different kinds of projection algorithms available in ClearVolume.
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
