/*
 * Copyright 1993-2012 NVIDIA Corporation.  All rights reserved.
 *
 * Please refer to the NVIDIA end user license agreement (EULA) associated
 * with this source code for terms and conditions that govern your use of
 * this software. Any use, reproduction, disclosure, or distribution of
 * this software and related documentation outside the terms of the EULA
 * is strictly prohibited.
 *
 */
 
 /*
  Adapted from the Nvidia CUDA samples
  http://developer.download.nvidia.com/compute/cuda/4_2/rel/sdk/website/OpenCL/html/samples.html
 

  Authors: 	Loic Royer		 (royer@mpi-cbg.de)
  					Martin Weigert (mweigert@mpi-cbg.de)
 */

// Simple 3D volume renderer
// calculates the eye coordinate from user provided projection matrix 


#include <helper_cuda.h>
#include <helper_math.h>


// Loop unrolling length:
#define LOOPUNROLL 16

// Typedefs:
typedef unsigned int  uint;
typedef unsigned char uchar;
typedef unsigned char VolumeType;
typedef unsigned char VolumeType1;
typedef unsigned short VolumeType2;

typedef struct
{
    float4 m[3];
} float3x4;

typedef struct
{
    float4 m[4];
} float4x4;

// Arrays:
cudaArray *d_volumeArray = 0;
cudaArray *d_transferFuncArray;

// Textures:
texture<VolumeType/*BytesPerVoxel*/, 3, cudaReadModeNormalizedFloat> tex;         // 3D texture
texture<float4, 1, cudaReadModeElementType>         transferTex; // 1D transfer function texture

// Constants holding matrices:
__constant__ float c_sizeOfTransfertFunction;
__constant__ float4x4 c_invViewMatrix;  // inverse view matrix
__constant__ float4x4 c_invProjectionMatrix;  //  inverse projection matrix

// Ray structure:
struct Ray
{
    float3 o;   // origin
    float3 d;   // direction
};


// random number generator for dithering
__forceinline__
__device__
float random(uint x, uint y)
{   
    uint a = 4421 +(1+x)*(1+y) +x +y;
    for(int i=0; i < 10; i++)
    {
        a = (uint(1664525) * a + uint(1013904223)) % uint(79197919);
    }
    float rnd = (a*1.0)/(79197919.f);
    return rnd-0.5f;
}


// intersect ray with a box
// http://www.siggraph.org/education/materials/HyperGraph/raytrace/rtinter3.htm
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


// transform vector by matrix (no translation)
__forceinline__
__device__
float3 mul(const float3x4 &M, const float3 &v)
{
    float3 r;
    r.x = dot(v, make_float3(M.m[0]));
    r.y = dot(v, make_float3(M.m[1]));
    r.z = dot(v, make_float3(M.m[2]));
    return r;
}

// transform vector by matrix with translation
__forceinline__
__device__
float4 mul(const float3x4 &M, const float4 &v)
{
    float4 r;
    r.x = dot(v, M.m[0]);
    r.y = dot(v, M.m[1]);
    r.z = dot(v, M.m[2]);
    r.w = 1.0f;
    return r;
}

// transform vector by matrix with translation:
__forceinline__
__device__
float4 mul(const float4x4 &M, const float4 &v)
{
    float4 r;
    r.x = dot(v, M.m[0]);
    r.y = dot(v, M.m[1]);
    r.z = dot(v, M.m[2]);
		r.w = dot(v, M.m[3]);

    return r;
}


// convert float4 into uint:
__forceinline__ 
__device__ 
uint rgbaFloatToInt(float4 rgba)
{
    rgba.x = __saturatef(rgba.x);   // clamp to [0.0, 1.0]
    rgba.y = __saturatef(rgba.y);
    rgba.z = __saturatef(rgba.z);
    rgba.w = __saturatef(rgba.w);
    return (uint(rgba.w*255)<<24) | (uint(rgba.z*255)<<16) | (uint(rgba.y*255)<<8) | uint(rgba.x*255);
}


// convert float4 into uint and take the max with an existing RGBA value in uint form:
__forceinline__ 
__device__ 
uint rgbaFloatToIntAndMax(uint existing, float4 rgba)
{
    rgba.x = __saturatef(rgba.x);   // clamp to [0.0, 1.0]
    rgba.y = __saturatef(rgba.y);
    rgba.z = __saturatef(rgba.z);
    rgba.w = __saturatef(rgba.w);
    
    const uint nr = uint(rgba.x*255);
    const uint ng = uint(rgba.y*255);
    const uint nb = uint(rgba.z*255);
    const uint na = uint(rgba.w*255);
    
    const uint er = existing&0xFF;
    const uint eg = (existing>>8)&0xFF;
    const uint eb = (existing>>16)&0xFF;
    const uint ea = (existing>>24)&0xFF;
    
    const uint  r = max(nr,er);
    const uint  g = max(ng,eg);
    const uint  b = max(nb,eb);
    const uint  a = max(na,ea);
    
    return a<<24|b<<16|g<<8|r ;
}



// Render function,
// performs max projection and then uses the transfert function to obtain a color per pixel:
extern "C" __global__ void
volumerender(       uint *d_output, 
							const uint imageW, 
							const uint imageH,
							const float brightness, 
							const float trangemin, 
							const float trangemax, 
							const float gamma, 
							const int   maxsteps,
							const float dithering,
							const float phase,
							const int   clear)
{
		
		// convert range bounds to linear map:
    const float ta = 1.0f/(trangemax-trangemin);
    const float tb = trangemin/(trangemin-trangemax); 
    
   	// box bounds:
    const float3 boxMin = make_float3(-1.f, -1.f, -1.f);
    const float3 boxMax = make_float3(1.f,1.f,1.f);

		// thread int coordinates:
    const uint x = blockIdx.x*blockDim.x + threadIdx.x;
    const uint y = blockIdx.y*blockDim.y + threadIdx.y;

    if ((x >= imageW) || (y >= imageH)) return;

		// thread float coordinates:
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

    // calculate eye ray in world space
    Ray eyeRay;
		eyeRay.o = make_float3(orig);
		eyeRay.d = make_float3(direc);	
	
    // find intersection with box
    float tnear, tfar;
    const int hit = intersectBox(eyeRay, boxMin, boxMax, &tnear, &tfar);

    if (!hit) 
    {
	  	d_output[x+imageW*y] = 0.f;
	    return;
    }

    // clamp to near plane:
		if (tnear < 0.0f) tnear = 0.0f;     

		// compute stape size:		
		const float tstep = abs(tnear-tfar)/maxsteps;

	  //if ((u == 0f) && (v == 0f))
    //  printf("tnear=%f tfar=%f diff=%f tstep=%f ratio=%f \n",tnear,tfar, abs(tnear-tfar),tstep, abs(tnear-tfar)/tstep);

		// apply phase:
		orig += phase*tstep*direc;

		// randomize origin point a bit:
		const uint entropy = (uint)( 6779514*length(orig) + 6257327*length(direc) );
		orig += dithering*tstep*random(entropy+x,entropy+y)*direc;
		
		// precompute vectors: 
		const float4 vecstep = 0.5f*tstep*direc;
		float4 pos = orig*0.5f+0.5f + tnear*0.5f*direc;

		// Loop unrolling setup: 
    const uint unrolledmaxsteps = (maxsteps/LOOPUNROLL)+1;
		
		// raycasting loop:
		float maxp = 0.0f;
		for(int i=0; i<unrolledmaxsteps; i++) 
		{
			for(int j=1; j<LOOPUNROLL; j++)
			{
		  	maxp = fmaxf(maxp,tex3D(tex, pos.x,pos.y,pos.z));
		  	pos+=vecstep;
		  }
		}
		
		// Mapping to transfert function range and gamma correction: 
		const float mappedsample = __saturatef(powf(ta*maxp+tb,gamma));
	 
		// lookup in transfer function texture:
		const float4 color = brightness * tex1D(transferTex,mappedsample);

    
    // write output color:
    d_output[y*imageW + x] = rgbaFloatToIntAndMax(clear*d_output[y*imageW + x],color);
}

