package clearvolume.renderer.jogl.utils;

import java.util.Arrays;

import javax.media.opengl.glu.GLU;

import cleargl.GLMatrix;

import com.jogamp.opengl.math.VectorUtil;

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

	private static ThreadLocal<GLU> sThreadLocalGLU = new ThreadLocal<GLU>();

	public static final EyeRay convert(	int width,
																			int height,
																			int mouseX,
																			int mouseY,
																			GLMatrix pInverseModelViewMatrix,
																			GLMatrix pInverseProjectionMatrix)
	{
		final float u = (mouseX / (float) width) * 2.0f - 1.0f;
		final float v = (mouseY / (float) height) * 2.0f - 1.0f;



		System.out.format("U=%g, V=%g \n", u, v);

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

		/*	// thread float coordinates:
		    const float u = (x / (float) imageW)*2.0f-1.0f;
		    const float v = (y / (float) imageH)*2.0f-1.0f;

				// Back and front before all transformations.
		   	const float4 front = make_float4(u,v,-1.f,1.f);
				const float4 back = make_float4(u,v,1.f,1.f);

		    // calculate eye ray in world space
		    float4 orig0, orig;
		    float4 direc0, direc;
		  
		  	// Origin point
		    orig0 = mul(c_invProjectionMatrix,front);
				orig0 *= 1.f/orig0.w;
		    orig = mul(c_invViewMatrix,orig0);
				orig *= 1.f/orig.w;
		  
		  	// Direction:
		    direc0 = mul(c_invProjectionMatrix,back);
				direc0 *= 1.f/direc0.w;
				direc0 = normalize(direc0-orig0);
				direc = mul(c_invViewMatrix,direc0);
				direc.w = 0.0f;

		    // eye ray in world space:
		    Ray eyeRay;
				eyeRay.o = make_float3(orig);
				eyeRay.d = make_float3(direc);	*/

	}

	public static final EyeRay convertOld(int mouseX,
																				int mouseY,
																				float[] model,
																				float[] proj,
																				int[] view)
	{
		final GLU lGLU = ensureGLUAllocated();

		final float[] lFront = new float[3];
		final float[] lBack = new float[3];

		lGLU.gluUnProject(mouseX,
											mouseY,
											-1,
											model,
											0,
											proj,
											0,
											view,
											0,
											lFront,
											0);
		lGLU.gluUnProject(mouseX,
											mouseY,
											1,
											model,
											0,
											proj,
											0,
											view,
											0,
											lBack,
											0);

		final float[] lDirection = VectorUtil.subVec3(new float[3],
																									lBack,
																									lFront);
		VectorUtil.normalizeVec3(lDirection);

		final EyeRay lEyeRay = new EyeRay();
		lEyeRay.org = lFront;
		lEyeRay.dir = lDirection;

		return lEyeRay;
	}

	private static GLU ensureGLUAllocated()
	{
		GLU lGLU = sThreadLocalGLU.get();

		if (lGLU == null)
		{
			lGLU = new GLU();
			sThreadLocalGLU.set(lGLU);
		}

		return lGLU;
	}

}
