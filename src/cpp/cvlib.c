/**
 * cvlib.cpp 
 * 
 * Defines the exported functions for the cvlib native binding library to ClearVolume.
 *
 * @author Loic Royer, Ulrik Guenther 2014
 *
 */

#include "cvlib.h"
#include <stdbool.h>

//#include "jvmlib/jni.h"       /* where everything is defined */
#include <stdio.h>
#include <string.h>

#define cErrorNone "No Error"

#ifdef __WINDOWS__
const char * getEnvWin(const char * name)
{
    const DWORD buffSize = 65535;
    static char buffer[buffSize];
    if (GetEnvironmentVariableA(name, buffer, buffSize))
    {
        return buffer;
    }
    else
    {
        return 0;
    }
}
#endif

typedef jint (JNICALL *CreateJavaVM)(JavaVM **pvm, void **penv, void *args);
static JavaVM *sJVM; /* denotes a Java VM */
JavaVMInitArgs sJVMArgs; /* JDK/JRE 6 VM initialization arguments */
char* sJavaLastError = cErrorNone;

jclass sClearVolumeClass;
static jmethodID 	getLastExceptionMessageID,
            createRendererID,
            destroyRendererID,
            createServerID,
            destroyServerID,
            setVoxelDimensionsInRealUnitsID,
            setVolumeIndexAndTimeID,
            send8bitUINTVolumeDataToSinkID,
            send16bitUINTVolumeDataToSinkID;

#ifndef __WINDOWS__
#include <signal.h>

static struct sigaction old_sa[NSIG];

void cv_sigaction(int signal, siginfo_t *info, void *reserved)
{
    printf("Signal caught: %i\n", signal);

    JNIEnv* lJNIEnv;
    jthrowable exc;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

        exc = (*lJNIEnv)->ExceptionOccurred(lJNIEnv);
        if (exc) {
            jclass newExcCls;
            (*lJNIEnv)->ExceptionDescribe(lJNIEnv);
            (*lJNIEnv)->ExceptionClear(lJNIEnv);

        }

//	(*env)->CallVoidMethod(env, obj, nativeCrashed);
	old_sa[signal].sa_handler(signal);
}
#endif

__declspec(dllexport) unsigned long __cdecl begincvlib(char* pClearVolumeJarPath, backend_t backend)
{
    clearError();
    JNIEnv* lJNIEnv;
#ifdef __WINDOWS__
    const char* JAVAHOME  =getEnvWin("JAVA_HOME");
    printf("JAVAHOME=%s\n");

    char JREFolderPath[1024];

    strcpy(JREFolderPath,JAVAHOME);
    strcat(JREFolderPath,"\\bin\\server\\jvm.dll");
    printf("trying to load from a JRE path: %s", JREFolderPath);

    HINSTANCE lDLLInstance = LoadLibraryA(JREFolderPath);
    if( lDLLInstance == 0)
    {
        printf( "JAVAHOME=%s\n", JAVAHOME);
        strcpy(JREFolderPath,JAVAHOME);
        strcat(JREFolderPath,"\\jre\\bin\\server\\jvm.dll");
        printf( "trying to load from a JDK path:%s\n ", JREFolderPath);

        lDLLInstance = LoadLibraryA(JREFolderPath);
        if( lDLLInstance == 0)
        {
            sJavaLastError = "Cannot load Jvm.dll (wrong path given)";
            return 1;
        }
    }
    CreateJavaVM lCreateJavaVM = (CreateJavaVM)GetProcAddress(lDLLInstance, "JNI_CreateJavaVM");
    if (lCreateJavaVM == NULL )
    {
        sJavaLastError = "Cannot load Jvm.dll (wrong path given)";
        return 2;
    }

    size_t lJREFolderPathLength= strlen(JREFolderPath);

#endif
    char lClassPathPrefix[] = "-Djava.class.path=";
    size_t lClassPathPrefixLength= strlen(lClassPathPrefix);

    char lClassPathString[1024];

    strcpy(lClassPathString,lClassPathPrefix);
    strcat(lClassPathString,pClearVolumeJarPath);

    memset(&sJVMArgs, 0, sizeof(sJVMArgs));

    JavaVMOption options[3];
    options[0].optionString = (char*)"-Xmx4G";
    options[1].optionString = (char*)lClassPathString;

    switch(backend) {
        case CUDA:
            printf("Forcing CUDA backend.\n");
            options[2].optionString = "-DClearVolume.disableOpenCL";
            sJVMArgs.nOptions = 3;
            break;
        case OPENCL:
            printf("Forcing OpenCL backend.\n");
            options[2].optionString = "-DClearVolume.disableCUDA";
            sJVMArgs.nOptions = 3;
            break;
        case USEBEST:
        default:
            printf("No backend preference selected.\n");
            sJVMArgs.nOptions = 2;
    }

    sJVMArgs.version = JNI_VERSION_1_6;
    sJVMArgs.options = options;
    sJVMArgs.ignoreUnrecognized = 0;

#ifdef __WINDOWS
    jint res = lCreateJavaVM(&sJVM, (void **)&lJNIEnv, &sJVMArgs);
#else
    jint res = JNI_CreateJavaVM(&sJVM, (void**)&lJNIEnv, &sJVMArgs);
#endif
    if (res == JNI_ERR)
    {
        return 3;
    } else if (res == JNI_OK) {
        fprintf(stderr, "JVM created.\n");
    }

    sClearVolumeClass = (*lJNIEnv)->FindClass(lJNIEnv, "clearvolume/interfaces/ClearVolumeC");

    if (sClearVolumeClass == 0)
    {
        fprintf(stderr, "Unable to locate class %s in %s\n", "clearvolume/interfaces/ClearVolumeC", pClearVolumeJarPath);
        fprintf(stderr, "Java class path was %s\n", options[1].optionString);

        jthrowable exc;
        exc = (*lJNIEnv)->ExceptionOccurred(lJNIEnv);
        if (exc) {
            jclass newExcCls;
            (*lJNIEnv)->ExceptionDescribe(lJNIEnv);
            (*lJNIEnv)->ExceptionClear(lJNIEnv);

        }

        return 4;
    }

    /*
       B = byte
       C = char
       D = double
       F = float
       I = int
       J = long
       S = short
       V = void
       Z = boolean
       Lfully-qualified-class = fully qualified class
       [type = array of type
       (argument types)return type = method type. If no arguments, use empty argument types: (). 
       If return type is void (or constructor) use (argument types)V.
       Observe that the ; is needed after the class name in all situations. 
       This won't work "(Ljava/lang/String)V" but this will "(Ljava/lang/String;)V". 
       */


    getLastExceptionMessageID		= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "getLastExceptionMessage", "()Ljava/lang/String;");
    createRendererID 				= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "createRenderer", "(IIIIIIZZ)I");
    destroyRendererID 				= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "destroyRenderer", "(I)I");
    createServerID    				= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "createServer", "(I)I");
    destroyServerID 				= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "destroyServer", "(I)I");
    setVoxelDimensionsInRealUnitsID	= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "setVoxelDimensionsInRealUnits", "(IDDD)I");
    setVolumeIndexAndTimeID 		= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "setVolumeIndexAndTime", "(IID)I");
    send8bitUINTVolumeDataToSinkID 	= (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "send8bitUINTVolumeDataToSink", "(IIJJIII)I");
    send16bitUINTVolumeDataToSinkID = (*lJNIEnv)->GetStaticMethodID(lJNIEnv, sClearVolumeClass, "send16bitUINTVolumeDataToSink", "(IIJJIII)I");

    if (getLastExceptionMessageID == 0) return 101;
    if (createRendererID == 0) return 102;
    if (destroyRendererID == 0) return 103;
    if (createServerID == 0) return 104;
    if (destroyServerID == 0) return 105;
    if (setVoxelDimensionsInRealUnitsID == 0) return 106;
    if (setVolumeIndexAndTimeID == 0) return 107;
    if (send8bitUINTVolumeDataToSinkID == 0) return 108;
    if (send16bitUINTVolumeDataToSinkID == 0) return 109;

    jthrowable exc;
    exc = (*lJNIEnv)->ExceptionOccurred(lJNIEnv);
    if (exc) {
        jclass newExcCls;
        (*lJNIEnv)->ExceptionDescribe(lJNIEnv);
        (*lJNIEnv)->ExceptionClear(lJNIEnv);

    }

#ifndef WINDOWS
       struct sigaction handler;
	memset(&handler, 0, sizeof(sigaction));
	handler.sa_sigaction = cv_sigaction;
	handler.sa_flags = SA_RESETHAND;
#define CATCHSIG(X) sigaction(X, &handler, &old_sa[X])
	CATCHSIG(SIGILL);
	CATCHSIG(SIGABRT);
	CATCHSIG(SIGBUS);
	CATCHSIG(SIGFPE);
	CATCHSIG(SIGSEGV);
	CATCHSIG(SIGPIPE);
#endif

    return 0;
}

__declspec(dllexport) unsigned long __cdecl endcvlib()
{
    clearError();
    // This hangs the system for no good reason:
    //sJVM->DetachCurrentThread();
    //sJVM->DestroyJavaVM();
    return 0;
}

__declspec(dllexport)  void __cdecl clearError()
{
    sJavaLastError=cErrorNone;
}



jstring sLastJavaExceptionMessageJString = NULL;
char* sLastJavaExceptionMessage = NULL;

__declspec(dllexport) char* __cdecl getLastJavaExceptionMessage()
{
    JNIEnv* lJNIEnv;

    clearError();
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    if(sLastJavaExceptionMessageJString!=NULL && sLastJavaExceptionMessage != NULL)
    {
        (*lJNIEnv)->ReleaseStringUTFChars(lJNIEnv, sLastJavaExceptionMessageJString, sLastJavaExceptionMessage);
    }

    sLastJavaExceptionMessageJString = (jstring)(*lJNIEnv)->CallStaticObjectMethod(lJNIEnv, sClearVolumeClass,getLastExceptionMessageID);
    sLastJavaExceptionMessage = NULL;

    if(sLastJavaExceptionMessageJString!=NULL)
    {
        sLastJavaExceptionMessage = (char*)(*lJNIEnv)->GetStringUTFChars(lJNIEnv, sLastJavaExceptionMessageJString, NULL);
    }
    return sLastJavaExceptionMessage;
}

__declspec(dllexport) char* __cdecl getLastError()
{
    char* lLastJavaExceptionMessage = getLastJavaExceptionMessage();
    if(lLastJavaExceptionMessage!=NULL) return lLastJavaExceptionMessage;
    else return sJavaLastError;
}

__declspec(dllexport) long __cdecl createRenderer(				int pRendererId,
        int pWindowWidth,
        int pWindowHeight,
        int pBytesPerVoxel,
        int pMaxTextureWidth,
        int pMaxTextureHeight)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);
    jint envRes = (*sJVM)->GetEnv(sJVM, (void**)&lJNIEnv, JNI_VERSION_1_6);

    if(envRes != JNI_OK) {
        fprintf(stderr, "Error attaching JRE to current thread! %d\n", envRes);
        return 1;
    }

    jthrowable exc;
    exc = (*lJNIEnv)->ExceptionOccurred(lJNIEnv);
    if (exc) {
        jclass newExcCls;
        (*lJNIEnv)->ExceptionDescribe(lJNIEnv);
        (*lJNIEnv)->ExceptionClear(lJNIEnv);

    }

    printf("Creating renderer now...\n");
    int res = (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            createRendererID,
            pRendererId, 
            pWindowWidth, 
            pWindowHeight, 
            pBytesPerVoxel,
            pMaxTextureWidth,
            pMaxTextureHeight,
            0,
            0);

    printf("Checking for exceptions...\n");
    exc = (*lJNIEnv)->ExceptionOccurred(lJNIEnv);
    if (exc) {
        jclass newExcCls;
        (*lJNIEnv)->ExceptionDescribe(lJNIEnv);
        (*lJNIEnv)->ExceptionClear(lJNIEnv);
        return 0;
    } else {
        return res;
    }
}

__declspec(dllexport) long __cdecl createRendererWithTimeShiftAndChannels(	long pRendererId,
        long pWindowWidth,
        long pWindowHeight,
        long pBytesPerVoxel,
        long pMaxTextureWidth,
        long pMaxTextureHeight,
        bool pTimeShift,
        bool pChannelSelector)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            createRendererID,
            pRendererId, 
            pWindowWidth, 
            pWindowHeight, 
            pBytesPerVoxel,
            pMaxTextureWidth,
            pMaxTextureHeight,
            pTimeShift,
            pChannelSelector);

}

__declspec(dllexport) long __cdecl destroyRenderer(long pRendererId)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            destroyRendererID,
            pRendererId);
}

__declspec(dllexport) long __cdecl createServer(	long pServerId)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            createServerID,
            pServerId);
}

__declspec(dllexport) long __cdecl destroyServer(long pServerId)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            destroyServerID,
            pServerId);
}


__declspec(dllexport) long __cdecl setVoxelDimensionsInRealUnits( 	long pSinkId,
        double pVoxelWidthInRealUnits,
        double pVoxelHeightInRealUnits,
        double pVoxelDepthInRealUnits)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            setVoxelDimensionsInRealUnitsID,
            pSinkId,
            pVoxelWidthInRealUnits,
            pVoxelHeightInRealUnits,
            pVoxelDepthInRealUnits);
    sJavaLastError = "Error while setting voxel dimensions in real units";
    return -1;
}



__declspec(dllexport) long __cdecl setVolumeIndexAndTime( 			long pSinkId,
        long pVolumeIndex,
        double pVolumeTimeInSeconds)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            setVolumeIndexAndTimeID,
            pSinkId,
            pVolumeIndex,
            pVolumeTimeInSeconds);
}


__declspec(dllexport) long __cdecl send8bitUINTVolumeDataToSink( 	long pSinkId,
        long pChannelId,
        char *pBufferAddress,
        __int64 pBufferLength,																 	
        long pWidth,
        long pHeight,
        long pDepth)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            send8bitUINTVolumeDataToSinkID,
            pSinkId,
            pChannelId,
            (__int64)pBufferAddress,
            (__int64)pBufferLength,
            pWidth,
            pHeight,
            pDepth);
}

__declspec(dllexport) long __cdecl send16bitUINTVolumeDataToSink( 	long pSinkId,
        long pChannelId,
        short *pBufferAddress,
        __int64 pBufferLength,
        long pWidth,
        long pHeight,
        long pDepth)
{
    clearError();
    JNIEnv* lJNIEnv;
    (*sJVM)->AttachCurrentThread(sJVM, (void**)&lJNIEnv, NULL);

    return (*lJNIEnv)->CallStaticIntMethod(lJNIEnv, sClearVolumeClass,
            send16bitUINTVolumeDataToSinkID,
            pSinkId,
            pChannelId,
            (__int64)pBufferAddress,
            (__int64)pBufferLength,
            pWidth,
            pHeight,
            pDepth);
}

