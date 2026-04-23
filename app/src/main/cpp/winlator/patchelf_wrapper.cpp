#include <jni.h>
#include <android/log.h>

namespace {
constexpr char TAG[] = "PatchElfJNI";
constexpr char MESSAGE[] = "PatchElf JNI bridge is not wired in the Android build; returning unsupported";

void log_unsupported(const char *method) {
    __android_log_print(ANDROID_LOG_WARN, TAG, "%s: %s", method, MESSAGE);
}

jobjectArray empty_string_array(JNIEnv *env) {
    jclass string_class = env->FindClass("java/lang/String");
    if (!string_class) return nullptr;
    return env->NewObjectArray(0, string_class, nullptr);
}
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_core_PatchElf_createElfObject(JNIEnv *env, jobject thiz, jstring path) {
    (void)env;
    (void)thiz;
    (void)path;
    log_unsupported("createElfObject");
    return 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_destroyElfObject(JNIEnv *env, jobject thiz, jlong object_ptr) {
    (void)env;
    (void)thiz;
    (void)object_ptr;
    log_unsupported("destroyElfObject");
    return JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_isChanged(JNIEnv *env, jobject thiz, jlong object_ptr) {
    (void)env;
    (void)thiz;
    (void)object_ptr;
    log_unsupported("isChanged");
    return JNI_FALSE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_winlator_cmod_core_PatchElf_getInterpreter(JNIEnv *env, jobject thiz, jlong object_ptr) {
    (void)thiz;
    (void)object_ptr;
    log_unsupported("getInterpreter");
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_setInterpreter(JNIEnv *env, jobject thiz, jlong object_ptr,
                                                     jstring interpreter) {
    (void)env;
    (void)thiz;
    (void)object_ptr;
    (void)interpreter;
    log_unsupported("setInterpreter");
    return JNI_FALSE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_winlator_cmod_core_PatchElf_getOsAbi(JNIEnv *env, jobject thiz, jlong object_ptr) {
    (void)thiz;
    (void)object_ptr;
    log_unsupported("getOsAbi");
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_replaceOsAbi(JNIEnv *env, jobject thiz, jlong object_ptr,
                                                   jstring os_abi) {
    (void)env;
    (void)thiz;
    (void)object_ptr;
    (void)os_abi;
    log_unsupported("replaceOsAbi");
    return JNI_FALSE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_winlator_cmod_core_PatchElf_getSoName(JNIEnv *env, jobject thiz, jlong object_ptr) {
    (void)thiz;
    (void)object_ptr;
    log_unsupported("getSoName");
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_replaceSoName(JNIEnv *env, jobject thiz, jlong object_ptr,
                                                    jstring so_name) {
    (void)env;
    (void)thiz;
    (void)object_ptr;
    (void)so_name;
    log_unsupported("replaceSoName");
    return JNI_FALSE;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_winlator_cmod_core_PatchElf_getRPath(JNIEnv *env, jobject thiz, jlong object_ptr) {
    (void)thiz;
    (void)object_ptr;
    log_unsupported("getRPath");
    return empty_string_array(env);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_addRPath(JNIEnv *env, jobject thiz, jlong object_ptr,
                                               jstring rpath) {
    (void)env;
    (void)thiz;
    (void)object_ptr;
    (void)rpath;
    log_unsupported("addRPath");
    return JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_removeRPath(JNIEnv *env, jobject thiz, jlong object_ptr,
                                                  jstring rpath) {
    (void)env;
    (void)thiz;
    (void)object_ptr;
    (void)rpath;
    log_unsupported("removeRPath");
    return JNI_FALSE;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_winlator_cmod_core_PatchElf_getNeeded(JNIEnv *env, jobject thiz, jlong object_ptr) {
    (void)thiz;
    (void)object_ptr;
    log_unsupported("getNeeded");
    return empty_string_array(env);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_addNeeded(JNIEnv *env, jobject thiz, jlong object_ptr,
                                                jstring needed) {
    (void)env;
    (void)thiz;
    (void)object_ptr;
    (void)needed;
    log_unsupported("addNeeded");
    return JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_removeNeeded(JNIEnv *env, jobject thiz, jlong object_ptr,
                                                   jstring needed) {
    (void)env;
    (void)thiz;
    (void)object_ptr;
    (void)needed;
    log_unsupported("removeNeeded");
    return JNI_FALSE;
}
