LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := pythonPatch
LOCAL_SRC_FILES := redirects.c
LOCAL_EXPORT_CFLAGS := -include redirects.h \
                       -D isatty=redirectedIsATty \
                       -D ioctl=redirectedIOCtl \
                       -D 'PLATFORM="android"' \
                       -D 'VERSION="2.7"' \
                       -D fdatasync=fsync #TODO: Maybe do this better? http://linux.die.net/man/2/fdatasync
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)

include $(BUILD_SHARED_LIBRARY)
