/** @file Cave3DPolygon.java
 *
 * @author marco corvi
 * @date may 2017
 *
 * @brief non-convex 2D polygon
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

// import android.util.Log;

import java.io.StringWriter;
import java.io.PrintWriter;

import java.util.ArrayList;

class Cave3DPolygon
{
  ArrayList< Cave3DSite > points;

  Cave3DPolygon( )
  {
    points = new ArrayList< Cave3DSite >();
  }

  int size() { return points.size(); }

  Cave3DSite get( int k ) { return points.get(k); }

  // return true if the site is already in the polygon
  boolean addPoint( Cave3DSite s )
  {
    for ( Cave3DSite pt : points ) if ( pt == s ) return true;
    points.add( s );
    // points.add( new Cave3DPoint( s.x, s.y ) );
    return false;
  }
}
