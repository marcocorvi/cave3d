/** @file SketchLine.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief TopoDroid sketch Line
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.util.ArrayList;

class SketchLine
{
  ArrayList< Vector3D > pts;
  String thname;
  float red, green, blue;
  float alpha;

  SketchLine( String th, float r, float g, float b ) // LINES
  {
    pts = new ArrayList< Vector3D >();
    thname = th;
    red   = r;
    green = g;
    blue  = b;
    alpha = 1.0f;
  }

  SketchLine( String th, float r, float g, float b, float a ) // AREAS
  {
    pts = new ArrayList< Vector3D >();
    thname = th;
    red   = r;
    green = g;
    blue  = b;
    alpha = a;
  }

  void insertPoint( float x, float y, float z )
  {
    pts.add( new Vector3D( x, y, z ) );
  }

  int size() { return pts.size(); }
}

