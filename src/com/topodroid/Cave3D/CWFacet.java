/** @file CWFacet.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief triangular facet (STL)
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.util.List;
import java.io.PrintWriter;
// import java.io.PrintStream;
// import java.io.IOException;

import android.util.Log;

public class CWFacet
{
  CWPoint v1, v2, v3;
  protected Cave3DVector u;  // (v2-v1)x(v3-v1): U points "inside"
  Cave3DVector un; // u normalized
  protected float u22, u23, u33; // u2*u2 / det, ... etc
  Cave3DVector u2; // v2-v1 
  Cave3DVector u3; // v3-v1
  Cave3DVector u1; // v3-v2
  
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
  
  public CWFacet( CWPoint v1, CWPoint v2, CWPoint v3 )
  {
    buildFacet( v1, v2, v3 );
  }
  
  public CWFacet( int tag, CWPoint v1, CWPoint v2, CWPoint v3 )
  {
    buildFacet( v1, v2, v3 );
  }
  
  protected void buildFacet( CWPoint v1, CWPoint v2, CWPoint v3 )
  {
    this.v1 = v1;
    this.v2 = v2;
    this.v3 = v3;
    computeVectors();
  }

  void computeVectors()
  {
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
  }

  // void serialize( PrintWriter out )
  // {
  //   out.format( "F %.3f %.3f %.3f %.3f %.3f %.3f %.3f %.3f %.3f %.3f %.3f %.3f\n",
  //               un.x, un.y, un.z, v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, v3.x, v3.y, v3.z );
  // }

  
  /* returns true if P is a vertex of the facet
   */
  boolean contains( CWPoint p ) { return p == v1 || p == v2 || p == v3; } 
  
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
  
  /** compute the area of the facet
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
	  
  /** computes the projection of V0 in the plane of the triangle
   * and checks if it lies inside the triangle
   * @param v0   vector 
   * @note v0 already reduced to v1.
   * @return true if the projection of v0 falls inside the triangle
   */
  protected boolean isProjectionInside( Cave3DVector v0 )
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
  
  /** get an intersection point with another facet
   *  the intersection line is P + alpha (N1 ^ N2)
   * @param t the other triangle
   * @return an intersection point 
   */
  Cave3DVector intersectionBasepoint( CWFacet t )
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
  
  Cave3DVector intersectionDirection( CWFacet t ) { return un.cross( t.un ); }
  
  protected float beta1 ( Cave3DVector v, Cave3DVector n ) { return beta( n, u1, v2.minus(v) ); }
  protected float beta2 ( Cave3DVector v, Cave3DVector n ) { return beta( n, u2, v1.minus(v) ); }
  protected float beta3 ( Cave3DVector v, Cave3DVector n ) { return beta( n, u3, v1.minus(v) ); }
  
  protected float alpha1 ( Cave3DVector v, Cave3DVector n ) { return alpha( n, u1, v2.minus(v) ); }
  protected float alpha2 ( Cave3DVector v, Cave3DVector n ) { return alpha( n, u2, v1.minus(v) ); }
  protected float alpha3 ( Cave3DVector v, Cave3DVector n ) { return alpha( n, u3, v1.minus(v) ); }
  
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
  private float beta( Cave3DVector n, Cave3DVector u, Cave3DVector vv )
  {
    float nu = n.dot(u);
    float nn = n.dot(n);
    float uu = u.dot(u);
    float nv = n.dot( vv );
    float uv = u.dot( vv );
    return ( nu * nv - nn * uv ) / ( nn * uu - nu * nu );
  }
  
  private float alpha( Cave3DVector n, Cave3DVector u, Cave3DVector vv )
  {
    float nu = n.dot(u);
    float nn = n.dot(n);
    float uu = u.dot(u);
    float nv = n.dot( vv );
    float uv = u.dot( vv );
    return ( uu * nv - nu * uv ) / ( nn * uu - nu * nu );
  }
  
}
