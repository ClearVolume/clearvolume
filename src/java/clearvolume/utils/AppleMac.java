package clearvolume.utils;

import java.awt.Image;
import java.awt.Window;
import java.lang.reflect.Method;

public class AppleMac
{

	// final static com.apple.eawt.Application cApplication =
	// com.apple.eawt.Application.getApplication();
	static Class<?> cClass;
	static Object cApplication;

	static
	{
		try
		{
			cClass = Class.forName("com.apple.eawt.Application");
			Method lMethod = cClass.getDeclaredMethod("getApplication");
			cApplication = lMethod.invoke(null);
		}
		catch (Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
		}
	}

	private static String OS = System.getProperty("os.name")
										.toLowerCase();

	public static boolean isMac()
	{
		return (OS.indexOf("mac") >= 0);
	}

	public static final boolean setApplicationIcon(final Image pImage)
	{
		if (!isMac())
			return false;

		try
		{
			final Thread lThread = new Thread("Apple Set Application Icon")
			{
				@Override
				public void run()
				{
					try
					{
						Method lMethod = cClass.getDeclaredMethod(	"setDockIconImage",
																	Image.class);
						lMethod.invoke(cApplication, pImage);
						// cApplication.setDockIconImage(pImage);
					}
					catch (final Throwable e)
					{
						System.out.println(AppleMac.class.getSimpleName() + ": Could not set Dock Icon (not on osx?)");
					}
					super.run();
				}
			};

			lThread.start();

			return true;
		}
		catch (final Throwable e)
		{
			System.err.println(e.getMessage());
			return false;
		}
	}

	public static final boolean setApplicationName(final String pString)
	{
		if (!isMac())
			return false;

		try
		{
			System.setProperty(	"com.apple.mrj.application.apple.menu.about.name",
								pString);
			return true;
		}
		catch (final Throwable e)
		{
			System.err.println(e.getMessage());
			return false;
		}
	}

	@SuppressWarnings(
	{ "unchecked", "rawtypes" })
	public static boolean enableOSXFullscreen(final Window window)
	{
		if (!isMac())
			return false;

		if (window == null)
			return false;

		try
		{
			final Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
			final Class params[] = new Class[]
			{ Window.class, Boolean.TYPE };
			final Method method = util.getMethod(	"setWindowCanFullScreen",
													params);
			method.invoke(util, window, true);
			return true;
		}
		catch (final Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return false;
		}

	}

	public final static boolean requestFullscreen(final Window pWindow)
	{
		if (!isMac())
			return false;

		try
		{
			// cApplication.requestToggleFullScreen(pWindow);
			Method lMethod = cClass.getDeclaredMethod(	"requestToggleFullScreen",
														Window.class);
			lMethod.invoke(cApplication, pWindow);
			return true;
		}
		catch (final Throwable e)
		{
			System.err.println(e.getLocalizedMessage());
			return false;
		}
	}

}
