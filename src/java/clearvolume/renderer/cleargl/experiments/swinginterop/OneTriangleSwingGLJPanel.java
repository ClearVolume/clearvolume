package clearvolume.renderer.cleargl.experiments.swinginterop;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

/**
 * A minimal program that draws with JOGL in a Swing JFrame using the AWT
 * GLJPanel.
 *
 * @author Wade Walker
 */
public class OneTriangleSwingGLJPanel {

	public static void main(String[] args)
	{
		final JFrame jframe = new JFrame("One Triangle Swing GLJPanel");
		jframe.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent windowevent)
			{
				jframe.dispose();
				System.exit(0);
			}
		});
		jframe.setLayout(new MigLayout(	"",
																		"[190px,grow,fill]",
																		"[grow,fill][29px][29px][29px]"));

		final GLProfile glprofile = GLProfile.getDefault();
		final GLCapabilities glcapabilities = new GLCapabilities(glprofile);
		final GLJPanelWB gljpanel = new GLJPanelWB(glcapabilities);

		gljpanel.addGLEventListener(new GLEventListener()
		{

			@Override
			public void reshape(GLAutoDrawable glautodrawable,
													int x,
													int y,
													int width,
													int height)
			{
				OneTriangle.setup(glautodrawable.getGL(),
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
				OneTriangle.render(	glautodrawable.getGL(),
														glautodrawable.getSurfaceWidth(),
														glautodrawable.getSurfaceHeight());
			}
		});


		jframe.add(gljpanel,
																"cell 0 0,alignx center,aligny top");

		final JSlider slide1 = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
		slide1.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {

				OneTriangle.r = slide1.getValue() / 255f;
				gljpanel.repaint();
				
			}
		});

		jframe.add(slide1,
																"cell 0 1,alignx center,aligny top");

		final JSlider slide2 = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
		slide2.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				OneTriangle.g = slide2.getValue() / 255f;
				gljpanel.repaint();
				
			}
		});

		jframe.add(slide2,
																"cell 0 2,alignx center,aligny top");

		final JSlider slide3 = new JSlider(JSlider.HORIZONTAL, 0, 255, 128);
		slide3.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				OneTriangle.b = slide3.getValue() / 255f;
				gljpanel.repaint();
			}
		});

		jframe.add(slide3,
																"cell 0 3,alignx center,aligny top");

		jframe.setSize(840, 680);
		jframe.setVisible(true);
	}
}