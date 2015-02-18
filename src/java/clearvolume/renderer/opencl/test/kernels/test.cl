__kernel void
test(__constant float *input,__global float *output,
			uint Nx, uint Ny)
{
  const sampler_t volumeSampler =   CLK_NORMALIZED_COORDS_TRUE |
	CLK_ADDRESS_CLAMP_TO_EDGE |
	// CLK_FILTER_NEAREST ;
	CLK_FILTER_LINEAR ;

  
  uint i = get_global_id(0);
  uint j = get_global_id(1);

  output[i+Nx*j] = 2.f*input[i+Nx*j];
}

__kernel void
test_char(__global uchar *output,
     uint Nx)
{
    
    uint i = get_global_id(0);
    uint j = get_global_id(1);
    
    output[i+Nx*j] = (uchar)(128) ;
}




__kernel void
test_int(__global uint *output,
     uint Nx)
{
    
    uint i = get_global_id(0);
    uint j = get_global_id(1);
    
    output[i+Nx*j] = (uint)(1<<24) | (uint)(2<<16) ;
}

__kernel void
test_img(__global uint *output, uint Nx, __read_only image3d_t volume)
{
    const sampler_t sampler =   CLK_NORMALIZED_COORDS_TRUE |
		CLK_ADDRESS_CLAMP_TO_EDGE |
		// CLK_FILTER_NEAREST ;
		CLK_FILTER_LINEAR ;
	
    uint i = get_global_id(0);
    uint j = get_global_id(1);
    
    output[i+Nx*j] = (uint)read_imageui(volume,sampler,(float4)(0.f,0.f,0.f,0.f)).x;
    
}

__kernel void
test_float(__global float *output,
     int N)
{
    
    int i = get_global_id(0);
    
    output[i] = 1.f*i;
    
    //if (i==0)
    //printf("kernel!  %d \n", N);
}