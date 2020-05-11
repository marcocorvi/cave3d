/* @file NativeTiffFactory.h
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
#ifndef NATIVE_TIFF_FACTORY_H
#define NATIVE_TIFF_FACTORY_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jobject JNICALL
Java_com_topodroid_Cave3D_TiffFactory_getBitmap
  ( JNIEnv *, jclass, jstring, jdouble, jdouble, jdouble, jdouble );


#ifdef __cplusplus
}
#endif

#endif
