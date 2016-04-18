#include "main.h"
#include "Log/log.h"
#include <stdio.h>
#include <unistd.h>
#include "Interpreter/py_utils.h"
#include "Interpreter/py_compatibility.h"

const char* getPythonLibraryName() {
    char* name = getenv("PYTHON_LIBRARY_NAME");
    if (name == NULL) {
        name = "libpython3.4.so";
        LOG_WARN("Python executable was unable to retrieve the python library name! Using default (%s).", name);
    }
    return (const char*) name;
}

int main(int argc, char** argv) {
    const char* defaultPath  = "/data/data/com.apython.python.pythonhost/"; // This is an fallback if argv[0] is not set right;
    const char* libAppendix  = "lib";
    const char* homeAppendix = "files";
    const char* tempAppendix = "cache";
    const char* xdgAppendix  = "pythonApps";
    const char* basePath     = NULL;
    const char *programName  = NULL;
    int hasAbsolutePath      = 0;
    if (argc > 0 && access(argv[0], F_OK) != -1) {
        programName     = argv[0];
        hasAbsolutePath = 1;
    } else {
        programName     = "Python";
    }
    if (hasAbsolutePath != 0) {
        char* indexPointer = strrchr(programName, '/');
        int index = ((indexPointer - programName) / sizeof(char)) + 1;
        basePath = malloc(sizeof(char) * (index + 1));
        strncpy((char*) basePath, programName, index);
        *(((char*) basePath) + index) = 0;
    } else {
        basePath = malloc(sizeof(char) * (strlen(defaultPath) + 1));
        strcpy((char*) basePath, defaultPath);
    }
    // load pythonLib
    if (!setPythonLibrary(getPythonLibraryName())) { return 1; }
    // libs
    const char* pythonLibs = malloc(sizeof(char) * (strlen(basePath) + strlen(libAppendix) + 1));
    ASSERT(pythonLibs != NULL, "Not enough memory to construct the path to the Python libraries!");
    strcpy((char*) pythonLibs, basePath);
    strcat((char*) pythonLibs, libAppendix);
    // home
    const char *pythonHome = malloc(sizeof(char) * (strlen(basePath) + strlen(homeAppendix) + 1));
    ASSERT(pythonHome != NULL, "Not enough memory to construct the path to the Python home directory!");
    strcpy((char*) pythonHome, basePath);
    strcat((char*) pythonHome, homeAppendix);
    // temp
    const char *pythonTemp = malloc(sizeof(char) * (strlen(basePath) + strlen(tempAppendix) + 1));
    ASSERT(pythonTemp != NULL, "Not enough memory to construct the path to the Python temp directory!");
    strcpy((char*) pythonTemp, basePath);
    strcat((char*) pythonTemp, tempAppendix);
    // xdg
    const char *xdgBasePath = malloc(sizeof(char) * (strlen(basePath) + strlen(xdgAppendix) + 1));
    ASSERT(xdgBasePath != NULL, "Not enough memory to construct the xdg base path!");
    strcpy((char*) xdgBasePath, basePath);
    strcat((char*) xdgBasePath, xdgAppendix);
    setupPython(programName, pythonLibs, "", pythonHome, pythonTemp, xdgBasePath);
    int result = call_Py_Main(argc, argv);
    free((char*) basePath);
    free((char*) pythonLibs);
    free((char*) pythonHome);
    free((char*) pythonTemp);
    free((char*) xdgBasePath);
    closePythonLibrary();
    return result;
}