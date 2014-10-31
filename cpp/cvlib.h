extern "C" __declspec(dllexport) unsigned long 	__cdecl begincvlib(char* AutoPilotJarPath);
extern "C" __declspec(dllexport) unsigned long 	__cdecl endcvlib();

extern "C" __declspec(dllexport) void 			__cdecl clearError();
extern "C" __declspec(dllexport) char* 			__cdecl getLastJavaExceptionMessage();
extern "C" __declspec(dllexport) char* 			__cdecl getLastError();

extern "C" __declspec(dllexport) int __cdecl createRenderer(	int pRendererId,
																int pWindowWidth,
																int pWindowHeight,
																int pBytesPerVoxel,
																int pMaxTextureWidth,
																int pMaxTextureHeight);

extern "C" __declspec(dllexport) int __cdecl destroyRenderer(int pRendererId);

extern "C" __declspec(dllexport) int __cdecl send8bitUINTVolumeDataToSink(  int pSinkId,
																			char *pBufferAddress,
																			long pBufferLength,
																			long pWidth,
																			long pHeight,
																			long pDepth);

extern "C"__declspec(dllexport) int __cdecl send16bitUINTVolumeDataToSink( 	int pSinkId,
																			short *pBufferAddress,
																			long pBufferLength,
																			long pWidth,
																			long pHeight,
																			long pDepth);