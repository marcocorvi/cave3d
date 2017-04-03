/* @file Cave3DDelaunay.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D Delaunay-based triangulation
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

//  import android.util.FloatMath;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

import android.util.Log;

class Cave3DDelaunay
{
  class DelaunayPoint
  {
    int index; // debug
    Cave3DVector orig;
    Cave3DVector v;
    boolean used;
 
    DelaunayPoint( Cave3DVector vv, int kk ) 
    {
      orig  = vv;
      index = kk;
      v = new Cave3DVector( vv.x, vv.y, vv.z );
      v.normalized();
      used = false;
    }

    float distance( DelaunayPoint p ) { return orig.distance( p.orig ); }
  }

  class DelaunaySide // half-side
  {
    DelaunayPoint    p1, p2;
    DelaunaySide     otherHalf;
    DelaunayTriangle t;
    Cave3DVector     n; // normal
    // Cave3DVector     u; // unit versor of orig's
    // double           angle;
    // float            length; // distance between original points

    DelaunaySide( DelaunayPoint i0, DelaunayPoint j0 )
    {
      p1=i0;
      p2=j0;
      n = p1.v.cross( p2.v );
      n.normalized();
      // u = p2.v.minus( p1.v );
      // u.normalized();
      t = null;
      // angle = arc_angle( p1, p2 );
      // length = p1.distance( p2 );
    }

    boolean coincide( DelaunayPoint i0, DelaunayPoint j0 ) 
    {
      return ( p1==i0 && p2==j0 );
    }

    boolean opposite( DelaunayPoint i0, DelaunayPoint j0 )
    {
      return ( p2==i0 && p1==j0 );
    }

    boolean isPositive( Cave3DVector v ) { return n.dot( v ) >= 0; }

    boolean isNegative( Cave3DVector v ) { return n.dot( v ) <= 0; }

    boolean contains( Cave3DVector v, double eps ) { return Math.abs( n.dot( v ) ) < eps; } 

    float dot( Cave3DVector v ) { return n.dot( v ); }
  }

  void setOpposite( DelaunaySide s1, DelaunaySide s2 )
  {
    s1.otherHalf = s2;
    s2.otherHalf = s1;
  }

  class DelaunayTriangle
  {
    DelaunaySide s1, s2, s3;
    int   sign;
    float radius;
    Cave3DVector center;
    Cave3DVector excenter;
    float exradius;

    DelaunayTriangle( DelaunaySide i0, DelaunaySide j0, DelaunaySide k0 )
    {
      s1 = i0;
      s2 = j0;
      s3 = k0;
      Cave3DVector v1 = s1.p1.v;
      Cave3DVector v2 = s2.p1.v.minus( v1 );
      Cave3DVector v3 = s3.p1.v.minus( v1 );
      Cave3DVector v0 = v1.plus( s2.p1.v ).plus( s3.p1.v );
      center = v2.cross( v3 );
      center.normalized();
      sign = ( center.dot(v0) > 0 )? -1 : +1;
      radius = (float)Math.abs( center.dot( v1 ));
      excenter = s1.p1.orig.plus( s2.p1.orig ).plus( s3.p1.orig );
      excenter.mul( 1.0f/3.0f );
      exradius = excenter.distance( s1.p1.orig );
    }

    DelaunayTriangle( DelaunayTriangle t )
    {
      s1 = t.s1;
      s2 = t.s2;
      s3 = t.s3;
      sign   = t.sign;
      radius = t.radius;
      center = t.center;
    }

    boolean contains( DelaunayPoint p ) { return contains( p.v ); }

    boolean contains( Cave3DVector v )
    { 
      if ( sign > 0 ) {
        return s1.isPositive( v ) && s2.isPositive( v ) && s3.isPositive( v );
      } // else {
      return s1.isNegative( v ) || s2.isNegative( v ) || s3.isNegative( v );
    }

    DelaunayPoint vertexOf( DelaunaySide s )
    { 
      if ( s == s1 ) return s2.p2;
      if ( s == s2 ) return s3.p2;
      if ( s == s3 ) return s1.p2;
      return null;
    }

    DelaunaySide next( DelaunaySide s )
    { 
      if ( s == s1 ) return s2;
      if ( s == s2 ) return s3;
      if ( s == s3 ) return s1;
      return null;
    }

    DelaunaySide prev( DelaunaySide s )
    { 
      if ( s == s1 ) return s3;
      if ( s == s2 ) return s1;
      if ( s == s3 ) return s2;
      return null;
    }

  }

  // -----------------------------------------------------------------
  int N;  // number of points
  DelaunayPoint[] mPts;
  ArrayList< DelaunayTriangle > mTri;
  ArrayList< DelaunaySide > mSide;
  // float[] mDist; // precomputed arc-distances between points

  // For each DelaunayTriangle add a Cave3DTriangle into the array list
  // The Cave3DTriangle has vertex vectors the Cave3DStation + the original points 
  // of the DelaunayTriangle
  // @param triangles   array of triangles
  // @param st          base station (e,n,z)
  //
  void insertTrianglesIn( ArrayList<Cave3DTriangle> triangles, Cave3DStation st )
  {
    Cave3DVector p0 = new Cave3DVector( st.e, st.n, st.z );
    for ( DelaunayTriangle t : mTri ) {
      if ( t.s1.p1.orig != null && t.s2.p1.orig != null && t.s3.p1.orig != null ) {
        Cave3DVector v1 = p0.plus( t.s1.p1.orig );
        Cave3DVector v2 = p0.plus( t.s2.p1.orig );
        Cave3DVector v3 = p0.plus( t.s3.p1.orig );
        Cave3DTriangle t0 = new Cave3DTriangle( v1, v2, v3 );
        triangles.add( t0 );
      }
    }
  }

  // cross-product of two Cave3DVectors
  // Cave3DVector cross_product( Cave3DVector p1, Cave3DVector p2 )
  // {
  //   return new Cave3DVector( p1.y * p2.z - p1.z * p2.y,
  //                            p1.z * p2.x - p1.x * p2.z,
  //   			     p1.x * p2.y - p1.y * p2.x );
  // }

  // dot-product of two Cave3DVectors
  double dot_product( Cave3DVector v1, Cave3DVector v2 ) { return v1.dot( v2 ); }

  // arc-angle = ( range in [0, 2] )
  double arc_angle( DelaunayPoint p1, DelaunayPoint p2 ) { return 1 - p1.v.dot( p2.v ); }
  
  // arc-distance = arccos of the dot-product ( range in [0, PI] )
  float arc_distance( DelaunayPoint p1, DelaunayPoint p2 ) { return (float)( Math.acos( p1.v.dot( p2.v ) ) ); }
  float arc_distance( Cave3DVector v1, Cave3DVector v2 ) { return (float)( Math.acos( v1.dot( v2 ) ) ); }

  float distance( Cave3DVector v1, Cave3DVector v2 ) { return v1.distance( v2 ); }

  // triple-product of three Cave3DVectors
  // double triple_product( Cave3DVector p1, Cave3DVector p2, Cave3DVector p3 ) { return p1.cross( p2 ).dot( p3 ); }

  // add a delaunay triangle
  void addTriangle( DelaunaySide s1, DelaunaySide s2, DelaunaySide s3 )
  {
    DelaunayTriangle tri = new DelaunayTriangle( s1, s2, s3 );
    mTri.add( tri );
    s1.t = tri;
    s2.t = tri;
    s3.t = tri;
  }

  DelaunaySide addSide( DelaunayPoint p1, DelaunayPoint p2 ) 
  {
    DelaunaySide side = new DelaunaySide( p1, p2 );
    mSide.add( side );
    return side;
  }

  Cave3DDelaunay( Cave3DVector[] pts )
  {
    float sqrt2 = 1/(float)(Math.sqrt(2.0));
    N = pts.length;
    mPts = new DelaunayPoint[ N ]; // delaunay points on the unit sphere
    // mDist = new float[ N*N ];
    for ( int n=0; n<N; ++n ) {
      mPts[n] = new DelaunayPoint( pts[n], n );
      // for ( int n2=0; n2<n; ++n2 ) {
      //   mDist[ n*N + n2 ] = mDist[ n2*N + n ] = arc_distance( mPts[n], mPts[n2] );
      // }
      // mDist[ n*N + n ] = 0;
    }


    // prepare null-initialized triangles
    mTri  = new ArrayList<DelaunayTriangle>();
    mSide = new ArrayList<DelaunaySide>();

    if ( N >= 4 ) {
      computeLawson( 0.0001 );
    }
    // dump();
  }

  // @param p   input point (on the sphere)
  // @return triangles the point lies above
  // @note   SIDE EFFECT these triangles are also removed from the array of triangles
  //
  DelaunaySide findSide( Cave3DVector v, double eps )
  {
    for ( DelaunaySide side : mSide ) {
      if ( side.contains( v, eps ) ) return side;
    }
    return null;
  }

  DelaunayTriangle findTriangle( Cave3DVector v )
  {
    for ( DelaunayTriangle tri : mTri ) {
      if ( tri.contains( v ) ) return tri;
    }
    return null;
  }

  private void handle( DelaunaySide s0, DelaunayPoint p0 )
  {
    DelaunayTriangle t0 = s0.t;
    DelaunaySide sh = s0.otherHalf;
    DelaunayTriangle th = sh.t;
    DelaunayPoint ph = th.vertexOf( sh );
    DelaunaySide sh2 = th.prev( sh );
    DelaunaySide sh1 = th.next( sh ); 
    DelaunaySide s02 = t0.prev( s0 );  
    DelaunaySide s01 = t0.next( s0 );   
    if ( arc_distance( ph.v, t0.center ) < t0.radius ) { 
    // if ( distance( ph.orig, t0.excenter ) < t0.exradius ) {
    // float aext = sh1.u.dot( sh2.u ) + s01.u.dot( s02.u );
    // float aint = sh2.u.dot( s01.u ) + s02.u.dot( sh1.u );
    // if ( aext < aint ) {
      DelaunaySide pph = addSide( p0, ph );    
      DelaunaySide php = addSide( ph, p0 );    
      setOpposite( pph, php );               
      mTri.remove( th );                    
      mTri.remove( t0 );
      mSide.remove( s0 );
      mSide.remove( sh );
      addTriangle( sh2, s01, pph ); 
      addTriangle( sh1, php, s02 );
      // Log.v("Cave3D", "add two T " + sh2.p1.k + " " + s0p2p.p1.k + " " + pph.p1.k 
      //      + "   " + sh1.p1.k + " " + php.p1.k + " " + ps0p1.p1.k );
      handle( sh2, p0 );
      handle( sh1, p0 );
    }
  
  }

  boolean checkConsistency()
  {
    for ( DelaunaySide s0 : mSide ) {
      DelaunaySide sh = s0.otherHalf;
      if ( sh == null ) {
        Log.v("Cave3D", "MISSING opposite sides of " + s0.p1.index + "-" + s0.p2.index );
        return false; 
      }
      if ( s0.p1 != sh.p2 || s0.p2 != sh.p1 ) {
        Log.v("Cave3D", "BAD opposite sides S0 " + s0.p1.index + "-" + s0.p2.index + " SH " + sh.p1.index + "-" + sh.p2.index );
        return false; 
      }
      DelaunayTriangle t0 = s0.t;
      if ( t0 == null ) {
        Log.v("Cave3D", "MISSING triangle" );
        return false; 
      }
      if ( t0.s1 != s0 && t0.s2 != s0 && t0.s3 != s0 ) {
        Log.v("Cave3D", "Bad triangle" );
        return false; 
      }
    }
    for ( DelaunayTriangle t : mTri ) {
      if ( t.s1.p2 != t.s2.p1 ) {
        Log.v("Cave3D", "MISMATCH 1-2 " + t.s1.p2.index + " " + t.s2.p1.index );
        return false;
      }
      if ( t.s2.p2 != t.s3.p1 ) {
        Log.v("Cave3D", "MISMATCH 2-3 " + t.s2.p2.index + " " + t.s3.p1.index );
        return false;
      }
      if ( t.s3.p2 != t.s1.p1 ) {
        Log.v("Cave3D", "MISMATCH 3-1 " + t.s3.p2.index + " " + t.s1.p1.index );
        return false;
      }
    }
    return true;
  }

  private void handleTriangle( DelaunayTriangle tri, DelaunayPoint p ) 
  {
    DelaunaySide s1 = tri.s1;
    DelaunaySide s2 = tri.s2;
    DelaunaySide s3 = tri.s3;
    mTri.remove( tri );
    DelaunaySide s1p2p = addSide( s1.p2, p ); //   s1.p2 <------------ s1.p1
    DelaunaySide ps1p1 = addSide( p, s1.p1 ); //   s2.p1 ====>   ====> s3.p2
    DelaunaySide s2p2p = addSide( s2.p2, p ); //       \       p       ^ 
    DelaunaySide ps2p1 = addSide( p, s2.p1 ); //         \    ^ |     /
    DelaunaySide s3p2p = addSide( s3.p2, p ); //           v  | v   / 
    DelaunaySide ps3p1 = addSide( p, s3.p1 ); //          s2.p2 s3.p1
    setOpposite( s1p2p, ps2p1 );   
    setOpposite( s2p2p, ps3p1 );  
    setOpposite( s3p2p, ps1p1 ); 
    addTriangle( s1, s1p2p, ps1p1 );
    addTriangle( s2, s2p2p, ps2p1 );
    addTriangle( s3, s3p2p, ps3p1 );
    handle( s1, p );
    handle( s2, p );
    handle( s3, p );
  }

  private void handleSide( DelaunaySide s0, DelaunayPoint p )
  {
    DelaunaySide sh = s0.otherHalf;
    DelaunayTriangle t0 = s0.t;
    DelaunayTriangle th = sh.t;
    DelaunayPoint p0 = t0.vertexOf( s0 );
    DelaunayPoint ph = th.vertexOf( sh );

    DelaunaySide pp0   = addSide( p, p0 );
    DelaunaySide p0p   = addSide( p0, p );
    setOpposite( pp0, p0p );
    DelaunaySide pph   = addSide( p, ph );
    DelaunaySide php   = addSide( ph, p );
    setOpposite( pph, php );
    DelaunaySide s0p1p = addSide( s0.p1, p     ); 
    DelaunaySide ps0p2 = addSide( p,     s0.p2 ); 
    DelaunaySide shp1p = addSide( sh.p1, p     ); 
    DelaunaySide pshp2 = addSide( p,     sh.p2 ); 
    setOpposite( s0p1p, pshp2 );
    setOpposite( ps0p2, shp1p );

    DelaunaySide t0next = t0.next( s0 ); 
    DelaunaySide t0prev = t0.prev( s0 ); 
    DelaunaySide thnext = th.next( sh ); 
    DelaunaySide thprev = th.prev( sh ); 
    // remove t0, th, 
    // remove s0, sh
    mTri.remove( t0 );
    mTri.remove( th );
    mSide.remove( s0 );
    mSide.remove( sh );
    // insert four triangles
    addTriangle( t0prev, s0p1p, pp0 );
    addTriangle( t0next, p0p,   ps0p2 );
    addTriangle( thprev, shp1p, pph );
    addTriangle( thnext, php,   pshp2 );
  }
    
  // n is the third vertex of the triangle

  private void computeLawson( double eps )
  {
    // Log.v("Cave3D", "compute Lawson N pts " + N );
    if ( N < 4 ) return;
    // decide whether 0,1,2,3 is right-handed
    DelaunayPoint[] pp = new DelaunayPoint[4];
    for ( int k = 0; k<4;  ) {
      int n = (int)(N*Math.random());
      if ( ! mPts[n].used ) {
        mPts[n].used = true;
        pp[k] = mPts[n];
        ++k;
      }
    }
    DelaunaySide s01 = addSide( pp[0], pp[1] );
    DelaunaySide s10 = addSide( pp[1], pp[0] );
    DelaunaySide s12 = addSide( pp[1], pp[2] );
    DelaunaySide s21 = addSide( pp[2], pp[1] );
    DelaunaySide s20 = addSide( pp[2], pp[0] );
    DelaunaySide s02 = addSide( pp[0], pp[2] );
    DelaunaySide s03 = addSide( pp[0], pp[3] );
    DelaunaySide s30 = addSide( pp[3], pp[0] );
    DelaunaySide s13 = addSide( pp[1], pp[3] );
    DelaunaySide s31 = addSide( pp[3], pp[1] );
    DelaunaySide s23 = addSide( pp[2], pp[3] );
    DelaunaySide s32 = addSide( pp[3], pp[2] );
    setOpposite( s01, s10 );
    setOpposite( s02, s20 );
    setOpposite( s03, s30 );
    setOpposite( s12, s21 );
    setOpposite( s13, s31 );
    setOpposite( s23, s32 );

    Cave3DVector v0 = pp[0].v;
    Cave3DVector v1 = pp[1].v.minus( v0 );
    Cave3DVector v2 = pp[2].v.minus( v0 );
    Cave3DVector v3 = pp[3].v.minus( v0 );
    double d = v1.cross(v2).dot( v3 );
    if ( d < 0 ) {
      addTriangle( s01, s12, s20 ); //          0
      addTriangle( s03, s31, s10 ); //          |
      addTriangle( s13, s32, s21 ); //          3
      addTriangle( s02, s23, s30 ); //     2          1
    } else {
      addTriangle( s02, s21, s10 ); //          0
      addTriangle( s03, s32, s20 ); //          |
      addTriangle( s23, s31, s12 ); //          3
      addTriangle( s01, s13, s30 ); //     1          2
    }
    // dumpTriangles( 2 );
    // Log.v("Cave3D", "start with volume " + d + " consistency " + checkConsistency() );

    int kmax = N/2;
    for ( int k=4; k<kmax; ++k ) {
      int n = (int)(N*Math.random());
      while ( mPts[n].used ) { n = (int)(N*Math.random()); }
      mPts[n].used = true;
      DelaunayPoint p = mPts[n];
      Cave3DVector v  = p.v;
      DelaunaySide s0 = findSide( v, eps );
      if ( s0 == null ) {
        DelaunayTriangle tri = findTriangle( v );
        if ( tri == null ) {
          Log.v("Cave3D", "V on no triangle. " + v.x + " " + v.y + " " + v.z + " S " + mSide.size() + " T " + mTri.size() );
          return;
        }
        // Log.v("Cave3D", "K " + k + " Point " + p.index + " in T " + tri.s1.p1.k + " " + tri.s2.p1.k + " " + tri.s3.p1.k );
        handleTriangle( tri, p );
      } else {
        handleSide( s0, p );
      }
      // Log.v("Cave3D", "point " + n + "/" + N + " S " + mSide.size() + " T " + mTri.size() 
      //       + " consistency " + checkConsistency() );
    }
    for ( int n=0; n<N; ++n ) {
      if ( mPts[n].used ) continue;
      mPts[n].used = true;
      DelaunayPoint p = mPts[n];
      Cave3DVector v  = p.v;
      DelaunaySide s0 = findSide( v, eps );
      if ( s0 == null ) {
        DelaunayTriangle tri = findTriangle( v );
        if ( tri == null ) {
          Log.v("Cave3D", "V on no triangle. " + v.x + " " + v.y + " " + v.z + " S " + mSide.size() + " T " + mTri.size() );
          return;
        }
        // Log.v("Cave3D", "N " + n + " Point " + p.index + " in T " + tri.s1.p1.k + " " + tri.s2.p1.k + " " + tri.s3.p1.k );
        handleTriangle( tri, p );
      } else {
        handleSide( s0, p );
      }
      // Log.v("Cave3D", "point " + n + "/" + N + " S " + mSide.size() + " T " + mTri.size() 
      //       + " consistency " + checkConsistency() );
    }

  }

// ------------------------------------------------------------------
// sample main
//
  // public static void main(String [] args)
  // {
  //   ArrayList<Cave3DVector> vec = new ArrayList<Cave3DVector>();

  //   vec.add( new Cave3DVector(  0,  1.1f, 0 ) );
  //   vec.add( new Cave3DVector(  0, -0.9f, -0.1f ) );
  //   vec.add( new Cave3DVector(  1.1f,  0.1f, 0 ) );
  //   vec.add( new Cave3DVector( -1.2f,  0.0f, 0.1f ) );
  //   vec.add( new Cave3DVector(  0.1f,  0.5f, -1.1f) );
  //   vec.add( new Cave3DVector( -0.4f,  0.2f, 1 ) );
  //   // vec.add( new Cave3DVector(  0.5f,  0.4f,  0.3f ) );
  //   // vec.add( new Cave3DVector(  0.5f, -0.6f,  0.6f ) );
  //   // vec.add( new Cave3DVector(  0.6f,  0.5f, -0.6f ) );
  //   // vec.add( new Cave3DVector( -0.3f,  0.5f, -0.8f ) );

  //   Cave3DDelaunay delaunay = new Cave3DDelaunay( vec );

  //   delaunay.dump();

  // }

}
      
  
