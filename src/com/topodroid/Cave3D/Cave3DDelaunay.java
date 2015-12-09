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
  class DelaunayTriangle
  {
    int i, j, k; 
    int o; // orientation
    DelaunayTriangle( int i0, int j0, int k0, int o0 )
    {
      setVertices( i0, j0, k0, o0 );
    }

    void setVertices( int i0, int j0, int k0, int o0 )
    {
      i = i0;
      j = j0;
      k = k0;
      o = o0;
    }

    boolean contains( int n )
    { 
      return n == i || n == j || n == k;
    }
  }

  // -----------------------------------------------------------------
  int N;  // number of points
  int NT; // max nr of triangles
  int nt; // number of triangles
  Cave3DVector[] mPts;
  Cave3DVector[] mOrigPts;
  // ArrayList< DelaunayTriangle > mTri;
  DelaunayTriangle[] mTri;

  // For each DelaunayTriangle add a Cave3DTriangle into the array list
  // The Cave3DTriangle has vertex vectors the Cave3DStation + the original points 
  // of the DelaunayTriangle
  //
  void insertTrianglesIn( ArrayList<Cave3DTriangle> triangles, Cave3DStation st )
  {
    Cave3DVector p0 = new Cave3DVector( st.e, st.n, st.z );
    for ( int kt=0; kt<nt; ++kt ) {
      DelaunayTriangle t = mTri[kt];
      Cave3DVector v1 = p0.plus( mOrigPts[ t.i ] );
      Cave3DVector v2 = p0.plus( mOrigPts[ t.j ] );
      Cave3DVector v3 = p0.plus( mOrigPts[ t.k ] );
      Cave3DVector v0 = new Cave3DVector( v1.x+v2.x+v3.x, 
                                          v1.y+v2.y+v3.y,
                                          v1.z+v2.z+v3.z );
      Cave3DTriangle t0 = new Cave3DTriangle( v1, v2, v3 );
      if ( v0.dot( t0.normal ) < 0.0 ) {
        t0.flip();
      }
      triangles.add( t0 );
    }
  }

  public void dump()
  {
    dumpPoints();
    dumpTriangles(-1);
  }

  public void dumpPoints()
  {
    Log.v("Cave3D", "N. pts: " + N );
    for (int k = 0; k<N; ++k ) {
      Log.v("Cave3D", "["+k+"] " + mPts[k].x + " " + mPts[k].y + " " + mPts[k].z );
    }
  }

  public void dumpTriangles( int n ) 
  {
    Log.v("Cave3D", "Triangles " + n + " / " + nt );
    for (int kt=0; kt<nt; ++kt ) {
      DelaunayTriangle t = mTri[kt];
      // double d1 = arc_distance( mPts[t.i], mPts[t.j] );
      // double d2 = arc_distance( mPts[t.j], mPts[t.k] );
      // double d3 = arc_distance( mPts[t.k], mPts[t.i] );
      Log.v("Cave3D", "(" + ( (t.o>0)? '+':'-' ) + ") [" + t.i + " " + t.j + " " + t.k );
    }
  }

  // cross-product of two Cave3DVectors
  Cave3DVector cross_product( Cave3DVector p1, Cave3DVector p2 )
  {
    return new Cave3DVector( p1.y * p2.z - p1.z * p2.y,
                             p1.z * p2.x - p1.x * p2.z,
    			     p1.x * p2.y - p1.y * p2.x );
  }

  // dot-product of two Cave3DVectors
  double dot_product( Cave3DVector p1, Cave3DVector p2 )
  {
    return p1.x * p2.x + p1.y * p2.y + p1.z * p2.z;
  }

  // arc-distance = arccos of the dot-product ( range in [0, PI] )
  double arc_distance( Cave3DVector p1, Cave3DVector p2 )
  {
    double ca1 = dot_product( p1, p2 );
    return Math.acos( ca1 );
  }

  // triple-product of three Cave3DVectors
  double triple_product( Cave3DVector p1, Cave3DVector p2, Cave3DVector p3 )
  {
    return dot_product( cross_product( p1, p2 ), p3 );
  }

  // cosine of the spherical angle
  // double spherical_angle( Cave3DVector p1, Cave3DVector p2, Cave3DVector p3 )
  // {
  //   Cave3DVector p12 = cross_product( p1, p2 );
  //   Cave3DVector p13 = cross_product( p1, p3 );
  //   p12.normalized();
  //   p13.normalized();
  //   return dot_product( p12, p13 );
  // }

  // relations
  // V = nr of vertices
  // S = nr of sides = 3*V - 6
  // T = nr of faces = 2*V - 4 (nr of triangles)

  // orientation of a triplet of Cave3DVectors (i,j,k = indices of the vertices)
  int orientation( int i, int j, int k )
  {
    return ( triple_product( mPts[i], mPts[j], mPts[k] ) > 0 ) ? 1 : -1;
  }

  // add a delaunay triangle
  DelaunayTriangle addTriangle( int i, int j, int k )
  {
    int o = orientation(i, j, k);
    // Log.v( "Cave3D", "add triangle " + nt + ": " + i + " " + j + " " + k + " orientation " + o );
    mTri[nt].setVertices( i, j, k, o );
    ++nt;
    return mTri[ nt-1 ];
  }

  void removeTriangle( int kt )
  {
    for (++kt; kt<nt; ++kt ) {
          mTri[kt-1].setVertices( mTri[kt].i, mTri[kt].j, mTri[kt].k, mTri[kt].o );
    }
    --nt;
    mTri[nt].setVertices( -1, -1, -1, 1 );
  }

  // boolean removeTriangle( int i, int j, int k )
  // {
  //   for (int kt=0; kt<nt; ++kt ) {
  //     DelaunayTriangle t = mTri[kt];
  //     if (   ( t.i == i && t.j == j && t.k == k ) 
  //         || ( t.j == i && t.k == j && t.i == k ) 
  //         || ( t.k == i && t.i == j && t.j == k ) ) {
  //       for (++kt; kt<nt; ++kt ) {
  //         mTri[kt-1].setVertices( mTri[kt].i, mTri[kt].j, mTri[kt].k, mTri[k].o );
  //       }
  //       --nt;
  //       mTri[nt].setVertices( -1, -1, -1, 1 );
  //       return true;
  //     }
  //   }
  //   return false;
  // }

  // Cave3DDelaunay( List<Cave3DVector> pts )
  // {
  //   N = pts.size();
  //   NT = 2*N; //  - 4;
  //   mPts = new Cave3DVector[ N ];
  //   for ( int n=0; n<N; ++n ) {
  //     Cave3DVector p = pts.get( n );
  //     mPts[n] = new Cave3DVector( p.x, p.y, p.z );
  //     mPts[n].normalized();
  //   }
  //   // mTri = new ArrayList< DelaunayTriangle >();
  //   mTri = new DelaunayTriangle[NT];
  //   for (int kt=0; kt<NT; ++kt ) mTri[kt] = new DelaunayTriangle( -1, -1, -1, 1 );
  //   nt = 0;
  //   computeLawson();
  // }

  Cave3DDelaunay( Cave3DVector[] pts )
  {
    N = pts.length;
    NT = 2*N; //  - 4;
    mPts = new Cave3DVector[ N ];
    mOrigPts = pts;
    for ( int n=0; n<N; ++n ) {
      Cave3DVector p = pts[n];
      mPts[n] = new Cave3DVector( p.x, p.y, p.z );
      mPts[n].normalized();
    }
    // mTri = new ArrayList< DelaunayTriangle >();
    mTri = new DelaunayTriangle[NT];
    for (int kt=0; kt<NT; ++kt ) mTri[kt] = new DelaunayTriangle( -1, -1, -1, 1 );
    nt = 0;
    // Log.v("Cave3D", "Delaunay n.pt " + N + " n.tri " + NT );
    if ( N >= 4 ) {
      computeLawson();
    }
    // dump();
  }

  // DelaunayTriangle findTriangle( Cave3DVector p )
  int findTriangle( Cave3DVector p ) // return triangle index
  {
    int ret = -1;
    for (int kt=0; kt<nt; ++kt ) {
      DelaunayTriangle t = mTri[kt];
      Cave3DVector p1 = mPts[ t.i ];
      Cave3DVector p2 = mPts[ t.j ];
      Cave3DVector p3 = mPts[ t.k ];
      double s0 = triple_product( p1, p2, p3 );
      double s1 = triple_product( p1, p2, p );
      double s2 = triple_product( p2, p3, p );
      double s3 = triple_product( p3, p1, p );
      // System.out.printf("triangle [%c] %d %d %d %.2f products %.2f %.2f %.2f\n", 
      //   (t.o>0)? '+':'-', t.i, t.j, t.k, s0, s1, s2, s3 );
      // System.out.flush();
      if ( s0 > 0 ) {
        if ( s1 > 0 && s2 > 0 && s3 > 0 ) {
	  ret = kt;
        }
      } else {
        if ( ! ( s1 < 0 && s2 < 0 && s3 < 0 ) ) {
          ret = kt;
        }
      }
    }
    return ret;
  }

  // n is the third vertex of the triangle
  private void checkEdge( int n, DelaunayTriangle t1 )
  {
    int i = -1;
    int j = -1;
    int k = -1; 
    if ( t1.k == n )      { i = t1.i; j = t1.j; }
    else if ( t1.i == n ) { i = t1.j; j = t1.k; }
    else if ( t1.j == n ) { i = t1.k; j = t1.i; }
    else { return; }
    // System.out.printf("checkEdge %d tri %d %d %d --> i %d j %d \n", n, t1.i, t1.j, t1.k, i, j );

    // find opposite triangle
    DelaunayTriangle t2 = null;
    for ( int kt=0; kt<nt; ++kt ) {
      DelaunayTriangle t = mTri[kt];
      if ( t != t1 ) {
        if ( t.i == j && t.j == i ) { // opposite is t.k
          t2 = t;
          k = t.k;
          break;
        } else if ( t.j == j && t.k == i ) { // opposite is t.i
          t2 = t;
          k = t.i;
          break;
        } else if ( t.k == j && t.i == i ) { // opposite is t.j
          t2 = t;
          k = t.j;
          break;
        }
      }
    }
    if ( t2 == null || k == -1 ) { // !!!!
      Log.e( "Cave3D", "ERROR checkEdge no triangle for " + n );
    } else {
      // check point k is not connected to n
      // System.out.printf(" opposite triangle %d %d %d\n", t2.i, t2.j, t2.k );
      int kt1=0;
      for ( ; kt1<nt; ++kt1 ) {
        if ( mTri[kt1].contains( n ) && mTri[kt1].contains( k ) ) break;
      }
      if ( kt1 == nt ) {
        // System.out.printf(" opposite triangle ok \n");
        if ( k != n && dot_product( mPts[n], mPts[k] ) > dot_product( mPts[i], mPts[j] ) ) {
          // System.out.printf(" invert triangles %d %d %d %d \n", i, j, k, n );
          int o2 = orientation( k, j, n );
          int o1 = orientation( i, k, n );
          t2.setVertices( k, j, n, o2 );
          t1.setVertices( i, k, n, o1 );
          checkEdge( n, t1 );
          checkEdge( n, t2 );
        }
      } else {
        // System.out.printf("  triangle %d %d %d contains k %d and n %d\n",
        //   mTri[kt1].i, mTri[kt1].j, mTri[kt1].k, k, n );
      }
    }
  }


/*
  // k3 opposite point
  private int findClosestPoint( int k1, int k2, int k3 )
  {
    int k0 = -1;
    double d0 = 0;
    double d;
    Cave3DVector p10 = mPts[k1];
    Cave3DVector p20 = mPts[k2];
    for ( int k=0; k<N; ++k ) {
      if ( k == k1 || k == k2 || k == k3 ) continue;
      d = spherical_angle( mPts[k], p10, p20 );
      if ( d > d0 ) {
        k0 = k;
	d0 = d;
      }
    }
    return k0;
  }

  private boolean hasTriangle( int k1, int k2, int k3 )
  {
    for (int kt=0; kt<nt; ++kt ) {
      DelaunayTriangle t = mTri[kt];
      if ( k1 == t.i && k2 == t.j && k3 == t.k ) return true;
      if ( k2 == t.i && k3 == t.j && k1 == t.k ) return true;
      if ( k3 == t.i && k1 == t.j && k2 == t.k ) return true;
    }
    return false;
  }

  private void computeGiftWrapping()
  {
    if ( N < 3 ) return;
    int k1, k2;
    int k10 = 0;
    int k20 = 1;
    double d;
    double d0 = dot_product( mPts[k10], mPts[k20] ); // closest points means d0 max
    for ( k1 = 0; k1 < N; ++ k1 ) {
      for ( k2 = k1+1; k2 < N; ++ k2 ) {
        d = dot_product( mPts[k1], mPts[k2] );
	if ( d > d0 ) {
	  k10 = k1;
	  k20 = k2;
	  d0  = d;
	}
      }
    }
    d0 = 0; // spherical angle 
    Cave3DVector p10 = mPts[k10];
    Cave3DVector p20 = mPts[k20];
    int k30 = 0;
    for ( k1 = 0; k1 < N; ++ k1 ) {
      if ( k1 == k10 || k1 == k20 ) continue;
      d = spherical_angle( mPts[k1], p10, p20 );
      if ( d > d0 ) {
        k30 = k1;
	d0  = d;
      }
    }
    Cave3DVector p30 = mPts[k30];
    if ( triple_product( p10, p20, p30 ) > 0 ) {
      addTriangle( k10, k20, k30 ) );
    } else {
      addTriangle( k20, k10, k30 ) );
    }
    int n_tri = 0; // nr of triangles checked
    while ( n_tri < NT ) {
      DelaunayTriangle t = mTri[ n_tri ];
      p10 = mPts[ t.i ];
      p20 = mPts[ t.j ];
      p30 = mPts[ t.k ];

      Cave3DVector p;
      int k = findClosestPoint( t.i, t.j, t.k );
      if ( k >= 0 ) {
        p = mPts[k];
        if ( triple_product( p10, p20, p ) > 0 ) {
	  if ( ! hasTriangle( k10, k20, k ) ) {
            addTriangle( k10, k20, k ) );
	  }
        } else {
	  if ( ! hasTriangle( k20, k10, k ) ) {
            addTriangle( k20, k10, k ) );
	  }
        }
      }
      k = findClosestPoint( t.j, t.k, t.i );
      if ( k >= 0 ) {
        p = mPts[k];
        if ( triple_product( p20, p30, p ) > 0 ) {
	  if ( ! hasTriangle( k20, k30, k ) ) {
            addTriangle( k20, k30, k ) );
	  }
        } else {
	  if ( ! hasTriangle( k30, k20, k ) ) {
            addTriangle( k30, k20, k ) );
	  }
        }
      }
      k = findClosestPoint( t.k, t.i, t.j );
      if ( k >= 0 ) {
        p = mPts[k];
        if ( triple_product( p30, p10, p ) > 0 ) {
	  if ( ! hasTriangle( k30, k10, k ) ) {
            addTriangle( k30, k10, k ) );
	  }
        } else {
	  if ( ! hasTriangle( k10, k30, k ) ) {
            addTriangle( k10, k30, k ) );
	  }
        }
      }
    }
  }
*/

  private void computeLawson()
  {
    if ( N < 3 ) return;
    addTriangle(0,1,2);
    addTriangle(0,2,1);
    // dumpTriangles( 2 );
    for ( int n=3; n<N; ++n ) {
      int kt = findTriangle( mPts[n] );
      if ( kt == -1 ) { // !!! 
        Log.e( "Cave3D", "null triangle for point " + n );
      } else { // split triangle
        int i = mTri[kt].i;
        int j = mTri[kt].j;
        int k = mTri[kt].k;
        // System.out.printf( "point %d falls inside %d %d %d\n", n, i, j, k );
        // removeTriangle( i, j, k );
        removeTriangle( kt );
        DelaunayTriangle t1 = addTriangle( i, j, n );
	DelaunayTriangle t2 = addTriangle( j, k, n );
	DelaunayTriangle t3 = addTriangle( k, i, n );
        // dumpTriangles( -3 );
	checkEdge( n, t1 );
        // dumpTriangles( -2 );
	checkEdge( n, t2 );
        // dumpTriangles( -1 );
	checkEdge( n, t3 );
        // dumpTriangles( n );
      }
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
      
  
