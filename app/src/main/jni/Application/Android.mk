LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := application
LOCAL_SRC_FILES := main.c
LOCAL_CFLAGS += -I$(LOCAL_PATH)/../
LOCAL_SHARED_LIBRARIES := pythonLog python2.7.2

include $(BUILD_SHARED_LIBRARY)
