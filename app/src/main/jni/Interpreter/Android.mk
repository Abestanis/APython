LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := pyInterpreter
LOCAL_SRC_FILES := interpreter.c
LOCAL_SHARED_LIBRARIES := pyLog python2.7.2 pythonPatch

include $(BUILD_SHARED_LIBRARY)
