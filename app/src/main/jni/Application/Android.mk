LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := application
LOCAL_SRC_FILES := main.c
LOCAL_CFLAGS += -I$(LOCAL_PATH)/../
LOCAL_SHARED_LIBRARIES := pyLog pyInterpreter

include $(BUILD_SHARED_LIBRARY)
