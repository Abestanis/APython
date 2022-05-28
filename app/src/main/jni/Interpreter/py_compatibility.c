//
// Created by Sebastian on 05.08.2015.
//

#include "py_compatibility.h"
#include <dlfcn.h>
#include <string.h>
#include <wchar.h>
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
            LOG_ERROR("Py_compatibility: Didn't found method '%s' in the Python library.",
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
    LOG_ERROR("Py_compatibility: Didn't found method '%s' in the Python library.", name);
    return 0;
}

static uint8_t isPython3OrNewer() {
    return pythonVersion[0] >= '3' || pythonVersion[1] != '.';
}

static wchar_t* charToWchar(const char* string) {
    mbstate_t state;
    memset(&state, 0, sizeof(state));
    size_t length = mbsrtowcs(NULL, &string, 0, &state) + 1;
    wchar_t* result = malloc(sizeof(wchar_t) * length);
    if (result != NULL) {
        memset(&state, 0, sizeof(state));
        mbsrtowcs(result, &string, length, &state);
    }
    return result;
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
    LOG_ERROR("Py_compatibility: Didn't found method '%s' in the Python library.", mainFuncName);
    return 1;
}

void callCharSetterFunction(const char* funcName, char* arg) {
    if (!isPython3OrNewer()) {
        void (*func)(char *);
        func = dlsym(pythonLib, funcName);
        if (func != NULL) {
            return func(arg);
        }
    } else {
        void (*func)(wchar_t *);
        func = dlsym(pythonLib, funcName);
        if (func != NULL) {
            wchar_t* wArg = charToWchar(arg);
            if (wArg == NULL) {
                LOG_ERROR("Py_compatibility: Failed to convert argument of function "
                          "'%s' to wchar_t.", funcName);
                return;
            }
            func(wArg);
            free(wArg);
            return;
        }
    }
    LOG_ERROR("Py_compatibility: Didn't found method '%s' in the Python library.", funcName);
}

void call_Py_SetPythonHome(char* arg) {
    callCharSetterFunction("Py_SetPythonHome", arg);
}

void call_Py_SetProgramName(char* arg) {
    callCharSetterFunction("Py_SetProgramName", arg);
}
