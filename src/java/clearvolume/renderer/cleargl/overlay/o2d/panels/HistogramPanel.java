package clearvolume.renderer.cleargl.overlay.o2d.panels;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JRadioButton;

import clearvolume.renderer.cleargl.overlay.o2d.HistogramOverlay;
import clearvolume.renderer.processors.impl.OpenCLHistogram;

public class HistogramPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final JRadioButton mLograrithmRadioButton;

	private HistogramOverlay mHistogramOverlay;
	private OpenCLHistogram mOpenCLHistogram;

	public HistogramPanel()
	{
		setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		mLograrithmRadioButton = new JRadioButton("Logarithm");
		mLograrithmRadioButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				// System.out.println(mLograrithmRadioButton.isSelected());
				if (mOpenCLHistogram != null)
					mOpenCLHistogram.setLogarithm(mLograrithmRadioButton.isSelected());
				if (mHistogramOverlay != null)
					mHistogramOverlay.setLogarithm(mLograrithmRadioButton.isSelected());
			}
		});

		add(mLograrithmRadioButton);
	}

	public HistogramPanel(OpenCLHistogram pOpenCLHistogram)
	{
		this();
		mOpenCLHistogram = pOpenCLHistogram;
	}

	public HistogramPanel(HistogramOverlay pHistogramOverlay)
	{
		this();
		mHistogramOverlay = pHistogramOverlay;
	}

}
