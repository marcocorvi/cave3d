/** @file DEMparser.java
 *
 * @author marco corvi
 * @date apr 2020
 *
 * @brief Cave3D DEM parser
 *
 * Usage:
 *    DEMparser DEM = new DEMparser( filename );
 *    if ( DEM.valid() ) DEM.readData( west, east, south, north );
 *    if ( DEM.valid() ) { // use data
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.util.Log;

class DEMparser extends Cave3DSurface
{
  protected boolean mValid;    // header is valid
  // double mDim1; protected float   xcell;     // cell size
  // double mDim2; protected float   ycell;     // cell size
  // double[] mZ; protected float[] mData;     // DEM data
  // int mNr1; protected int     mX;    // grid dimension
  // int mNr2; protected int     mY;    // grid dimension

  // double mEast1 mEast2 mNorth1, mNorth2
  // protected float   mWest, mEast, mSouth, mNorth; // bounds
  double nodata;    // nodata value
  String mFilename; // DEM filename

  DEMparser( String filename )
  {
    mFilename = filename;
    mValid = readHeader( mFilename );
  }

  boolean valid() { return mValid; }
  double west()  { return mEast1; }
  double east()  { return mEast2; }
  double south() { return mNorth1; }
  double north() { return mNorth2; }

  double Z( int i, int j ) { return mZ[j*mNr1+i]; }
  double cellXSize() { return mDim1; }
  double cellYSize() { return mDim2; }
  int dimX() { return mNr1; }
  int dimY() { return mNr2; }

  boolean readData( float xwest, float xeast, float ysouth, float ynorth ) { return false; }

  protected  boolean readHeader( String filename ) { mValid = false; return false; }

  protected void makeNormal() 
  {
    if ( mValid ) {
      mNormal = new float[ 3*mNr1*mNr2 ];
      initNormal();
    }
  }

}

