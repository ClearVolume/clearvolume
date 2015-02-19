package clearvolume.network.ringbuffer;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class RingBuffer<T>
{
	private final int mRingBufferMaxLength;
	private volatile int mPointer;
	private final ArrayList<T> mArrayList;

	private final ReentrantLock mLock = new ReentrantLock();

	public RingBuffer(final int pRingBufferMaxLength)
	{
		mRingBufferMaxLength = pRingBufferMaxLength;
		mArrayList = new ArrayList<T>(mRingBufferMaxLength);
		for (int i = 0; i < mRingBufferMaxLength; i++)
			mArrayList.add(null);
	}

	public int advance()
	{
		mLock.lock();
		try
		{
			mPointer = normalizePointer(mPointer + 1);
			final int lPointer = mPointer;
			return lPointer;
		}
		finally
		{
			if (mLock.isHeldByCurrentThread())
				mLock.unlock();
		}

	}

	public void set(T pNewEntry)
	{
		mLock.lock();
		mArrayList.set(mPointer, pNewEntry);
		mLock.unlock();
	}

	public T get()
	{
		mLock.lock();
		final T lEntry = mArrayList.get(mPointer);
		mLock.unlock();
		return lEntry;
	}

	private int normalizePointer(int pPointer)
	{
		return pPointer % mRingBufferMaxLength;
	}

}
