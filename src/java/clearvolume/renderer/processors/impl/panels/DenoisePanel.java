package clearvolume.renderer.processors.impl.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import clearvolume.renderer.processors.impl.OpenCLDenoise;
import net.miginfocom.swing.MigLayout;

public class DenoisePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final OpenCLDenoise mOpenCLDenoise;
	private final JTextField mBlockSizeTextField;
	private final JTextField mSigmaTextField;
	private final JTextField mSigmaSpaceTextField;

	public DenoisePanel()
	{
		this(null);
	}

	public DenoisePanel(OpenCLDenoise pOpenCLDenoise)
	{
		mOpenCLDenoise = pOpenCLDenoise;

		final JComboBox<OpenCLDenoise.DenoiseAlgorithm> lAlgorithmSelectionComboBox = new JComboBox<OpenCLDenoise.DenoiseAlgorithm>();
		lAlgorithmSelectionComboBox.addItem(OpenCLDenoise.DenoiseAlgorithm.BilateralFiltering);
		lAlgorithmSelectionComboBox.addItem(OpenCLDenoise.DenoiseAlgorithm.LocalMeans);
		lAlgorithmSelectionComboBox.setSelectedItem(mOpenCLDenoise.getDenoiseAlgorithm());

		setLayout(new MigLayout("",
								"[52px,grow][134px,grow]",
								"[][28px,grow][grow][grow][grow]"));

		final JCheckBox lDenoisingOnOffCheckBox = new JCheckBox("denoising on/off");
		add(lDenoisingOnOffCheckBox, "cell 0 0 2 1");
		lDenoisingOnOffCheckBox.setSelected(mOpenCLDenoise.isActive());

		add(lAlgorithmSelectionComboBox,
			"cell 0 1 2 1,alignx left,aligny center");

		final JLabel lBlockSizeLabel = new JLabel("Block size");
		add(lBlockSizeLabel, "cell 0 2,alignx right,aligny center");

		mBlockSizeTextField = new JTextField(""	+ mOpenCLDenoise.getBlockSize());
		add(mBlockSizeTextField, "cell 1 2,growx,aligny center");
		mBlockSizeTextField.setColumns(10);

		final JLabel lSigmaLabel = new JLabel("Sigma");
		add(lSigmaLabel, "cell 0 3,alignx right");

		mSigmaTextField = new JTextField(""	+ mOpenCLDenoise.getSigma());
		add(mSigmaTextField, "cell 1 3,growx");
		mSigmaTextField.setColumns(10);

		final JLabel lSigmaSpaceLabel = new JLabel("Sigma space");
		add(lSigmaSpaceLabel, "cell 0 4,alignx trailing");

		mSigmaSpaceTextField = new JTextField("" + mOpenCLDenoise.getSigmaSpace());
		add(mSigmaSpaceTextField, "cell 1 4,growx");
		mSigmaSpaceTextField.setColumns(10);

		lDenoisingOnOffCheckBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				mOpenCLDenoise.setActive(lDenoisingOnOffCheckBox.isSelected());
			}
		});

		lAlgorithmSelectionComboBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				mOpenCLDenoise.setDenoiseAlgorithm((OpenCLDenoise.DenoiseAlgorithm) lAlgorithmSelectionComboBox.getSelectedItem());
			}
		});

		mBlockSizeTextField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				mOpenCLDenoise.setBlockSize(Integer.parseInt(mBlockSizeTextField.getText()));
			}
		});

		mSigmaTextField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				mOpenCLDenoise.setSigma(Float.parseFloat(mSigmaTextField.getText()));
			}
		});

		mSigmaSpaceTextField.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent pE)
			{
				mOpenCLDenoise.setSigmaSpace(Float.parseFloat(mSigmaSpaceTextField.getText()));
			}
		});
	}

}
