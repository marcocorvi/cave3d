/** @file Vector3D.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief 3D vector
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

class Vector3D
{
  double x, y, z;

  // -------------------- CSTR
  Vector3D() { x=0f; y=0f; z=0f; }

  Vector3D( double xx, double yy, double zz )
  {
    x = xx;
    y = yy;
    z = zz;
  }

  Vector3D( float[] data, int offset ) 
  {
    x = data[offset];
    y = data[offset+1];
    z = data[offset+2];
  }

  Vector3D ( Vector3D v ) 
  {
    x = v.x;
    y = v.y;
    z = v.z;
  }

  // Vector3D( Cave3DStation st )
  // {
  //   x = st.e;
  //   y = st.n;
  //   z = st.z;
  // }

  // --------------------- ASSIGNMENT
  void fromArray( float[] data, int offset )
  { 
    x = data[offset];
    y = data[offset+1];
    z = data[offset+2];
  }
 
  void toArray( float[] data, int offset )
  {
    data[offset]   = (float)x;
    data[offset+1] = (float)y;
    data[offset+2] = (float)z;
  }

  void copy( Vector3D v ) 
  {
    x = v.x;
    y = v.y;
    z = v.z;
  }

  // ------------------- "EQUALITY"
  boolean coincide( Vector3D v, double eps )
  {
    if ( Math.abs(x - v.x) > eps ) return false;
    if ( Math.abs(y - v.y) > eps ) return false;
    if ( Math.abs(z - v.z) > eps ) return false;
    return true;
  }
    
  boolean coincide( double x0, double y0, double z0, double eps )
  {
    if ( Math.abs(x - x0) > eps ) return false;
    if ( Math.abs(y - y0) > eps ) return false;
    if ( Math.abs(z - z0) > eps ) return false;
    return true;
  }

  // ------------------- LENGTH 
  double lengthSquare()
  {
    return x*x + y*y + z*z;
  }

  static double lengthSquare( Vector3D v ) { return v.lengthSquare(); }

  static double lengthSquare( float[] v, int off ) { return v[off+0]*v[off+0] + v[off+1]*v[off+1] + v[off+2]*v[off+2]; }

  double length() { return Math.sqrt( lengthSquare() ); }

  static double length( Vector3D v ) { return Math.sqrt( v.lengthSquare() ); }

  static double length( float[] v, int off ) { return Math.sqrt( v[off+0]*v[off+0] + v[off+1]*v[off+1] + v[off+2]*v[off+2] ); }

  // ------------------- DISTANCE
  double squareDistance3D( Vector3D v )
  {
    double a = x - v.x;
    double b = y - v.y;
    double c = z - v.z;
    return ( a*a + b*b + c*c );
  }

  double distance3D( Vector3D v ) { return Math.sqrt( squareDistance3D( v ) ); }

  static double squareDistance3D( Vector3D v1, Vector3D v2 ) { return v1.squareDistance3D( v2 ); }

  static double distance3D( Vector3D v1, Vector3D v2 ) { return Math.sqrt( v1.squareDistance3D( v2 ) ); }

  // ------------------- SCALE
  void scaleBy( double f ) // mul
  {
    x *= f;
    y *= f;
    z *= f;
  }

  Vector3D scaledBy( double f ) // this.times( c )
  { 
    return new Vector3D( x*f, y*f, z*f);
  }

  void normalized() 
  {
    double d = length();
    if ( d > 0f ) scaleBy( 1f/d );
  }

  static void normalize( float[] v, int off )
  {
    double d = length( v, off );
    if ( d > 0f ) {
      v[0] /= d;
      v[1] /= d;
      v[2] /= d;
    }
  }

  // --------------------- REVERSE and OPPOSITE
  void reverse()
  {
    x = -x;
    y = -y;
    z = -z;
  }

  Vector3D opposite( )
  {
    return new Vector3D( -x, -y, -z );
  }

  // --------------------- SUBTRACT and DIFFERENCE
  // subtract v2 from v1 - result in v1
  static void difference( float[] v1, int off1, float[] v2, int off2 ) // minus
  {
    v1[off1+0] -= v2[off2+0];
    v1[off1+1] -= v2[off2+1];
    v1[off1+2] -= v2[off2+2];
  }

  void subtracted( Vector3D v )  // sub
  {
    x -= v.x;
    y -= v.y;
    z -= v.z;
  }

  Vector3D difference( Vector3D v ) // this.minus.v
  {
    return new Vector3D( x - v.x, y - v.y, z - v.z ); 
  }

  Vector3D difference( float[] v, int off )
  {
    return new Vector3D( x - v[off], y - v[off+1], z - v[off+2] ); 
  }

  Vector3D difference( double x0, double y0, double z0 )
  {
    return new Vector3D( x - x0, y - y0, z - z0 ); 
  }

  static Vector3D difference( Vector3D v1, Vector3D v2 )
  {
    return new Vector3D( v1.x - v2.x, v1.y - v2.y, v1.z - v2.z ); 
  }

  static void difference( float[] v, int off, float[] v1, int off1, float[] v2, int off2 )
  {
    v[off+0] = v1[off1+0] - v2[off2+0];
    v[off+1] = v1[off1+1] - v2[off2+1];
    v[off+2] = v1[off1+2] - v2[off2+2];
  }

  // --------------------- ADD and SUM
  void add( Vector3D v ) 
  {
    x += v.x;
    y += v.y;
    z += v.z;
  }

  void add( Vector3D v, double c ) // this.plus( v, c )
  {
    x += v.x * c;
    y += v.y * c;
    z += v.z * c;
  }

  Vector3D sum( Vector3D v ) // this.plus( v )
  {
    return new Vector3D( x + v.x, y + v.y, z + v.z ); 
  }

  Vector3D sum( float[] v, int off )
  {
    return new Vector3D( x + v[off], y + v[off+1], z + v[off+2] ); 
  }

  Vector3D sum( double x0, double y0, double z0 )
  {
    return new Vector3D( x + x0, y + y0, z + z0 ); 
  }

  static Vector3D sum( Vector3D v1, Vector3D v2 )
  {
    return new Vector3D( v1.x + v2.x, v1.y + v2.y, v1.z + v2.z ); 
  }

  static void sum( float[] v, int off, float[] v1, int off1, float[] v2, int off2 )
  {
    v[off+0] = v1[off1+0] + v2[off2+0];
    v[off+1] = v1[off1+1] + v2[off2+1];
    v[off+2] = v1[off1+2] + v2[off2+2];
  }

  // ---------------------- DOT PRODUCT
  double dotProduct( Vector3D v )
  {
    return x * v.x + y * v.y + z * v.z;
  }
  
  static double dotProduct( Vector3D v1, Vector3D v2 )
  {
    return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
  }

  double dotProduct( float[] v, int off )
  {
    return x * v[off] + y * v[off+1] + z * v[off+2];
  }

  // ---------------------- CROSS PRODUCT
  static double crossProductLengthSquare( float[] v1, int off1, float[] v2, int off2 )
  {
    double x = v1[off1+1] * v2[off2+2] - v1[off1+2] * v2[off2+1];
    double y = v1[off1+2] * v2[off2+0] - v1[off1+0] * v2[off2+2];
    double z = v1[off1+0] * v2[off2+1] - v1[off1+1] * v2[off2+0];
    return ( x*x + y*y + z*z );
  }

  static void crossProduct( float[] v, int off, float[] v1, int off1, float[] v2, int off2 )
  {
    v[off+0] = v1[off1+1] * v2[off2+2] - v1[off1+2] * v2[off2+1];
    v[off+1] = v1[off1+2] * v2[off2+0] - v1[off1+0] * v2[off2+2];
    v[off+2] = v1[off1+0] * v2[off2+1] - v1[off1+1] * v2[off2+0];
  }

  static Vector3D crossProduct( Vector3D v1, Vector3D v2 ) 
  {
    return new Vector3D( v1.y * v2.z - v1.z * v2.y,
                         v1.z * v2.x - v1.x * v2.z,
                         v1.x * v2.y - v1.y * v2.x );
  }

  Vector3D crossProduct( Vector3D v2 )
  {
    return new Vector3D( y * v2.z - z * v2.y,
                         z * v2.x - x * v2.z,
                         x * v2.y - y * v2.x );
  }

  // ----------------------- MIDPOINT
  Vector3D midpoint( Vector3D v )
  {
    return new Vector3D( (x + v.x)/2, (y + v.y)/2, (z + v.z)/2 ); 
  }
  
  static Vector3D midpoint( Vector3D v1, Vector3D v2 )
  {
    return new Vector3D( (v1.x + v2.x)/2, (v1.y + v2.y)/2, (v1.z + v2.z)/2 ); 
  }

  // ----------------------- 
  void randomize( double delta )
  {
    x += ( delta * ( Math.random() - 0.5 ) );
    y += ( delta * ( Math.random() - 0.5 ) );
    z += ( delta * ( Math.random() - 0.5 ) );
  }

  Point2D projectXY( ) { return new Point2D( x, y ); }

  // m1 = min( this, m1 )
  // m2 = max( this, m2 )
  void minMax( Vector3D m1, Vector3D m2 )
  {
    if ( m1.x > x ) m1.x = x; else if ( m2.x < x ) m2.x = x;
    if ( m1.y > y ) m1.y = y; else if ( m2.y < y ) m2.y = y;
    if ( m1.z > z ) m1.z = z; else if ( m2.z < z ) m2.z = z;
  }

  // void dump() 
  // {
  //   Log.v("TopoGL", x + " " + y + " " + z );
  // } 
}


