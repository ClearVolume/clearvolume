
__kernel void bilat(__read_only image3d_t input, 
										__global float* output, 
										const int blockSize, 
										const float sigmaSpace,
										const float sigmaValue)
{

  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP_TO_EDGE |	CLK_FILTER_NEAREST ;

  const int i = get_global_id(0);
  const int j = get_global_id(1);
  const int k = get_global_id(2);
  
  const int Nx = get_global_size(0);
  const int Ny = get_global_size(1);
  const int Nz = get_global_size(2);
  
  const float pix0 = read_imagef(input,sampler,(int4)(i,j,k,0)).x;
  
  const float isv2= -native_recip(sigmaValue*sigmaValue);
  const float iss2= -native_recip(sigmaSpace*sigmaSpace);
  
  
  float res = 0;
  float sum = 0;
  
  for(int i2 = -blockSize;i2<=blockSize;i2++)
  {
  	const float i22 = i2*i2;
		for(int j2 = -blockSize;j2<=blockSize;j2++)
		{
			const float j22 = j2*j2;
	  	for(int k2 = -blockSize;k2<=blockSize;k2++)
	  	{
				const float pix1 = read_imagef(input,sampler,(int4)(i+i2,j+j2,k+k2,0)).x;
				const float diff = 1.f*pix0-pix1; 
				float weight = native_exp(iss2*(i22+j22+k2*k2))* native_exp(isv2*diff*diff);
		
				res += pix1*weight;
				sum += weight;
		  }
		}
  }
  
  output[i+j*Nx+k*Nx*Ny] = res/sum;

}
