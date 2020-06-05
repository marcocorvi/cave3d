/** @file DEMsurface.java
 *
 * @author marco corvi
 * @date july 2014
 *
 * @brief DEM surface grid
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;

import android.util.Log;
import android.graphics.RectF;

public class DEMsurface
{
  double mEast1, mNorth1; // (west, south) center of LL-cornel cell
  double mEast2, mNorth2; // (east, north) center of UR-corner cell

  float[] mZ;         // vertical (upwards)
  int mNr1;           // number of centers in East 
  int mNr2;           // number of centers in North
  double mDim1, mDim2; // spacing between grid centers
  // float mNormal[];    // normal vectors (3 float per vertex)

  // lefttop right bottom
  RectF getBounds( )
  {
    return new RectF( (float)(mEast1-mDim1/2), (float)(mNorth2+mDim2/2), (float)(mEast2+mDim1/2), (float)(mNorth1-mDim2/2) );
  }

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
   *
   * called by Therion/Lox parser
   * Therion/Loch have (East,North) bounds at the LL-corner of the cell
   */
  public DEMsurface( double e1, double n1, double delta_e, double delta_n, int dim1, int dim2 )
  {
    mEast1  = e1 + delta_e/2;
    mNorth1 = n1 + delta_n/2;
    mEast2  = e1 + delta_e * (dim1-1);
    mNorth2 = n1 + delta_n * (dim2-1);
    mNr1  = dim1;
    mNr2  = dim2;
    mDim1 = delta_e;
    mDim2 = delta_n;
    mZ      = null; // new float[ d1*d2 ]; // to catch errors
    // mNormal = new float[ 3*d1*d2 ];
    // Log.v("TopoGL-SURFACE", "E " + mEast1 + " " + mEast2 + " " + mNr1 + " " + mDim1 + " N " + mNorth1 + " " + mNorth2 + " " + mNr2 + " " + mDim2 );
  }

  protected DEMsurface( ) { }

  float computeZ( double e, double n )
  {
    if ( e < mEast1 || n < mNorth1 || e > mEast2 || n > mNorth2 ) return -9999.0f;
    int i1 = (int)((e-mEast1)/mDim1);
    int j1 = (int)((n-mNorth1)/mDim2);
    double dx2 = (e - ( mEast1 + i1 * mDim1 ))/mDim1;
    double dx1 = 1.0 - dx2;
    double dy2 = (n - ( mNorth1 + j1 * mDim2 ))/mDim2;
    double dy1 = 1.0 - dy2;
    int i2 = i1 + 1;
    int j2 = j1 + 1;
    return (float)( ( j2 < mNr2 ) ?
        ( (i2 < mNr1 )? mZ[j1*mNr1+i1] * dx1 + mZ[j1*mNr1+i2] * dx2 : mZ[j1*mNr1+i1] ) * dy1 + ( (i2 < mNr1 )? mZ[j2*mNr1+i1] * dx1 + mZ[j2*mNr1+i2] * dx2 : mZ[j2*mNr1+i1] ) * dy2
      : ( (i2 < mNr1 )? mZ[j1*mNr1+i1] * dx1 + mZ[j1*mNr1+i2] * dx2 : mZ[j1*mNr1+i1] ) );
    // Log.v("TopoGL-SURFACE", "i " + i1 + " j " + j2 + " z " + ret );
  }

  // ( dx, 0, dzx) ^ ( 0, dy, dzy ) = ( -dy * dzx, -dx * dzy, dx * dy ) 
  //                                = -(dx*dy) * ( dzx/dx, dzy/dy, -1 )
  // normal = (dzx/dx, dzy/dy, -1) / | its norm |
  // void initNormal()
  // { 
  //   for ( int j=0; j<mNr2-1; ++j ) {
  //     int j0 = j*mNr1;
  //     int j1 = ( j > 0 )?      (j-1) : j;
  //     int j2 = ( j < mNr2-1 )? (j+1) : j;
  //     double dy = (j2-j1) * mDim2;
  //     int j1 *= mNr1;
  //     int j2 *= mNr1;
  //     for ( int i=0; i<mNr1-1; ++i ) {
  //       int i1 = ( i > 0 )?      (i-1) : i;
  //       int i2 = ( i < mNr1-1 )? (i+1) : i;
  //       double x = ( mZ[j0 + i2] - mZ[j0 + i1] ) / ((i2-i1)*mDim1);
  //       double y = ( mZ[j2 + i ] - mZ[j1 + i ] ) / dy;
  //       double m = Math.sqrt( 1 + x*x + y*y );
  //       mNormal[ 3*j0     ] =  x / m;
  //       mNormal[ 3*j0 + 1 ] =  y / m;
  //       mNormal[ 3*j0 + 2 ] = -1 / m;
  //       ++ j0;
  //     }
  //   }
  // }

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
  // called only by ParserTh
  void readGridData( double units, int flip, BufferedReader br, String filename )
      throws ParserException
  {
    int linenr = 0;
    int x, y;
    int x1 = 0;
    int x2 = mNr1;
    int dx = 1;
    // int y1 = mNr2-1;
    // int y2 = -1;
    // int dy = -1;
    int y1 = 0;
    int y2 = mNr2;
    int dy = 1;
    x = x1;
    y = y1;
    mZ = new float[ mNr1 * mNr2 ];

    // Log.v("TopoGL-GRID", "read grid data - units " + units );
    // Log.v("TopoGL-GRID", "x " + x + " x1 " + x1 + " x2 " + x2 + " dx " + dx + " Nr1 " + mNr1 ); 
    // Log.v("TopoGL-GRID", "y " + y + " y1 " + y1 + " y2 " + y2 + " dy " + dy + " Nr2 " + mNr2 ); 
    // Log.v("TopoGL", "surface data: initial row " + y );
    try {
      while ( y != y2 ) {
        ++linenr;
        String line = br.readLine();
        line = line.trim();
        // Log.v("TopoGL", "y " + y + ": " + line );
        int pos = line.indexOf( '#' );
        if ( pos >= 0 ) {
          line = line.substring( 0, pos );
        } 
        if ( line.length() == 0 ) continue;
        if ( line.startsWith("endsurface" ) ) {
          // something went wrong
          Log.e("TopoGL", "run out of surface data");
          throw new ParserException( filename, linenr );
        }
        String[] vals = line.replaceAll("\\s+", " ").split( " " );
        if ( vals.length > 0 ) {
          int idx = ParserTh.nextIndex( vals, -1 );
          if ( vals[idx].equals( "grid_flip" ) ) {
            idx = ParserTh.nextIndex( vals, idx );
            if ( idx < vals.length ) {
              flip = ParserTh.parseFlip( vals[idx] );
              if ( flip == ParserTh.FLIP_HORIZONTAL ) {
                x1 = mNr1-1;
                x2 = -1;
                dx = -1;
              } else if ( flip == ParserTh.FLIP_VERTICAL ) {
                // y1 = 0;
                // y2 = mNr2;
                // dy = 1;
                y1 = mNr2-1;
                y2 = -1;
                dy = -1;
              }
              x = x1;
              y = y1;
            } 
            // Log.v("TopoGL-SURFACE", "flip x " + x + " x1 " + x1 + " x2 " + x2 + " dx " + dx + " Nr1 " + mNr1 ); 
            // Log.v("TopoGL-SURFACE", "flip y " + y + " y1 " + y1 + " y2 " + y2 + " dy " + dy + " Nr2 " + mNr2 ); 
          } else if ( vals[idx].equals( "grid-units" ) ) {
            // FIXME TODO units not parsed yet
          } else { // data
            try { Float.parseFloat( vals[0] ); } catch ( NumberFormatException e ) { continue; }
            while ( idx < vals.length ) {
              mZ[ y*mNr1+x ] = Float.parseFloat( vals[idx] );
              x += dx;
              if ( x == x2 ) {
                x = x1;
                y += dy;
                if ( y == y2 ) break;
              }
              idx = ParserTh.nextIndex( vals, idx );
            }
          }
        }
      }
    } catch ( IOException e ) {
      // TODO
      Log.e( "TopoGL-SURFACE", "exception " + e.getMessage() );
      throw new ParserException( filename, linenr );
    }
    // initNormal();
    // Log.v( "TopoGL", "surface data: final row " + y );
  }

  // used to set the grid data to the LoxSurface grid
  void setGridData( double[] grid, int xoff, int yoff, int step, int d1_grid, int d2_grid )
  { 
    // Log.v("TopoGL-SURFACE", "offset " + xoff + " " + yoff + " size " + mNr1 + " " + mNr2 + " dim_grid " + d1_grid + " " + d2_grid );
    // int len = grid.length;
    mZ = new float[ mNr1 * mNr2 ];
    // int j0off = ( mNr2 - 1 - yoff + step/2) * d1_grid;
    int j0off = (yoff * step + step/2) * d1_grid;
    int i0off = xoff * step + step/2;
    int joff = j0off + i0off; // step/2 to place in the middle of the block
    d1_grid *= step; // increase by blocks of (d1 * step)
    // Log.v("TopoGL-SURFACE", "d1_grid " + d1_grid + " off " + i0off + " " + j0off + " joff " + joff + " step " + step );
    for ( int j=0; j<mNr2; ++j ) {
      // int j1 = j * mNr1;
      int j1 = j*mNr1;
      // int j2 = joff - j * d1_grid;
      int j2 = joff + j * d1_grid;
      for ( int i=0; i<mNr1; ++i ) mZ[ j1 + i ] = (float)grid[ j2 + i * step ];
    }
    // logZMinMax();

    // initNormal(); 
    // for ( int j=0; j<mNr2; ++j ) {
    //   StringBuilder sb = new StringBuilder();
    //   for ( int i=0; i<mNr1; ++i ) { sb.append( " " + Float.toString( mZ[j*mNr1+i] ) ); }
    //   Log.v("Cave3D-SURFACE", sb.toString() );
    // }
  }

  private void logZMinMax()
  { 
    float zmin, zmax;
    zmin = zmax = mZ[0];
    for ( int k=1; k<mNr1*mNr2; ++k ) {
      if ( mZ[k] < zmin ) { zmin = mZ[k]; } else if ( mZ[k] > zmax ) { zmax = mZ[k]; }
    }
    Log.i("TopoGL-DEM", "Z " + zmin + " " + zmax );
  }
}

