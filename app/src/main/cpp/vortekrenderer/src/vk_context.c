#include "vk_context.h"
#include "vulkan_helper.h"
#include "request_handler.h"
#include "sysvshared_memory.h"
#include "string_utils.h"
#include "jni_utils.h"

static VkTrackedHandle* findTrackedHandle(VkContext* context, VkObjectType type, void* handle) {
    if (!context || !handle) return NULL;

    for (int i = 0; i < context->trackedHandles.size; i++) {
        VkTrackedHandle* trackedHandle = context->trackedHandles.elements[i];
        if (!trackedHandle) continue;
        if (trackedHandle->type == type && trackedHandle->handle == handle) return trackedHandle;
    }

    return NULL;
}

static void freeTrackedHandles(VkContext* context) {
    if (!context) return;

    for (int i = context->trackedHandles.size - 1; i >= 0; i--) {
        MEMFREE(context->trackedHandles.elements[i]);
        ArrayList_removeAt(&context->trackedHandles, i);
    }
}

static void loadJMethods(JMethods* jmethods) {
    JNIEnv* env;
    (*jmethods->jvm)->AttachCurrentThread(jmethods->jvm, &env, NULL);
    jmethods->env = env;

    jclass cls = (*env)->GetObjectClass(env, jmethods->obj);
    jmethods->getWindowWidth = (*env)->GetMethodID(env, cls, "getWindowWidth", "(I)I");
    jmethods->getWindowHeight = (*env)->GetMethodID(env, cls, "getWindowHeight", "(I)I");
    jmethods->getWindowHardwareBuffer = (*env)->GetMethodID(env, cls, "getWindowHardwareBuffer", "(IZ)J");
    jmethods->updateWindowContent = (*env)->GetMethodID(env, cls, "updateWindowContent", "(I)V");
}

static ExtraDataRequest* waitForExtraDataRequest(VkContext* context, uint16_t requestId) {
    ExtraDataRequest* result = NULL;
    uint32_t busyWaitIter = 0;

    while (context->status >= 0) {
        result = NULL;
        pthread_mutex_lock(&context->extraDataRequestsMutex);
        for (int i = 0; i < context->extraDataRequests.size; i++) {
            ExtraDataRequest* extraDataRequest = context->extraDataRequests.elements[i];
            if (extraDataRequest->requestId == requestId) {
                result = extraDataRequest;
                ArrayList_removeAt(&context->extraDataRequests, i);
                break;
            }
        }
        pthread_mutex_unlock(&context->extraDataRequestsMutex);

        if (result) break;
        busyWait(&busyWaitIter);
    }

    return context->status >= 0 ? result : NULL;
}

static void* requestHandlerThread(void* param) {
    VkContext* context = param;
    loadJMethods(&context->jmethods);
    ExtraDataRequest* extraDataRequest = NULL;

    while (context->status >= 0) {
        int requestCode = vt_recv(context->serverRing, &context->inputBuffer, &context->inputBufferSize, &context->memoryPool);
        if (requestCode < 0) break;

        if (requestCode > INT16_MAX) {
            uint16_t requestId = requestCode & 0xffff;
            requestCode = requestCode >> 16;
            extraDataRequest = waitForExtraDataRequest(context, requestId);
            if (!extraDataRequest) break;
            context->inputBufferSize = extraDataRequest->size;
            context->inputBuffer = extraDataRequest->data;
        }

#if DEBUG_MODE
        println("handleRequest name=%s size=%d", requestCodeToString(requestCode), context->inputBufferSize);
#endif

        HandleRequestFunc handleRequestFunc = getHandleRequestFunc(requestCode);
        if (handleRequestFunc) handleRequestFunc(context);

        vt_free(&context->memoryPool);

        if (extraDataRequest) {
            MEMFREE(extraDataRequest->data);
            MEMFREE(extraDataRequest);
        }

        context->inputBuffer = NULL;
        context->inputBufferSize = 0;
    }

    (*context->jmethods.jvm)->DetachCurrentThread(context->jmethods.jvm);
    vt_free(&context->memoryPool);
    return NULL;
}

static bool setupRingBuffers(VkContext* context) {
    int shmFds[2];
    shmFds[0] = ashmemCreateRegion("vt-server-ring", RingBuffer_getSHMemSize(SERVER_RING_BUFFER_SIZE));
    shmFds[1] = ashmemCreateRegion("vt-client-ring", RingBuffer_getSHMemSize(CLIENT_RING_BUFFER_SIZE));

    context->serverRing = RingBuffer_create(shmFds[0], SERVER_RING_BUFFER_SIZE);
    if (!context->serverRing) return false;

    context->clientRing = RingBuffer_create(shmFds[1], CLIENT_RING_BUFFER_SIZE);
    if (!context->clientRing) return false;

    int result = send_fds(context->clientFd, shmFds, 2, NULL, 0);
    close(shmFds[0]);
    close(shmFds[1]);

    if (result < 0) return false;

    pthread_create(&context->requestHandlerThread, NULL, requestHandlerThread, context);
    return true;
}

void trackVkHandle(VkContext* context, VkObjectType type, void* handle, void* owner) {
    if (!context || !handle) return;

    VkTrackedHandle* trackedHandle = findTrackedHandle(context, type, handle);
    if (trackedHandle) {
        trackedHandle->owner = owner;
        return;
    }

    trackedHandle = calloc(1, sizeof(VkTrackedHandle));
    trackedHandle->handle = handle;
    trackedHandle->type = type;
    trackedHandle->owner = owner;
    ArrayList_add(&context->trackedHandles, trackedHandle);
}

void untrackVkHandle(VkContext* context, VkObjectType type, void* handle) {
    if (!context || !handle) return;

    for (int i = context->trackedHandles.size - 1; i >= 0; i--) {
        VkTrackedHandle* trackedHandle = context->trackedHandles.elements[i];
        if (!trackedHandle) continue;
        if (trackedHandle->type != type || trackedHandle->handle != handle) continue;

        MEMFREE(trackedHandle);
        ArrayList_removeAt(&context->trackedHandles, i);
        break;
    }
}

void untrackVkHandlesByOwner(VkContext* context, VkObjectType type, void* owner) {
    if (!context || !owner) return;

    for (int i = context->trackedHandles.size - 1; i >= 0; i--) {
        VkTrackedHandle* trackedHandle = context->trackedHandles.elements[i];
        if (!trackedHandle) continue;
        if (trackedHandle->type != type || trackedHandle->owner != owner) continue;

        MEMFREE(trackedHandle);
        ArrayList_removeAt(&context->trackedHandles, i);
    }
}

bool isTrackedVkHandle(VkContext* context, VkObjectType type, void* handle) {
    return findTrackedHandle(context, type, handle) != NULL;
}

VkContext* createVkContext(JNIEnv* env, jobject obj, int clientFd, jobject options) {
    jobjectArray exposedDeviceExtensions = getJFieldByName(env, options, "exposedDeviceExtensions", JSIGNATURE_ARRAY_STRING).l;
    jobjectArray disabledDeviceExtensions = getJFieldByName(env, options, "disabledDeviceExtensions", JSIGNATURE_ARRAY_STRING).l;

    VkContext* context = calloc(1, sizeof(VkContext));
    context->clientFd = clientFd;
    context->vkMaxVersion = getJFieldByName(env, options, "vkMaxVersion", "I").i;
    context->maxDeviceMemory = getJFieldByName(env, options, "maxDeviceMemory", "S").s;
    context->imageCacheSize = getJFieldByName(env, options, "imageCacheSize", "S").s;
    context->resourceMemoryType = getJFieldByName(env, options, "resourceMemoryType", "B").b;
    context->exposedDeviceExtensions = jstringArrayToCharArray(env, exposedDeviceExtensions);
    context->disabledDeviceExtensions = jstringArrayToCharArray(env, disabledDeviceExtensions);

    pthread_mutex_init(&context->extraDataRequestsMutex, NULL);

    context->memoryPool.data = calloc(MEMORY_POOL_MAX_SIZE, 1);
    context->threadPool = ThreadPool_init(THREAD_POOL_NUM_THREADS);

    (*env)->GetJavaVM(env, &context->jmethods.jvm);
    context->jmethods.obj = (*env)->NewGlobalRef(env, obj);

    if (!setupRingBuffers(context)) {
        destroyVkContext(env, context);
        return NULL;
    }

    return context;
}

void destroyVkContext(JNIEnv* env, VkContext* context) {
    context->status = VK_ERROR_DEVICE_LOST;

    if (context->requestHandlerThread) {
        RingBuffer_setStatus(context->serverRing, RING_STATUS_EXIT);
        RingBuffer_setStatus(context->clientRing, RING_STATUS_EXIT);
        pthread_join(context->requestHandlerThread, NULL);

        ThreadPool_destroy(context->threadPool);
        context->threadPool = NULL;

        context->requestHandlerThread = 0;
        RingBuffer_free(context->serverRing);
        RingBuffer_free(context->clientRing);
    }

    if (context->jmethods.obj) {
        (*env)->DeleteGlobalRef(env, context->jmethods.obj);
        context->jmethods.obj = NULL;
    }

    context->graphicsQueueIndex = 0;

    if (context->textureDecoder) {
        TextureDecoder_destroy(context->textureDecoder);
        context->textureDecoder = NULL;
    }

    ArrayList_free(context->exposedDeviceExtensions, true);
    context->exposedDeviceExtensions = NULL;

    ArrayList_free(context->disabledDeviceExtensions, true);
    context->disabledDeviceExtensions = NULL;

    freeTrackedHandles(context);
    ArrayList_free(&context->extraDataRequests, true);
    ArrayList_free(&context->trackedHandles, false);
    pthread_mutex_destroy(&context->extraDataRequestsMutex);

    MEMFREE(context->memoryPool.data);
    MEMFREE(context->engineName);
    free(context);
}

bool handleExtraDataRequest(VkContext* context, uint16_t requestId, int requestLength) {
    void* data = NULL;
    if (requestLength > 0) {
        data = calloc(requestLength, 1);
        int bytesRead = sock_read(context->clientFd, data, requestLength);
        if (bytesRead != requestLength) return false;
    }

    ExtraDataRequest* extraDataRequest = calloc(1, sizeof(ExtraDataRequest));
    extraDataRequest->requestId = requestId;
    extraDataRequest->size = requestLength;
    extraDataRequest->data = data;

    pthread_mutex_lock(&context->extraDataRequestsMutex);
    ArrayList_add(&context->extraDataRequests, extraDataRequest);
    pthread_mutex_unlock(&context->extraDataRequestsMutex);
    return true;
}
