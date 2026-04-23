#include <android/hardware_buffer.h>

#include "native_handle.h"

extern const native_handle_t* _Nullable AHardwareBuffer_getNativeHandle(const AHardwareBuffer* _Nonnull buffer);

int AHardwareBuffer_getFd(AHardwareBuffer* hardwareBuffer) {
    const native_handle_t* nativeHandle = AHardwareBuffer_getNativeHandle(hardwareBuffer);
    return nativeHandle && nativeHandle->numFds > 0 ? nativeHandle->data[0] : -1;
}
