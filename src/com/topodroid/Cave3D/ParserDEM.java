/** @file ParserDEM.java
 *
 * @author marco corvi
 * @date apr 2020
 *
 * @brief DEM parser
 *
 * Usage:
 *    ParserDEM DEM = new ParserDEM( filename );
 *    if ( DEM.valid() ) DEM.readData( west, east, south, north );
 *    if ( DEM.valid() ) { // use data
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

class ParserDEM extends DEMsurface
{
  protected boolean mValid;    // header is valid
  protected int   mMaxSize;    // max DEM size (in each direction)
  // float mDim1; protected float   xcell;     // cell size
  // float mDim2; protected float   ycell;     // cell size
  // float[] mZ; protected float[] mData;     // DEM data
  // int mNr1; protected int     mX;    // grid dimension
  // int mNr2; protected int     mY;    // grid dimension

  // float mEast1 mEast2 mNorth1, mNorth2
  // protected float   mWest, mEast, mSouth, mNorth; // bounds
  protected float nodata;    // nodata value
  String mFilename; // DEM filename

  ParserDEM( String filename, int size )
  {
    mFilename = filename;
    mMaxSize  = size;
    mValid = readHeader( mFilename );
  }

  boolean valid() { return mValid; }
  float west()  { return mEast1; }
  float east()  { return mEast2; }
  float south() { return mNorth1; }
  float north() { return mNorth2; }

  float Z( int i, int j ) { return mZ[j*mNr1+i]; }
  float cellXSize() { return mDim1; }
  float cellYSize() { return mDim2; }
  int dimX() { return mNr1; }
  int dimY() { return mNr2; }
  
  float[] data() { return mZ; }

  boolean readData( float xwest, float xeast, float ysouth, float ynorth ) { return false; }

  protected  boolean readHeader( String filename ) { mValid = false; return false; }

  // protected void makeNormal() 
  // {
  //   if ( mValid ) {
  //     mNormal = new float[ 3*mNr1*mNr2 ];
  //     initNormal();
  //   }
  // }

}

