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

UNUSED_POWERCRUST = \
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

# --------------------------------------------------------------------

include $(CLEAR_VARS)

# LOCAL_ARM_MODE := arm

LOCAL_TIFF_SRC_FILES := \
	tiff/libtiff/tif_dirread.c \
	tiff/libtiff/tif_zip.c \
	tiff/libtiff/tif_flush.c \
	tiff/libtiff/tif_next.c \
	tiff/libtiff/tif_ojpeg.c \
	tiff/libtiff/tif_dirwrite.c \
	tiff/libtiff/tif_dirinfo.c \
	tiff/libtiff/tif_dir.c \
	tiff/libtiff/tif_compress.c \
	tiff/libtiff/tif_close.c \
	tiff/libtiff/tif_tile.c \
	tiff/libtiff/tif_open.c \
	tiff/libtiff/tif_getimage.c \
	tiff/libtiff/tif_pixarlog.c \
	tiff/libtiff/tif_warning.c \
	tiff/libtiff/tif_dumpmode.c \
	tiff/libtiff/tif_jpeg.c \
	tiff/libtiff/tif_jbig.c \
	tiff/libtiff/tif_predict.c \
	tiff/libtiff/mkg3states.c \
	tiff/libtiff/tif_write.c \
	tiff/libtiff/tif_error.c \
	tiff/libtiff/tif_version.c \
	tiff/libtiff/tif_print.c \
	tiff/libtiff/tif_color.c \
	tiff/libtiff/tif_read.c \
	tiff/libtiff/tif_extension.c \
	tiff/libtiff/tif_thunder.c \
	tiff/libtiff/tif_lzw.c \
	tiff/libtiff/tif_fax3.c \
	tiff/libtiff/tif_luv.c \
	tiff/libtiff/tif_codec.c \
	tiff/libtiff/tif_unix.c \
	tiff/libtiff/tif_packbits.c \
	tiff/libtiff/tif_aux.c \
	tiff/libtiff/tif_fax3sm.c \
	tiff/libtiff/tif_swab.c \
	tiff/libtiff/tif_strip.c

UNUSED_TIFF = \
	Tiff.c \
	TiffDecoder.cpp \
	NativeTiffFactory.cpp \
	TiffWrapper.c

LOCAL_TIFF_SRC_FILES += tiff/port/lfind.c 

#######################LIBTIFF#################################
LOCAL_MODULE := libtiff

LOCAL_SRC_FILES:= $(LOCAL_TIFF_SRC_FILES)
LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/tiff/libtiff \
	$(LOCAL_PATH)/jpeg

LOCAL_CFLAGS += -DAVOID_TABLES
LOCAL_CFLAGS += -O3 -fstrict-aliasing -fprefetch-loop-arrays

LOCAL_LDLIBS := -lz

# LOCAL_LDLIBS += $(LOCAL_PATH)/libs/$(TARGET_ARCH_ABI)/libjpeg.a
LOCAL_STATIC_LIBRARIES := jpeg png

include $(BUILD_SHARED_LIBRARY)

###############################################################

###############################################################
include $(CLEAR_VARS)
LOCAL_MODULE := libpng

LOCAL_SRC_FILES := libs/$(TARGET_ARCH_ABI)/libpng.a
include $(PREBUILT_STATIC_LIBRARY)

###############################################################
include $(CLEAR_VARS)
LOCAL_MODULE := libjpeg

LOCAL_SRC_FILES := libs/$(TARGET_ARCH_ABI)/libjpeg.a
include $(PREBUILT_STATIC_LIBRARY)

###############################################################
# include $(CLEAR_VARS)
# LOCAL_MODULE := libtiffreader
# 
# LOCAL_CFLAGS := -DANDROID_NDK
# LOCAL_SRC_FILES := \
# 	Tiff.c \
#         TiffWrapper.c 
# 
# # LOCAL_C_INCLUDES := libs/$(TARGET_ARCH_ABI)/libpng.a
# 
# LOCAL_C_INCLUDES := \
# 	$(LOCAL_PATH)/jpeg \
#  	$(LOCAL_PATH)/png 
# 
# LOCAL_LDLIBS := -lz -ldl -llog -ljnigraphics
# LOCAL_LDFLAGS +=-ljnigraphics
# LOCAL_STATIC_LIBRARIES := jpeg png tiff
# include $(BUILD_SHARED_LIBRARY)
# 
# # include $(BUILD_STATIC_LIBRARY)

#################################################################
include $(CLEAR_VARS)
LOCAL_MODULE := libtiffdecoder

LOCAL_SRC_FILES:= \
        Tiff.c \
        TiffDecoder.cpp \
        NativeTiffFactory.cpp

LOCAL_CFLAGS := -DANDROID_NDK

LOCAL_LDLIBS := -ldl -llog -ljnigraphics

LOCAL_LDFLAGS += -ljnigraphics

LOCAL_SHARED_LIBRARIES := tiff

# LOCAL_STATIC_LIBRARIES := jpeg png
include $(BUILD_SHARED_LIBRARY)

#################################################################
# include $(CLEAR_VARS)
# LOCAL_MODULE := libtiffwrapper
# 
# LOCAL_SRC_FILES:= \
#         Tiff.c \
#         TiffWrapper.cpp 
# 
# LOCAL_CFLAGS := -DANDROID_NDK
# 
# LOCAL_LDLIBS := -ldl -llog -ljnigraphics
# 
# LOCAL_LDFLAGS += -ljnigraphics
# 
# LOCAL_SHARED_LIBRARIES := tiff
# 
# # LOCAL_STATIC_LIBRARIES := jpeg png
# include $(BUILD_SHARED_LIBRARY)
