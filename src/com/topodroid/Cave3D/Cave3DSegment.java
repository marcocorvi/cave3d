/** @file Cave3DSegment.java
 *
 * @author marco corvi
 * @date may 2017
 *
 * @brief 3D segment
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import java.io.StringWriter;
import java.io.PrintWriter;

class Cave3DSegment
{
  Cave3DIntersection v1, v2;
  Cave3DSegment next;
  float s;

  Cave3DSegment( Cave3DIntersection q1, Cave3DIntersection q2 )
  {
    v1 = q1;
    v2 = q2;
    s = ( q1.s + q2.s )/2;
    next = null;
  }
/*
  int hasEndPoint( Vector3D v, float eps )
  {
    if ( v1.coincide( v, eps ) ) return 1;
    if ( v2.coincide( v, eps ) ) return 2;
    return 0;
  }

  boolean touches( Cave3DSegment s, float eps ) 
  {
    return hasEndPoint( s.v1, eps ) != 0 || hasEndPoint( s.v2, eps  ) != 0;
  }
*/

  float s() { return s; }

}
