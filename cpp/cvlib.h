
extern "C" __declspec(dllexport) unsigned long 	__cdecl begincvlib(char* ClearVolumeJarPath);
extern "C" __declspec(dllexport) unsigned long 	__cdecl endcvlib();

extern "C" __declspec(dllexport) void 			__cdecl clearError();
extern "C" __declspec(dllexport) char* 			__cdecl getLastJavaExceptionMessage();
extern "C" __declspec(dllexport) char* 			__cdecl getLastError();

extern "C" __declspec(dllexport) long __cdecl createRenderer(				long pRendererId,
																			long pWindowWidth,
																			long pWindowHeight,
																			long pBytesPerVoxel,
																			long pMaxTextureWidth,
																			long pMaxTextureHeight);

extern "C" __declspec(dllexport) long __cdecl destroyRenderer(				long pRendererId);

extern "C" __declspec(dllexport) long __cdecl createServer(					long pServerId);

extern "C" __declspec(dllexport) long __cdecl destroyServer(				long pServerId);

extern "C"__declspec(dllexport) long __cdecl setVoxelDimensionsInRealUnits( long pSinkId,
																			double pVoxelWidthInRealUnits,
																			double pVoxelHeightInRealUnits,
																			double pVoxelDepthInRealUnits);

extern "C"__declspec(dllexport) long __cdecl setVolumeIndexAndTime( 		long pSinkId,
																			long pVolumeIndex,
																			double pVolumeTimeInSeconds);

extern "C" __declspec(dllexport) long __cdecl send8bitUINTVolumeDataToSink( long pSinkId,
																			long pChannelId,
																			char *pBufferAddress,
																			__int64 pBufferLength,
																			long pWidthInVoxels,
																			long pHeightInVoxels,
																			long pDepthInVoxels);

extern "C"__declspec(dllexport) long __cdecl send16bitUINTVolumeDataToSink( long pSinkId,
																			long pChannelId,
																			short *pBufferAddress,
																			__int64 pBufferLength,																			
																			long pWidthInVoxels,
																			long pHeightInVoxels,
																			long pDepthInVoxels);