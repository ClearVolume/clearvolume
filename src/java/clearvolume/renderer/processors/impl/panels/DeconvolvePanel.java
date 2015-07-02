package clearvolume.renderer.processors.impl.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import clearvolume.renderer.processors.impl.OpenCLDeconvolutionLR;

public class DeconvolvePanel extends JPanel
{

	private static final long serialVersionUID = 1L;


	private final OpenCLDeconvolutionLR mOpenCLDeconvolution;
	private final JTextField mBlockSizeTextField;
	private final JTextField mSigmaTextField;
	private final JTextField mSigmaSpaceTextField;
	private final JTextField mNumberOfIterationsTextField;

	public DeconvolvePanel()
	{
		this(null);
	}

	public DeconvolvePanel(OpenCLDeconvolutionLR pOpenCLDeconvolution)
	{
		mOpenCLDeconvolution = pOpenCLDeconvolution;


		setLayout(new MigLayout("",
														"[52px,grow][134px,grow]",
														"[][28px,grow][grow][grow][grow]"));

		final JCheckBox lDeconvolutionOnOffCheckBox = new JCheckBox("Lucy-Richardson deconvolution on/off");
		add(lDeconvolutionOnOffCheckBox, "cell 0 0 2 1");
		lDeconvolutionOnOffCheckBox.setSelected(mOpenCLDeconvolution.isActive());

		final JLabel lNumberOfIterationsLabel = new JLabel("Number of iterations");
		add(lNumberOfIterationsLabel, "cell 0 1,alignx trailing");

		mNumberOfIterationsTextField = new JTextField("" + mOpenCLDeconvolution.getNumberOfIterations());
		mNumberOfIterationsTextField.setColumns(10);
		add(mNumberOfIterationsTextField, "cell 1 1,growx");


		final JLabel lBlockSizeLabel = new JLabel("Sigma X");
		add(lBlockSizeLabel, "cell 0 2,alignx right,aligny center");

		mBlockSizeTextField = new JTextField("" + mOpenCLDeconvolution.getSigmaX());
		add(mBlockSizeTextField, "cell 1 2,growx,aligny center");
		mBlockSizeTextField.setColumns(10);

		final JLabel lSigmaLabel = new JLabel("Sigma Y");
		add(lSigmaLabel, "cell 0 3,alignx right");

		mSigmaTextField = new JTextField("" + mOpenCLDeconvolution.getSigmaY());
		add(mSigmaTextField, "cell 1 3,growx");
		mSigmaTextField.setColumns(10);

		final JLabel lSigmaSpaceLabel = new JLabel("Sigma Z");
		add(lSigmaSpaceLabel, "cell 0 4,alignx trailing");
		
		mSigmaSpaceTextField = new JTextField("" + mOpenCLDeconvolution.getSigmaZ());
		add(mSigmaSpaceTextField, "cell 1 4,growx");
		mSigmaSpaceTextField.setColumns(10);

		lDeconvolutionOnOffCheckBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				mOpenCLDeconvolution.setActive(lDeconvolutionOnOffCheckBox.isSelected());
			}
		});

		mNumberOfIterationsTextField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				mOpenCLDeconvolution.setNumberOfIterations(Integer.parseInt(mNumberOfIterationsTextField.getText()));
			}
		});

		mBlockSizeTextField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				mOpenCLDeconvolution.setSigmaX(Float.parseFloat(mBlockSizeTextField.getText()));
			}
		});

		mSigmaTextField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				mOpenCLDeconvolution.setSigmaY(Float.parseFloat(mSigmaTextField.getText()));
			}
		});

		mSigmaSpaceTextField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				mOpenCLDeconvolution.setSigmaZ(Float.parseFloat(mSigmaSpaceTextField.getText()));
			}
		});
	}


}
