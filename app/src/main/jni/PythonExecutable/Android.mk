LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := python
LOCAL_SRC_FILES := main.c
LOCAL_SHARED_LIBRARIES := pythonPatch pyLog pyInterpreter

include $(BUILD_EXECUTABLE)