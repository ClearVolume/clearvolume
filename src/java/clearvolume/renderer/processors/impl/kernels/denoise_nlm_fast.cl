
#ifndef FS
#define FS 1
#endif

#ifndef BS
#define BS 2
#endif /* BS */

#define NPATCH ((2.f*FS+1)*(2.f*FS+1)*(2.f*FS+1))




__kernel void dist(__read_only image3d_t input,__global float * output, const int dx,const int dy,const int dz){


const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP_TO_EDGE |	CLK_FILTER_NEAREST ;

uint i = get_global_id(0);
uint j = get_global_id(1);
uint k = get_global_id(2);

int Nx = get_global_size(0);
int Ny = get_global_size(1);
  
float pix1  = read_imagef(input,sampler,(int4)(i,j,k,0)).x;
float pix2  = read_imagef(input,sampler,(int4)(i+dx,j+dy,k+dz,0)).x;

float d = (pix1-pix2);


d = d*d/NPATCH;


output[i+Nx*j+Nx*Ny*k] = d;


}


__kernel void convolve(	__global float * 	input, 
						__global float * 	output, 
						const int 			flag)
{

  // flag = 1 -> in x axis 
  // flag = 2 -> in y axis 
  // flag = 4 -> in z axis 

  int i = get_global_id(0);
  int j = get_global_id(1);
  int k = get_global_id(2);

    
  int Nx = get_global_size(0);
  int Ny = get_global_size(1);
  int Nz = get_global_size(2);
    
  const int dx = flag & 1;
  const int dy = (flag&2)/2;
  const int dz = (flag&4)/4;

	if (flag==0){
		output[i+Nx*j+Nx*Ny*k] = input[i+Nx*j+Nx*Ny*k];
		return;
	}


  const int Nh = 2*FS+1;
  
  float res = 0.f;
  int delta = (Nh-1)/2;
  

  for (int p = 0; p < Nh; ++p){

	int i1 = i+dx*(p-delta);
	int j1 = j+dy*(p-delta);
	int k1 = k+dz*(p-delta);
	
	float val = 0.f;
	
	
	//clamp to border
	i1 = clamp(i1,0,Nx-1);
	j1 = clamp(j1,0,Ny-1);
	k1 = clamp(k1,0,Nz-1);

	val = input[i1+Nx*j1+Nx*Ny*k1];
		
	res += val;

  }

   output[i+Nx*j+Nx*Ny*k] = res;  
   
 }

__kernel void computePlus(	__read_only image3d_t input,
							__global float * 	distImg,
						  __global float* accBuf,__global float* weightBuf,
						  const int dx,const int dy,const int dz, const float sigma){

  
    const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP_TO_EDGE |	CLK_FILTER_NEAREST ;
  
  
  uint i0 = get_global_id(0);
  uint j0 = get_global_id(1);
  uint k0 = get_global_id(2);

  int Nx = get_global_size(0);
  int Ny = get_global_size(1);
  int Nz = get_global_size(2);
  
  float dist  = distImg[i0+Nx*j0+Nx*Ny*k0];

  float pix  = pix  = read_imagef(input,sampler,(int4)(i0+dx,j0+dy,k0+dz,0)).x;

  float weight = exp(-1.f*dist/sigma/sigma);

  accBuf[i0+Nx*j0+Nx*Ny*k0] += (float)(weight*pix);
  weightBuf[i0+Nx*j0+Nx*Ny*k0] += (float)(weight);


}

__kernel void computeMinus(	__read_only image3d_t input,
							__global float * 	distImg,
						  __global float* accBuf,__global float* weightBuf,
						  const int dx,const int dy,const int dz, const float sigma){

  
    const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP_TO_EDGE |	CLK_FILTER_NEAREST ;
  
  
  uint i0 = get_global_id(0);
  uint j0 = get_global_id(1);
  uint k0 = get_global_id(2);

  int Nx = get_global_size(0);
  int Ny = get_global_size(1);
  int Nz = get_global_size(2);
  
  int i1 = clamp((int)(i0-dx),(int)0,(int)(Nx-1));
  int j1 = clamp((int)(j0-dy),(int)0,(int)(Ny-1));
  int k1 = clamp((int)(k0-dz),(int)0,(int)(Nz-1));
  
  float dist  = distImg[i1+Nx*j1+Nx*Ny*k1];

  float pix  = pix  = read_imagef(input,sampler,(int4)(i0-dx,j0-dy,k0-dz,0)).x;

  float weight = exp(-1.f*dist/sigma/sigma);

  accBuf[i0+Nx*j0+Nx*Ny*k0] += (float)(weight*pix);
  weightBuf[i0+Nx*j0+Nx*Ny*k0] += (float)(weight);


}

__kernel void assemble(	__global float * accBuf,
						__global float* weightBuf,
						__global float* outBuf){

  
    
  uint i0 = get_global_id(0);
  uint j0 = get_global_id(1);
  uint k0 = get_global_id(2);
  
  int Nx = get_global_size(0);
  int Ny = get_global_size(1);
  
  outBuf[i0+Nx*j0+Nx*Ny*k0] = accBuf[i0+Nx*j0+Nx*Ny*k0]/weightBuf[i0+Nx*j0+Nx*Ny*k0];
  
  

}


__kernel void startup(	__global float * accBuf,
						__global float* weightBuf){

  
    
  uint i0 = get_global_id(0);
  uint j0 = get_global_id(1);
  uint k0 = get_global_id(2);
  
  int Nx = get_global_size(0);
  int Ny = get_global_size(1);
  
  accBuf[i0+Nx*j0+Nx*Ny*k0] = 0.f;
  weightBuf[i0+Nx*j0+Nx*Ny*k0] = 0.f;
  

}
