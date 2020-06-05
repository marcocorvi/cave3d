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
  double x, y;

  Point2D( double xx, double yy )
  {
    x = xx;
    y = yy;
  }

  Point2D midpoint2D( Point2D p ) { return new Point2D( (x+p.x)/2, (y+p.y)/2 ); }

  double distance2D( Point2D p ) 
  { 
    double dx = x - p.x;
    double dy = y - p.y;
    return Math.sqrt( dx*dx + dy*dy );
  }

  // boolean isDistinct2D( Point2D p, double eps ) 
  // {
  //   return distance2D(p) > eps;
  // }

}
