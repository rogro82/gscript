LOCAL_PATH := $(call my-dir)
MODULE_PATH := $(LOCAL_PATH)

include $(CLEAR_VARS)

LOCAL_MODULE    := gscript

LOCAL_CFLAGS	:= \
	-O2 \
	-Wall \
	-DANDROID \

LOCAL_C_INCLUDES := \
	$(MODULE_PATH)/include \

LOCAL_SRC_FILES := \
	/src/JNIReference.cpp \
	/src/PseudoTerminal.cpp \
	/src/SubProcess.cpp \
	/src/ScreenBuffer.cpp \
	
LOCAL_STATIC_LIBRARIES := stlport_static

LOCAL_LDLIBS := \
	-llog \

include $(BUILD_SHARED_LIBRARY)