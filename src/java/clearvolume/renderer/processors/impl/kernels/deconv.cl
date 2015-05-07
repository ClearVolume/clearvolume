
__kernel void blur_sep(__global float * input, __global float * output , const float sigma, const int Nh, const int flag){

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


  
  float res = 0.f;
  int delta = (Nh-1)/2;
  float hSum = 0.f;

  float one_over_sig2 = 1.f/sigma/sigma;
  
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
	
	float exponent = (p-delta)*(p-delta)*one_over_sig2;
	
	//float hVal = exp(-1.f*(p-delta)*(p-delta)/sigma/sigma);
	float hVal = exp(-exponent);
	
	
	//float hVal = exponent; 
	
	res += hVal*val;

     hSum += hVal;
  }

   res *= 1./hSum;
	
   output[i+Nx*j+Nx*Ny*k] = res;  
 
 }


__kernel void multiply(__global float * input, __global float * output){


	int i = get_global_id(0);

	output[i] = input[i]*output[i];
 }

__kernel void divide (__global float * input, __global float * input2,__global float * output){


	int i = get_global_id(0);

	float denom = input2[i];
	if (denom>0)
		output[i] = input[i]/denom;
	else
		output[i] = input[i];
 }


__kernel void copy(__read_only image3d_t input, __global float* output){

  
  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP_TO_EDGE |	CLK_FILTER_NEAREST ;

  int i = get_global_id(0);
  int j = get_global_id(1);
  int k = get_global_id(2);

    
  int Nx = get_global_size(0);
  int Ny = get_global_size(1);
  int Nz = get_global_size(2); 
 

  output[i+Nx*j+Nx*Ny*k] = read_imagef(input,sampler,
					   (float4)(i,j,k,0.f)).x;
 
 
 }

