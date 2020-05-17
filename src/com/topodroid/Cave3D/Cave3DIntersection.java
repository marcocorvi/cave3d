/** @file Cave3DIntersection.java
 *
 * @author marco corvi
 * @date may 2017
 *
 * @brief 3D intersection od shot and segment
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;


// 3D intersection point on the segment
class Cave3DIntersection extends Vector3D
{
  float s; // line abscissa on the shot P(s) = p1 + s ( p2-p1)
 
  Cave3DIntersection( Vector3D pt, float s0 )
  {
    super( pt );
    s = s0;
  }

  Cave3DIntersection( float x0, float y0, float z0, float s0 )
  {
    super( x0, y0, z0 );
    s = s0;
  }
}
