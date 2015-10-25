/*
volume and iso surface rendering

		volume rendering adapted from the Nvidia sdk sample
  		http://developer.download.nvidia.com/compute/cuda/4_2/rel/sdk/website/OpenCL/html/samples.html
 

  Author: Martin Weigert (mweigert@mpi-cbg.de)
    	  Loic Royer		 (royer@mpi-cbg.de)
*/


// Loop unrolling length:
#define LOOPUNROLL 16

float __constant Exposure = 0.315f;
float __constant White = 0.928f;

// rgb2xyz and back
inline float3 XYZToRGB(const float3 xyz) {
    float3 rgb;
	rgb.x =  3.240479f*xyz.x - 1.537150f*xyz.y - 0.498535f*xyz.z;
	rgb.y = -0.969256f*xyz.x + 1.875991f*xyz.y + 0.041556f*xyz.z;
	rgb.z =  0.055648f*xyz.x - 0.204043f*xyz.y + 1.057311f*xyz.z;

	return rgb;
}

inline float3 RGBToXYZ(const float3 rgb) {
    float3 xyz;
	xyz.x = 0.412453f*rgb.x + 0.357580f*rgb.y + 0.180423f*rgb.z;
	xyz.y = 0.212671f*rgb.x + 0.715160f*rgb.y + 0.072169f*rgb.z;
	xyz.z = 0.019334f*rgb.x + 0.119193f*rgb.y + 0.950227f*rgb.z;

	return xyz;
}


// random number generator for dithering
inline
float random(uint x, uint y)
{   
    uint a = 4421 +(1+x)*(1+y) +x +y;

    for(uint i=0; i < 10; i++)
    {
        a = ((uint)1664525 * a + (uint)1013904223) % (uint)79197919;
    }

    float rnd = (a*1.0f)/(79197919.f);
    
    return rnd-0.5f;
}


// intersect ray with a box
// http://www.siggraph.org/education/materials/HyperGraph/raytrace/rtinter3.htm
inline
int intersectBox(float4 r_o, float4 r_d, float4 boxmin, float4 boxmax, float *tnear, float *tfar)
{
    // compute intersection of ray with all six bbox planes
    float4 invR = (float4)(1.0f,1.0f,1.0f,1.0f) / r_d;
    float4 tbot = invR * (boxmin - r_o);
    float4 ttop = invR * (boxmax - r_o);

    // re-order intersections to find smallest and largest on each axis
    float4 tmin = min(ttop, tbot);
    float4 tmax = max(ttop, tbot);

    // find the largest tmin and the smallest tmax
    float largest_tmin = max(max(tmin.x, tmin.y), max(tmin.x, tmin.z));
    float smallest_tmax = min(min(tmax.x, tmax.y), min(tmax.x, tmax.z));

	*tnear = largest_tmin;
	*tfar = smallest_tmax;

	return smallest_tmax > largest_tmin;
}


// convert float4 into uint:
inline
uint rgbaFloatToInt(float4 rgba)
{
    rgba = clamp(rgba,(float4)(0.f,0.f,0.f,0.f),(float4)(1.f,1.f,1.f,1.f));
    
    return ((uint)(rgba.w*255)<<24) | ((uint)(rgba.z*255)<<16) | ((uint)(rgba.y*255)<<8) | (uint)(rgba.x*255);
}

inline
int4 rgbaFloatToVInt(float4 rgba) {
    rgba = 255.0f*clamp(rgba,(float4)(0.f,0.f,0.f,0.f),(float4)(1.f,1.f,1.f,1.f));
    return convert_int4(rgba);
}

// convert float4 into uint and take the max with an existing RGBA value in uint form:
inline
uint rgbaFloatToIntAndMax(uint existing, float4 rgba)
{
    rgba = clamp(rgba,(float4)(0.f,0.f,0.f,0.f),(float4)(1.f,1.f,1.f,1.f));
    
    const uint nr = (uint)(rgba.x*255);
    const uint ng = (uint)(rgba.y*255);
    const uint nb = (uint)(rgba.z*255);
    const uint na = (uint)(rgba.w*255);
    
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


// multiply matrix with vector
float4 mult(__constant float* M, float4 v){
  float4 res;
  res.x = dot(v, (float4)(M[0],M[1],M[2],M[3]));
  res.y = dot(v, (float4)(M[4],M[5],M[6],M[7]));
  res.z = dot(v, (float4)(M[8],M[9],M[10],M[11]));
  res.w = dot(v, (float4)(M[12],M[13],M[14],M[15]));
  return res;
}


__kernel void AvgLuminance(
    __global float *d_input,
    __global float *avgL,
    const uint imageW,
    const uint imageH) {

    const uint x = get_global_id(0);
    const uint y = get_global_id(1);

    if(x >= imageW || y >= imageH) {
      return;
    }

    float3 color = float3(d_input[x + y*imageW + 0],
                          d_input[x + y*imageW + 1],
                          d_input[x + y*imageW + 2]);

    *avgL += dot(color, float3(0.2126f, 0.7152f, 0.0722f));
}

__kernel void tonemapping(
    __global float *d_input,
    __global unsigned char *d_output,
    const uint imageW,
    const uint imageH,
    float avgLuminance
) {
  const uint x = get_global_id(0);
  const uint y = get_global_id(1);

  if(x >= imageW || y >= imageH) {
    return;
  }

  float4 rgbColor = float4(d_input[x*4 + y*imageW*4 + 0],
                           d_input[x*4 + y*imageW*4 + 1],
                           d_input[x*4 + y*imageW*4 + 2],
                           d_input[x*4 + y*imageW*4 + 3]);


  float3 xyzCol = RGBToXYZ(rgbColor.xyz);

  float xyzSum = xyzCol.x + xyzCol.y + xyzCol.z;
  float3 xyYCol = float3(0.0f);

  if(xyzSum > 0.0f) {
    xyYCol = float3(xyzCol.x / xyzSum,
                    xyzCol.y / xyzSum,
                    xyzCol.y);
  }

  float L = (Exposure * xyYCol.z)/avgLuminance;
  L = (L * (1+L/(White*White))) / (1+L);

  if(xyYCol.y > 0.0f) {
    xyzCol.x = (L*xyYCol.x)/(xyYCol.y);
    xyzCol.y = L;
    xyzCol.z = (L * (1 - xyYCol.x - xyYCol.y))/xyYCol.y;
  }
  //printf("%f %f %f\n", xyzCol.x, xyzCol.y, xyzCol.z);

  float3 outcolor = XYZToRGB(xyzCol);
  float4 oc;
  bool noToneMap = true;

  if(noToneMap) {
    oc = rgbColor;
  } else {
    oc = float4(outcolor.x, outcolor.y, outcolor.z, rgbColor.w);
  }

  int4 final_oc = rgbaFloatToVInt(oc);

  d_output[4*x + y*imageW*4 + 0] = final_oc.x;
  d_output[4*x + y*imageW*4 + 1] = final_oc.y;
  d_output[4*x + y*imageW*4 + 2] = final_oc.z;
  d_output[4*x + y*imageW*4 + 3] = final_oc.w;
}

// Render function,
// performs max projection and then uses the transfert function to obtain a color per pixel:
__kernel void
mcrt_render(								__global float	*d_output,
													const	uint  imageW, 
													const	uint  imageH,
													const	float brightness,
													const	float trangemin, 
													const	float trangemax, 
													const	float gamma,
													const	int   maxsteps,
													const	float dithering,
													const	float phase,
													const	int   clear,
									__read_only image2d_t 	transferColor4,
									__constant float* 		invP,
									__constant float* 		invM,
									__read_only image3d_t 	volume)
{
	// samplers:
  const sampler_t volumeSampler   =   CLK_NORMALIZED_COORDS_TRUE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR ;
  const sampler_t transferSampler =   CLK_NORMALIZED_COORDS_TRUE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR ;

	// convert range bounds to linear map:
  const float ta = 1.f/(trangemax-trangemin);
  const float tb = trangemin/(trangemin-trangemax); 

  // box bounds:
  const float4 boxMin = (float4)(-1.0f, -1.0f, -1.0f,-1.0f);
  const float4 boxMax = (float4)(1.0f, 1.0f, 1.0f,1.0f);

	// thread int coordinates:
  const uint x = get_global_id(0);
  const uint y = get_global_id(1);
  
  if ((x >= imageW) || (y >= imageH)) return;
  
  // thread float coordinates:
  const float u = (x / (float) imageW)*2.0f-1.0f;
  const float v = (y / (float) imageH)*2.0f-1.0f;

  // front and back:
  const float4 front = (float4)(u,v,-1.f,1.f);
  const float4 back = (float4)(u,v,1.f,1.f);
  
  // calculate eye ray in world space
  float4 orig0, orig;
  float4 direc0, direc;
  
  orig0.x = dot(front, ((float4)(invP[0],invP[1],invP[2],invP[3])));
  orig0.y = dot(front, ((float4)(invP[4],invP[5],invP[6],invP[7])));
  orig0.z = dot(front, ((float4)(invP[8],invP[9],invP[10],invP[11])));
  orig0.w = dot(front, ((float4)(invP[12],invP[13],invP[14],invP[15])));

  orig0 *= 1.f/orig0.w;

  orig.x = dot(orig0, ((float4)(invM[0],invM[1],invM[2],invM[3])));
  orig.y = dot(orig0, ((float4)(invM[4],invM[5],invM[6],invM[7])));
  orig.z = dot(orig0, ((float4)(invM[8],invM[9],invM[10],invM[11])));
  orig.w = dot(orig0, ((float4)(invM[12],invM[13],invM[14],invM[15])));

  orig *= 1.f/orig.w;
  
  direc0.x = dot(back, ((float4)(invP[0],invP[1],invP[2],invP[3])));
  direc0.y = dot(back, ((float4)(invP[4],invP[5],invP[6],invP[7])));
  direc0.z = dot(back, ((float4)(invP[8],invP[9],invP[10],invP[11])));
  direc0.w = dot(back, ((float4)(invP[12],invP[13],invP[14],invP[15])));

  direc0 *= 1.f/direc0.w;

  direc0 = normalize(direc0-orig0);

  direc.x = dot(direc0, ((float4)(invM[0],invM[1],invM[2],invM[3])));
  direc.y = dot(direc0, ((float4)(invM[4],invM[5],invM[6],invM[7])));
  direc.z = dot(direc0, ((float4)(invM[8],invM[9],invM[10],invM[11])));
  direc.w = 0.0f;

 
  // find intersection with box
  float tnear, tfar;
  const int hit = intersectBox(orig,direc, boxMin, boxMax, &tnear, &tfar);
  if (!hit || tfar<=0) 
  {
  	d_output[4*x+y*imageW*4+0] = 0.0f;
      d_output[4*x+y*imageW*4+1] = 0.0f;
      d_output[4*x+y*imageW*4+2] = 0.0f;
      d_output[4*x+y*imageW*4+3] = 0.0f;
  	return;
  }
  
  // clamp to near plane:
  if (tnear < 0.0f) tnear = 0.0f;     

  // compute step size:
  const float tstep = fabs(tnear-tfar)/((maxsteps/LOOPUNROLL)*LOOPUNROLL);
  
  // apply phase:
  orig += phase*tstep*direc;
  
  // randomize origin point a bit:
  const uint entropy = (uint)( 6779514*fast_length(orig) + 6257327*fast_length(direc) );
  orig += dithering*tstep*random(entropy+x,entropy+y)*direc;
	
  // precompute vectors: 
  const float4 vecstep = 0.5f*tstep*direc;
  float4 pos = orig*0.5f + 0.5f + tnear*0.5f*direc;

  // Loop unrolling setup: 
  const int unrolledmaxsteps = (maxsteps/LOOPUNROLL);

  // raycasting loop:
  float maxp = 0.0f;
	for(int i=0; i<unrolledmaxsteps; i++) 
	{
		for(int j=1; j<LOOPUNROLL; j++)
		{
     	  	maxp = fmax(maxp,read_imagef(volume, volumeSampler, pos).x);
     	  	//maxp = 1.0f-(1.0f-maxp)*(1.0f-read_imagef(volume, volumeSampler, pos).x);
     	  	//maxp = maxp*(1.0f-read_imagef(volume, volumeSampler, pos).x/50.0f);
     	  	pos+=vecstep;
     	  	/*if(maxp >= 1.0f) {
     	    	break;
     	  	}*/
		}
	}

  // Mapping to transfert function range and gamma correction:
  const float mappedVal = pow(mad(ta,maxp,tb),gamma);

	// lookup in transfer function texture:
  const float4 color = brightness*mappedVal;//read_imagef(transferColor4, transferSampler, (float2)(mappedVal,0.0f));
  
  // Alpha pre-multiply:
  color.x = color.x*color.w;
  color.y = color.y*color.w;
  color.z = color.z*color.w;
  
  // write output color:
  //  d_output[x + y*imageW] = rgbaFloatToIntAndMax(clear*d_output[x + y*imageW],color); //d_output[x + y*imageW]

  d_output[4*x+y*imageW*4+0] = color.x;
  d_output[4*x+y*imageW*4+1] = color.y;
  d_output[4*x+y*imageW*4+2] = color.z;
  d_output[4*x+y*imageW*4+3] = color.w;
}
// clears a buffer
__kernel void
clearbuffer(__global uint *buffer, 
                     uint imageW, 
                     uint imageH)
{

	// thread int coordinates:
  const uint x = get_global_id(0);
  const uint y = get_global_id(1);
  
   // clears buffer:
  if ((x < imageW) || (y < imageH))
  	buffer[x + y*imageW] = 0;
}
