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
 * ------------------------------------------------------------------
 */

#include <android/bitmap.h>

#include <string.h>
#include <stdlib.h>
// #include <stdio.h>

#include "TiffDecoder.h"
#include "Tiff.h"

TiffDecoder::TiffDecoder( JNIEnv *e, jclass c, jstring path )
{
  // NDLOGI("TiffDecoder cstr");
  env = e;
  clazz = c;
  jPath = path;
  setFilename( );
}

TiffDecoder::~TiffDecoder()
{
  // NDLOGI("TiffDecoder dstr");
  if ( mFilename != NULL ) free( mFilename );
}

jobject
TiffDecoder::getBitmap( jdouble x1, jdouble y1, jdouble x2, jdouble y2 )
{
  // NDLOGIS( "File %s\n", ((mFilename==NULL)? "null" : mFilename) );
  if ( mFilename == NULL ) return NULL;
  unsigned char * buffer = getSubImage( mFilename, x1, y1, x2, y2 );
  if ( buffer == NULL ) {
    return NULL;
  }

  int width  = getImageWidth();
  int height = getImageHeight();
  int size = width * height;
  if ( width == 0 || height == 0 ) {
    return NULL;
  }

  // BitmapConfig.ARGB_8888
  jclass bitmapConfigClass   = (env)->FindClass( "android/graphics/Bitmap$Config");
  jfieldID bitmapConfigField = (env)->GetStaticFieldID( bitmapConfigClass, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
  jobject config = (env)->GetStaticObjectField( bitmapConfigClass, bitmapConfigField );
  (env)->DeleteLocalRef( bitmapConfigClass );

  // Bitmap.createBitmap( int, int, BitmapConfig )
  jclass bitmapClass = (env)->FindClass( "android/graphics/Bitmap");
  jmethodID methodid = (env)->GetStaticMethodID( bitmapClass, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
  jobject java_bitmap = NULL;
  java_bitmap = (env)->CallStaticObjectMethod( bitmapClass, methodid, width, height, config);
  (env)->DeleteLocalRef( config );
  (env)->DeleteLocalRef( bitmapClass );

  unsigned char * buffer4 = new unsigned char[ height * width * 4 ];
  for ( int j=0; j<height; ++j ) {
    unsigned char * buf3 = buffer  + j * width * 3;
    unsigned char * buf4 = buffer4 + j * width * 4;
    for ( int i=0; i<width; i++ ) {
      buf4[0] = buf3[2];
      buf4[1] = buf3[1];
      buf4[2] = buf3[0];
      buf4[3] = 255;
      buf4 += 4;
      buf3 += 3;
    }
  }

  void *bitmap_pixels;
  if ( AndroidBitmap_lockPixels( env, java_bitmap, &bitmap_pixels ) < 0 ) { //error
    // LOGE("Lock pixels failed");
    free( buffer );
    return NULL;
  }
  // memcpy( bitmap_pixels, (jint *) buffer, sizeof(jint) * size );
  memcpy( bitmap_pixels, buffer4, sizeof(jint) * size );
  
  AndroidBitmap_unlockPixels( env, java_bitmap );

  delete[] buffer4;
  free( buffer );

  return java_bitmap;

}

void
TiffDecoder::setFilename( )
{
  const char *strPath = NULL;
  strPath = env->GetStringUTFChars( jPath, 0 );
  // NDLOGIS("nativeTiffOpen", strPath);
  int len = strlen( strPath );
  mFilename = (char *)malloc( len + 1 );
  strcpy( mFilename, strPath );
  mFilename[len] = 0;
}

