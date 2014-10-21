package clearvolume.network.ringbuffer.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import clearvolume.network.ringbuffer.RingBuffer;

public class RingBufferTests
{

	@Test
	public void test()
	{
		RingBuffer<String> lRingBuffer = new RingBuffer<String>(3);

		lRingBuffer.set("1");
		lRingBuffer.advance();
		lRingBuffer.set("2");
		lRingBuffer.advance();
		lRingBuffer.set("3");
		lRingBuffer.advance();
		assertEquals("1", lRingBuffer.get());
		lRingBuffer.advance();
		assertEquals("2", lRingBuffer.get());
		lRingBuffer.advance();
		assertEquals("3", lRingBuffer.get());

	}

}
