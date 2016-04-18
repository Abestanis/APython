LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := python
LOCAL_SRC_FILES := main.c
# To work around "error: only position independent executables (PIE) are supported."
# http://stackoverflow.com/questions/30498776/position-independent-executables-and-android-lollipop#30547603
LOCAL_CFLAGS += -fPIE
LOCAL_LDFLAGS += -fPIE -pie
LOCAL_SHARED_LIBRARIES := pythonPatch pyLog pyInterpreter

include $(BUILD_EXECUTABLE)
