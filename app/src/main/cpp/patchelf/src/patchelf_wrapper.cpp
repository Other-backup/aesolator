#include <jni.h>

#include <fstream>
#include <memory>
#include <set>
#include <string>
#include <vector>

#include "patchelf.h"

using ElfFile32 = ElfFile<Elf32_Ehdr, Elf32_Phdr, Elf32_Shdr, Elf32_Addr, Elf32_Off, Elf32_Dyn, Elf32_Sym, Elf32_Versym, Elf32_Verdef, Elf32_Verdaux, Elf32_Verneed, Elf32_Vernaux, Elf32_Rel, Elf32_Rela, 32>;
using ElfFile64 = ElfFile<Elf64_Ehdr, Elf64_Phdr, Elf64_Shdr, Elf64_Addr, Elf64_Off, Elf64_Dyn, Elf64_Sym, Elf64_Versym, Elf64_Verdef, Elf64_Verdaux, Elf64_Verneed, Elf64_Vernaux, Elf64_Rel, Elf64_Rela, 64>;

namespace {

class ElfFileInterface {
public:
    virtual ~ElfFileInterface() = default;
    virtual bool is_changed() const = 0;
    virtual std::string get_interpreter() const = 0;
    virtual void set_interpreter(const std::string &interpreter) = 0;
    virtual void modify_rpath(bool add, const std::string &rpath) = 0;
    virtual void modify_soname(const std::string &soname) = 0;
    virtual void modify_os_abi(const std::string &os_abi) = 0;
    virtual void add_needed(const std::string &needed) = 0;
    virtual void remove_needed(const std::string &needed) = 0;
    virtual void rewrite_sections() = 0;
};

template <typename ElfT>
class ElfFileImpl final : public ElfFileInterface {
public:
    explicit ElfFileImpl(FileContents contents) : elf(contents) {}

    bool is_changed() const override {
        return elf.isChanged();
    }

    std::string get_interpreter() const override {
        return elf.getInterpreter();
    }

    void set_interpreter(const std::string &interpreter) override {
        elf.setInterpreter(interpreter);
    }

    void modify_rpath(bool add, const std::string &rpath) override {
        elf.modifyRPath(add ? ElfT::rpAdd : ElfT::rpRemove, {}, rpath);
    }

    void modify_soname(const std::string &soname) override {
        elf.modifySoname(ElfT::replaceSoname, soname);
    }

    void modify_os_abi(const std::string &os_abi) override {
        elf.modifyOsAbi(ElfT::replaceOsAbi, os_abi);
    }

    void add_needed(const std::string &needed) override {
        elf.addNeeded(std::set<std::string>{needed});
    }

    void remove_needed(const std::string &needed) override {
        elf.removeNeeded(std::set<std::string>{needed});
    }

    void rewrite_sections() override {
        elf.rewriteSections();
    }

private:
    ElfT elf;
};

struct ElfObject {
    std::string path;
    FileContents file_contents;
    std::unique_ptr<ElfFileInterface> elf;
};

ElfObject *from_ptr(jlong object_ptr) {
    return reinterpret_cast<ElfObject *>(object_ptr);
}

std::string jstring_to_string(JNIEnv *env, jstring value) {
    if (!env || !value) return "";
    const char *chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return "";
    std::string out(chars);
    env->ReleaseStringUTFChars(value, chars);
    return out;
}

jobjectArray empty_string_array(JNIEnv *env) {
    jclass string_class = env->FindClass("java/lang/String");
    if (!string_class) return nullptr;
    return env->NewObjectArray(0, string_class, nullptr);
}

bool write_back(ElfObject *obj) {
    if (!obj || !obj->elf || !obj->file_contents) return false;
    try {
        if (obj->elf->is_changed()) {
            obj->elf->rewrite_sections();
        }
        std::ofstream out(obj->path, std::ios::binary | std::ios::trunc);
        if (!out) return false;
        const auto &contents = *obj->file_contents;
        if (!contents.empty()) {
            out.write(reinterpret_cast<const char *>(contents.data()), static_cast<std::streamsize>(contents.size()));
        }
        return static_cast<bool>(out);
    }
    catch (...) {
        return false;
    }
}

bool with_string_arg(JNIEnv *env, jlong object_ptr, jstring value, void (ElfFileInterface::*method)(const std::string &)) {
    ElfObject *obj = from_ptr(object_ptr);
    if (!obj || !obj->elf || !value) return false;
    try {
        (obj->elf.get()->*method)(jstring_to_string(env, value));
        return true;
    }
    catch (...) {
        return false;
    }
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_winlator_cmod_core_PatchElf_createElfObject(JNIEnv *env, jobject, jstring path) {
    std::string native_path = jstring_to_string(env, path);
    if (native_path.empty()) return 0;

    try {
        std::ifstream in(native_path, std::ios::binary | std::ios::ate);
        if (!in) return 0;
        std::streamsize size = in.tellg();
        if (size < 5) return 0;
        in.seekg(0, std::ios::beg);

        auto contents = std::make_shared<std::vector<unsigned char>>(static_cast<size_t>(size));
        if (!in.read(reinterpret_cast<char *>(contents->data()), size)) return 0;

        unsigned char *data = contents->data();
        if (data[0] != 0x7f || data[1] != 'E' || data[2] != 'L' || data[3] != 'F') return 0;

        auto obj = std::make_unique<ElfObject>();
        obj->path = native_path;
        obj->file_contents = contents;
        if (data[4] == 1) {
            obj->elf = std::make_unique<ElfFileImpl<ElfFile32>>(contents);
        }
        else if (data[4] == 2) {
            obj->elf = std::make_unique<ElfFileImpl<ElfFile64>>(contents);
        }
        else {
            return 0;
        }
        return reinterpret_cast<jlong>(obj.release());
    }
    catch (...) {
        return 0;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_destroyElfObject(JNIEnv *, jobject, jlong object_ptr) {
    delete from_ptr(object_ptr);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_isChanged(JNIEnv *, jobject, jlong object_ptr) {
    ElfObject *obj = from_ptr(object_ptr);
    return obj && obj->elf && obj->elf->is_changed() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_rewriteElfObject(JNIEnv *, jobject, jlong object_ptr) {
    return write_back(from_ptr(object_ptr)) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_winlator_cmod_core_PatchElf_getInterpreter(JNIEnv *env, jobject, jlong object_ptr) {
    ElfObject *obj = from_ptr(object_ptr);
    if (!obj || !obj->elf) return env->NewStringUTF("");
    try {
        return env->NewStringUTF(obj->elf->get_interpreter().c_str());
    }
    catch (...) {
        return env->NewStringUTF("");
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_setInterpreter(JNIEnv *env, jobject, jlong object_ptr, jstring interpreter) {
    return with_string_arg(env, object_ptr, interpreter, &ElfFileInterface::set_interpreter) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_winlator_cmod_core_PatchElf_getOsAbi(JNIEnv *env, jobject, jlong) {
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_replaceOsAbi(JNIEnv *env, jobject, jlong object_ptr, jstring os_abi) {
    return with_string_arg(env, object_ptr, os_abi, &ElfFileInterface::modify_os_abi) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_winlator_cmod_core_PatchElf_getSoName(JNIEnv *env, jobject, jlong) {
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_replaceSoName(JNIEnv *env, jobject, jlong object_ptr, jstring so_name) {
    return with_string_arg(env, object_ptr, so_name, &ElfFileInterface::modify_soname) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_winlator_cmod_core_PatchElf_getRPath(JNIEnv *env, jobject, jlong) {
    return empty_string_array(env);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_addRPath(JNIEnv *env, jobject, jlong object_ptr, jstring rpath) {
    ElfObject *obj = from_ptr(object_ptr);
    if (!obj || !obj->elf || !rpath) return JNI_FALSE;
    try {
        obj->elf->modify_rpath(true, jstring_to_string(env, rpath));
        return JNI_TRUE;
    }
    catch (...) {
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_removeRPath(JNIEnv *env, jobject, jlong object_ptr, jstring rpath) {
    ElfObject *obj = from_ptr(object_ptr);
    if (!obj || !obj->elf || !rpath) return JNI_FALSE;
    try {
        obj->elf->modify_rpath(false, jstring_to_string(env, rpath));
        return JNI_TRUE;
    }
    catch (...) {
        return JNI_FALSE;
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_winlator_cmod_core_PatchElf_getNeeded(JNIEnv *env, jobject, jlong) {
    return empty_string_array(env);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_addNeeded(JNIEnv *env, jobject, jlong object_ptr, jstring needed) {
    return with_string_arg(env, object_ptr, needed, &ElfFileInterface::add_needed) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_core_PatchElf_removeNeeded(JNIEnv *env, jobject, jlong object_ptr, jstring needed) {
    return with_string_arg(env, object_ptr, needed, &ElfFileInterface::remove_needed) ? JNI_TRUE : JNI_FALSE;
}
