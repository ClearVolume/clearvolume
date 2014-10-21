package clearvolume.network.ringbuffer;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class RingBuffer<T>
{
	private int mRingBufferMaxLength;
	private volatile int mPointer;
	private ArrayList<T> mArrayList;

	private ReentrantLock mLock = new ReentrantLock();

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
		mPointer = normalizePointer(mPointer + 1);
		final int lPointer = mPointer;
		mLock.unlock();
		return lPointer;
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
		T lEntry = mArrayList.get(mPointer);
		mLock.unlock();
		return lEntry;
	}

	private int normalizePointer(int pPointer)
	{
		return pPointer % mRingBufferMaxLength;
	}

}
