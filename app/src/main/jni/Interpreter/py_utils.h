#ifndef PY_UTILS_H
#define PY_UTILS_H

#include <stdio.h>
#include <signal.h>
#include "terminal.h"

extern FILE *stdin_writer;

void setupPython(const char* pythonProgramPath, const char* pythonLibs, const char* pythonHostLibs,
                 const char* pythonHome, const char* pythonTemp, const char* xdgBasePath,
                 const char* dataDir);
void setupStdinEmulation(void);
void readFromStdin(char* inputBuffer, int bufferSize);
int runPythonInterpreter(int argc, char** argv);
__sighandler_t setSignalHandler(int signal, __sighandler_t signalHandler);

#endif // PY_UTILS_H //
