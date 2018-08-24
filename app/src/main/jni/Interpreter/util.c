#include <unistd.h>
#include <log.h>
#include "util.h"

JNIEXPORT jboolean NATIVE_FUNCTION(Util_nativeCreateSymlink)
        (JNIEnv* env, jclass __unused cls, jstring jTarget, jstring jLink) {
    const char *target = (*env)->GetStringUTFChars(env, jTarget, 0);
    const char *link   = (*env)->GetStringUTFChars(env, jLink, 0);
    int result = symlink(target, link);
    (*env)->ReleaseStringUTFChars(env, jTarget, target);
    (*env)->ReleaseStringUTFChars(env, jLink, link);
    return (jboolean) (result == 0);
}

__sighandler_t setSignalHandler(int signal, __sighandler_t signalHandler) {
    struct sigaction context, oldContext;
    context.sa_handler = signalHandler;
    sigemptyset(&context.sa_mask);
    context.sa_flags = 0;
    if (sigaction(signal, &context, &oldContext) == -1) {
        return SIG_ERR;
    }
    return oldContext.sa_handler;
}

jobject createFileDescriptor(JNIEnv* env, int fd) {
    jclass *fileDescriptorClass = NULL;
    jmethodID mid = NULL;
    jfieldID fdFieldId = NULL;
    // TODO: Initialize these once.
    // TODO: https://developer.android.com/training/articles/perf-jni.html#jclass_jmethodID_and_jfieldID
    fileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");
    ASSERT(fileDescriptorClass, "Could not find class 'java/io/FileDescriptor'!");
    mid = (*env)->GetMethodID(env, fileDescriptorClass, "<init>", "()V");
    ASSERT(mid, "Could not find the constructor of the FileDescriptor class!");
    fdFieldId = (*env)->GetFieldID(env, fileDescriptorClass, "descriptor", "I");
    ASSERT(fdFieldId, "Could not find the 'descriptor' field of the FileDescriptor class!");
    jobject fileDescriptor = (*env)->NewObject(env, fileDescriptorClass, mid);
    if (fileDescriptor != NULL) {
        (*env)->SetIntField(env, fileDescriptor, fdFieldId, fd);
    }
    return fileDescriptor;
}
