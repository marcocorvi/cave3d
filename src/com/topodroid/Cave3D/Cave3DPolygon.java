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

import java.io.StringWriter;
import java.io.PrintWriter;

import java.util.ArrayList;

import android.util.Log;

class Cave3DPolygon
{
  ArrayList< Cave3DSite > points;

  Cave3DPolygon( )
  {
    points = new ArrayList< Cave3DSite >();
  }

  int size() { return points.size(); }

  Cave3DSite get( int k ) { return points.get(k); }

  void addPoint( Cave3DSite s )
  {
    points.add( s );
    // points.add( new Cave3DPoint( s.x, s.y ) );
  }
}
