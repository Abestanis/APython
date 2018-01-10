#include <unistd.h>
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
