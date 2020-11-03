cmake_minimum_required(VERSION 3.4.1)
project(PythonExecutable C)

add_executable(python main.c main.h)
set_property(TARGET python PROPERTY POSITION_INDEPENDENT_CODE OFF)

add_executable(python-pie main.c main.h)
set_property(TARGET python-pie PROPERTY POSITION_INDEPENDENT_CODE ON)
