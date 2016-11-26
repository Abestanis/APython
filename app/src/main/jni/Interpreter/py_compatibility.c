//
// Created by Sebastian on 05.08.2015.
//

#include "py_compatibility.h"
#include <dlfcn.h>
#include <link.h>
#include "Log/log.h"

void* pythonLib = NULL;
char pythonVersion[32] = { [0] = '\0'};

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
                strncpy(pythonVersion, pythonVersionStr, versionEndIndex);
                LOG_INFO_("Py_compatibility: PyVersion is '%s'", pythonVersion);
                return 1;
            } else {
                LOG_ERROR("Py_compatibility: Unable to extract the Python version from the return "
                          "value of 'Py_GetVersion' : '%s'", pythonVersionStr);
            }
        } else {
            LOG_ERROR("Py_compatibility: Didn't found method 'Py_GetVersion' in the python library.");
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

int call_Py_Main(int argc, char** argv) {
    static const char* pyMain = "Py_Main";
    static const char* pyMain3 = "main";
    const char* mainFuncName = pythonVersion[0] >= '3' || pythonVersion[1] != '.' ? pyMain3 : pyMain;
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
            LOG_ERROR("Py_compatibility: Didn't found method '_Py_char2wchar' in the python library.");
            return;
        }
        func = dlsym(pythonLib, funcName);
        if (func != NULL) {
            return func(_Py_char2wchar(arg, NULL));
        }
        LOG_ERROR("Py_compatibility: Didn't found method '%s' in python library.", funcName);
    }
    LOG_ERROR("Py_compatibility: Didn't found '%s' in the python library.", funcName);
}

void call_Py_SetPythonHome(char* arg) {
    return callCharSetterFunction("Py_SetPythonHome", arg);
}

void call_Py_SetProgramName(char* arg) {
    return callCharSetterFunction("Py_SetProgramName", arg);
}

void* call_PyMem_Malloc(size_t length) {
    char* pyMallocFuncName = pythonVersion[0] >= '3' && pythonVersion[2] >= '4'
                           ? "PyMem_RawMalloc" : "PyMem_Malloc";
    void* (*PyMalloc)(size_t) = dlsym(pythonLib, pyMallocFuncName);
    if (PyMalloc != NULL) {
        return PyMalloc(length);
    }
    LOG_ERROR("Py_compatibility: Didn't found method '%s' in the python library.", pyMallocFuncName);
    return NULL;
}

PyOS_InputHookFunc get_PyOS_InputHook() {
    PyOS_InputHookFunc (*get_PyOS_InputHook_func)(void) = dlsym(pythonLib, "get_PyOS_InputHook");
    if (get_PyOS_InputHook_func == NULL) {
        LOG_ERROR("Could not find 'get_PyOS_InputHook'");
        return NULL;
    }
    return get_PyOS_InputHook_func();
}

void set_PyOS_ReadlineFunctionPointer(char *(*func)(FILE *, FILE *, const char *)) {
    void (*set_PyOS_ReadlineFunctionPointer)(char *(*)(FILE *, FILE *, const char *));
    set_PyOS_ReadlineFunctionPointer = dlsym(pythonLib, "set_PyOS_ReadlineFunctionPointer");
    if (set_PyOS_ReadlineFunctionPointer != NULL) {
        set_PyOS_ReadlineFunctionPointer(func);
        return;
    }
    LOG_ERROR("Py_compatibility: Didn't found method 'set_PyOS_ReadlineFunctionPointer' in the python library.");
}
