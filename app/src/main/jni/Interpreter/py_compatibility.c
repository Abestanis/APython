//
// Created by Sebastian on 05.08.2015.
//

#include "py_compatibility.h"
#include <dlfcn.h>
#include <link.h>
#include "Log/log.h"

void* pythonLib = NULL;
const char* pythonVersion = NULL;

int setPythonLibrary(const char* libName) {
    pythonLib = dlopen(libName, RTLD_LAZY);
    if (pythonLib != NULL) {
        setenv("PYTHON_LIBRARY_NAME", libName, 1);
        const char* (*Py_getVersionString)(void);
        Py_getVersionString = dlsym(pythonLib, "Py_getVersionString");
        if (Py_getVersionString != NULL) {
            pythonVersion = (const char*) Py_getVersionString();
            return 1;
        } else {
            LOG_ERROR("Py_compability: Didn't found method 'Py_getVersionString' in the python library.");
            return 0;
        }
    } else {
        LOG_ERROR("Failed to load the Python library (%s): %s", libName, dlerror());
        return 0;
    }
}

void closePythonLibrary() {
    if (pythonLib == NULL) {
        LOG_WARN("Ignored an attempt to close the Python library while it was not opend.");
        return;
    }
    dlclose(pythonLib);
}

const char* getPythonVersion() {
    return pythonVersion;
}

int call_Py_Main(int argc, char** argv) {
    if (pythonVersion[0] == '2' || pythonVersion[0] == '1') {
        int (*Py_Main)(int, char **);
        Py_Main = dlsym(pythonLib, "Py_Main");
        if (Py_Main != NULL) {
            return (int) Py_Main(argc, argv);
        }
        LOG_ERROR("Py_compability: Didn't found method 'Py_Main' in the python library.");
    } else {
        int (*oldPy_Main)(int, char **);
        oldPy_Main = dlsym(pythonLib, "oldPy_Main");
        if (oldPy_Main != NULL) {
            return (int) oldPy_Main(argc, argv);
        }
        LOG_ERROR("Py_compability: Didn't found method 'oldPy_Main' in the python library.");
    }
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
            LOG_ERROR("Py_compability: Didn't found method '_Py_char2wchar' in the python library.");
            return;
        }
        func = dlsym(pythonLib, funcName);
        if (func != NULL) {
            return func(_Py_char2wchar(arg, NULL));
        }
        LOG_ERROR("Py_compability: Didn't found method '%s' in python library.", funcName);
    }
    LOG_ERROR("Py_compability: Didn't found '%s' in the python library.", funcName);
}

void call_Py_SetPythonHome(char* arg) {
    return callCharSetterFunction("Py_SetPythonHome", arg);
}

void call_Py_SetProgramName(char* arg) {
    return callCharSetterFunction("Py_SetProgramName", arg);
}

void* call_PyMem_Malloc(size_t length) {
    if (pythonVersion[0] - '0' >= 3 && pythonVersion[2] - '0' >= 4) {
        void* (*PyMem_RawMalloc)(size_t);
        PyMem_RawMalloc = dlsym(pythonLib, "PyMem_RawMalloc");
        if (PyMem_RawMalloc != NULL) {
            LOG("Running new PyMem_RawMalloc");
            return (void*) PyMem_RawMalloc(length);
        }
        LOG_ERROR("Py_compability: Didn't found method 'PyMem_RawMalloc' in the python library.");
    } else {
        void* (*PyMem_Malloc)(size_t);
        PyMem_Malloc = dlsym(pythonLib, "PyMem_Malloc");
        if (PyMem_Malloc != NULL) {
            LOG("Running old PyMem_Malloc");
            return (void*) PyMem_Malloc(length); // TODO: Not thread safe
        }
        LOG_ERROR("Py_compability: Didn't found method 'PyMem_Malloc' in the python library.");
    }
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
    void (*set_PyOS_ReadlineFunctionPointer)(char *(*func)(FILE *, FILE *, const char *));
    set_PyOS_ReadlineFunctionPointer = dlsym(pythonLib, "set_PyOS_ReadlineFunctionPointer");
    if (set_PyOS_ReadlineFunctionPointer != NULL) {
        set_PyOS_ReadlineFunctionPointer(func);
        return;
    }
    LOG_ERROR("Py_compability: Didn't found method 'set_PyOS_ReadlineFunctionPointer' in the python library.");
}
