cmake_minimum_required(VERSION 3.4.1)

project(APython C)

add_subdirectory(Application)
add_subdirectory(Interpreter)
add_subdirectory(IPC)
add_subdirectory(LOG)
add_subdirectory(PythonExecutable)
if(IS_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/DevLibs)
    add_subdirectory(DevLibs)
endif()
