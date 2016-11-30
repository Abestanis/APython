LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := pyInterpreter
LOCAL_SRC_FILES := interpreter.c py_utils.c py_compatibility.c terminal.c
LOCAL_SHARED_LIBRARIES := pyLog pythonPatch
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../

include $(BUILD_SHARED_LIBRARY)
