#include "main.h"
#include <string.h>
#include <errno.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/stat.h>

void printUsage(const char* programName) {
    static int mExecPrev = 0;
    if (mExecPrev++ != 0) return;  // We only want to print the usage once ;-p
    printf(
        "Python Launcher for Android\n"
        "\n"
        "usage: %s [ launcher-arguments ] [ python-arguments ]\n"
        "\n"
        "Launcher arguments:\n"
        "\n"
        "-N  : Launch the latest Python N.x version\n"
        "-X.Y: Launch the specified Python version\n"
        "\n"
        "The following help text is from Python:\n\n",
        programName
    );
}

void removeArgFromArgv(size_t index, int *argc, char** argv) {
    size_t i;
    for (i = index; i < *argc - 1; i++) {
        argv[i] = argv[i + 1];
    }
    argv[--(*argc)] = NULL;
}

/*
 * Parse the command line arguments for this launcher.
 * Return the command line argument that specifies which Python version
 * we should load, if one was specified.
 *
 * This function removes the launcher arguments from the given
 * arguments, so that they may be passed to the python interpreter
 */
char* parseLauncherArgs(const char* programName, int* argc, char** argv) {
    // Check the command arguments for -X.Y, -N, -h or --help
    char* versionArg = NULL;
    size_t i, j;
    int hadPossibleLauncherArg = 0;
    for (i = 0; i < *argc; i++) {
        if (argv[i][0] == '-') {
            if (argv[i][1] == 'h' || (argv[i][1] == '-' && (strcmp(&argv[i][2], "help") == 0))) {
                printUsage(programName);
            } else if (!hadPossibleLauncherArg && versionArg == NULL) {
                int haveLauncherArg = 0;
                int hasDecimalPoint = 0;
                hadPossibleLauncherArg = 1;
                for (j = 1; argv[i][j] != '\0'; j++) {
                    if (argv[i][j] < '0' || argv[i][j] > '9') {
                        if (argv[i][j] != '.' || hasDecimalPoint++ > 1 || j == 1 || argv[i][j + 1] == '\0') {
                            haveLauncherArg = 0; break;
                        }
                    }
                    haveLauncherArg = 1;
                }
                if (haveLauncherArg) {
                    versionArg = argv[i];
                    removeArgFromArgv(i, argc, argv);
                    i--;
                }
            } else break;
        } else if (i != 0) break;
    }
    return versionArg;
}

char* getPythonLibName(const char* pythonLibDir, char* pyVersionArg) {
    char* libName = NULL;
    if (pyVersionArg != NULL) {
        if (strchr(pyVersionArg, '.') != NULL) { // Exact Python version (-X.Y)
            size_t libNameLen = strlen("libpython.so") + strlen(&pyVersionArg[1]) + 1;
            libName = malloc(sizeof(char) * libNameLen);
            snprintf(libName, libNameLen, "libpython%s.so", &pyVersionArg[1]);
            char libFilePath[PATH_MAX];
            snprintf(libFilePath, PATH_MAX, "%s/%s", pythonLibDir, libName);
            struct stat statBuffer;
            if (stat(libFilePath, &statBuffer) != 0) {
                LOG_ERROR("Failed to find a python version matching command line argument %s",
                          pyVersionArg);
                free(libName);
                libName = NULL;
            }
        } else { // Get best installed Python version of major version N (-N)
            // Check for the best match of the specified version in the lib dir
            char fileNameStart[32];
            size_t fileNameStartLen = 32;
            size_t libNameLen = 0, nameLen = 0;
            int minorVersion, maxMinorVersion = 0;
            snprintf(fileNameStart, fileNameStartLen, "libpython%s.", &pyVersionArg[1]);
            fileNameStartLen = strlen(fileNameStart);
            DIR* pyLibDir = opendir(pythonLibDir);
            struct dirent* entry;
            if (pyLibDir != NULL) {
                while ((entry = readdir(pyLibDir)) != NULL) {
                    if (strncmp(entry->d_name, fileNameStart, fileNameStartLen) == 0) {
                        minorVersion = atoi(&entry->d_name[strlen("libpython.") + strlen(&pyVersionArg[1])]);
                        if (minorVersion > maxMinorVersion) {
                            maxMinorVersion = minorVersion;
                            nameLen = strlen(entry->d_name) + 1;
                            if (nameLen > libNameLen) {
                                if (libName != NULL) free(libName);
                                libName = malloc(sizeof(char) * nameLen);
                                libNameLen = nameLen;
                            }
                            strncpy(libName, entry->d_name, libNameLen);
                        }
                    }
                }
                closedir(pyLibDir);
                if (libName == NULL) {
                    LOG_ERROR("Failed to find a python version matching command line argument %s",
                              pyVersionArg);
                }
            } else {
                LOG_ERROR("Failed to open the Python library directory (%s): %s",
                          pythonLibDir, strerror(errno));
            }
        }
        return libName;
    }
    // Check the environment for PYTHON_LIBRARY_NAME
    const char* envLibName = (const char*) getenv("PYTHON_LIBRARY_NAME");
    if (envLibName != NULL) {
        libName = strdup(envLibName);
        if (libName != NULL) {
            return libName;
        }
        LOG_ERROR("Failed to copy the Python library name from the environment: Out of memory!");
    }
    // Fall back to newest Python version from python Lib directory
    size_t libNameLen = 0, nameLen = 0;
    int majorVersion, maxMajorVersion = 0;
    int minorVersion, maxMinorVersion = 0;
    DIR* pyLibDir = opendir(pythonLibDir);
    struct dirent* entry;
    if (pyLibDir != NULL) {
        while ((entry = readdir(pyLibDir)) != NULL) {
            if (strncmp(entry->d_name, "libpython", strlen("libpython")) == 0) {
                if (sscanf(entry->d_name, "libpython%d.%d.so", &majorVersion, &minorVersion) != 2) continue;
                if (majorVersion > maxMajorVersion || (majorVersion == maxMajorVersion && minorVersion > maxMinorVersion)) {
                    maxMajorVersion = majorVersion;
                    maxMinorVersion = minorVersion;
                    nameLen = strlen(entry->d_name) + 1;
                    if (nameLen > libNameLen) {
                        if (libName != NULL) free(libName);
                        libName = malloc(sizeof(char) * nameLen);
                        libNameLen = nameLen;
                    }
                    strncpy(libName, entry->d_name, libNameLen);
                }
            }
        }
        closedir(pyLibDir);
    } else {
        LOG_ERROR("Failed to open the Python library directory (%s): %s",
                  pythonLibDir, strerror(errno));
        return NULL;
    }
    if (libName == NULL) {
        LOG_ERROR("Failed to find any installed python version!");
    }
    return libName;
}

void addPathToEnvVariable(const char* variableName, const char* path) {
    const char* value = (const char*) getenv(variableName);
    if (value == NULL) value = "";
    if (strstr(value, path) == NULL) { // Check if our path is already in LD_LIBRARY_PATH
        size_t valueLen = strlen(path) + strlen(value) + 2;
        const char* newValue = malloc(sizeof(char) * (valueLen));
        if (newValue == NULL) {
            LOG_ERROR("Not enough memory to change '%s'!", variableName); return;
        }
        snprintf((char*) newValue, valueLen, "%s:%s", path, value);
        setenv(variableName, newValue, 1);
        free((char*) newValue);
    }
}

void* openInterpreterHandle(const char* pyLibName, const char* hostLibPath) {
    void* handle = dlopen("libpyInterpreter.so", RTLD_LAZY);
    if (handle == NULL) {
        if (strncmp(hostLibPath, getenv("LD_LIBRARY_PATH"), strlen(hostLibPath)) != 0) {
            // Add the hostLibPath to LD_LIBRARY_PATH
            addPathToEnvVariable("LD_LIBRARY_PATH", hostLibPath);
            handle = dlopen("libpyInterpreter.so", RTLD_LAZY);
        }
        if (handle == NULL) {
            // Try to load the absolute path
            char buff[512], subLibBuff[512];
            subLibBuff[0] = '\0';
            snprintf(buff, 512, "%s/libpyInterpreter.so", hostLibPath);
            handle = dlopen(buff, RTLD_LAZY);
        }
    }
    if (handle == NULL) {
        LOG_ERROR("Failed to load library %s: %s", "interpreter.so", dlerror());
        LOG_ERROR("Try to type 'export LD_LIBRARY_PATH=%s' and retry", getenv("LD_LIBRARY_PATH"));
    } else {
        int (*setPythonLibrary)(const char*) = dlsym(handle, "setPythonLibrary");
        if (setPythonLibrary == NULL) {
            LOG_ERROR("Failed to find function %s in interpreter library!", "setPythonLibrary");
            return NULL;
        }
        if (!setPythonLibrary(pyLibName)) {
            LOG_ERROR("Failed to load the Python library %s!", pyLibName);
            return NULL;
        }
    }
    return handle;
}

void closePythonLibrary(void* handle) {
    void (*closePyLibrary)(void) = dlsym(handle, "closePythonLibrary");
    if (closePyLibrary != NULL) {
        closePyLibrary();
    }
    dlclose(handle);
}

int main(int argc, char** argv) {
    static const char* DEFAULT_PROGRAM_PATH = "./python";
    #define ASSERT(expr, message, args...) if (!(expr)) {LOG_ERROR(message, ##args); return 1; }
    const char* defaultPath  = "/data/data/com.apython.python.pythonhost/"; // This is an fallback if argv[0] is not set right; // TODO: consider better solution
    const char* libAppendix  = "lib";
    const char* pyLibAppendix  = "dynLibs";
    const char* homeAppendix = "files";
    const char* dataAppendix = "data";
    const char* tempAppendix = "cache";
    const char* xdgAppendix  = "pythonApps";
    const char* basePath     = NULL;
    const char *programPath  = NULL;
    void* handle;
    const char* indexPointer = NULL; 
    if (argc > 0 && access(argv[0], F_OK) != -1) {
        programPath = argv[0];
        indexPointer = strrchr(programPath, '/');
    } else {
        programPath = DEFAULT_PROGRAM_PATH;
        indexPointer = &DEFAULT_PROGRAM_PATH[1];
    }
    if (indexPointer != NULL) {
        size_t baseDirLen = (((indexPointer - programPath) / sizeof(char)) + 1);
        if (programPath[0] == '.') {
            size_t cwdLen = 0;
            char cwdBuff[PATH_MAX];
            ASSERT(getcwd(cwdBuff, sizeof(cwdBuff) / sizeof(cwdBuff[0])) != NULL, "getcwd failed!");
            cwdLen = strlen(cwdBuff);
            const char* tmpPath = malloc(sizeof(char) * (cwdLen + strlen(programPath)));
            strncpy((char*) tmpPath, cwdBuff, cwdLen + 1);
            strcpy((char*) &tmpPath[cwdLen], ++programPath);
            programPath = tmpPath;
            baseDirLen = cwdLen + baseDirLen - 1;
        }
        basePath = malloc(sizeof(char) * (baseDirLen + 1));
        strncpy((char*) basePath, programPath, baseDirLen);
        *(((char*) basePath) + baseDirLen) = '\0';
    } else {
        basePath = malloc(sizeof(char) * (strlen(defaultPath) + 1));
        strcpy((char*) basePath, defaultPath);
    }
    // Setup the path to the other Python host libs
    const char* hostLibPath = malloc(sizeof(char) * (strlen(basePath) + strlen(libAppendix) + 1));
    ASSERT(hostLibPath != NULL, "Not enough memory to construct the path to the Python host libraries!");
    strcpy((char*) hostLibPath, basePath);
    strcat((char*) hostLibPath, libAppendix);
    // home
    const char *pythonHome = malloc(sizeof(char) * (strlen(basePath) + strlen(homeAppendix) + 1));
    ASSERT(pythonHome != NULL, "Not enough memory to construct the path to the Python home directory!");
    strcpy((char*) pythonHome, basePath);
    strcat((char*) pythonHome, homeAppendix);
    // Construct the path to the Python libs
    const char* pythonLibs = malloc(sizeof(char) * (strlen(pythonHome) + strlen(pyLibAppendix) + 2));
    ASSERT(pythonLibs != NULL, "Not enough memory to construct the path to the Python libraries!");
    snprintf((char*) pythonLibs, strlen(pythonHome) + strlen(pyLibAppendix) + 2, "%s/%s", pythonHome, pyLibAppendix);
    // Parse cmd args
    char* pyVersionArg = parseLauncherArgs(programPath, &argc, argv);
    // load pythonLib
    char* pythonLibName = getPythonLibName(pythonLibs, pyVersionArg);
    if (pythonLibName == NULL) return 1;
    if ((handle = openInterpreterHandle(pythonLibName, hostLibPath)) == NULL) return 1;
    free(pythonLibName);
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
    // dataDir
    const char *dataDir = malloc(sizeof(char) * (strlen(pythonHome) + strlen(dataAppendix) + 2));
    ASSERT(dataDir != NULL, "Not enough memory to construct the data dir path!");
    sprintf((char *) dataDir, "%s/%s", pythonHome, dataAppendix);
    void (*setupPython)(const char*, const char*, const char*, const char*, const char*, const char*, const char*);
    // Setup and start the python interpreter
    setupPython = dlsym(handle, "setupPython");
    ASSERT(setupPython != NULL, "Could not find the method 'setupPython' in the interpreter library!");
    setupPython(programPath, pythonLibs, "", pythonHome, pythonTemp, xdgBasePath, dataDir);
    free((char*) basePath);
    free((char*) pythonLibs);
    free((char*) pythonHome);
    free((char*) pythonTemp);
    free((char*) xdgBasePath);
    free((char*) dataDir);
    int (*pyMain)(int, char**) = dlsym(handle, "call_Py_Main");
    ASSERT(pyMain != NULL, "Could not find the method 'call_Py_Main' in the interpreter library!");
    int result = pyMain(argc, argv);
    // Cleanup
    closePythonLibrary(handle);
    return result;
}
