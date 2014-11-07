package clearvolume.volume.sink.filter.gui;

import gnu.trove.list.array.TIntArrayList;

import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.AbstractListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import clearvolume.volume.sink.filter.ChannelFilterSink;

public class ChannelFilterSinkJPanel extends JPanel	implements
																										ListSelectionListener,
																										ListDataListener
{

	private JList<String> mActiveChannelJList;
	private ChannelFilterSink mChannelFilterSink;

	public static final void createJFrame(ChannelFilterSink pChannelFilterSink)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					JFrame lJFrame = new JFrame();

					ChannelFilterSinkJPanel lChannelFilterSinkJPanel = new ChannelFilterSinkJPanel(pChannelFilterSink);
					lJFrame.getContentPane().add(lChannelFilterSinkJPanel);
					lJFrame.setVisible(true);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the panel.
	 */
	public ChannelFilterSinkJPanel(final ChannelFilterSink pChannelFilterSink)
	{
		mChannelFilterSink = pChannelFilterSink;
		setBackground(Color.WHITE);
		setLayout(new MigLayout("",
														"[grow,fill]",
														"[26px:26px,grow,fill]"));

		mActiveChannelJList = new JList<String>();
		mActiveChannelJList.setVisibleRowCount(16);
		mActiveChannelJList.setSelectionBackground(new Color(	102,
																													102,
																													255));
		mActiveChannelJList.setModel(new AbstractListModel()
		{
			String[] values = new String[]
			{ "1",
				"2",
				"3",
				"4",
				"5",
				"6",
				"7",
				"8",
				"9",
				"10",
				"11",
				"12",
				"13",
				"14",
				"15",
				"16" };

			@Override
			public int getSize()
			{
				return values.length;
			}

			@Override
			public Object getElementAt(int index)
			{
				return values[index];
			}
		});
		mActiveChannelJList.addListSelectionListener(this);
		add(mActiveChannelJList, "cell 0 0,grow");
		// mActiveChannelJList.setModel(null);

		if (pChannelFilterSink != null)
		{
			ListModel<String> lChannelListModel = pChannelFilterSink.getChannelListModel();
			mActiveChannelJList.setModel(lChannelListModel);


			lChannelListModel.addListDataListener(this);
		}
		else
		{
		}



	}

	private void update()
	{
		int[] lActiveChannels = mActiveChannelJList.getSelectedIndices();
		mChannelFilterSink.setActiveChannels(lActiveChannels);
	}

	@Override
	public void valueChanged(ListSelectionEvent pE)
	{
		update();
	}

	@Override
	public void intervalAdded(ListDataEvent pE)
	{
		SwingUtilities.invokeLater(() -> {
			TIntArrayList lSelectedIndices = new TIntArrayList(mActiveChannelJList.getSelectedIndices());
			for (int i = pE.getIndex0(); i < pE.getIndex1(); i++)
				lSelectedIndices.add(i);
			mActiveChannelJList.setSelectedIndices(lSelectedIndices.toArray());
		});
	}

	@Override
	public void intervalRemoved(ListDataEvent pE)
	{
	}

	@Override
	public void contentsChanged(ListDataEvent pE)
	{
	}

}
