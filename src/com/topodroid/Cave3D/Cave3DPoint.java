/** @file Cave3DPoint.java
 *
 * @author marco corvi
 * @date may 2017
 *
 * @brief 2D point
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.io.StringWriter;
import java.io.PrintWriter;

class Cave3DPoint
{
  float x, y;

  Cave3DPoint( float xx, float yy )
  {
    x = xx;
    y = yy;
  }

  Cave3DPoint midpoint( Cave3DPoint v ) { return new Cave3DPoint( (x+v.x)/2, (y+v.y)/2 ); }

  float distance( Cave3DPoint p ) 
  { 
    float dx = x - p.x;
    float dy = y - p.y;
    return (float)( Math.sqrt( dx*dx + dy*dy ) );
  }

  boolean isDistinct( Cave3DPoint p, float eps ) 
  {
    return distance(p) > eps;
  }


}
