package clearvolume.main.demo;

import org.junit.Test;

import clearvolume.main.ClearVolumeMain;

public class ClearVolumeMainDemo
{

	@Test
	public void testDemoServer()
	{
		ClearVolumeMain.main(new String[]
		{ "-demoserver" });
	}

	@Test
	public void testClient() throws InterruptedException
	{
		ClearVolumeMain.main(new String[] {});
		Thread.sleep(10000000);
	}

}
