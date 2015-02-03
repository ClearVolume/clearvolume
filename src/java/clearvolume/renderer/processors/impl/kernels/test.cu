
// Dumb unoptimized summation kernel - for demonstration purposes


typedef unsigned char VolumeType;
typedef unsigned char VolumeType1;
typedef unsigned short VolumeType2;

texture<VolumeType/*BytesPerVoxel*/, 3, cudaReadModeNormalizedFloat> tex;


extern "C" __global__ void test()
{
	if(blockIdx.x == 0 && threadIdx.x==0)
		printf("value=%f\n", tex3D(tex, 10, 10, 10));
}


