cmake_minimum_required(VERSION 3.4.1)

add_library(application SHARED application.c application.h)
target_include_directories(application INTERFACE $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}>)
find_library(log-lib log)

target_link_libraries(
        application
        ${log-lib})
