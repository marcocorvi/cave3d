/** @file CWPoint.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief CW point
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.util.ArrayList;
import java.util.Locale;

import java.io.PrintWriter;
// import java.io.PrintStream;
import java.io.IOException;

import android.util.Log;

public class CWPoint extends Cave3DVector
{
  private static final String TAG = "Cave3D VECTOR";

  private static int cnt = 0;
  static void resetCounter() { cnt = 0; }

  int mCnt;
  ArrayList<CWTriangle> mTriangle;

  CWPoint( float x, float y, float z )
  {
    super( x, y, z );
    mCnt = cnt++;
    mTriangle = new ArrayList<CWTriangle>();
  }

  CWPoint( int tag, float x, float y, float z )
  {
    super( x, y, z );
    mCnt = tag;
    if ( cnt <= tag ) cnt = tag+1;
    mTriangle = new ArrayList<CWTriangle>();
  }

  void addTriangle( CWTriangle t ) 
  {
    if ( t == null || mTriangle.contains(t) ) return;
    mTriangle.add( t );
  }
  
  void removeTriangle( CWTriangle t )
  {
    if ( t == null ) return;
    mTriangle.remove( t );
  }
  
  // the triangles are ordered rightward around the outward direction
  boolean orderTriangles()
  {
    int k = 0;
    CWTriangle t0 = mTriangle.get(0);
    CWSide s0 = t0.leftSideOf( this );
    CWSide s1 = t0.rightSideOf( this );
    ++k;
    while ( k<mTriangle.size() ) {
      int j=k;
      for ( ; j<mTriangle.size(); ++j ) {
        if ( mTriangle.get(j).contains( s1 ) ) break;
      } 
      if ( j == mTriangle.size() ) return false;
      // assert ( j < mTriangle.size() );
      CWTriangle tj = mTriangle.get(j);
      if ( j > k ) {
        CWTriangle tk = mTriangle.get(k);
        mTriangle.set(j, tk );
        mTriangle.set(k, tj );
      }
      s1 = tj.rightSideOf( this );
      ++k;
    }
    return true;
  }
  
  CWTriangle rightTriangleOf( CWSide s )
  {
    if ( ! s.contains(this) ) return null;
    for ( CWTriangle t : mTriangle ) if ( s == t.leftSideOf(this) ) return t;
    return null;
  }
  
  CWTriangle leftTriangleOf( CWSide s )
  {
    if ( ! s.contains(this) ) return null;
    for ( CWTriangle t : mTriangle ) if ( s == t.rightSideOf(this) ) return t;
    return null;
  }

  // check if this point triangles are all marked "outside"
  boolean areAllTrianglesOutside()
  {
    for ( CWTriangle t : mTriangle ) {
      if ( ! t.isOutside() ) return false;
    }
    return true;
  }

  void dump( )
  {
    StringBuilder sb = new StringBuilder();
    for ( CWTriangle t : mTriangle ) sb.append( "-" + t.mCnt );
    Log.v( TAG, "Point " + mCnt + " T" + sb.toString() + " " + x + " " + y + " " + z );
  }
  
  void serialize( PrintWriter out )
  {
    int size = ( mTriangle != null )? mTriangle.size() : -1;
    out.format(Locale.US, "V %d %d %.3f %.3f %.3f\n", mCnt, size, x, y, z );
  }

}

