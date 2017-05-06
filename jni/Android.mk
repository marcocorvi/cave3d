LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := powercrust
LOCAL_SRC_FILES := \
  ch.c \
  crust.c \
  heap.c \
  hull.c \
  hullmain.c \
  io.c \
  label.c \
  math.c \
  pointops.c \
  power.c \
  predicates.c \
  rand.c \
  vv_arena.c \
  powercrust.c

UNUSED = \
  fg.c \
  main.c

LOCAL_LDLIBS := -llog

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)

include $(BUILD_SHARED_LIBRARY)

# include $(CLEAR_VARS)
# 
# LOCAL_MODULE := powercrust
# LOCAL_SRC_FILES := \
#   powercrust.c
# 
# LOCAL_LDLIBS := -llog
# 
# LOCAL_STATIC_LIBRARIES := powercrust_lib
# 
# include $(BUILD_SHARED_LIBRARY)

