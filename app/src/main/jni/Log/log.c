#include "log.h"
#include <pthread.h>
#include <stdio.h>

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

static void *outputCatcher(void* arg) {
    ssize_t outputSize;
    char buffer[256];
    pthread_mutex_lock(&mutex);
    outputCaptureThreadStarted = 1;
    pthread_cond_signal(&condition);
    pthread_mutex_unlock(&mutex);
    //LOG("Started!");
    while((outputSize = read(outputPipe[0], buffer, sizeof(buffer) - 1)) > 0) {
        buffer[outputSize] = 0; // add null-terminator
        if (stdout_write != NULL) {
            stdout_write(buffer);
        } else {
            // Remove trailing newline
            if (buffer[outputSize - 1] == '\n') { buffer[outputSize - 1] = 0; }
            LOG(buffer);
        }
    }
    return 0;
}

int setupOutputRedirection() {
    // Make stdout line-buffered and stderr unbuffered
    setvbuf(stdout, 0, _IOLBF, 0);
    setvbuf(stderr, 0, _IONBF, 0);

    // Create the pipe and redirect stdout and stderr
    pipe(outputPipe);
    dup2(outputPipe[1], fileno(stdout));
    dup2(outputPipe[1], fileno(stderr));

    // Create the output capturing thread
    if (pthread_create(&outputCaptureThread, NULL, outputCatcher, 0) != 0) {
        LOG_ERROR("Could not create the output capture thread!");
        return -1;
    }
    pthread_detach(outputCaptureThread);
    // Wait for the thread to be started
    // TODO: This still does not wait long enough!
    pthread_mutex_lock(&mutex);
    while (!outputCaptureThreadStarted) { pthread_cond_wait(&condition, &mutex); }
    pthread_mutex_unlock(&mutex);
    return 0;
}
