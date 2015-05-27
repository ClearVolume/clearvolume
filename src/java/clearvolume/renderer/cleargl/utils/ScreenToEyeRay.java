package clearvolume.renderer.cleargl.utils;

import java.util.Arrays;

import cleargl.GLMatrix;

public class ScreenToEyeRay
{

	static public class EyeRay
	{
		public float[] org;
		public float[] dir;

		@Override
		public String toString()
		{
			return String.format(	"EyeRay [org=%s, dir=%s]",
														Arrays.toString(org),
														Arrays.toString(dir));
		}

	}

	public static final EyeRay convert(	int width,
																			int height,
																			int mouseX,
																			int mouseY,
																			GLMatrix pInverseModelViewMatrix,
																			GLMatrix pInverseProjectionMatrix)
	{
		final float u = (mouseX / (float) width) * 2.0f - 1.0f;
		final float v = ((height - mouseY) / (float) height) * 2.0f - 1.0f;

		final float[] front = new float[]
		{ u, v, -1.f, 1.f };
		final float[] back = new float[]
		{ u, v, 1.f, 1.f };

		final float[] orig0 = pInverseProjectionMatrix.mult(front);
		GLMatrix.mult(orig0, 1.0f / orig0[3]);
		final float[] orig = pInverseModelViewMatrix.mult(orig0);
		GLMatrix.mult(orig, 1.0f / orig[3]);

		final float[] direc0 = pInverseProjectionMatrix.mult(back);
		GLMatrix.mult(direc0, 1.0f / direc0[3]);
		GLMatrix.sub(direc0, orig0);
		GLMatrix.normalize(direc0);
		final float[] direc = pInverseModelViewMatrix.mult(direc0);
		direc[3] = 0;

		GLMatrix.mult(orig, 0.5f);
		GLMatrix.add(orig, 0.5f);

		final EyeRay lEyeRay = new EyeRay();
		lEyeRay.org = orig;
		lEyeRay.dir = direc;

		return lEyeRay;
	}

	/*
	public static final int intersectBox(	EyeRay pEyeRay, float[] pBoxMin, float[] pBoxMax, )
	{
	
		
	}
	
	//intersect ray with a box
	//http://www.siggraph.org/education/materials/HyperGraph/raytrace/rtinter3.htm
	__forceinline__
	__device__
	int intersectBox(Ray r, float3 boxmin, float3 boxmax, float *tnear, float *tfar)
	{
	 // compute intersection of ray with all six bbox planes
	 float3 invR = make_float3(1.0f) / r.d;
	 float3 tbot = invR * (boxmin - r.o);
	 float3 ttop = invR * (boxmax - r.o);

	 // re-order intersections to find smallest and largest on each axis
	 float3 tmin = fminf(ttop, tbot);
	 float3 tmax = fmaxf(ttop, tbot);

	 // find the largest tmin and the smallest tmax
	 float largest_tmin = fmaxf(fmaxf(tmin.x, tmin.y), fmaxf(tmin.x, tmin.z));
	 float smallest_tmax = fminf(fminf(tmax.x, tmax.y), fminf(tmax.x, tmax.z));

	 *tnear = largest_tmin;
	 *tfar = smallest_tmax;


	 return smallest_tmax > largest_tmin;
	}

	/**/

}
