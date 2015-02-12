/*
  adapted from the Nvidia sdk sample
  http://developer.download.nvidia.com/compute/cuda/4_2/rel/sdk/website/OpenCL/html/samples.html
 

  Author: Martin Weigert (mweigert@mpi-cbg.de)
    			Loic Royer		 (royer@mpi-cbg.de)
  				Martin Weigert (mweigert@mpi-cbg.de)
*/



#define maxSteps 200
#define tstep 0.04f


inline
float random(uint x, uint y)
{   
    uint a = 4421 +(1+x)*(1+y) +x +y;

    for(int i=0; i < 10; i++)
    {
        a = (1664525 * a + 1013904223) % 79197919;
    }

    float rnd = (a*1.0f)/(79197919);
    
    return rnd;
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



__kernel void
max_project_Short2(__global short *d_output, 
			uint Nx, uint Ny,
			__constant float* invP,
			__constant float* invM,
			__read_only image3d_t volume)
{
  uint x = get_global_id(0);
  uint y = get_global_id(1);

	d_output[x+Nx*y] = 1000+x+Nx*y;
			
			
}
			


inline
uint rgbaFloatToInt(float4 rgba)
{
    rgba = clamp(rgba,(float4)(0.f,0.f,0.f,0.f),(float4)(1.f,1.f,1.f,1.f));
    
    return ((uint)(rgba.w*255)<<24) | ((uint)(rgba.z*255)<<16) | ((uint)(rgba.y*255)<<8) | (uint)(rgba.x*255);
}



__kernel void
volumerender(__global uint *d_output, 
			uint Nx, uint Ny,
			float brightness,
			float trangemin, 
			float trangemax, 
			float gamma,
			__read_only image2d_t transferColor4,
			__constant float* invP,
			__constant float* invM,
			__read_only image3d_t volume)
{
  const sampler_t volumeSampler =   CLK_NORMALIZED_COORDS_TRUE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR ;

	const sampler_t transferSampler =   CLK_NORMALIZED_COORDS_TRUE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR ;



  uint x = get_global_id(0);
  uint y = get_global_id(1);

  float ta = 1.f/(trangemax-trangemin);
  float tb = trangemin/(trangemin-trangemax); 
  //float4 color = (float4)(transferColor4[0],transferColor4[1],transferColor4[2],transferColor4[3]);
  
  float u = (x / (float) Nx)*2.0f-1.0f;
  float v = (y / (float) Ny)*2.0f-1.0f;

  float4 boxMin = (float4)(-1.0f, -1.0f, -1.0f,-1.0f);
  float4 boxMax = (float4)(1.0f, 1.0f, 1.0f,1.0f);

  // calculate eye ray in world space
  float4 orig0, orig;
  float4 direc0, direc;
  float4 temp;
  float4 back,front;
  
  
  front = (float4)(u,v,-1,1);
  back = (float4)(u,v,1,1);
  
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
  int hit = intersectBox(orig,direc, boxMin, boxMax, &tnear, &tfar);
  if (!hit) {
  	if ((x < Nx) && (y < Ny)) {
  	  d_output[x+Nx*y] = 0.f;
  	}
  	return;
  }
  if (tnear < 0.0f) tnear = 0.0f;     // clamp to near plane


	uint entropy = (uint)( 6779514*fast_length(orig) + 6257327*fast_length(direc) );
	orig += tstep*random(entropy+x,entropy+y)*direc;
	
	//printf("kernel: %.5f  %.5f  %.5f  %.5f\n",orig.x,orig.y,orig.z,orig.w); 

  float4 colVal = 0.f;
  float t = tnear;
  float4 pos;
  for(uint i=0; i<maxSteps && t < tfar; i++) 
  {
   	pos = orig + t*direc;
	  pos = pos*0.5f+0.5f;    // map position to [0, 1] coordinates

  	// read from 3D texture        
  	float newVal = read_imagef(volume, volumeSampler, pos).x;
		float mappedVal = pow(ta*newVal+tb,gamma);

    float4 color = read_imagef(transferColor4,transferSampler, (float2)(mappedVal,0.0f));
		colVal = max(colVal, color);

	  t += tstep; 	
  }

  colVal = brightness * colVal;
  
  
  d_output[x+Nx*y] = rgbaFloatToInt(colVal);
	
	

}

