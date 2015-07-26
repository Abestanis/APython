LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := pyInterpreter
LOCAL_SRC_FILES := interpreter.c py_utils.c
LOCAL_SHARED_LIBRARIES := pyLog python2.7.2 pythonPatch
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../

include $(BUILD_SHARED_LIBRARY)
