PY_EXEC_LOCAL_PATH := $(call my-dir)
PY_EXEC_LD_FLAGS := -rpath '$$ORIGIN/lib'

# With position independent code support
LOCAL_PATH := $(PY_EXEC_LOCAL_PATH)
include $(CLEAR_VARS)

LOCAL_MODULE := python_pie
LOCAL_LDFLAGS := -pie $(PY_EXEC_LD_FLAGS)
LOCAL_SRC_FILES := main.c

include $(BUILD_EXECUTABLE)

# Without position independent code support
LOCAL_PATH := $(PY_EXEC_LOCAL_PATH)
include $(CLEAR_VARS)

LOCAL_MODULE := python
LOCAL_LDFLAGS := $(PY_EXEC_LD_FLAGS)
LOCAL_SRC_FILES := main.c

include $(BUILD_EXECUTABLE)
