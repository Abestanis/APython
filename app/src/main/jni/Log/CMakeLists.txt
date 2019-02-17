cmake_minimum_required(VERSION 3.4.1)

add_library(pyLog SHARED log.c log.h)
target_include_directories(pyLog INTERFACE $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}>)
find_library(log-lib log)

target_link_libraries(
        pyLog
        ${log-lib})
