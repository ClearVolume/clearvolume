package clearvolume.jcuda;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Inner class encapsulating the MouseMotionListener and MouseWheelListener for
 * the interaction
 */
class MouseControl implements MouseMotionListener, MouseWheelListener
{
	/**
	 * 
	 */
	private final JCudaClearVolumeRenderer mRenderer;

	/**
	 * @param pJCudaClearVolumeRenderer
	 */
	MouseControl(final JCudaClearVolumeRenderer pJCudaClearVolumeRenderer)
	{
		mRenderer = pJCudaClearVolumeRenderer;
	}

	private Point previousMousePosition = new Point();

	@Override
	public void mouseDragged(final MouseEvent e)
	{
		final int dx = e.getX() - previousMousePosition.x;
		final int dy = e.getY() - previousMousePosition.y;

		// If the left button is held down, move the object
		if (!e.isShiftDown() && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK)
		{
			mRenderer.translationX += dx / 100.0f;
			mRenderer.translationY -= dy / 100.0f;
		}

		// If the right button is held down, rotate the object
		else if (e.isShiftDown() && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK)
		{
			mRenderer.rotationX += dy;
			mRenderer.rotationY += dx;
		}
		previousMousePosition = e.getPoint();
	}

	@Override
	public void mouseMoved(final MouseEvent e)
	{
		previousMousePosition = e.getPoint();
	}

	@Override
	public void mouseWheelMoved(final MouseWheelEvent e)
	{
		// Translate along the Z-axis
		mRenderer.translationZ += e.getWheelRotation() * 0.25f;
		previousMousePosition = e.getPoint();
	}
}