package clearvolume.renderer.cleargl;

import cleargl.ClearGLWindow;

import com.jogamp.opengl.GLProfile;

public class ClearVolumeIcon
{
	static public void setIcon()
	{
		// attempt at solving Jug's Dreadlock bug:
		final GLProfile lProfile = GLProfile.getMaxProgrammable(true);
		// System.out.println( lProfile );

		// load icons:
		ClearGLWindow.setWindowIcons(	"clearvolume/icon/ClearVolumeIcon16.png",
																	"clearvolume/icon/ClearVolumeIcon32.png",
																	"clearvolume/icon/ClearVolumeIcon64.png",
																	"clearvolume/icon/ClearVolumeIcon128.png",
																	"clearvolume/icon/ClearVolumeIcon256.png",
																	"clearvolume/icon/ClearVolumeIcon512.png");
	}
}
