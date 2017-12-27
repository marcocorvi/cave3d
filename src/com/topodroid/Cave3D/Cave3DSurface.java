/** @file Cave3DSurface.java
 *
 * @author marco corvi
 * @date july 2014
 *
 * @brief DEM surface grid
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.io.BufferedReader;
import java.io.IOException;

import android.util.Log;

public class Cave3DSurface
{
  private static final String TAG = "Cave3D";

  double mEast1, mNorth1, mEast2, mNorth2;
  double[] mZ;  // vertical (upwards)
  int mNr1;     // number of centers in East 
  int mNr2;     // number of centers in North
  double mDim1, mDim2; // spacing between grid centers

  /**
   *        ^
   * d2 = 2 |-----------+P2 = (e2,n2)
   *        |   |   |   |
   *      1 |---+---+---|
   *        |P1 |   |   |
   *      0 +------------->
   *        0   1   2   3 = d1
   *
   * d1 number of centers of the grid in X (east) direction
   * d2 number of centers of the grid in Y (north) direction
   *
   * mZ[ Y * n1 + X ] is the elevation of point
   *    E = e1 + X * mDim1
   *    N = n1 + Y * mDim2
   */
  public Cave3DSurface( double e1, double n1, double e2, double n2, int d1, int d2 )
  {
    mEast1  = e1;
    mNorth1 = n1;
    mEast2  = e2;
    mNorth2 = n2;
    mNr1  = d1;
    mNr2  = d2;
    mDim1 = (e2-e1)/(d1-1);
    mDim2 = (n2-n1)/(d2-1);
    mZ = new double[ d1*d2 ]; 
  }

  double computeZ( double e, double n )
  {
    if ( e < mEast1 || n < mNorth1 || e > mEast2 || n > mNorth2 ) return -9999.0;
    int i1 = (int)((e-mEast1)/mDim1);
    int j1 = (int)((n-mNorth1)/mDim2);
    double dx2 = e - ( mEast1 + i1 * mDim1 );
    double dx1 = 1.0 - dx2;
    double dy2 = n - ( mNorth1 + j1 * mDim2 );
    double dy1 = 1.0 - dy2;
    int i2 = i1 + 1;
    int j2 = j1 + 1;
    return ( j2 < mNr2 ) ?
        ( (i2 < mNr1 )? mZ[j1*mNr1+i1] * dx1 + mZ[j1*mNr1+i2] * dx2 : mZ[j1*mNr1+i1] ) * dy1 
      + ( (i2 < mNr1 )? mZ[j2*mNr1+i1] * dx1 + mZ[j2*mNr1+i2] * dx2 : mZ[j2*mNr1+i1] ) * dy2
      : ( (i2 < mNr1 )? mZ[j1*mNr1+i1] * dx1 + mZ[j1*mNr1+i2] * dx2 : mZ[j1*mNr1+i1] ) ;
  }
 

  // the DEM is stored as
  //    (e1,n1)   (e1+1,n1)   ... (e2,n1) 
  //    (e1,n1+1) (e1+1,n1+1) ... (e2,n1+1)
  //    ...
  // with no flip the storing is straightforward
  // with flip horizontal storing is by-row but
  //   each row is filled from e2 to e1 backward
  // with flip vertical the rows of the matrix are filled
  //   from the bottom (n2) to the top (n1)
  //   each row being filled left (e1) to right (e2)
  //
  void readGridData( double units, int flip, BufferedReader br, String filename )
      throws Cave3DParserException
  {
    int linenr = 0;
    int x, y;
    int x1 = 0;
    int x2 = mNr1;
    int dx = 1;
    int y1 = mNr2-1;
    int y2 = -1;
    int dy = -1;
    if ( flip == Cave3DThParser.FLIP_HORIZONTAL ) {
      x1 = mNr1-1;
      x2 = -1;
      dx = -1;
    } else if ( flip == Cave3DThParser.FLIP_VERTICAL ) {
      y1 = 0;
      y2 = mNr2;
      dy = 1;
    }
    x = x1;
    y = y1;

    try {
      while ( y != y2 ) {
        ++linenr;
        String line = br.readLine();
        line = line.trim();
        int pos = line.indexOf( '#' );
        if ( pos >= 0 ) {
          line = line.substring( 0, pos );
        } 
        if ( line.length() == 0 ) continue;
        String[] vals = line.split( " " );
        if ( vals.length > 0 ) {
          int idx = Cave3DThParser.nextIndex( vals, -1 );
          if ( vals[idx].equals( "grid_flip" ) ) {
            idx = Cave3DThParser.nextIndex( vals, idx );
            if ( idx < vals.length ) {
              flip = Cave3DThParser.parseFlip( vals[idx] );
              x1 = 0;
              x2 = mNr1;
              dx = 1;
              y1 = mNr2-1;
              y2 = -1;
              dy = -1;
              if ( flip == Cave3DThParser.FLIP_HORIZONTAL ) {
                x1 = mNr1-1;
                x2 = -1;
                dx = -1;
              } else if ( flip == Cave3DThParser.FLIP_VERTICAL ) {
                y1 = 0;
                y2 = mNr2;
                dy = 1;
              }
              x = x1;
              y = y1;
            } 
          } else if ( vals[idx].equals( "grid-units" ) ) {
            // FIXME TODO units not parsed yet
          } else { // data
            while ( idx < vals.length ) {
              mZ[ y*mNr1+x ] = Double.parseDouble( vals[idx] );
              x += dx;
              if ( x == x2 ) {
                x = x1;
                y += dy;
                if ( y == y2 ) break;
              }
              idx = Cave3DThParser.nextIndex( vals, idx );
            }
          }
        }
      }
    } catch ( IOException e ) {
      // TODO
      Log.e(TAG, "exception " + e.toString() );
      throw new Cave3DParserException( filename, linenr );
    }
    // Log.v( "Cave3D", "surface data: rows " + y );
  }

  // used to set the grid data to the LoxSurface grid
  void setGridData( double[] grid ) { mZ = grid; }

}

