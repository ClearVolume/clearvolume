package clearvolume.utils;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

public class ClearVolumeDefaultFont
{

	private final static String cFontPath = "/clearvolume/fonts/SourceCodeProLight.ttf";
	private static Font sFont;

	public static Font getFontPlain(float pSize)
	{
		if (sFont != null)
			return sFont;

		try
		{
			sFont = Font.createFont(Font.TRUETYPE_FONT,
															ClearVolumeDefaultFont.class.getResourceAsStream(cFontPath))
									.deriveFont(Font.PLAIN, pSize);
		}
		catch (final FontFormatException | IOException e)
		{
			// use a fallback font in case the original couldn't be found or there has
			// been a problem
			// with the font format
			System.err.println("Could not use \"" + cFontPath
													+ "\" ("
													+ e.toString()
													+ "), falling back to Sans.");
			sFont = new Font("Sans", Font.PLAIN, 12);
		}
		return sFont;
	}
}
