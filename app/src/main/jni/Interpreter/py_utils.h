#ifndef PY_UTILS_H
#define PY_UTILS_H

#include <stdio.h>

extern FILE *stdin_writer;

void setupPython(const char* pythonProgramPath, const char* pythonLibs, const char* pythonHome, const char* pythonTemp, const char* xdgBasePath);
int setupStdinEmulation(void);
int runPythonInterpreter(int argc, char** argv);

#endif // PY_UTILS_H //
