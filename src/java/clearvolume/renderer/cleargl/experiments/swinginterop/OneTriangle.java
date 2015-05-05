package clearvolume.renderer.cleargl.experiments.swinginterop;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

public class OneTriangle
{
	public static volatile float r = 1, g = 1, b = 1;

	protected static void setup(GL pGL, int width, int height)
	{
		pGL.getGL2().glMatrixMode(GL2.GL_PROJECTION);
		pGL.getGL2().glLoadIdentity();

		// coordinate system origin at lower left with width and height same as the
		// window
		final GLU glu = new GLU();
		glu.gluOrtho2D(0.0f, width, 0.0f, height);

		pGL.getGL2().glMatrixMode(GL2.GL_MODELVIEW);
		pGL.getGL2().glLoadIdentity();

		pGL.glViewport(0, 0, width, height);
	}

	protected static void render(GL pGL, int width, int height)
	{
		pGL.glClear(GL.GL_COLOR_BUFFER_BIT);

		// draw a triangle filling the window
		pGL.getGL2().glLoadIdentity();
		pGL.getGL2().glBegin(GL.GL_TRIANGLES);
		pGL.getGL2().glColor3f(r, 0, 0);
		pGL.getGL2().glVertex2f(0, 0);
		pGL.getGL2().glColor3f(0, g, 0);
		pGL.getGL2().glVertex2f(width, 0);
		pGL.getGL2().glColor3f(0, 0, b);
		pGL.getGL2().glVertex2f(width / 2, height);
		pGL.getGL2().glEnd();
	}
}