/** @file Cave3DHull.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief convex hull of the 2D projs of splays on the plane normal to the shot
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.util.ArrayList;

import android.util.Log;

class Cave3DHull
{
  Cave3DShot    shot;    
  Cave3DStation mStationFrom;  // base station
  Cave3DStation mStationTo;
  Cave3DVector  normal;   // normal to the plane (unit vector along the shot)
  Cave3DVector  center;   // hull center (in the plane)
  ArrayList< Cave3DShot > rays1;  
  ArrayList< Cave3DShot > rays2;  
  ArrayList< Cave3DTriangle > triangles;
  ArrayList< Cave3DProjection > projs1;
  ArrayList< Cave3DProjection > projs2;

  /** get the size of the projections
   * @param k    proj index: 0 at FROM, 1 at TO
   */
  int size( int k ) { return (k==0)? projs1.size() : projs2.size(); }

  Cave3DHull( Cave3DShot sh,                   // shot
              ArrayList< Cave3DShot > splays1, // splays at FROM station
              ArrayList< Cave3DShot > splays2, // splays at TO station
              Cave3DStation sf,                // shot FROM station
              Cave3DStation st )               // shot TO station
  {
    // Log.v( "Cave3D", "Hull at station " + st.name + " shot " + sh.from_station.name + "-" + sh.to_station.name );
    mStationFrom = sf;
    mStationTo   = st;
    shot    = sh;
    normal = shot.toCave3DVector();
    normal.normalized();
    rays1 = splays1;
    rays2 = splays2;
    projs1 = new ArrayList< Cave3DProjection >();
    projs2 = new ArrayList< Cave3DProjection >();
    computeHull();
  }

  void dumpHull()
  {
    int s1 = projs1.size();
    int s2 = projs2.size();
    Log.v( "Cave3D", "Hull at station " + mStationFrom.name + " size " + s1 + " " + mStationTo.name + " " + s2 );
    // for (int k=0; k<s1; ++k ) {
    //   Cave3DProjection p = projs1.get(k);
    //   Log.v( "Cave3D", k + ": " + p.angle + " - " + p.proj.x + " " + p.proj.y + " " + p.proj.z );
    // }
    // for (int k=0; k<s2; ++k ) {
    //   Cave3DProjection p = projs2.get(k);
    //   Log.v( "Cave3D", k + ": " + p.angle + " - " + p.proj.x + " " + p.proj.y + " " + p.proj.z );
    // }
  }

  private void addTriangle( Cave3DProjection p1, Cave3DProjection p2, Cave3DProjection p3 )
  {
    triangles.add( new Cave3DTriangle( p1.vector, p2.vector, p3.vector ) );
  }

  void makeTriangles()
  {
    triangles = new ArrayList< Cave3DTriangle >();
    int s1 = projs1.size();
    int s2 = projs2.size();
    if ( s1 == 0 || s2 == 0 ) return;
    if ( s1 == 1 && s2 == 1 ) return;
    int k1 = 0;
    int k2 = 0;
    Cave3DProjection p1 = projs1.get(0);
    Cave3DProjection p2 = projs2.get(0);
    while ( k1 < s1 || k2 < s2 ) {
      if ( k1 == s1 ) {  // next point on projs2
        k2 ++;
        Cave3DProjection q2 = projs2.get( k2%s2 );
        addTriangle( p1, p2, q2 );
        p2 = q2;
      } else if ( k2 == s2 ) { // next point on projs1
        k1 ++;
        Cave3DProjection q1 = projs1.get( k1%s1 );
        addTriangle( p1, p2, q1);
        p1 = q1;
      } else { // must choose
        Cave3DProjection q1 = projs1.get( (k1+1)%s1 );
        Cave3DProjection q2 = projs2.get( (k2+1)%s2 );
        if ( q1.angle < q2.angle ) {
          k1++;
          addTriangle( p1, p2, q1 );
          p1 = q1;
        } else {
          k2++;
          addTriangle( p1, p2, q2 );
          p2 = q2;
        }
      }
    }
  }

  /** make triangles from the HULL to a vertex
   * @param vertex   vertex
   */
  void makeTriangles( Cave3DVector vertex )
  {
    triangles = new ArrayList< Cave3DTriangle >();
    int s1 = projs1.size();
    if ( s1 < 2 ) return;
    // Log.v( "Cave3D", "Triangles at " + mStationFrom.name + " with vertex. Nr triangles " + s1 );
    for ( int k=0; k<s1; ++k ) {
      Cave3DProjection p1 = projs1.get(k);
      Cave3DProjection p2 = projs1.get((k+1)%s1);
      Cave3DVector v1 = vertex.plus( p1.proj );
      Cave3DVector v2 = vertex.plus( p2.proj );
      triangles.add( new Cave3DTriangle( p1.vector, p2.vector, v2 ) );
      triangles.add( new Cave3DTriangle( p1.vector, v2, v1 ) );
    }
  }

  private void computeHull()
  {
    // Log.v( "Cave3D", "compute Hull : splays " + rays1.size() + " " + rays2.size() );

    computeHullProjs( rays1, projs1, mStationFrom );
    computeHullProjs( rays2, projs2, mStationTo );
    // Log.v( "Cave3D", "compute Hull [1]: projs " + projs1.size() + " " + projs2.size() );

    Cave3DVector p0 = null;
    if ( projs1.size() > 1 ) {
      p0 = new Cave3DVector( projs1.get(0).proj );
    } else if ( projs2.size() > 1 ) {
      p0 = new Cave3DVector( projs2.get(0).proj );
    } else {
      return;
    }
    p0.normalized();

    computeAnglesAndSort( p0, projs1 );
    computeAnglesAndSort( p0, projs2 );
    // Log.v( "Cave3D", "compute Hull [2]: projs " + projs1.size() + " " + projs2.size() );

    removeInsideProjs( projs1 );
    removeInsideProjs( projs2 );

    makeTriangles();

    // Log.v( "Cave3D", "compute Hull [3]: projs " + projs1.size() + " " + projs2.size() );
  }


  private void computeHullProjs( ArrayList< Cave3DShot > rays, ArrayList< Cave3DProjection > projs, Cave3DStation st )
  {
    for ( Cave3DShot splay : rays ) {
      projs.add( new Cave3DProjection( st, splay, normal ) ); 
    }
    // projs.add( new Cave3DProjection( st, null, normal ) );

    // compute projections center and refer projected vectors to the center
    center = new Cave3DVector( 0, 0, 0 );
    for ( Cave3DProjection p : projs ) {
      center.add( p.proj );
    }
    center.mul( 1.0f/projs.size() );
    for ( Cave3DProjection p : projs ) {
      p.proj.sub( center );
    } 
  }
 
  private void computeAnglesAndSort( Cave3DVector ref, ArrayList< Cave3DProjection > projs )
  {
    // normalize projected vectors and compute the angles
    int s = projs.size();
    for (int k=1; k<s; ++k ) 
    {
      Cave3DVector p1 = new Cave3DVector( projs.get(k).proj );
      p1.normalized();
      projs.get(k).angle = angle( ref, p1 );
    }
    
    // sort projs by the angle
    if ( s <= 1 ) return;
    boolean repeat = true;
    while ( repeat ) {
      repeat = false;
      for ( int k=0; k<s-1; ++k ) {
        Cave3DProjection p1 = projs.get(k);
        Cave3DProjection p2 = projs.get(k+1);
        if ( p1.angle > p2.angle ) {
          projs.set(k, p2 );
          projs.set(k+1, p1 );
          repeat = true;
        }
      }
    }
  }

  private void removeInsideProjs( ArrayList< Cave3DProjection > projs )
  {
    int s = projs.size();
    if ( s <= 3 ) return;
    // int k1 = s - 1;
    int k2 = 0;
    int k3 = 1;
    Cave3DProjection p1 = projs.get( s-1 );
    Cave3DProjection p2 = projs.get( k2 );
    while ( k2 < projs.size() && projs.size() > 3 ) {
      Cave3DProjection p3 = projs.get( k3%projs.size() );
      Cave3DVector v21 = p1.proj.minus( p2.proj );
      Cave3DVector v23 = p3.proj.minus( p2.proj );
      float d = normal.dot( v21.cross(v23) );
      if ( d > 0 ) {
        projs.remove( k2 );
        // do not increase indices k2/k3
      } else {
        p1 = p2;
        ++k2;
        ++k3;
      }
      p2 = p3;
    }
  }


  private float angle( Cave3DVector p0, Cave3DVector p1 )
  {
    float cc = p0.dot(p1);
    float ss = normal.dot( p0.cross( p1 ) );
    double a = Math.atan2( ss, cc );
    if ( a >= 2*Math.PI ) a -= 2*Math.PI;
    if ( a < 0 )          a += 2*Math.PI;
    return (float)a;
  }


}
