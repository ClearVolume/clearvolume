package clearvolume.controller;

import clearvolume.renderer.ClearVolumeRendererInterface;

public interface RotationControllerWithRenderNotification
{
	void notifyRender(ClearVolumeRendererInterface pClearVolumeRendererInterface);
}
