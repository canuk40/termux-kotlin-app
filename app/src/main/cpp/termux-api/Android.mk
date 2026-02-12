LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := termux-api
LOCAL_SRC_FILES := termux-api.c termux-api-broadcast.c
LOCAL_LDLIBS := -llog
include $(BUILD_EXECUTABLE)
