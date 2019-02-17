cmake_minimum_required(VERSION 3.4.1)

add_library(pyInterpreter SHARED
        interpreter.c interpreter.h
        py_compatibility.c py_compatibility.h
        py_utils.c py_utils.h
        readline.c readline.h
        terminal.c terminal.h
        util.c util.h)
target_include_directories(pyInterpreter INTERFACE $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}>)

target_link_libraries(
        pyInterpreter
        pyLog IPC)
