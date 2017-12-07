#ifndef PYTHON_HOST_PY_COMPATIBILITY_H
#define PYTHON_HOST_PY_COMPATIBILITY_H

#include <stdlib.h>
#include <stdio.h>

typedef void (*_exitHandler)(int exitCode);

int setPythonLibrary(const char* libName);
void closePythonLibrary(void);
const char* getPythonVersion();
int call_Py_Main(int argc, char** argv);
int call_setExitHandler(_exitHandler exitHandler);
void call_Py_SetPythonHome(char* arg);
void call_Py_SetProgramName(char* arg);

#endif /* PYTHON_HOST_PY_COMPATIBILITY_H */
