package clearvolume.renderer;

/**
 * Enum RenderAlgorithm
 * 
 * Different kinds of mProjectionMatrix algorithms available in ClearVolume.
 *
 * @author Loic Royer 2014
 *
 */
public enum RenderAlgorithm
{
	/**
	 * Standard max projections.
	 */
	MaxProjection,
	/**
	 * Iso surface rendering.
	 */
	IsoSurface;

	public RenderAlgorithm next()
	{
		return values()[(ordinal() + 1) % values().length];
	}
}
