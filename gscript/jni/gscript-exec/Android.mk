LOCAL_PATH := $(call my-dir)
MODULE_PATH := $(LOCAL_PATH)

include $(CLEAR_VARS)

LOCAL_MODULE    := gscript-exec

LOCAL_CFLAGS	:= \
	-O2 \
	-Wall \
	-DANDROID \

LOCAL_C_INCLUDES := \
	$(MODULE_PATH)/include \

LOCAL_SRC_FILES := \
	/src/gscript-exec.c \
	
LOCAL_STATIC_LIBRARIES 	:= libc
LOCAL_LDLIBS 			:= -llog

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)

include $(BUILD_EXECUTABLE)