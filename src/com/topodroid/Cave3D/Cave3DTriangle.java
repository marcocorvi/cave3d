/** @file Cave3DTriangle.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief face triangle
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.io.StringWriter;
import java.io.PrintWriter;

class Cave3DTriangle
{
  int size;
  Cave3DVector[] vertex;
  Cave3DVector   normal;
  Cave3DVector   center;
  int direction;

  Cave3DTriangle( int sz )
  {
    size = sz;
    vertex = new Cave3DVector[size];
    direction = 0;
  }

  Cave3DTriangle( Cave3DVector v0, Cave3DVector v1, Cave3DVector v2 )
  {
    size = 3;
    vertex = new Cave3DVector[3];
    vertex[0] = v0;
    vertex[1] = v1;
    vertex[2] = v2;
    computeNormal();
    direction = 0;
  }

  void setVertex( int k, Cave3DVector v )
  {
    vertex[k] = v;
  }

  void computeNormal()
  {
    Cave3DVector w1 = vertex[1].minus( vertex[0] );
    Cave3DVector w2 = vertex[2].minus( vertex[0] );
    normal = w1.cross(w2);
    normal.normalized();
    center = new Cave3DVector( 0, 0, 0 );
    for ( int k=0; k<size; ++k ) {
      center.add( vertex[k] );
    }
    center.mul( 1.0f/size );
  }

  void flip()
  {
    Cave3DVector v = vertex[1];
    vertex[1] = vertex[2];
    vertex[2] = v;
    normal.reverse();
  }

  public String toString()
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter( sw );
    pw.format("%.2f %.2f %.2f - %.2f %.2f %.2f - %.2f %.2f %.2f",
      vertex[0].x, vertex[0].y, vertex[0].z,
      vertex[1].x, vertex[1].y, vertex[1].z,
      vertex[2].x, vertex[2].y, vertex[2].z );
    return sw.getBuffer().toString();
  }

  // 6 times the volume of the three vectors
  static float volume( Cave3DVector v1, Cave3DVector v2, Cave3DVector v3 )
  {
    return v1.x * ( v2.y * v3.z - v2.z * v3.y )
         + v1.y * ( v2.z * v3.x - v2.x * v3.z )
         + v1.z * ( v2.x * v3.y - v2.y * v3.x );
  }

  static float volume( Cave3DVector v0, Cave3DVector v1, Cave3DVector v2, Cave3DVector v3 )
  {
    return volume( v1.minus(v0), v2.minus(v0), v3.minus(v0) );
  }

  float volume( Cave3DVector v )
  {
    float ret = 0;
    Cave3DVector v0 = vertex[0].minus(v);
    Cave3DVector v1 = vertex[1].minus(v);
    for ( int k=2; k<size; ++k ) {
      Cave3DVector v2 = vertex[k].minus(v);
      ret += volume( v0, v1, v2 );
      v1 = v2;
    }
    return ret;
  }
    

}
