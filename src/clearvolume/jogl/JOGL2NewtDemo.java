package clearvolume.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

public class JOGL2NewtDemo implements GLEventListener
{ // Renderer
	private static String TITLE = "JOGL 2 with NEWT"; // window's title
	private static final int WINDOW_WIDTH = 640; // width of the drawable
	private static final int WINDOW_HEIGHT = 480; // height of the drawable
	private static final int FPS = 60; // animator's target frames per second

	private double theta = 0.0f; // rotational angle
	private static GLWindow sWindow;

	/** Constructor */
	public JOGL2NewtDemo()
	{
	}

	/** The entry main() method */
	public static void main(final String[] args)
	{
		// Get the default OpenGL profile, reflecting the best for your running
		// platform
		final GLProfile glp = GLProfile.getDefault();
		// Specifies a set of OpenGL capabilities, based on your profile.
		final GLCapabilities caps = new GLCapabilities(glp);
		sWindow = GLWindow.create(caps);

		// Create a animator that drives canvas' display() at the specified FPS.
		final FPSAnimator animator = new FPSAnimator(sWindow, FPS, true);

		sWindow.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowDestroyNotify(final WindowEvent arg0)
			{
				// Use a dedicate thread to run the stop() to ensure that the
				// animator stops before program exits.
				new Thread()
				{
					@Override
					public void run()
					{
						animator.stop(); // stop the animator loop
						System.exit(0);
					}
				}.start();
			};
		});

		sWindow.addGLEventListener(new JOGL2NewtDemo());
		sWindow.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		sWindow.setTitle(TITLE);
		sWindow.setVisible(true);
		animator.start();
	}

	/** Called back by the drawable to render OpenGL graphics */
	@Override
	public void display(final GLAutoDrawable drawable)
	{
		render(drawable);
		update();
	}

	/** Render the shape (triangle) */
	private void render(final GLAutoDrawable drawable)
	{
		final GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT);

		// Draw a triangle
		final float sine = (float) Math.sin(theta);
		final float cosine = (float) Math.cos(theta);
		gl.glBegin(GL.GL_TRIANGLES);
		gl.glColor3f(1, 0, 0);
		gl.glVertex2d(-cosine, -cosine);
		gl.glColor3f(0, 1, 0);
		gl.glVertex2d(0, cosine);
		gl.glColor3f(0, 0, 1);
		gl.glVertex2d(sine, -sine);
		gl.glEnd();
	}

	/** Update the rotation angle after each frame refresh */
	private void update()
	{
		theta += 0.01;
	}

	/** Called back immediately after the OpenGL context is initialized */
	@Override
	public void init(final GLAutoDrawable drawable)
	{
	}

	/** Called back before the OpenGL context is destroyed. */
	@Override
	public void dispose(final GLAutoDrawable drawable)
	{
	}

	/**
	 * Called back by the drawable when it is first set to visible, and during the
	 * first repaint after the it has been resized.
	 */
	@Override
	public void reshape(final GLAutoDrawable drawable,
											final int x,
											final int y,
											final int weight,
											final int height)
	{
		// sWindow.setFullscreen(true);
	}
}