cmake_minimum_required(VERSION 3.22.1)

project(Winlator)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -Wno-unused-function -Wimplicit-function-declaration -mno-outline-atomics")

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
set(AERO_REDIRECT_GLIBC_EXPORT_MAP "${CMAKE_CURRENT_SOURCE_DIR}/aero_redirect_glibc.exports.map.txt")
set(AERO_REDIRECT_BIONIC_EXPORT_MAP "${CMAKE_CURRENT_SOURCE_DIR}/aero_redirect_bionic.exports.map.txt")
set(AERO_EVSHIM_EXPORT_MAP "${CMAKE_CURRENT_SOURCE_DIR}/aero_evshim.exports.map.txt")
set(ANDROID_SYSVSHM_EXPORT_MAP "${CMAKE_CURRENT_SOURCE_DIR}/android_sysvshm.exports.map.txt")

function(aero_disable_host_rpath target_name)
    set_target_properties(${target_name} PROPERTIES
            SKIP_BUILD_RPATH TRUE
            BUILD_RPATH ""
            INSTALL_RPATH ""
            BUILD_WITH_INSTALL_RPATH FALSE
    )
endfunction()

add_library(winlator SHARED
        winlator/getauxval_shim.c
        winlator/drawable.c
        winlator/gpu_helper.c
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
aero_disable_host_rpath(winlator)

target_link_libraries(winlator
        log
        android
        jnigraphics
        aaudio
        EGL
        GLESv2
        GLESv3
)

if (HAS_ADRENOTOOLS)
    target_link_libraries(winlator adrenotools)
endif()

add_library(winlatorxr SHARED
        winlator/getauxval_shim.c
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
aero_disable_host_rpath(winlatorxr)

target_link_libraries(winlatorxr
        log
        android
        openxr_loader
        EGL
        GLESv2
        GLESv3
)

add_library(aero_redirect_glibc SHARED
        extras/redirect_common.c
        extras/preload_replace.c
)

target_compile_options(aero_redirect_glibc PRIVATE -Wall -Wextra)
target_link_options(aero_redirect_glibc PRIVATE
        "LINKER:--allow-multiple-definition"
        "LINKER:--version-script=${AERO_REDIRECT_GLIBC_EXPORT_MAP}"
)
aero_disable_host_rpath(aero_redirect_glibc)
target_link_libraries(aero_redirect_glibc
        log
        dl
)

add_library(aero_redirect_bionic SHARED
        extras/redirect_common.c
        extras/preload_replace_bionic.c
)

target_compile_options(aero_redirect_bionic PRIVATE -Wall -Wextra)
target_link_options(aero_redirect_bionic PRIVATE
        "LINKER:--allow-multiple-definition"
        "LINKER:--version-script=${AERO_REDIRECT_BIONIC_EXPORT_MAP}"
)
aero_disable_host_rpath(aero_redirect_bionic)
target_link_libraries(aero_redirect_bionic
        log
        dl
)

add_library(aero_evshim SHARED
        extras/evshim.c
)

target_compile_options(aero_evshim PRIVATE -Wall -Wextra)
target_link_options(aero_evshim PRIVATE
        "LINKER:--version-script=${AERO_EVSHIM_EXPORT_MAP}"
)
aero_disable_host_rpath(aero_evshim)
target_link_libraries(aero_evshim
        log
        dl
)

add_library(aero_android_sysvshm SHARED
        ../../../../android_sysvshm/android_sysvshm.c
)

target_include_directories(aero_android_sysvshm PRIVATE
        ../../../../android_sysvshm
)
target_compile_options(aero_android_sysvshm PRIVATE -Wall -Wextra -std=gnu99)
target_link_options(aero_android_sysvshm PRIVATE
        "LINKER:--version-script=${ANDROID_SYSVSHM_EXPORT_MAP}"
)
set_target_properties(aero_android_sysvshm PROPERTIES OUTPUT_NAME "aero_android_sysvshm")
aero_disable_host_rpath(aero_android_sysvshm)
