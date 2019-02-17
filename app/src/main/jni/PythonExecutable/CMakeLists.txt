cmake_minimum_required(VERSION 3.4.1)
project(PythonExecutable C)

set(linkerOptions -rpath $$ORIGIN/lib)

add_executable(python main.c main.h)
set_property(TARGET python PROPERTY POSITION_INDEPENDENT_CODE OFF)
target_link_libraries(python PRIVATE ${linkerOptions})

add_executable(python-pie main.c main.h)
set_property(TARGET python-pie PROPERTY POSITION_INDEPENDENT_CODE ON)
target_link_libraries(python-pie PRIVATE ${linkerOptions})
