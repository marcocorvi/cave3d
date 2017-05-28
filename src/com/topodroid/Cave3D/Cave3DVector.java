/** @file Cave3DVector.java
 *
 *e @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D 3D vector
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.util.Log;

class Cave3DVector
{
  float x, y, z; // vector components

  Cave3DVector( )
  {
    x = 0;
    y = 0;
    z = 0;
  }

  Cave3DVector( float x0, float y0, float z0 )
  {
    x = x0;
    y = y0;
    z = z0;
  }

  Cave3DVector( Cave3DVector v )
  {
    x = v.x;
    y = v.y;
    z = v.z;
  }

  Cave3DVector( Cave3DStation st ) 
  {
    x = st.e;
    y = st.n;
    z = st.z;
  }

  public void copy( Cave3DVector b ) // copy assignment
  {
    x = b.x;
    y = b.y;
    z = b.z;
  }

  void reverse()
  {
    x = -x;
    y = -y;
    z = -z;
  }

  void add( Cave3DVector v ) 
  {
    x += v.x;
    y += v.y;
    z += v.z;
  }

  void add( Cave3DVector v, float c ) 
  {
    x += v.x * c;
    y += v.y * c;
    z += v.z * c;
  }

  void mul( float c )
  {
    x *= c;
    y *= c;
    z *= c;
  }

  void sub( Cave3DVector v )
  {
    x -= v.x;
    y -= v.y;
    z -= v.z;
  }

  Cave3DVector times( float c ) { return new Cave3DVector( x*c, y*c, z*c ); }

  Cave3DVector plus( Cave3DVector v ) { return new Cave3DVector( x+v.x, y+v.y, z+v.z ); }
  Cave3DVector plus( Cave3DVector v, float c ) { return new Cave3DVector( x+c*v.x, y+c*v.y, z+c*v.z ); }
  Cave3DVector plus( float x0, float y0, float z0 ) { return new Cave3DVector( x+x0, y+y0, z+z0 ); }

  Cave3DVector minus( Cave3DVector v ) { return new Cave3DVector( x-v.x, y-v.y, z-v.z ); }
  Cave3DVector minus( Cave3DVector v, float c ) { return new Cave3DVector( x-c*v.x, y-c*v.y, z-c*v.z ); }
  Cave3DVector minus( float x0, float y0, float z0 ) { return new Cave3DVector( x-x0, y-y0, z-z0 ); }

  Cave3DVector cross( Cave3DVector v ) 
  {
    return new Cave3DVector( y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x );
  }

  /** midpoint between two vectors
   */
  Cave3DVector midpoint( Cave3DVector v ) { return new Cave3DVector( (x+v.x)/2, (y+v.y)/2, (z+v.z)/2 ); }

  boolean coincide( Cave3DVector p, double eps )
  {
    if ( Math.abs(x - p.x) > eps ) return false;
    if ( Math.abs(y - p.y) > eps ) return false;
    if ( Math.abs(z - p.z) > eps ) return false;
    return true;
  }

  // euclidean distance from another point
  float distance( Cave3DVector p )
  {
    float a = x - p.x;
    float b = y - p.y;
    float c = z - p.z;
    // return FloatMath.sqrt( a*a + b*b + c*c );
    return (float)Math.sqrt( a*a + b*b + c*c );
  }

  float squareDistance( Cave3DVector p )
  {
    float a = x - p.x;
    float b = y - p.y;
    float c = z - p.z;
    return ( a*a + b*b + c*c );
  }

  boolean normalized()
  {
    float d = (float)Math.sqrt( x*x + y*y + z*z );
    if ( d > 0.0 ) {
      x /= d;
      y /= d;
      z /= d;
      return true;
    }
    return false;
  }

  float dot( Cave3DVector v ) { return x * v.x + y * v.y + z * v.z; }

  float length() { return (float)Math.sqrt( x*x + y*y + z*z ); }

  void dump() 
  {
    Log.v("Cave3DX", x + " " + y + " " + z );
  } 

  void randomize( float delta )
  {
    x += delta * (float)( Math.random() - 0.5 );
    y += delta * (float)( Math.random() - 0.5 );
    z += delta * (float)( Math.random() - 0.5 );
  }

  Cave3DPoint projectXY( ) { return new Cave3DPoint( x, y ); }

  void minMax( Cave3DVector m1, Cave3DVector m2 )
  {
    if ( m1.x > x ) m1.x = x; else if ( m2.x < x ) m2.x = x;
    if ( m1.y > y ) m1.y = y; else if ( m2.y < y ) m2.y = y;
    if ( m1.z > z ) m1.z = z; else if ( m2.z < z ) m2.z = z;
  }

}

