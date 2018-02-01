#ifndef PYTHON_HOST_PY_UTILS_H
#define PYTHON_HOST_PY_UTILS_H

void setupPython(const char* pythonProgramPath, const char* pythonLibs, const char* pythonHostLibs,
                 const char* pythonHome, const char* pythonTemp, const char* xdgBasePath,
                 const char* dataDir);
int runPythonInterpreter(int argc, char** argv);

#endif /* PYTHON_HOST_PY_UTILS_H */
