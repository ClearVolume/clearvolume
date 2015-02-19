

__kernel void
test(__read_only image3d_t volume)
{
  const sampler_t volumeSampler =   CLK_NORMALIZED_COORDS_TRUE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_LINEAR ;

  uint x = get_global_id(0);
  uint y = get_global_id(1);

	if(x==0 && y==0)
	{
	  float4 value = read_imagef(volume, volumeSampler, (float4)(.51f,.5f,.5f,0.5f));
	//	printf("value: %.5f\n",value.r); 
  }
 
}

