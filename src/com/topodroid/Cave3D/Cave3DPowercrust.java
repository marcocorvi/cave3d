/* @file Cave3DPowercrust.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D Powercrust triangulation
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

//  import android.util.FloatMath;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

import android.util.Log;

class Cave3DPowercrust
{
  int np;
  int nf; 

  public native int nrPoles();
  public native int nextPole();
  public native double poleX();
  public native double poleY();
  public native double poleZ();

  public native int nrFaces();
  public native int nextFace();
  public native int faceSize();
  public native int faceVertex( int k );

  public native void initLog();

  public native void resetSites( int dd );
  public native void addSite( double x, double y, double z );
  public native long nrSites();

  public native int compute();
  public native void release();

  static {
    System.loadLibrary( "powercrust" );
  }

  Cave3DPowercrust()
  {
    // Log.v("Cave3D", "powercrust cstr");
    initLog();
    resetSites( 3 );
    np = 0;
    nf = 0;
  }

  Cave3DSite[] insertTrianglesIn( ArrayList< Cave3DTriangle > triangles )
  {
    float x, y, z;
    np = nrPoles();
    // Log.v("Cave3D", "Nr. poles " + np + " Creating vertices ...");
    Cave3DSite poles[] = new Cave3DSite[ np ];
    for ( int k=0; k<np; ++k ) {
      x = (float)(poleX());
      y = (float)(poleY());
      z = (float)(poleZ());
      poles[k] = new Cave3DSite( x, y, z );
      if ( nextPole() == 0 ) break;
    }

    nf = nrFaces();
    int small = 0;
    int large = 0;
    int good  = 0;
    int fail  = 0;
    do {
      int nn = faceSize();
      if ( nn > 2 && nn < 32 ) { // FIXME hard upper bound to the size of a face
        boolean ok = true;
        Cave3DTriangle tri = new Cave3DTriangle( nn );
        for ( int k = 0; k < nn; ++k ) {
          int idx = faceVertex( k );
          if ( idx < 0 || idx >= np ) { ok = false; break; }
          tri.setVertex( k, poles[ idx ] );
        }
        if ( ok ) {
          tri.computeNormal();
          triangles.add( tri );
          ++ good;
        } else {
          ++ fail;
        }
      } else if ( nn <= 2 ) {
        ++ small;
      } else {
        ++ large;
      }
    } while ( nextFace() != 0 );
    // Log.v("Cave3D", "Nr. faces " + nf + " Created faces ... G " + good + " F " + fail + " S " + small + " L " + large );
    // release();
    return poles;
  }


}
