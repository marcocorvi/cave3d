/* @file NativeTiffFactory.cpp
 *
 * @brief TiffFactory native C-side
 *
 * This class is written following the example in beyka.Android-TiffbitmapFactory
 * ------------------------------------------------------------------
 * This file is a modification of the original powercrust file
 * marco corvi - may 2017
 *
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * ------------------------------------------------------------------
 */
#ifdef __cplusplus
extern "C" {
#endif

#include <android/log.h>
#include <android/bitmap.h>

// #include <stdlib.h>
// #include <stdio.h>
// #include <tiffio.h>

#include "NativeTiffFactory.h"
#include "TiffDecoder.h"

JNIEXPORT jobject JNICALL
Java_com_topodroid_Cave3D_TiffFactory_getBitmap
  ( JNIEnv * env, jclass clazz, jstring path, jdouble x1, jdouble y1, jdouble x2, jdouble y2 )
{
  TiffDecoder * decoder = new TiffDecoder( env, clazz, path );
  jobject bitmap = decoder->getBitmap( x1, y1, x2, y2 );
  delete( decoder );
  return bitmap;
}

#ifdef __cplusplus
}
#endif
