cmake_minimum_required(VERSION 3.22.1)

project(Winlator)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -Wno-unused-function -Wimplicit-function-declaration")

add_subdirectory(OpenXR-SDK)
add_subdirectory(patchelf)
if (EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/adrenotools/CMakeLists.txt")
    add_subdirectory(adrenotools)
    set(HAS_ADRENOTOOLS ON)
else()
    message(WARNING "adrenotools native submodule is missing; building without native adrenotools target")
    set(HAS_ADRENOTOOLS OFF)
endif()

# Add Winlator shared libraries
set(WINLATOR_EXPORT_MAP "${CMAKE_CURRENT_SOURCE_DIR}/winlator.exports.map.txt")

add_library(winlator SHARED
        winlator/drawable.c
        winlator/gpu_image.c
        winlator/sysvshared_memory.c
        winlator/xconnector_epoll.c
        winlator/alsa_client.c
        winlator/vulkan.c
)

target_compile_options(winlator PRIVATE -Wall -Wextra)
target_compile_definitions(winlator PRIVATE AERO_HAS_ADRENOTOOLS=$<BOOL:${HAS_ADRENOTOOLS}>)
target_link_options(winlator PRIVATE
        "LINKER:--version-script=${WINLATOR_EXPORT_MAP}"
)

target_link_libraries(winlator
        log
        android
        jnigraphics
        c++_shared
        aaudio
        EGL
        GLESv2
        GLESv3
)

if (HAS_ADRENOTOOLS)
    target_link_libraries(winlator adrenotools)
endif()

add_library(winlatorxr SHARED
        xr/engine.c
        xr/framebuffer.c
        xr/input.c
        xr/main.c
        xr/math.c
        xr/renderer.c
)

target_compile_options(winlatorxr PRIVATE -Wall -Wextra)
target_link_options(winlatorxr PRIVATE
        "LINKER:--version-script=${WINLATOR_EXPORT_MAP}"
)

target_link_libraries(winlatorxr
        log
        android
        openxr_loader
        c++_shared
        EGL
        GLESv2
        GLESv3
)
