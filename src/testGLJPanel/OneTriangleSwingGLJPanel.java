package testGLJPanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import javax.swing.JSlider;

/**
 * A minimal program that draws with JOGL in a Swing JFrame using the AWT
 * GLJPanel.
 *
 * @author Wade Walker
 */
public class OneTriangleSwingGLJPanel
{

	public static void main(String[] args)
	{
		GLProfile glprofile = GLProfile.getDefault();
		GLCapabilities glcapabilities = new GLCapabilities(glprofile);
		GLJPanel gljpanel = new GLJPanel(glcapabilities);

		gljpanel.addGLEventListener(new GLEventListener()
		{

			@Override
			public void reshape(GLAutoDrawable glautodrawable,
													int x,
													int y,
													int width,
													int height)
			{
				OneTriangle.setup(glautodrawable.getGL().getGL2(),
													width,
													height);
			}

			@Override
			public void init(GLAutoDrawable glautodrawable)
			{
			}

			@Override
			public void dispose(GLAutoDrawable glautodrawable)
			{
			}

			@Override
			public void display(GLAutoDrawable glautodrawable)
			{
				OneTriangle.render(	glautodrawable.getGL().getGL2(),
														glautodrawable.getSurfaceWidth(),
														glautodrawable.getSurfaceHeight());
			}
		});

		final JFrame jframe = new JFrame("One Triangle Swing GLJPanel");
		jframe.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent windowevent)
			{
				jframe.dispose();
				System.exit(0);
			}
		});
		jframe.setLayout(new FlowLayout());
		jframe.getContentPane().add(gljpanel, BorderLayout.CENTER);

		JSlider slide = new JSlider(JSlider.HORIZONTAL, 0, 10, 2);

		jframe.getContentPane().add(slide);

		jframe.setSize(840, 680);
		jframe.setVisible(true);
	}
}