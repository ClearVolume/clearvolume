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

// Simple 3D volume renderer
// calculates the eye coordinate from user provided projection matrix 

#ifndef _VOLUMERENDER_KERNEL_CU_
#define _VOLUMERENDER_KERNEL_CU_

#include <helper_cuda.h>
#include <helper_math.h>

typedef unsigned int  uint;
typedef unsigned char uchar;

cudaArray *d_volumeArray = 0;
cudaArray *d_transferFuncArray;

typedef unsigned char VolumeType;
typedef unsigned char VolumeType1;
typedef unsigned short VolumeType2;

texture<VolumeType/*BytesPerVoxel*/, 3, cudaReadModeNormalizedFloat> tex;         // 3D texture
texture<float4, 1, cudaReadModeElementType>         transferTex; // 1D transfer function texture

__constant__ float c_sizeOfTransfertFunction;

typedef struct
{
    float4 m[3];
} float3x4;

typedef struct
{
    float4 m[4];
} float4x4;


__constant__ float4x4 c_invViewMatrix;  // inverse view matrix


__constant__ float4x4 c_invProjectionMatrix;  //  inverse projection matrix

struct Ray
{
    float3 o;   // origin
    float3 d;   // direction
};

// intersect ray with a box
// http://www.siggraph.org/education/materials/HyperGraph/raytrace/rtinter3.htm

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

__device__
void printf4(const float4 &v)
{
  printf("kernel: %.2f  %.2f  %.2f  %.2f\n",v.x,v.y,v.z,v.w); 
}


__device__ uint rgbaFloatToInt(float4 rgba)
{
    rgba.x = __saturatef(rgba.x);   // clamp to [0.0, 1.0]
    rgba.y = __saturatef(rgba.y);
    rgba.z = __saturatef(rgba.z);
    rgba.w = __saturatef(rgba.w);
    return (uint(rgba.w*255)<<24) | (uint(rgba.z*255)<<16) | (uint(rgba.y*255)<<8) | uint(rgba.x*255);
}

inline __device__ bool algoMaxProjection(float4 &acc, float4 &col )
{
       	acc = fmaxf(acc,col);
        
        return false ;
}

inline __device__ bool algoSumProjection(float4 &acc, float4 &col )
{
       	acc = fmaxf(acc,col);
        
        return false ;
}


inline __device__ bool algoBlendFrontToBack( float4 &acc, float4 &col )
{
        col *= col.w;
        // "over" operator for front-to-back blending
        acc = acc + col*(1.0f - acc.w);
        
        return false;
}

inline __device__ bool algoBlendBackToFront(float4 &acc, float4 &col )
{

        // "under" operator for back-to-front blending
        acc = lerp(acc, col, col.w);
        return false;
}

inline __device__ bool algo(float4 &acc, float4 &col )
{
		return algoMaxProjection(acc,col);
}




//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

extern "C" __global__ void
volumerender(uint *d_output, uint imageW, uint imageH,
		 			float scalex, float scaley, float scalez,
         	float brightness, float trangemin, float trangemax, float gamma)
{
		
    const int maxSteps = 512;
    const float tstep = 0.02f;
     
    const float ta = 1.0/(trangemax-trangemin);
    const float tb = trangemin/(trangemin-trangemax); 
    
    const float invscalex = 1/scalex;
    const float invscaley = 1/scaley;
    const float invscalez = 1/scalez;
    const float3 boxMin = make_float3(-scalex, -scaley, -scalez);
    const float3 boxMax = make_float3(scalex, scaley, scalez);

    const uint x = blockIdx.x*blockDim.x + threadIdx.x;
    const uint y = blockIdx.y*blockDim.y + threadIdx.y;

	  	
    if ((x >= imageW) || (y >= imageH)) return;

    const float u = (x / (float) imageW)*2.0f-1.0f;
    const float v = (y / (float) imageH)*2.0f-1.0f;

    // calculate eye ray in world space
    float4 orig0, orig;
    float4 direc0, direc;
    float4 temp;
    float4 back,front;


   	front = make_float4(u,v,-1,1);
	back = make_float4(u,v,1,1);
  
    orig0 = mul(c_invProjectionMatrix,front);
	orig0 *= 1.f/orig0.w;
  
    orig = mul(c_invViewMatrix,orig0);
	orig *= 1.f/orig.w;
  

    direc0 = mul(c_invProjectionMatrix,back);
     
	direc0 *= 1.f/direc0.w;

	direc0 = normalize(direc0-orig0);


	direc = mul(c_invViewMatrix,direc0);
	direc.w = 0;

	 

	
    // calculate eye ray in world space
    Ray eyeRay;

	eyeRay.o = make_float3(orig);
	eyeRay.d = make_float3(direc);	
	


    // find intersection with box
    float tnear, tfar;
    int hit = intersectBox(eyeRay, boxMin, boxMax, &tnear, &tfar);


    if (!hit) return;

    if (tnear < 0.0f) tnear = 0.0f;     // clamp to near plane

    // march along ray from front to back, accumulating color
    float4 acc = make_float4(0.0f);

	// float t = tnear;
    // float3 pos = eyeRay.o + eyeRay.d*tnear;
    // float3 step = eyeRay.d*tstep;


	float t = tnear;

	float4 pos;
	uint i;
	for(i=0; i<maxSteps; i++) {		
	  pos = orig + t*direc;


	  pos = pos*0.5f+0.5f;    // map position to [0, 1] coordinates

	  float sample = tex3D(tex, pos.x,pos.y,pos.z);
 
	  // Mapping to transfert function range and gamma correction: 
	  float mappedsample = powf(ta*sample+tb,gamma);
 
	  // lookup in transfer function texture
	  float4 col = tex1D(transferTex,mappedsample);
        
	  algo/*ProjectionAlgorythm*/(acc,col);

	  t += tstep;
	  if (t > tfar) break;
	}


    // for (int i=0; i<maxSteps; i++)
    // {
	  
    //     // read from 3D texture
    //     // remap position to [0, 1] coordinates
    //     float sample = tex3D(tex, invscalex*pos.x*0.5f+0.5f, invscaley*pos.y*0.5f+0.5f, invscalez*pos.z*0.5f+0.5f);
 
 	// 			// Mapping to transfert function range and gamma correction: 
 	// 			float mappedsample = powf(ta*sample+tb,gamma);
 
    //     // lookup in transfer function texture
    //     float4 col = tex1D(transferTex,mappedsample);
        
    //     algo/*ProjectionAlgorythm*/(acc,col);

    //     t += tstep;

    //     if (t > tfar) break;
    //     pos += step;
    // }
    
    acc *= brightness;
    
    // write output color
    d_output[y*imageW + x] = rgbaFloatToInt(acc);
}


/*
extern "C"
void render_kernel(dim3 gridSize, dim3 blockSize, uint *d_output, uint imageW, uint imageH,
                   float scalex, float scaley, float scalez, float brightness, float trangemin, float trangemax, float gamma)
{
    d_render<<<gridSize, blockSize>>>(d_output, imageW, imageH, scalex, scaley, scalez, 
                                      brightness, trangemin, trangemax, gamma);
}

extern "C"
void copyInvViewMatrix(float *invViewMatrix, size_t sizeofMatrix)
{
    checkCudaErrors(cudaMemcpyToSymbol(c_invViewMatrix, invViewMatrix, sizeofMatrix));
}
/**/


#endif // #ifndef _VOLUMERENDER_KERNEL_CU_
