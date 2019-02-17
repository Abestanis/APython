cmake_minimum_required(VERSION 3.4.1)

add_library(IPC STATIC ipc.c ipc.h)
target_include_directories(IPC INTERFACE $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}>)

install(TARGETS IPC EXPORT SDL2Targets ARCHIVE DESTINATION "lib${LIB_SUFFIX}")
