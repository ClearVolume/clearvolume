package clearvolume.main.demo;

import org.junit.Test;

import clearvolume.main.ClearVolumeMain;

public class ClearVolumeMainDemo
{

	@Test
	public void demoServer()
	{
		ClearVolumeMain.main(new String[]
		{ "-demoserver" });
	}

	@Test
	public void demoClient() throws InterruptedException
	{
		ClearVolumeMain.main(new String[] {});
		Thread.sleep(10000000);
	}

}
