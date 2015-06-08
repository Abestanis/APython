LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := pythonLog
LOCAL_SRC_FILES := log.c
LOCAL_SHARED_LIBRARIES := python2.7.2
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)