/* simple histogram opencl kernels

	Author: Martin Weigert (mweigert@mpi-cbg.de)


*/
  

// clear buffers
__kernel void clear_counts(__global uint *count)
{
   int i = get_global_id(0);
   count[i] = 0;
}



// histogram of floats
__kernel void histogram_naive(__read_only image3d_t input,
															volatile __global int *count, 
															const float pMin,
															const float pMax, 
															const int N_BINS)
{

  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP |	CLK_FILTER_NEAREST ;
 
  int i = get_global_id(0);
  int j = get_global_id(1);
  int k = get_global_id(2);

  float val = read_imagef(input,sampler,
					   (float4)(i,j,k,0.f)).x;

 
  uint pos = (uint) ((val-pMin)/(pMax-pMin)*(N_BINS-1));
  
  if ((pos>=0) &&(pos<N_BINS))
  	atomic_add(&count[pos],1);
   
}


#define NBINS 128
 
// histogram of floats with shared memory 
__kernel void histogram_shared(__read_only image3d_t input,volatile __global int *count, const float pMin,const float pMax, const int N_BINS){

  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP |	CLK_FILTER_NEAREST ;
 
  int i = get_global_id(0);
  int j = get_global_id(1);
  int k = get_global_id(2);
  
  int iLoc = get_local_id(0);
  int jLoc = get_local_id(1);
  int kLoc = get_local_id(2);

  int iGroup = get_group_id(0);
  int jGroup = get_group_id(1);
  int kGroup = get_group_id(2);
  
  __local int partialCount[NBINS];
   
  float val = read_imagef(input,sampler,
					   (float4)(i,j,k,0.f)).x;
					   
  uint pos = (uint) ((val-pMin)/(pMax-pMin)*(N_BINS-1));
  
  if ((pos>=0) &&(pos<N_BINS))
  	atomic_add(&partialCount[pos],1);
  
   
}

