LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := pyInterpreter
LOCAL_SRC_FILES := interpreter.c py_utils.c py_compatibility.c terminal.c util.c
LOCAL_SHARED_LIBRARIES := pyLog

include $(BUILD_SHARED_LIBRARY)
