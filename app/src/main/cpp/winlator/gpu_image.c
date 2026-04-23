#include <android/log.h>
#include <android/hardware_buffer.h>
#include <android/native_window.h>

#define EGL_EGLEXT_PROTOTYPES
#define GL_GLEXT_PROTOTYPES

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <jni.h>
#include <stdbool.h>
#include <sys/mman.h>
#include <unistd.h>
#include <string.h>

#include "hardware_buffer_utils.h"
#include "winlator.h"

#define HAL_PIXEL_FORMAT_BGRA_8888 5

EGLImageKHR createImageKHR(AHardwareBuffer* hardwareBuffer, int textureId) {
    const EGLint attribList[] = {EGL_IMAGE_PRESERVED_KHR, EGL_TRUE, EGL_NONE};
    AHardwareBuffer_acquire(hardwareBuffer);

    EGLClientBuffer clientBuffer = eglGetNativeClientBufferANDROID(hardwareBuffer);
    if (!clientBuffer) return NULL;

    EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    EGLImageKHR imageKHR = eglCreateImageKHR(eglDisplay, EGL_NO_CONTEXT, EGL_NATIVE_BUFFER_ANDROID, clientBuffer, attribList);
    if (!imageKHR) return NULL;

    glBindTexture(GL_TEXTURE_2D, textureId);
    glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, imageKHR);
    glBindTexture(GL_TEXTURE_2D, 0);
    return imageKHR;
}

AHardwareBuffer* createHardwareBuffer(int width, int height, bool cpuAccess, bool useHALPixelFormatBGRA8888) {
    AHardwareBuffer_Desc buffDesc = {0};
    buffDesc.width = width;
    buffDesc.height = height;
    buffDesc.layers = 1;
    buffDesc.usage = cpuAccess ? AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN : AHARDWAREBUFFER_USAGE_GPU_COLOR_OUTPUT;
    buffDesc.format = useHALPixelFormatBGRA8888 ? HAL_PIXEL_FORMAT_BGRA_8888 : AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM;

    AHardwareBuffer *hardwareBuffer = NULL;
    AHardwareBuffer_allocate(&buffDesc, &hardwareBuffer);
    return hardwareBuffer;
}

JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_renderer_GPUImage_hardwareBufferFromSocket(JNIEnv *env, jclass obj, jint fd) {
    (void)env;
    (void)obj;
    AHardwareBuffer *ahb;
    uint8_t buf = 1;
    if ((write(fd, &buf, 1)) == -1) return 0;
    if ((AHardwareBuffer_recvHandleFromUnixSocket(fd, &ahb)) != 0) return 0;
    return (jlong)ahb;
}

JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_renderer_GPUImage_createHardwareBuffer(JNIEnv *env, jobject obj, jshort width,
                                                              jshort height, jboolean cpuAccess, jboolean useHALPixelFormatBGRA8888) {
    AHardwareBuffer* hardwareBuffer = createHardwareBuffer(width, height, cpuAccess, useHALPixelFormatBGRA8888);
    if (hardwareBuffer) {
        jclass cls = (*env)->GetObjectClass(env, obj);

        AHardwareBuffer_Desc buffDesc = {0};
        AHardwareBuffer_describe(hardwareBuffer, &buffDesc);

        jmethodID setStride = (*env)->GetMethodID(env, cls, "setStride", "(S)V");
        (*env)->CallVoidMethod(env, obj, setStride, (jshort)buffDesc.stride);

        int fd = AHardwareBuffer_getFd(hardwareBuffer);
        if (fd != -1) {
            jmethodID setNativeHandle = (*env)->GetMethodID(env, cls, "setNativeHandle", "(I)V");
            (*env)->CallVoidMethod(env, obj, setNativeHandle, fd);
        }
    }
    return (jlong)hardwareBuffer;
}

JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_renderer_GPUImage_createImageKHR(JNIEnv *env, jclass obj, jlong hardwareBufferPtr, jint textureId) {
    (void)env;
    (void)obj;
    AHardwareBuffer* hardwareBuffer = (AHardwareBuffer*)hardwareBufferPtr;
    return (jlong)createImageKHR(hardwareBuffer, textureId);
}

JNIEXPORT void JNICALL
Java_com_winlator_cmod_renderer_GPUImage_destroyHardwareBuffer(JNIEnv *env, jclass obj, jlong hardwareBufferPtr, jboolean locked) {
    (void)env;
    (void)obj;
    AHardwareBuffer* hardwareBuffer = (AHardwareBuffer*)hardwareBufferPtr;
    if (hardwareBuffer) {
        if (locked) {
            AHardwareBuffer_unlock(hardwareBuffer, NULL);
            locked = false;
        }
        AHardwareBuffer_release(hardwareBuffer);
    }
}

JNIEXPORT jobject JNICALL
Java_com_winlator_cmod_renderer_GPUImage_lockHardwareBuffer(JNIEnv *env, jclass obj, jlong hardwareBufferPtr) {
    (void)obj;
    AHardwareBuffer* hardwareBuffer = (AHardwareBuffer*)hardwareBufferPtr;
    void *virtualAddr;
    AHardwareBuffer_lock(hardwareBuffer, AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN, -1, NULL, &virtualAddr);

    AHardwareBuffer_Desc buffDesc = {0};
    AHardwareBuffer_describe(hardwareBuffer, &buffDesc);
    jlong size = buffDesc.stride * buffDesc.height * 4;
    return (*env)->NewDirectByteBuffer(env, virtualAddr, size);
}

JNIEXPORT void JNICALL
Java_com_winlator_cmod_renderer_GPUImage_destroyImageKHR(JNIEnv *env, jclass obj, jlong imageKHRPtr) {
    (void)env;
    (void)obj;
    EGLImageKHR imageKHR = (EGLImageKHR)imageKHRPtr;
    if (imageKHR) {
        EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
        eglDestroyImageKHR(eglDisplay, imageKHR);
    }
}
