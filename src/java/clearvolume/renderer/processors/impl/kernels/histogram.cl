// histogram of floats
__kernel void histogram(__read_only image3d_t input,__constant float *bins,volatile __global uint *output, const int Nbins){

  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP |	CLK_FILTER_NEAREST ;
 
  int i = get_global_id(0);
  int j = get_global_id(1);
  int k = get_global_id(2);

  float val = read_imagef(input,sampler,
					   (float4)(i,j,k,0.f)).x;

  int pos;
  for (pos = 0; pos < Nbins; ++pos)
	if (bins[pos]>=val)
	  break;
	
  atomic_add(&output[pos],1);
  
  //if ((i==10) && (j==10) && (k==0))
  //printf("kernel: %.4f    %d\n",val, output[0]);
}

