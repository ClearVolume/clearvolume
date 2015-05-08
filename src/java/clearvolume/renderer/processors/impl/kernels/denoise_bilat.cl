
__kernel void bilat(__read_only image3d_t input, __global float* output,const int blockSize, const float sigmaSpace,const float sigmaValue)
{

  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP_TO_EDGE |	CLK_FILTER_NEAREST ;

  int i = get_global_id(0);
  int j = get_global_id(1);
  int k = get_global_id(2);
  
  int Nx = get_global_size(0);
  int Ny = get_global_size(1);
  int Nz = get_global_size(2);
  
  float pix0 = read_imagef(input,sampler,(int4)(i,j,k,0)).x;
  
  float res = 0;
  float sum = 0;


  for(int i2 = -blockSize;i2<=blockSize;i2++){
	for(int j2 = -blockSize;j2<=blockSize;j2++){
	  for(int k2 = -blockSize;k2<=blockSize;k2++){
	
		float pix1 = read_imagef(input,sampler,(int4)(i+i2,j+j2,k+k2,0)).x;
		float weight = exp(-1.f/sigmaSpace/sigmaSpace*(i2*i2+j2*j2+k2*k2))*
		  exp(-1.f/sigmaValue/sigmaValue*((1.f*pix0-pix1)*(1.f*pix0-pix1)));

		res += pix1*weight;
		sum += weight;
	  }
	}
  }
  
  output[i+j*Nx+k*Nx*Ny] = res/sum;

}
