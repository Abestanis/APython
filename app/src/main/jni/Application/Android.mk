LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := application
LOCAL_SRC_FILES := application.c
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
