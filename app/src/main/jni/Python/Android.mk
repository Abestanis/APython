LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := python2.7.2
LOCAL_SRC_FILES := lib/libpython2.7.2.so
LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/include/python2.7.2
LOCAL_EXPORT_LDFLAGS := -L$(LOCAL_PATH)/lib

include $(PREBUILT_SHARED_LIBRARY)