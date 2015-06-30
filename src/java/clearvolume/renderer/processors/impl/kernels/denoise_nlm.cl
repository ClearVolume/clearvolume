__kernel void nlm(__read_only image3d_t input, __global float* output,
					   const int FSIZE, const int BSIZE,const float SIGMA)
{
  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP_TO_EDGE |	CLK_FILTER_NEAREST ;

  uint i = get_global_id(0);
  uint j = get_global_id(1);
  uint k = get_global_id(2);

  int Nx = get_global_size(0);
  int Ny = get_global_size(1);
    

  float pix0 = read_imagef(input,sampler,(int4)(i,j,k,0)).x;
  
  float res = 0;
  float sum = 0;

  float pix1;


  for(int i1 = -BSIZE;i1<=BSIZE;i1++){
    for(int j1 = -BSIZE;j1<=BSIZE;j1++){
  	  for(int k1 = -BSIZE;k1<=BSIZE;k1++){
  		float weight = 0;
  		float dist = 0 ;

  		pix1 = read_imagef(input,sampler,(int4)(i+i1,j+j1,k+k1,0)).x;

	  
  		for(int i2 = -FSIZE;i2<=FSIZE;i2++){
  		  for(int j2 = -FSIZE;j2<=FSIZE;j2++){
  			for(int k2 = -FSIZE;k2<=FSIZE;k2++){
	
  			  float p0 = read_imagef(input,sampler,(int4)(i+i2,j+j2,k+k2,0)).x;

  			  float p1 = read_imagef(input,sampler,(int4)(i+i1+i2,j+j1+j2,k+k1+k2,0)).x;

  			  dist += (1.f*p1-1.f*p0)*(1.f*p1-1.f*p0)/(1.f+2.f*FSIZE)/(1.f+2.f*FSIZE);
  			}
  		  }
  		}
	  
  		weight = exp(-1.f/SIGMA/SIGMA*dist);
  		res += pix1*weight;
  		sum += weight;

  	  }
  	}
  }
  
  output[i+j*Nx+k*Nx*Ny] = 1.*res/sum;


}
