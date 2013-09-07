LOCAL_PATH := $(call my-dir)
ROOT_PATH := $(LOCAL_PATH)

include $(CLEAR_VARS)

include $(ROOT_PATH)/gscript/Android.mk
include $(ROOT_PATH)/gscript-exec/Android.mk
include $(ROOT_PATH)/gscript-input/Android.mk