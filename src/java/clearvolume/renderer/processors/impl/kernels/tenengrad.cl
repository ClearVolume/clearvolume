/* opencl implementation of the tenengrad filter for image focus estimation

	Author: Martin Weigert (mweigert@mpi-cbg.de)

*/  


__kernel void downsample(__read_only image3d_t input, __global float* output, const int Nx,const int Ny,const int Nz, const int Ndown){

  
  const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE |	CLK_ADDRESS_CLAMP_TO_EDGE |	CLK_FILTER_NEAREST ;

  int i = get_global_id(0);
  int j = get_global_id(1);
  int k = get_global_id(2);
 
 
  int i0 = Ndown*i;
  int j0 = Ndown*j;
  int k0 = Ndown*k;

  float res = 0.f;

  
  for (int n = 0; n < Ndown; ++n)
	for (int m = 0; m < Ndown; ++m)
	  for (int p = 0; p < Ndown; ++p)
			res += read_imagef(input,sampler,
					   (float4)(i0+n,j0+m,k0+p,0.f)).x;
 
  output[i+Nx*j+Nx*Ny*k] = res/Ndown/Ndown/Ndown;
 
 
 // if ((i==10) &&(j==10)&&(k==10))
  //	printf("kernel:  %.10f \n\n\n",output[i+Nx*j+Nx*Ny*k]);
}


__kernel void convolve_diff(__global float * input, __global float * output, const int Nx,const int Ny,const int Nz, const int flag){

  int i = get_global_id(0);
  int j = get_global_id(1);
  int k = get_global_id(2);

    
  const int dx = flag & 1;
  const int dy = (flag&2)/2;
  const int dz = (flag&4)/4;


  const float h[3] = {-.5f,0.f,.5f};
  const int Nh = 3;
  

  
     
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
	
	res += h[p]*val;

  }

  

  output[i+Nx*j+Nx*Ny*k] = res;  
 
 
// if ((i==10) &&(j==10)&&(k==10))
//  	printf("kernel:  %.10f \n\n\n",output[i+Nx*j+Nx*Ny*k]);
}



__kernel void convolve_smooth(__global float * input, __global float * output, const int Nx,const int Ny,const int Nz, const int flag){

  int i = get_global_id(0);
  int j = get_global_id(1);
  int k = get_global_id(2);

    
  const int dx = flag & 1;
  const int dy = (flag&2)/2;
  const int dz = (flag&4)/4;


  //const float h[3] = {1.f,2.f,1.f};
  const float h[3] = {.25f,.5f,.25f};
  const int Nh = 3;
  
   
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
	
	res += h[p]*val;

  }


  output[i+Nx*j+Nx*Ny*k] = res;  
 
 
 //if ((i==10) &&(j==10)&&(k==10))
 // 	printf("kernel:  %.5f %.5f \n\n\n",input[i+Nx*j+Nx*Ny*k],input[i+1+Nx*j+Nx*Ny*k]);
}

__kernel void sum(__global float * inputX, __global float * inputY, __global float * inputZ, __global float * output, const int N){

  int i = get_global_id(0);
  
 	
 	float Gx  = inputX[i];
	float Gy  = inputY[i];
	float Gz  = inputZ[i];
	
	float res = Gx*Gx+Gy*Gy+Gz*Gz;
	
	output[i] = sqrt(res);
	
 }
 
 
 

__kernel void blur(__global float * input, __global float * output , const float sigma, const int Nx,const int Ny,const int Nz, const int flag){

  int i = get_global_id(0);
  int j = get_global_id(1);
  int k = get_global_id(2);

    
  const int dx = flag & 1;
  const int dy = (flag&2)/2;
  const int dz = (flag&4)/4;

	if (flag==0){
		output[i+Nx*j+Nx*Ny*k] = input[i+Nx*j+Nx*Ny*k];
		return;
	}


  
  const int Nh = 11;
  
   
  float res = 0.f;
  int delta = (Nh-1)/2;
  float hSum = 0.f;

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
	
	float hVal = exp(-1.f*(p-delta)*(p-delta)/sigma/sigma);
	
	res += hVal*val;

   hSum += hVal;
  }

   res *= 1./hSum;
	
    output[i+Nx*j+Nx*Ny*k] = res;  
 
 
// if ((i==10) &&(j==10)&&(k==10))
//  	printf("kernel:  %.10f \n\n\n",res);
}





__kernel void copy(__global float * input, __global float * output , const int N){

  int i = get_global_id(0);
  
  		output[i] = input[i];
}
