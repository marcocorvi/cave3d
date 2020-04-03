/** @file DEMasciiParser.java
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

class DEMasciiParser extends DEMparser
{
  private double xll, yll;
  private int   cols, rows;

  DEMasciiParser( String filename )
  {
    super( filename );
  }

  @Override
  boolean readData( float xwest, float xeast, float ysouth, float ynorth )
  {
    if ( ! mValid ) return mValid;
    FileReader fr = null;
    try {
      fr = new FileReader( mFilename );
      BufferedReader br = new BufferedReader( fr );
      for ( int k=0; k<6; ++k) br.readLine();

      double y = yll + mDim2 * rows;
      int k = 0;
      for ( ; k < rows && y > ynorth; ++k ) {
        br.readLine();
        y -= mDim2;
      }
  
      double x = xll;
      int i = 0;
      for ( ; i < cols && x < xwest; ++i ) x += mDim1;
      mEast1 = x;
      int xoff = i;
      mNr1 = 0;
      for ( ; i < cols && x <= xeast; ++i ) { x += mDim1; ++mNr1; }
      mEast2 = ( x > xeast )? x - mDim1 : x;
      
      mNorth2 = y;
      mNr2 = 0;
      int kk = k;
      for ( ; kk < rows && y >= ysouth; ++kk ) { y -= mDim2; ++mNr2; }
      mNorth1 = ( y < ysouth )? y + mDim2 : y;
   
      mZ = new double[ mNr1 * mNr2 ];
      int j = 0;
      for ( ; k < rows && j < mNr2; ++k, ++j ) {
        String line = br.readLine();
        String[] vals = line.replaceAll("\\s+", " ").split(" ");
        for ( int ii=0; ii<mNr1; ++ii ) mZ[j*mNr1 + ii] = Double.parseDouble( vals[xoff+ii] );
      }
    } catch ( IOException e1 ) {
      mValid = false;
    } catch ( NumberFormatException e2 ) {
      mValid = false;
    } finally {
      if ( fr != null ) try { fr.close(); } catch ( IOException e ) {}
    }
    Log.v("Cave3D-DEM", "W " + mEast1 + " E " + mEast2 + " S " + mNorth1 + " N " + mNorth2 );
    Log.v("Cave3D-DEM", "size " + mNr1 + " " + mNr2 );
    makeNormal();
    return mValid;
  }

  @Override
  protected boolean readHeader( String filename )
  {
    try {
      FileReader fr = new FileReader( filename );
      BufferedReader br = new BufferedReader( fr );
      String line = br.readLine();
      String[] vals = line.replaceAll("\\s+", " ").split(" ");
      cols = Integer.parseInt( vals[1] ); // ncols
      line = br.readLine();
      vals = line.replaceAll("\\s+", " ").split(" ");
      rows = Integer.parseInt( vals[1] ); // nrows
      line = br.readLine();
      vals = line.replaceAll("\\s+", " ").split(" ");
      xll = Double.parseDouble( vals[1] ); // xllcorner
      line = br.readLine();
      vals = line.replaceAll("\\s+", " ").split(" ");
      yll = Double.parseDouble( vals[1] ); // yllcorner
      line = br.readLine();
      vals = line.replaceAll("\\s+", " ").split(" ");
      mDim1 = Double.parseDouble( vals[1] ); // cellsize
      mDim2 = mDim1;
      line = br.readLine();
      vals = line.replaceAll("\\s+", " ").split(" ");
      nodata = Double.parseDouble( vals[1] ); // nodata.value
      fr.close();
    } catch ( IOException e1 ) { 
      return false;
    } catch ( NumberFormatException e2 ) {
      return false;
    }
    Log.v("Cave3D-DEM", "cell " + mDim1 + " X " + xll + " Y " + yll + " Nx " + cols + " Ny " + rows );
    return true;
  }

}

