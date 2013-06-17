package clearvolume.controller;

import javax.media.opengl.GL2;

public interface RotationControllerInterface
{

	void putModelViewMatrixIn(float[] pModelViewMatrix);

	void rotateGL(GL2 pGl);

}
