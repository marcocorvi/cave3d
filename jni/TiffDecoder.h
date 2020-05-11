/* @file TiffDecoder.h
 *
 * @brief TiffDecoder C-side
 *
 * This class is written following the example in beyka.Android-TiffbitmapFactory
 * ------------------------------------------------------------------
 * This file is a modification of the original powercrust file
 * marco corvi - may 2017
 *
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 *
 * ------------------------------------------------------------------
 */
#ifndef TIFF_DECODER_H
#define TIFF_DECODER_H

#include <jni.h>
// #include <android/log.h>

/*
#define NDLOGI(x) __android_log_print(ANDROID_LOG_DEBUG,     "TiffDecoder", "%s", x)
#define NDLOGII(x, y) __android_log_print(ANDROID_LOG_DEBUG, "TiffDecoder", "%s %d", x, y)
#define NDLOGIL(x, y) __android_log_print(ANDROID_LOG_DEBUG, "TiffDecoder", "%s %ld", x, y)
#define NDLOGIF(x, y) __android_log_print(ANDROID_LOG_DEBUG, "TiffDecoder", "%s %f", x, y)
#define NDLOGIS(x, y) __android_log_print(ANDROID_LOG_DEBUG, "TiffDecoder", "%s %s", x, y)
#define NDLOGE(x) __android_log_print(ANDROID_LOG_ERROR,     "TiffDecoder", "%s", x)
#define NDLOGES(x, y) __android_log_print(ANDROID_LOG_ERROR, "TiffDecoder", "%s %s", x, y)
*/

class TiffDecoder
{
  public:
    explicit TiffDecoder(JNIEnv *, jclass, jstring );
    ~TiffDecoder();
    jobject getBitmap( jdouble, jdouble, jdouble, jdouble );

  private:
    JNIEnv *env;
    jclass clazz;
    jstring jPath;
    char * mFilename;

  private:
    void setFilename();

};

#endif


