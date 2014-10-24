
extern "C" __declspec(dllexport) unsigned long 	__cdecl begin(char* pJREFolderPath, char* AutoPilotJarPath);
extern "C" __declspec(dllexport) unsigned long 	__cdecl end();

extern "C" __declspec(dllexport) void 			__cdecl setLoggingOptions(bool pStdOut, bool pLogFile);

extern "C" __declspec(dllexport) void 			__cdecl clearError();
extern "C" __declspec(dllexport) char* 			__cdecl getLastJavaExceptionMessage();
extern "C" __declspec(dllexport) char* 			__cdecl getLastError();

extern "C" __declspec(dllexport) void 			__cdecl freePointer(void* pPointer);

extern "C" __declspec(dllexport) double 		__cdecl dcts16bit(		short* pBuffer, 
																		int pWidth, 
																		int pHeight, 
																		double pPSFSupportDiameter);

extern "C" __declspec(dllexport) double 		__cdecl tenengrad16bit(	short* pBuffer, 
																		int pWidth, 
																		int pHeight, 
																		double pPSFSupportDiameter);

extern "C" __declspec(dllexport) int		 	__cdecl l2solveSSP(		bool pAnchorDetection,
																		bool pSymmetricAnchor,
																		int pNumberOfWavelengths,
																		int pNumberOfPlanes,
																		int pSyncPlaneIndex,
																		double* pCurrentStateVector,
																		double* pObservationsVector,
																		bool* pMissingObservations,
																		double* pNewStateVector);
																		
extern "C" __declspec(dllexport) int		 	__cdecl l2solve(		bool pAnchorDetection,
																		bool pSymmetricAnchor,
																		int pNumberOfWavelengths,
																		int pNumberOfPlanes,
																		bool* pSyncPlaneIndices,
																		double* pCurrentStateVector,
																		double* pObservationsVector,
																		bool* pMissingObservations,
																		double* pNewStateVector);

extern "C" __declspec(dllexport) int		 	__cdecl qpsolve(		bool pAnchorDetection,
																		bool pSymmetricAnchor,
																		int pNumberOfWavelengths,
																		int pNumberOfPlanes,
																		bool* pSyncPlaneIndices,
																		double* pCurrentStateVector,
																		double* pObservationsVector,
																		bool* pMissingObservations,
																		double* pMaxCorrections,
																		double* pNewStateVector);
