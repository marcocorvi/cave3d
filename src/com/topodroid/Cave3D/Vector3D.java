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
  float x, y, z;

  // -------------------- CSTR
  Vector3D() { x=0f; y=0f; z=0f; }

  Vector3D( float xx, float yy, float zz )
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
    data[offset]   = x;
    data[offset+1] = y;
    data[offset+2] = z;
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
    
  boolean coincide( float x0, float y0, float z0, double eps )
  {
    if ( Math.abs(x - x0) > eps ) return false;
    if ( Math.abs(y - y0) > eps ) return false;
    if ( Math.abs(z - z0) > eps ) return false;
    return true;
  }

  // ------------------- LENGTH 
  float lengthSquare()
  {
    return x*x + y*y + z*z;
  }

  static float lengthSquare( Vector3D v ) { return v.lengthSquare(); }

  static float lengthSquare( float[] v, int off ) { return v[off+0]*v[off+0] + v[off+1]*v[off+1] + v[off+2]*v[off+2]; }

  double length() { return Math.sqrt( lengthSquare() ); }

  static double length( Vector3D v ) { return Math.sqrt( v.lengthSquare() ); }

  static double length( float[] v, int off ) { return Math.sqrt( v[off+0]*v[off+0] + v[off+1]*v[off+1] + v[off+2]*v[off+2] ); }

  // ------------------- DISTANCE
  float squareDistance3D( Vector3D v )
  {
    float a = x - v.x;
    float b = y - v.y;
    float c = z - v.z;
    return ( a*a + b*b + c*c );
  }

  double distance3D( Vector3D v ) { return Math.sqrt( squareDistance3D( v ) ); }

  static float squareDistance3D( Vector3D v1, Vector3D v2 ) { return v1.squareDistance3D( v2 ); }

  static double distance3D( Vector3D v1, Vector3D v2 ) { return Math.sqrt( v1.squareDistance3D( v2 ) ); }

  // ------------------- SCALE
  void scaleBy( float f ) // mul
  {
    x *= f;
    y *= f;
    z *= f;
  }

  Vector3D scaledBy( float f ) // this.times( c )
  { 
    return new Vector3D( x*f, y*f, z*f);
  }

  void normalized() 
  {
    float d = (float)length();
    if ( d > 0f ) scaleBy( 1f/d );
  }

  static void normalize( float[] v, int off )
  {
    float d = (float)length( v, off );
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

  Vector3D difference( float x0, float y0, float z0 )
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

  void add( Vector3D v, float c ) // this.plus( v, c )
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

  Vector3D sum( float x0, float y0, float z0 )
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
  float dotProduct( Vector3D v )
  {
    return x * v.x + y * v.y + z * v.z;
  }
  
  static float dotProduct( Vector3D v1, Vector3D v2 )
  {
    return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
  }

  float dotProduct( float[] v, int off )
  {
    return x * v[off] + y * v[off+1] + z * v[off+2];
  }

  // ---------------------- CROSS PRODUCT
  static float crossProductLengthSquare( float[] v1, int off1, float[] v2, int off2 )
  {
    float x = v1[off1+1] * v2[off2+2] - v1[off1+2] * v2[off2+1];
    float y = v1[off1+2] * v2[off2+0] - v1[off1+0] * v2[off2+2];
    float z = v1[off1+0] * v2[off2+1] - v1[off1+1] * v2[off2+0];
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
    x += (float)( delta * ( Math.random() - 0.5 ) );
    y += (float)( delta * ( Math.random() - 0.5 ) );
    z += (float)( delta * ( Math.random() - 0.5 ) );
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


