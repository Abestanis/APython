LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := bzip
FILE_LIST := $(wildcard $(LOCAL_PATH)/source/*.c)
EXCLUDED_FILES := bzip2recover.c bzip2.c dlltest.c mk251.c spewG.c unzcrash.c
EXCLUDED_FILES := $(addprefix source/, $(EXCLUDED_FILES))

LOCAL_SRC_FILES := $(filter-out $(EXCLUDED_FILES), $(FILE_LIST:$(LOCAL_PATH)/%=%))

LOCAL_C_INCLUDES := $(LOCAL_PATH)/source

LOCAL_EXPORT_C_INCLUDES += $(LOCAL_PATH)/source
include $(BUILD_SHARED_LIBRARY)