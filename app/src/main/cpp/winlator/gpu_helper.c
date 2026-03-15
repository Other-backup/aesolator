#include <jni.h>
#include <vulkan/vulkan.h>
#include <stdlib.h>
#include <dlfcn.h>
#include <string.h>
#include <android/log.h>

#define LOG_TAG "GPUHelper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static jobjectArray make_empty_array(JNIEnv *env) {
    jclass stringCls = (*env)->FindClass(env, "java/lang/String");
    if (!stringCls) return NULL;
    return (*env)->NewObjectArray(env, 0, stringCls, NULL);
}

typedef VkResult (VKAPI_PTR *PFN_vkEnumerateInstanceVersionCompat)(uint32_t *pApiVersion);

static uint32_t query_instance_api_version(void *vulkan_handle, PFN_vkGetInstanceProcAddr gip) {
    PFN_vkEnumerateInstanceVersionCompat enumerateInstanceVersion = NULL;
    if (gip) {
        enumerateInstanceVersion = (PFN_vkEnumerateInstanceVersionCompat) gip(VK_NULL_HANDLE, "vkEnumerateInstanceVersion");
    }
    if (!enumerateInstanceVersion && vulkan_handle) {
        enumerateInstanceVersion = (PFN_vkEnumerateInstanceVersionCompat) dlsym(vulkan_handle, "vkEnumerateInstanceVersion");
    }
    if (enumerateInstanceVersion) {
        uint32_t apiVersion = VK_API_VERSION_1_0;
        if (enumerateInstanceVersion(&apiVersion) == VK_SUCCESS) {
            LOGI("Detected Vulkan instance API version: %u.%u.%u",
                    VK_VERSION_MAJOR(apiVersion),
                    VK_VERSION_MINOR(apiVersion),
                    VK_VERSION_PATCH(apiVersion));
            return apiVersion;
        }
    }
    LOGI("vkEnumerateInstanceVersion unavailable, assuming Vulkan 1.0");
    return VK_API_VERSION_1_0;
}

JNIEXPORT jobjectArray JNICALL
Java_com_winlator_cmod_core_GPUHelper_vkGetDeviceExtensions(JNIEnv *env, jclass clazz) {
    (void) clazz;

    void *vulkan_handle = NULL;
    PFN_vkGetInstanceProcAddr gip = NULL;
    PFN_vkCreateInstance createInstance = NULL;
    PFN_vkEnumeratePhysicalDevices enumeratePhysicalDevices = NULL;
    PFN_vkEnumerateDeviceExtensionProperties enumerateDeviceExtensionProperties = NULL;
    PFN_vkGetPhysicalDeviceProperties getPhysicalDeviceProperties = NULL;
    PFN_vkDestroyInstance destroyInstance = NULL;
    VkInstance instance = VK_NULL_HANDLE;
    VkPhysicalDevice *physicalDevices = NULL;
    VkExtensionProperties *extensions = NULL;
    jobjectArray result = NULL;
    VkResult status;

    vulkan_handle = dlopen("libvulkan.so", RTLD_LOCAL | RTLD_NOW);
    if (!vulkan_handle) {
        LOGE("Failed to load libvulkan.so: %s", dlerror());
        return make_empty_array(env);
    }

    gip = (PFN_vkGetInstanceProcAddr) dlsym(vulkan_handle, "vkGetInstanceProcAddr");
    createInstance = (PFN_vkCreateInstance) dlsym(vulkan_handle, "vkCreateInstance");
    if (!gip || !createInstance) {
        LOGE("Failed to resolve Vulkan entry points");
        goto fail;
    }

    uint32_t instanceApiVersion = query_instance_api_version(vulkan_handle, gip);
    VkApplicationInfo appInfo;
    memset(&appInfo, 0, sizeof(appInfo));
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "Ae.solator";
    appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.pEngineName = "Ae.solator";
    appInfo.engineVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.apiVersion = instanceApiVersion;

    VkInstanceCreateInfo createInfo;
    memset(&createInfo, 0, sizeof(createInfo));
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;

    status = createInstance(&createInfo, NULL, &instance);
    if (status != VK_SUCCESS || instance == VK_NULL_HANDLE) {
        LOGE("vkCreateInstance failed: %d", status);
        goto fail;
    }

    enumeratePhysicalDevices =
            (PFN_vkEnumeratePhysicalDevices) gip(instance, "vkEnumeratePhysicalDevices");
    enumerateDeviceExtensionProperties =
            (PFN_vkEnumerateDeviceExtensionProperties) gip(instance, "vkEnumerateDeviceExtensionProperties");
    getPhysicalDeviceProperties =
            (PFN_vkGetPhysicalDeviceProperties) gip(instance, "vkGetPhysicalDeviceProperties");
    destroyInstance =
            (PFN_vkDestroyInstance) gip(instance, "vkDestroyInstance");

    if (!enumeratePhysicalDevices || !enumerateDeviceExtensionProperties || !destroyInstance) {
        LOGE("Failed to resolve Vulkan instance functions");
        goto fail;
    }

    uint32_t physicalDeviceCount = 0;
    status = enumeratePhysicalDevices(instance, &physicalDeviceCount, NULL);
    if (status != VK_SUCCESS || physicalDeviceCount == 0) {
        LOGE("No Vulkan physical devices available: %d", status);
        goto fail;
    }

    physicalDevices = (VkPhysicalDevice *) calloc(physicalDeviceCount, sizeof(VkPhysicalDevice));
    if (!physicalDevices) goto fail;

    status = enumeratePhysicalDevices(instance, &physicalDeviceCount, physicalDevices);
    if (status != VK_SUCCESS && status != VK_INCOMPLETE) {
        LOGE("Failed to enumerate Vulkan physical devices: %d", status);
        goto fail;
    }

    VkPhysicalDevice physicalDevice = physicalDevices[0];
    if (getPhysicalDeviceProperties && physicalDeviceCount > 1) {
        VkPhysicalDeviceProperties props;
        for (uint32_t i = 0; i < physicalDeviceCount; ++i) {
            getPhysicalDeviceProperties(physicalDevices[i], &props);
            if (props.deviceType == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                physicalDevice = physicalDevices[i];
                break;
            }
        }
    }
    free(physicalDevices);
    physicalDevices = NULL;

    uint32_t extensionCount = 0;
    status = enumerateDeviceExtensionProperties(physicalDevice, NULL, &extensionCount, NULL);
    if (status != VK_SUCCESS || extensionCount == 0) {
        LOGW("No device extensions returned: %d", status);
        goto fail;
    }

    uint32_t allocatedCount = extensionCount + 16;
    extensions = (VkExtensionProperties *) calloc(allocatedCount, sizeof(VkExtensionProperties));
    if (!extensions) goto fail;

    uint32_t actualCount = allocatedCount;
    status = enumerateDeviceExtensionProperties(physicalDevice, NULL, &actualCount, extensions);
    if (status == VK_INCOMPLETE) {
        free(extensions);
        allocatedCount = actualCount + 32;
        extensions = (VkExtensionProperties *) calloc(allocatedCount, sizeof(VkExtensionProperties));
        if (!extensions) goto fail;
        actualCount = allocatedCount;
        status = enumerateDeviceExtensionProperties(physicalDevice, NULL, &actualCount, extensions);
    }
    if (status != VK_SUCCESS) {
        LOGE("Failed to enumerate device extensions: %d", status);
        goto fail;
    }

    jclass stringCls = (*env)->FindClass(env, "java/lang/String");
    if (!stringCls) goto fail;
    result = (*env)->NewObjectArray(env, (jsize) actualCount, stringCls, NULL);
    if (!result) goto fail;

    for (jsize i = 0; i < (jsize) actualCount; ++i) {
        jstring value = (*env)->NewStringUTF(env, extensions[i].extensionName);
        if (!value) continue;
        (*env)->SetObjectArrayElement(env, result, i, value);
        (*env)->DeleteLocalRef(env, value);
    }

    free(extensions);
    if (destroyInstance && instance != VK_NULL_HANDLE) destroyInstance(instance, NULL);
    if (vulkan_handle) dlclose(vulkan_handle);
    return result;

fail:
    if (physicalDevices) free(physicalDevices);
    if (extensions) free(extensions);
    if (destroyInstance && instance != VK_NULL_HANDLE) destroyInstance(instance, NULL);
    if (vulkan_handle) dlclose(vulkan_handle);
    return make_empty_array(env);
}

JNIEXPORT jint JNICALL
Java_com_winlator_cmod_core_GPUHelper_vkGetApiVersion(JNIEnv *env, jclass clazz) {
    (void) env;
    (void) clazz;

    void *vulkan_handle = NULL;
    PFN_vkGetInstanceProcAddr gip = NULL;
    PFN_vkCreateInstance createInstance = NULL;
    PFN_vkEnumeratePhysicalDevices enumeratePhysicalDevices = NULL;
    PFN_vkGetPhysicalDeviceProperties getPhysicalDeviceProperties = NULL;
    PFN_vkDestroyInstance destroyInstance = NULL;
    VkInstance instance = VK_NULL_HANDLE;
    VkPhysicalDevice *physicalDevices = NULL;
    VkResult status;
    jint result = VK_MAKE_VERSION(1, 0, 0);

    vulkan_handle = dlopen("libvulkan.so", RTLD_LOCAL | RTLD_NOW);
    if (!vulkan_handle) {
        LOGE("vkGetApiVersion: failed to load libvulkan.so");
        return result;
    }

    gip = (PFN_vkGetInstanceProcAddr) dlsym(vulkan_handle, "vkGetInstanceProcAddr");
    createInstance = (PFN_vkCreateInstance) dlsym(vulkan_handle, "vkCreateInstance");
    if (!gip || !createInstance) goto cleanup;

    uint32_t instanceApiVersion = query_instance_api_version(vulkan_handle, gip);
    VkApplicationInfo appInfo;
    memset(&appInfo, 0, sizeof(appInfo));
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.apiVersion = instanceApiVersion;

    VkInstanceCreateInfo createInfo;
    memset(&createInfo, 0, sizeof(createInfo));
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;

    status = createInstance(&createInfo, NULL, &instance);
    if (status != VK_SUCCESS || instance == VK_NULL_HANDLE) goto cleanup;

    enumeratePhysicalDevices =
            (PFN_vkEnumeratePhysicalDevices) gip(instance, "vkEnumeratePhysicalDevices");
    getPhysicalDeviceProperties =
            (PFN_vkGetPhysicalDeviceProperties) gip(instance, "vkGetPhysicalDeviceProperties");
    destroyInstance =
            (PFN_vkDestroyInstance) gip(instance, "vkDestroyInstance");
    if (!enumeratePhysicalDevices || !getPhysicalDeviceProperties || !destroyInstance) goto cleanup;

    uint32_t physicalDeviceCount = 0;
    status = enumeratePhysicalDevices(instance, &physicalDeviceCount, NULL);
    if (status != VK_SUCCESS || physicalDeviceCount == 0) goto cleanup;

    physicalDevices = (VkPhysicalDevice *) calloc(physicalDeviceCount, sizeof(VkPhysicalDevice));
    if (!physicalDevices) goto cleanup;

    status = enumeratePhysicalDevices(instance, &physicalDeviceCount, physicalDevices);
    if (status != VK_SUCCESS && status != VK_INCOMPLETE) goto cleanup;

    VkPhysicalDeviceProperties props;
    getPhysicalDeviceProperties(physicalDevices[0], &props);
    result = (jint) props.apiVersion;

cleanup:
    if (physicalDevices) free(physicalDevices);
    if (destroyInstance && instance != VK_NULL_HANDLE) destroyInstance(instance, NULL);
    if (vulkan_handle) dlclose(vulkan_handle);
    return result;
}
