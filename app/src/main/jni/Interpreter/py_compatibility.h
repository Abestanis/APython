//
// Created by Sebastian on 05.08.2015.
//

#ifndef PYTHON_HOST_PY_COMPATIBILITY_H
#define PYTHON_HOST_PY_COMPATIBILITY_H

#include <stdlib.h>
#include <stdio.h>

typedef int (*PyOS_InputHookFunc)(void);

int setPythonLibrary(const char* libName);
void closePythonLibrary(void);
const char* getPythonVersion();
int call_Py_Main(int argc, char** argv);
void call_Py_SetPythonHome(char* arg);
void call_Py_SetProgramName(char* arg);
void* call_PyMem_Malloc(size_t length);
PyOS_InputHookFunc get_PyOS_InputHook();
void set_PyOS_ReadlineFunctionPointer(char *(*func)(FILE *, FILE *, const char *));

#endif //PYTHON_HOST_PY_COMPATIBILITY_H
