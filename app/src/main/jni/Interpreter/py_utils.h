#ifndef PY_UTILS_H
#define PY_UTILS_H

#include <stdio.h>

extern FILE *stdin_writer;

void setupPython(const char* pythonProgramPath, const char* pythonLibs, const char* pythonHostLibs,
                 const char* pythonHome, const char* pythonTemp, const char* xdgBasePath,
                 const char* dataDir);
void setupStdinEmulation(void);
void readFromStdin(char* inputBuffer, int bufferSize);
int runPythonInterpreter(int argc, char** argv);
void interruptPython(void);
void terminatePython(void);

#endif // PY_UTILS_H //
