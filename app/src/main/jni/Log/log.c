#include "log.h"
#include <string.h>
#include <assert.h>
#include <stdio.h>
#include <malloc.h>

// The Log system //

#define MAX_TAG_LENGTH 64
#define MAX_ERROR_LENGTH 2048

char appTag[MAX_TAG_LENGTH] = "Python";

int _log_write_(int priority, const char *text, ...) {
    va_list argP;
    va_start(argP, text);
    int res = __android_log_vprint(priority, appTag, text, argP);
    va_end(argP);
    return res;
}

void _assert_(const char* expression, const char* file, int line, const char* errorString, ...) {
    va_list argP;
    size_t bufferSize = strlen(errorString) + MAX_ERROR_LENGTH;
    char* message = malloc(sizeof(char) * bufferSize);
    if (message == NULL) {
        LOG_ERROR("Failed to allocate memory for assert message (format = '%s')", errorString);
        assert(message != NULL);
    }
    va_start(argP, errorString);
    vsnprintf(message, bufferSize, errorString, argP);
    va_end(argP);
    __android_log_assert(expression, appTag, "Assertion failed (%s) at %s, line %i: %s", expression, file, line, message);
}

void setApplicationTag(const char* newAppTag) {
    strncpy(appTag, newAppTag, MAX_TAG_LENGTH / sizeof(appTag[0])); 
}
