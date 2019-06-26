//
// Created by Sebastian on 05.08.2015.
//

#include "py_compatibility.h"
#include <dlfcn.h>
#include <string.h>
#include "log.h"

#ifndef MIN
#  define MIN(a, b) (((a)<(b))?(a):(b))
#endif /* MIN */

#define MAX_PY_VERSION_SIZE 32
void* pythonLib = NULL;
char pythonVersion[MAX_PY_VERSION_SIZE] = { [0] = '\0'};

int setPythonLibrary(const char* libName) {
    pythonLib = dlopen(libName, RTLD_LAZY);
    if (pythonLib != NULL) {
        setenv("PYTHON_LIBRARY_NAME", libName, 1);
        const char* (*Py_GetVersion)(void) = dlsym(pythonLib, "Py_GetVersion");
        if (Py_GetVersion != NULL) {
            const char* pythonVersionStr = Py_GetVersion();
            char* spacePointer = strchr(pythonVersionStr, ' ');
            if (spacePointer != NULL) {
                size_t versionEndIndex = (spacePointer - pythonVersionStr) / sizeof(char);
                strncpy(pythonVersion, pythonVersionStr, MIN(versionEndIndex, MAX_PY_VERSION_SIZE));
                LOG_INFO_("Py_compatibility: PyVersion is '%s'", pythonVersion);
                return 1;
            } else {
                LOG_ERROR("Py_compatibility: Unable to extract the Python version from the return "
                          "value of 'Py_GetVersion' : '%s'", pythonVersionStr);
            }
        } else {
            LOG_ERROR("Py_compatibility: Didn't found method '%s' in the python library.",
                      "Py_GetVersion");
        }
    } else {
        LOG_ERROR("Failed to load the Python library (%s): %s", libName, dlerror());
    }
    return 0;
}

void closePythonLibrary() {
    if (pythonLib == NULL) {
        LOG_WARN("Ignored an attempt to close the Python library while it was not opened.");
        return;
    }
    dlclose(pythonLib);
}

const char* getPythonVersion() {
    return pythonVersion;
}

int call_setExitHandler(_exitHandler exitHandler) {
    static const char* name = "setExitHandler";
    void (*setExitHandler)(_exitHandler) = dlsym(pythonLib, name);
    if (setExitHandler != NULL) {
        setExitHandler(exitHandler);
        return 1;
    }
    LOG_ERROR("Py_compatibility: Didn't found method '%s' in the python library.", name);
    return 0;
}

int call_Py_Main(int argc, char** argv) {
    static const char* pyMain = "Py_Main";
    static const char* pyMain3 = "main";
    const char* mainFuncName = pythonVersion[0] >= '3' || pythonVersion[1] != '.'
                               ? pyMain3 : pyMain;
    int (*Py_Main)(int, char **) = dlsym(pythonLib, mainFuncName);
    if (Py_Main != NULL) {
        return Py_Main(argc, argv);
    }
    LOG_ERROR("Py_compatibility: Didn't found method '%s' in the python library.", mainFuncName);
    return 1;
}

void callCharSetterFunction(const char* funcName, char* arg) {
    if (pythonVersion[0] == '2' || pythonVersion[0] == '1') {
        void (*func)(char *);
        func = dlsym(pythonLib, funcName);
        if (func != NULL) {
            return func(arg);
        }
    } else {
        void (*func)(wchar_t *);
        wchar_t* (*_Py_char2wchar) (char*, size_t*);
        _Py_char2wchar = dlsym(pythonLib, "_Py_char2wchar");
        if (_Py_char2wchar == NULL) {
            LOG_ERROR("Py_compatibility: Didn't found method '%s' in the python library.",
                      "_Py_char2wchar");
            return;
        }
        func = dlsym(pythonLib, funcName);
        if (func != NULL) {
            return func(_Py_char2wchar(arg, NULL));
        }
    }
    LOG_ERROR("Py_compatibility: Didn't found method '%s' in the python library.", funcName);
}

void call_Py_SetPythonHome(char* arg) {
    return callCharSetterFunction("Py_SetPythonHome", arg);
}

void call_Py_SetProgramName(char* arg) {
    return callCharSetterFunction("Py_SetProgramName", arg);
}
