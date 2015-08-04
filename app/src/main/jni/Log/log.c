#include "log.h"
#include <pthread.h>
#include <stdio.h>
#include <errno.h>

// The Log system //

const char* appTag = "Python";
static pthread_t outputCaptureThread;
static int outputPipe[2];
pthread_mutex_t mutex;
pthread_cond_t condition;
int outputCaptureThreadStarted = 0;
void (*stdout_write)(const char*) = NULL;
void (*stderr_write)(const char*) = NULL;

#define LOG_ASSERT(x) __android_log_assert(ANDROID_LOG_FATAL, appTag, (x))

int _log_write_(int priority, const char *text) {
    __android_log_write(priority, appTag, text);
}

void _assert_(const char* expression, const char* file, int line, const char* errorString) {
    __android_log_assert(expression, appTag, "Assertion failed (%s) at %s, line %i: %s", expression, file, line, errorString);
}

void setApplicationTag(const char* newAppTag) {
    appTag = newAppTag;
}

void setStdoutRedirect(void (*f)(const char*)) {
    stdout_write = f;
}

void setStderrRedirect(void (*f)(const char*)) {
    stderr_write = f;
}

void captureOutput(int streamFD) {
    ssize_t outputSize;
    char buffer[4096];
    while((outputSize = read(streamFD, buffer, sizeof(buffer) - 1)) != 0) {
        if (outputSize == -1) {
            if (errno != EINTR) {
                LOG_WARN("Failed to read from output pipe:");
                LOG_WARN(strerror(errno));
                LOG_WARN("Stopp reading from output (output no longer valid).");
                break;
            }
            continue;
        }
        buffer[outputSize] = 0; // add null-terminator
        if (stdout_write != NULL) {
            stdout_write(buffer);
        } else {
            // Remove trailing newline
            if (buffer[outputSize - 1] == '\n') { buffer[outputSize - 1] = 0; }
            LOG(buffer);
        }
    }
}

int setupOutputRedirection(int pipe[2]) {
    // Make stdout line-buffered and stderr unbuffered
    setvbuf(stdout, 0, _IOLBF, 0);
    setvbuf(stderr, 0, _IONBF, 0);

    while ((dup2(pipe[1], fileno(stdout)) == -1) && (errno == EINTR)) {}
    while ((dup2(pipe[1], fileno(stderr)) == -1) && (errno == EINTR)) {}

    close(pipe[1]);
    return 1;
}
