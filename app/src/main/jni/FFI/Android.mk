LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := ffi
BUILD_TARGET := arm
FILE_LIST := $(wildcard $(LOCAL_PATH)/source/src/*.c) $(wildcard $(LOCAL_PATH)/source/src/$(BUILD_TARGET)/*.c) $(wildcard $(LOCAL_PATH)/source/src/$(BUILD_TARGET)/*.S)
EXCLUDED_FILES := source/src/dlmalloc.c

LOCAL_SRC_FILES := $(filter-out $(EXCLUDED_FILES), $(FILE_LIST:$(LOCAL_PATH)/%=%))

LOCAL_C_INCLUDES := $(LOCAL_PATH)/source/include $(LOCAL_PATH)/source/src/$(BUILD_TARGET)

LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/source/include $(LOCAL_PATH)/source/src/$(BUILD_TARGET)
include $(BUILD_SHARED_LIBRARY)