#ifndef VORTEK_CONTEXT_H
#define VORTEK_CONTEXT_H

#include "vortek.h"
#include "xwindow_swapchain.h"
#include "texture_decoder.h"
#include "async_pipeline_creator.h"
#include "timeline_semaphore.h"

typedef struct VkContext {
    int clientFd;
    int vkMaxVersion;
    uint32_t hostVkApiVersion;
    uint32_t physicalVkApiVersion;
    short maxDeviceMemory;
    short imageCacheSize;
    ResourceMemoryType resourceMemoryType;
    ArrayList* exposedDeviceExtensions;
    ArrayList* disabledDeviceExtensions;
    uint64_t totalAllocationSize;

    bool hasExternalMemoryFd;
    bool hasExternalMemoryDMABuf;

#if ENABLE_VALIDATION_LAYER
    VkDebugReportCallbackEXT debugReportCallback;
#endif

    char* inputBuffer;
    int inputBufferSize;
    MemoryPool memoryPool;

    pthread_t requestHandlerThread;
    RingBuffer* clientRing;
    RingBuffer* serverRing;
    VkResult status;

    int graphicsQueueIndex;

    TextureDecoder* textureDecoder;
    ShaderInspector* shaderInspector;
    ThreadPool* threadPool;

    ArrayList extraDataRequests;
    pthread_mutex_t extraDataRequestsMutex;
    ArrayList trackedHandles;

    JMethods jmethods;
    char* engineName;
} VkContext;

typedef struct ExtraDataRequest {
    uint16_t requestId;
    void* data;
    int size;
} ExtraDataRequest;

typedef struct VkTrackedHandle {
    void* handle;
    VkObjectType type;
    void* owner;
} VkTrackedHandle;

extern VkContext* createVkContext(JNIEnv *env, jobject obj, int clientFd, jobject options);
extern void destroyVkContext(JNIEnv *env, VkContext* context);
extern bool handleExtraDataRequest(VkContext* context, uint16_t requestId, int requestLength);
extern void trackVkHandle(VkContext* context, VkObjectType type, void* handle, void* owner);
extern void untrackVkHandle(VkContext* context, VkObjectType type, void* handle);
extern void untrackVkHandlesByOwner(VkContext* context, VkObjectType type, void* owner);
extern bool isTrackedVkHandle(VkContext* context, VkObjectType type, void* handle);

#endif
