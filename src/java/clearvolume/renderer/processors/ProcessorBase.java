package clearvolume.renderer.processors;

import java.util.ArrayList;

public abstract class ProcessorBase<R> implements Processor<R>
{
	private ArrayList<ProcessorResultListener<R>> mListenerList = new ArrayList<>();

	@Override
	public void addResultListener(ProcessorResultListener<R> pProcessorResultListener)
	{
		mListenerList.add(pProcessorResultListener);
	};

	@Override
	public void removeResultListener(ProcessorResultListener<R> pProcessorResultListener)
	{
		mListenerList.remove(pProcessorResultListener);
	};

	public void notifyListenersOfResult(R pResult)

	{
		for (ProcessorResultListener<R> lListener : mListenerList)
			lListener.notifyResult(this, pResult);
	};

	@Override
	public abstract boolean isCompatibleRenderer(Class<?> pRendererClass);

	@Override
	public abstract void process(	int pRenderLayerIndex,
																long pWidthInVoxels,
																long pHeightInVoxels,
																long pDepthInVoxels);

	/**
	 * Integral division, rounding the result to the next highest integer.
	 *
	 * @param a
	 *          Dividend
	 * @param b
	 *          Divisor
	 * @return a/b rounded to the next highest integer.
	 */
	protected static long iDivUp(final long a, final long b)
	{
		return ((a % b != 0) ? (a / b + 1) : (a / b));
	}
}
