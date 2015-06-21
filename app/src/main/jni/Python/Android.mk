LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := python2.7.2
FILE_LIST := $(wildcard $(LOCAL_PATH)/Python2.7.2/*/*.c)
LOCAL_SRC_FILES := $(FILE_LIST:$(LOCAL_PATH)/%=%)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/Python2.7.2/Include
LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/Python2.7.2/Include
LOCAL_SHARED_LIBRARIES := pythonPatch
LOCAL_LDLIBS := -lz

include $(BUILD_SHARED_LIBRARY)
