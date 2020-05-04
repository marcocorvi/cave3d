/** @file Point2D.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief 2D point
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import java.io.StringWriter;
import java.io.PrintWriter;

class Point2D
{
  float x, y;

  Point2D( float xx, float yy )
  {
    x = xx;
    y = yy;
  }

  Point2D midpoint2D( Point2D p ) { return new Point2D( (x+p.x)/2, (y+p.y)/2 ); }

  double distance2D( Point2D p ) 
  { 
    float dx = x - p.x;
    float dy = y - p.y;
    return Math.sqrt( dx*dx + dy*dy );
  }

  // boolean isDistinct2D( Point2D p, float eps ) 
  // {
  //   return distance2D(p) > eps;
  // }

}
