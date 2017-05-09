/* @file powercrust.c
 *
 * @brief powercrust jni interface
 *
 * marco corvi - may 2017
 *
 * ------------------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
#include "powercrust_lib.h"

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

#include <android/log.h>


#define MYJENV JNIEnv * env, jobject thisobj
// #define MYJENV JNIEnv * env

extern FILE * fplog;
// FILE * fpcgal;
// #define LOGI( x...) if ( fplog ) { fprintf(fplog, x ); fflush( fplog ); }
// #define LOGI(x...) __android_log_print( ANDROID_LOG_INFO, "Cave3D PC", x)
#define LOGI(x...) /* nothing */

JNIEXPORT
void
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_initLog( MYJENV ) 
{ 
  // fplog = fopen("/sdcard/cave3dPC.txt", "w" );
  // fpcgal = fopen("/sdcard/cgal", "w" );
}

JNIEXPORT
void
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_resetSites( MYJENV, jint dd ) { reset_sites( dd ); }

JNIEXPORT
jlong
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_nrSites( MYJENV )
{ 
  LOGI("get nr sites");
  return getNrSites(); 
}

JNIEXPORT
jint
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_nrPoles( MYJENV ) 
{ 
  int ret = getNrPoles();
  LOGI("Get Nr Poles %d", ret );
  return ret;
}

JNIEXPORT
jint
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_nextPole( MYJENV ) { return getNextPole(); }

JNIEXPORT
jdouble
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_poleX( MYJENV ) { return getPoleX(); }

JNIEXPORT
jdouble
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_poleY( MYJENV ) { return getPoleY(); }

JNIEXPORT
jdouble
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_poleZ( MYJENV ) { return getPoleZ(); }

JNIEXPORT
jint
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_nrFaces( MYJENV )
{ 
  int ret = getNrFaces();
  LOGI("Get Nr Faces %d", ret );
  return ret;
}

JNIEXPORT
jint
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_nextFace( MYJENV ) { return getNextFace(); }

JNIEXPORT
jint
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_faceSize( MYJENV ) { return getFaceSize(); }

JNIEXPORT
jint
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_faceVertex( MYJENV, jint k ) { return getFaceVertex(k); }

JNIEXPORT
void
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_addSite( MYJENV, jdouble x, jdouble y, jdouble z )
{
  // fprintf(fpcgal, "%.6lf %.6lf %.6lf\n", x, y, z );
  set_next_site( x, y, z );
}

JNIEXPORT
jint
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_compute( MYJENV )
{
  int num_poles = 0;
  long seed    = rand(); // 40619;
  int vd       = 1;
  short bad    = 0;
  int defer    = 0;
  double deep  = 0.0;
  double theta = 0.0;
  double est_r = 1.0;
  // fclose( fpcgal ) ;
  LOGI( "powercrust compute\n" );
  return driver( seed, est_r, defer, deep, theta, vd, bad );
  // num_poles = exec_first_part( 3, seed, mup, vd, bad,  NULL );
  // LOGI( "%d", num_poles );
  // exec_second_part( 0, 4, num_poles, seed, defer, deep, theta, NULL );
  // exec_third_part( num_poles );
}

JNIEXPORT
void
JNICALL
Java_com_topodroid_Cave3D_Cave3DPowercrust_release( MYJENV ) { release_wstructs(); }


