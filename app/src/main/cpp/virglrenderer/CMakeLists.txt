cmake_minimum_required(VERSION 3.22.1)

project(VirGLBridge)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -Wno-unused-function -Wimplicit-function-declaration")

include_directories(server)
include_directories(src)
include_directories(src/gallium/include)

add_library(virglbridge SHARED
        server/virgl_server.c
        server/virgl_server_shm.c
        server/virgl_server_renderer.c)

target_link_libraries(virglbridge
        winlator
        log
        android
        dl
        EGL
        GLESv2
        GLESv3)
