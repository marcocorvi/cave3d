/** @file CWTriangle.java
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

import java.util.List;
// import java.io.PrintStream;
// import java.io.IOException;

import android.util.Log;

public class CWTriangle
{
  private static int cnt = 0;
  int mCnt;
  CWPoint v1, v2, v3;
  CWSide s1, s2, s3;
  private Cave3DVector u;  // (v2-v1)x(v3-v1): U points "inside"
  Cave3DVector un; // u normalized
  private float u22, u23, u33; // u2*u2 / det, ... etc
  Cave3DVector u2; // v2-v1 
  Cave3DVector u3; // v3-v1
  Cave3DVector u1; // v3-v2
  
  // private Cave3DVector mVolume;
  // private float mVolumeOffset;

  private boolean mOutside; // work variable
  
  // points are ordered v1--v2--v3 (looking at the triangle from "inside"
  CWPoint nextOf( CWPoint p )
  {
    if ( p == v1 ) return v2;
    if ( p == v2 ) return v3;
    if ( p == v3 ) return v1;
    return null;
  }
  
  CWPoint prevOf( CWPoint p )
  {
    if ( p == v1 ) return v3;
    if ( p == v2 ) return v1;
    if ( p == v3 ) return v2;
    return null;
  }

  float distance( Cave3DVector p ) { return (float)Math.abs( un.dot(p) ); } 
  
  CWSide nextWithPoint( CWSide s, CWPoint p )
  {
    if ( s == s1 ) {
  	  if ( s2.contains(p) ) return s2;
  	  if ( s3.contains(p) ) return s3;
    } else if ( s == s2 ) {
  	  if ( s1.contains(p) ) return s1;
  	  if ( s3.contains(p) ) return s3;
    } else if ( s == s3 ) {
  	  if ( s1.contains(p) ) return s1;
  	  if ( s2.contains(p) ) return s2;
    }
    return null;
  }
  
  CWSide leftSideOf( CWPoint p ) // left is prev
  {
    if ( p == v1 ) return s2;
    if ( p == v2 ) return s3;
    if ( p == v3 ) return s1;
    return null;
  }
  
  CWSide rightSideOf( CWPoint p ) // right is next
  {
    if ( p == v1 ) return s3;
    if ( p == v2 ) return s1;
    if ( p == v3 ) return s2;
    return null;
  }
  
  CWSide oppositeSideOf( CWPoint p )
  {
    if ( p == v1 ) return s1;
    if ( p == v2 ) return s2;
    if ( p == v3 ) return s3;
    return null;
  }
  
  CWPoint oppositePointOf( CWSide s )
  {
    if ( s == s1 ) return v1;
    if ( s == s2 ) return v2;
    if ( s == s3 ) return v3;
    return null;
  }

  public CWTriangle( CWPoint v1, CWPoint v2, CWPoint v3, CWSide s1, CWSide s2, CWSide s3 )
  {
    mCnt = cnt ++;
    buildTriangle( v1, v2, v3, s1, s2, s3 );
  }
  
  public CWTriangle( int tag, CWPoint v1, CWPoint v2, CWPoint v3, CWSide s1, CWSide s2, CWSide s3 )
  {
    mCnt = tag;
    if ( cnt <= tag ) cnt = tag+1;
    buildTriangle( v1, v2, v3, s1, s2, s3 );
  }
  
  void rebuildTriangle()
  {
    v1.removeTriangle( this );
    v2.removeTriangle( this );
    v3.removeTriangle( this );
    buildTriangle( v1, v2, v3, s1, s2, s3 );
  }
  
  private void buildTriangle( CWPoint v1, CWPoint v2, CWPoint v3, CWSide s1, CWSide s2, CWSide s3 )
  {
    this.v1 = v1;
    this.v2 = v2;
    this.v3 = v3;
    u2 = v2.minus(v1); // side s3
    u3 = v3.minus(v1); // side s2
    u1 = v3.minus(v2); // side s1
    
    u = u2.cross(u3);
    un = new Cave3DVector( u );
    un.normalized();
    u22 = u2.dot(u2);
    u23 = u2.dot(u3);
    u33 = u3.dot(u3);
    float udet = u22 * u33 - u23 * u23;
    u22 /= udet;
    u23 /= udet;
    u33 /= udet;
    
    this.s1 = (s1 == null) ? new CWSide( v2, v3 ) : s1;
    this.s2 = (s2 == null) ? new CWSide( v3, v1 ) : s2;
    this.s3 = (s3 == null) ? new CWSide( v1, v2 ) : s3;
    this.s1.setTriangle( this );
    this.s2.setTriangle( this );
    this.s3.setTriangle( this );
    this.v1.addTriangle( this );
    this.v2.addTriangle( this );
    this.v3.addTriangle( this );
    // mVolume = new Cave3DVector(
    //   ( v1.y * v2.z + v3.y * v1.z + v2.y * v3.z - v1.y * v3.z - v3.y * v2.z - v2.y * v1.z ),
    //   ( v1.x * v3.z + v3.x * v2.z + v2.x * v1.z - v1.x * v2.z - v3.x * v1.z - v2.x * v3.z ),
    //   ( v1.x * v2.y + v3.x * v1.y + v2.x * v3.y - v1.x * v3.y - v3.x * v2.y - v2.x * v1.y ) );
    // mVolumeOffset = v1.x * (v2.y*v3.z - v2.z*v3.y) + v1.y * (v2.z*v3.x - v2.x*v3.z) + v1.z * (v2.x*v3.y - v2.y*v3.z);

  }

  void dump( )
  {
    Log.v("Cave3D", "Tri " + mCnt + " P " + v1.mCnt + "-" + v2.mCnt + "-" + v3.mCnt 
                     + " S " + s1.mCnt + " " + s2.mCnt + " " + s3.mCnt 
                     // + " U " + un.x + " " + un.y + " " + un.z
    );
  }
  
  // void serialize( PrintStream out )
  // {
  //   out.println(mCnt + " " + v1.mCnt + " " + v2.mCnt + " " + v3.mCnt 
  //                    + " " + s1.mCnt + " " + s2.mCnt + " " + s3.mCnt
  //                    + " U " + un.x + " " + un.y + " " + un.z 
  //   );
  // }

  /* if vector P is "outside" the triangle-plane (ie on the other side than the hull)
   * set mOutside to true.
   * P is outside if the volume of the tetrahedron of P and the triangle is negative
   * because the normal U of the triangle points "inside" the convex hull
   */
  boolean setOutside( Cave3DVector p )
  {
    mOutside = ( volume(p) < 0.0f );
    return mOutside;
  }
  
  boolean isOutside() { return mOutside; }
  
  /* returns true if P is a vertex of the triangle
   */
  boolean contains( CWPoint p ) { return p == v1 || p == v2 || p == v3; } 
  
  /* returns true is S is a side of the triangle
   */
  boolean contains( CWSide s ) { return s == s1 || s == s2 || s == s3; } 
  
  boolean hasPointAbove( Cave3DVector v ) { return isProjectionInside( v.minus(v1) ); }
  
  float maxAngleOfPoint( Cave3DVector p )
  {
	  Cave3DVector pp = p.minus(v1);
	  float c1 = un.dot( pp ) / pp.length();
	  pp = p.minus(v2);
	  float c2 = un.dot( pp ) / pp.length();
	  pp = p.minus(v3);
	  float c3 = un.dot( pp ) / pp.length();
	  if ( c2 > c1 ) c1 = c2;
	  if ( c3 > c1 ) c1 = c3;
	  return (float)Math.acos(c1);
  }
  
  /** compute the area of the triangle
   * @return area
   */
  float area() { return (float)(Math.abs( u2.cross(u3).length() )); }

  /** compute the volume of the tetrahedrom of this triangle and the point P0
   * @param p0    point "external" to the triangle
   * @return volume
   * @note the volume has sign: since the triangle is directed towards the inside of the CW the volume is
   *       positive if the point is on-the-"inside" the CW 
   */
  float volume( Cave3DVector p0 ) { return u.dot( p0.minus(v1) ); }
  
  /** solid angle of the triangle as seen from a point
   * A. van Oosterom, J. Strackee "A solid angle of a plane traiangle" IEEE Trans. Biomed. Eng. 30:2 1983 125-126
   */
  double solidAngle( Cave3DVector p )
  {
    Cave3DVector p1 = v1.minus(p);  p1.normalized();
    Cave3DVector p2 = v2.minus(p);  p2.normalized();
    Cave3DVector p3 = v3.minus(p);  p3.normalized();
    float s = p1.cross(p3).dot(p2);
    float c = 1 + p1.dot(p2) + p2.dot(p3) + p3.dot(p1);
    return 2 * Math.atan2( s, c );
  }
  
  /* returns true if
   * - the vector P is on the surface of the triangle (eps = 0.001)
   * - and it projects inside the triangle
   */
  boolean isPointInside( Cave3DVector p, float eps )
  {
    Cave3DVector v0 = p.minus(v1);
    if ( Math.abs( u.dot(v0) ) > eps ) return false;
    return isProjectionInside( v0 );
  }
  
  /** 
   * @param list   list of points
   * @return true if the three vertices of the triangle are in the list
   */
  boolean hasVerticesInList( List<CWPoint> list )
  {
    return list.contains( v1 ) && list.contains( v2 ) && list.contains( v3 );
  }
	  
  /** computes the projection of V0 in the plane of the triangle
   * and checks if it lies inside the triangle
   * @param v0   vector 
   * @note v0 already reduced to v1.
   * @return true if the projection of v0 falls inside the triangle
   */
  private boolean isProjectionInside( Cave3DVector v0 )
  {
	  float v02 = v0.dot( u2 );
      float v03 = v0.dot( u3 );
	  float a = u33 * v02 - u23 * v03;
	  float b = u22 * v03 - u23 * v02;
	  return ( a >= 0 && b >= 0 && (a+b) <= 1 );
  }
  
  /** intersection with the segment P1-P2
   * computes the point of the line P1+s*(P2-P1)
   * 
   * @param p1  first segment endpoint (inside)
   * @param p2  second segment endpoint (outside)
   * @return intersection point or null
   */
  Cave3DVector intersection( Cave3DVector p1, Cave3DVector p2, Float res )
  {
	  Cave3DVector dp = new Cave3DVector( p2.x-p1.x, p2.y-p1.y, p2.z-p1.z);
	  float dpu = u.x * dp.x + u.y * dp.y + u.z * dp.z;
	  // if ( Math.abs(dpu) < 0.001 ) return null;
	  Cave3DVector vp = new Cave3DVector( v1.x-p1.x, v1.y-p1.y, v1.z-p1.z);
	  float s = (u.x * vp.x + u.y * vp.y + u.z * vp.z)/dpu;
	  res = s;
	  if ( s < 0.0 || s > 1.0 ) return null;
	  Cave3DVector j = new Cave3DVector( p1.x+s*dp.x, p1.y+s*dp.y, p1.z+s*dp.z); // intersection point
	  Cave3DVector j0 = j.minus(v1);
	  if ( isProjectionInside(j0) ) return j;
	  return null;
  }
  
  /** get an intersection point with another triangle
   *  the intersection line is P + alpha (N1 ^ N2)
   * @param t the other triangle
   * @return an intersection point 
   */
  Cave3DVector intersectionBasepoint( CWTriangle t )
  {
	Cave3DVector ret = new Cave3DVector();
	Cave3DVector n = un.cross( t.un );
	float vn1 = v1.dot(un);
	float vn2 = t.v1.dot(t.un);
	if ( Math.abs(n.x) > Math.abs(n.y) ) {
		if ( Math.abs(n.x) > Math.abs(n.z) ) { // solve Y-Z for X=0
			ret.y = (   t.un.z * vn1 - un.z * vn2 ) / n.x;
			ret.z = ( - t.un.y * vn1 + un.y * vn2 ) / n.x;
		} else { // solve X-Y for Z=0
			ret.x = (   t.un.y * vn1 - un.y * vn2 ) / n.z;
			ret.y = ( - t.un.x * vn1 + un.x * vn2 ) / n.z;
		}
	} else {
		if ( Math.abs(n.y) > Math.abs(n.z) ) { // solve Z-X for Y=0
			ret.z = (   t.un.x * vn1 - un.x * vn2 ) / n.y;
			ret.x = ( - t.un.z * vn1 + un.z * vn2 ) / n.y;
		} else { // solve X-Y for Z=0
			ret.x = (   t.un.y * vn1 - un.y * vn2 ) / n.z;
			ret.y = ( - t.un.x * vn1 + un.x * vn2 ) / n.z;
		}
	}
	// check
	// Log.v("Cave3D", "t " + mCnt + " intersection with " + t.mCnt + " " 
	//    + un.dot(ret.minus(v1)) + " " + t.un.dot(ret.minus(t.v1)) );
	return ret;
  }
  
  Cave3DVector intersectionDirection( CWTriangle t ) { return un.cross( t.un ); }
  
  boolean intersectionPoints( Cave3DVector v, Cave3DVector n, CWLinePoint lp1, CWLinePoint lp2 )
  {
	float b2 = beta2( v, n );
	float b3 = beta3( v, n );
	float b1 = beta1( v, n );
	float a1, a2, a3;
	if ( b1 >= 0 && b1 <= 1 ) {
		a1 = alpha1( v, n );
		// Cave3DVector vb1 = v2.plus( u1.times(b1) ).minus( v.plus( n.times(a1) ) );
		// if ( vb1.length() > 0.001 ) {
		//   Log.v("Cave3D", "Vdiff 1 " + vb1.x + " " + vb1.y + " " + vb1.z );
		// }
        lp1.copy( a1, s1, this, v.plus( n.times(a1) ) );
		// Log.v("Cave3D", "t " + mCnt + " beta1 " + b1 + " " + b2 + " " + b3 );
		if ( b2 >= 0 && b2 <= 1 ) {
			a2 = alpha2( v, n );
			// Cave3DVector vb2 = v1.plus( u2.times(b2) ).minus( v.plus( n.times(a2) ) );
			// if ( vb2.length() > 0.001 ) {
			//  Log.v("Cave3D", "Vdiff 2 " + vb2.x + " " + vb2.y + " " + vb2.z );
			// }
			lp2.copy( a2, s3, this, v.plus( n.times(a2) ) );
			return true;
		} else if ( b3 >= 0 && b3 <= 1 ) {
			a3 = alpha3( v, n );
			// Cave3DVector vb3 = v1.plus( u3.times(b3) ).minus( v.plus( n.times(a3) ) );
			// if ( vb3.length() > 0.001 ) {
			//  Log.v("Cave3D", "Vdiff 3 " + vb3.x + " " + vb3.y + " " + vb3.z );
			// }
			lp2.copy( a3, s2, this, v.plus( n.times(a3) ) );
			return true;
		}
	} else if ( b2 >= 0 && b2 <= 1 ) {
		a2 = alpha2( v, n );
		// Cave3DVector vb2 = v1.plus( u2.times(b2) ).minus( v.plus( n.times(a2) ) );
		// if ( vb2.length() > 0.001 ) {
		//  Log.v("Cave3D", "Vdiff 2 " + vb2.x + " " + vb2.y + " " + vb2.z );
		// }
		lp1.copy( a2, s3, this, v.plus( n.times(a2) ) );
		// Log.v("Cave3D", "t " + mCnt + " beta2 " + b2 + " " + b3 + " (" + b1 + ")" );
	    if ( b3 >= 0 && b3 <= 1 ) {
		   a3 = alpha3( v, n );
		   // Cave3DVector vb3 = v1.plus( u3.times(b3) ).minus( v.plus( n.times(a3) ) );
		   // if ( vb3.length() > 0.001 ) {
		   //   Log.v("Cave3D", "Vdiff 3 " + vb3.x + " " + vb3.y + " " + vb3.z );
		   // }
		   lp2.copy( a3, s2, this, v.plus( n.times(a3) ) );
		   return true;
	    }
	}
	return false;
  }
  
  private float beta1 ( Cave3DVector v, Cave3DVector n ) { return beta( v, n, u1, v2 ); }
  private float beta2 ( Cave3DVector v, Cave3DVector n ) { return beta( v, n, u2, v1 ); }
  private float beta3 ( Cave3DVector v, Cave3DVector n ) { return beta( v, n, u3, v1 ); }
  
  private float alpha1 ( Cave3DVector v, Cave3DVector n ) { return alpha( v, n, u1, v2 ); }
  private float alpha2 ( Cave3DVector v, Cave3DVector n ) { return alpha( v, n, u2, v1 ); }
  private float alpha3 ( Cave3DVector v, Cave3DVector n ) { return alpha( v, n, u3, v1 ); }
  
  /** compute line param for the intersection point of
   * V + alpha N and VV + beta U
   * the equations are
   *    alpha N*N - beta U*N =  (VV-V)*N
   *   -alpha U*N + beta U*U = -(VV-V)*U
   * therefore
   *    alpha    U*U  U*N        (VV-V)*N
   *    beta  =  U*N  N*N times -(VV-V)*U divided (N*N * U*U - U*n * U*N)
   * @param v
   * @param n
   * @param u
   * @param vv
   * @return value of beta (if in [0,1] the intersection point is on the triangle side] 
   */
  private float beta( Cave3DVector v, Cave3DVector n, Cave3DVector u, Cave3DVector vv )
  {
    float nu = n.dot(u);
    float nn = n.dot(n);
    float uu = u.dot(u);
    float nv = n.dot( vv.minus(v) );
    float uv = u.dot( vv.minus(v) );
    return ( nu * nv - nn * uv ) / ( nn * uu - nu * nu );
  }
  
  private float alpha( Cave3DVector v, Cave3DVector n, Cave3DVector u, Cave3DVector vv )
  {
    float nu = n.dot(u);
    float nn = n.dot(n);
    float uu = u.dot(u);
    float nv = n.dot( vv.minus(v) );
    float uv = u.dot( vv.minus(v) );
    return ( uu * nv - nu * uv ) / ( nn * uu - nu * nu );
  }
  
  /** count number of vertices in the list
   * 
   * @param list  list of points
   * @param pts   vertices of the triangle in the list
   * @return number of vertices in the list
   */
  int countVertexIn( List<CWPoint> list, CWPoint[] pts )
  {
    int n = 0;
    if ( list.contains( v1 ) ) pts[n++] = v1;
    if ( list.contains( v2 ) ) pts[n++] = v2;
    if ( list.contains( v3 ) ) pts[n++] = v3;
    return n;
  }
  
	  
}
