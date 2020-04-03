/** @file DEMgridParser.java
 *
 * @author marco corvi
 * @date apr 2020
 *
 * @brief Cave3D ASCII grid DEM parser
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

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

class DEMgridParser extends DEMparser
{
  private double xll, yll;
  private int   cols, rows;
  private boolean flip_vert;

  DEMgridParser( String filename )
  {
    super( filename );
    flip_vert = false;
  }

  @Override
  boolean readData( float xwest, float xeast, float ysouth, float ynorth ) 
  {
    if ( ! mValid ) return mValid;
    FileReader fr = null;
    try {
      fr = new FileReader( mFilename );
      BufferedReader br = new BufferedReader( fr );
      String line = br.readLine();
      while ( line.startsWith("#") || line.startsWith("grid") ) line = br.readLine();

      double x = xll;
      int i = 0;
      for ( ; i < cols && x < xwest; ++i ) x += mDim1;
      mEast1 = x;    // X-coord of first data
      int xoff = i; // row-index of first data 
      mNr1 = 0;       // numver of X-data
      for ( ; i < cols && x <= xeast; ++i ) { x += mDim1; ++mNr1; }
      mEast2 = ( x > xeast )? x - mDim1 : x; // X-coord of last data
        
      int k = 0;
      double y; 
      if ( flip_vert ) { // yll is TOP-LEFT
        y = yll;
        for ( ; k < rows && y < ysouth; ++k ) {
          br.readLine();
          y += mDim2;
        }
        mNorth1 = y; // Y coord of first Y row
        mNr2 = 0;     // number of Y-rows
        int kk = k;
        for ( ; kk < rows && y <= ynorth; ++kk ) { y += mDim2; ++mNr2; }
        mNorth2 = ( y < ynorth )? y - mDim2 : y; // Y-coord of last Y-row
        mZ = new double[ mNr1 * mNr2 ];
        int j = mNr2 - 1;
        for ( ; k < rows && j >= 0; ++k, --j ) {
          line = br.readLine();
          String[] vals = line.replaceAll("\\s+", " ").split(" ");
          for ( int ii=0; ii<mNr1; ++ii ) mZ[j*mNr1 + ii] = Double.parseDouble( vals[xoff+ii] );
        }
      } else {
        y = yll + mDim2 * rows;
        for ( ; k < rows && y > ynorth; ++k ) {
          br.readLine();
          y -= mDim2;
        }
        mNorth2 = y;
        mNr2 = 0;
        int kk = k;
        for ( ; kk < rows && y >= ysouth; ++kk ) { y -= mDim2; ++mNr2; }
        mNorth1 = ( y < ysouth )? y + mDim2 : y;
        mZ = new double[ mNr1 * mNr2 ];
        int j = 0;
        for ( ; k < rows && j < mNr2; ++k, ++j ) {
          line = br.readLine();
          String[] vals = line.replaceAll("\\s+", " ").split(" ");
          for ( int ii=0; ii<mNr1; ++ii ) mZ[j*mNr1 + ii] = Double.parseDouble( vals[xoff+ii] );
        }
      }
    } catch ( IOException e1 ) {
      mValid = false;
    } catch ( NumberFormatException e2 ) {
      mValid = false;
    } finally {
      if ( fr != null ) try { fr.close(); } catch ( IOException e ) {}
    }
    makeNormal();
    return mValid;
  }

  @Override
  protected boolean readHeader( String filename )
  {
    try {
      FileReader fr = new FileReader( filename );
      BufferedReader br = new BufferedReader( fr );
      String line;
      while ( ( line = br.readLine() ) != null ) {
        if ( line.startsWith("#" ) ) continue;
        if ( line.startsWith("grid ") ) {
          String[] vals = line.replaceAll("\\s+", " ").split(" ");
          cols = Integer.parseInt( vals[5] ); // ncols
          rows = Integer.parseInt( vals[6] ); // nrows
          xll = Double.parseDouble( vals[1] ); // xcorner
          yll = Double.parseDouble( vals[2] ); // ycorner
          mDim1 = Double.parseDouble( vals[3] ); // cellsize
          mDim2 = Double.parseDouble( vals[4] ); // cellsize
          continue;
        }
        if ( line.startsWith("grid-flip ") ) {
          String[] vals = line.replaceAll("\\s+", " ").split(" ");
          if ( vals[1].startsWith("vert") ) flip_vert = true;
          continue;
        }
        break;
      }
      fr.close();
    } catch ( IOException e1 ) { 
      return false;
    } catch ( NumberFormatException e2 ) {
      return false;
    }
    Log.v("Cave3D-DEM", "cell " + mDim1 + " " + mDim2 + " X " + xll + " Y " + yll + " Nx " + cols + " Ny " + rows );
    return true;
  }

}

