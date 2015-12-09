/** @file CWConvexHull.java
 *
 *e @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D convex-concave hull
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
// import java.util.Collection;
// import static java.util.stream.Collectors.toList;

// import java.io.PrintStream;
// import java.io.FileNotFoundException;

import android.util.Log;

public class CWConvexHull 
{
  Cave3DStation mFrom;
  Cave3DStation mTo;
  private ArrayList< CWPoint > mVertex;
  private ArrayList< CWSide > mSide;
  ArrayList< CWTriangle > mFace;
  float mVolume;
  boolean hasVolume;
  
  public CWConvexHull( )
  {
    mVertex = new ArrayList< CWPoint >();
    mFace   = new ArrayList< CWTriangle >();
    mSide   = new ArrayList< CWSide >();
    hasVolume = false;
  }

  float getVolume()
  {
    if ( ! hasVolume ) {
      mVolume = computeVolume();
      hasVolume = true;
    }
    return mVolume;
  }
  
  // public void create( ArrayList<Cave3DVector> pts,
  //   float distance_concavity, float angle_concavity, float eps )
  // {
  //   for ( Cave3DVector p : pts ) insertPoint( p );
  //   // dump();
  //   makeConcave( pts, distance_concavity, angle_concavity, eps );
  // }

  public void create( ArrayList< Cave3DShot > splays1, // splays at FROM station
                      ArrayList< Cave3DShot > splays2, // splays at TO station
                      Cave3DStation sf,                // shot FROM station
                      Cave3DStation st )               // shot TO station
    throws RuntimeException
  {
    mFrom = sf;
    mTo   = st;
    // Log.v("Cave3D", "CW " + sf.name + " " + st.name + " splays " + splays1.size() + " " + splays2.size() );
    ArrayList< Cave3DVector > pts = new ArrayList<Cave3DVector>();
    Cave3DVector vf = new Cave3DVector( sf );
    Cave3DVector vt = new Cave3DVector( st );
    try {
      // Log.v("Cave3D", "insert point T " + vt.x + " " + vt.y + " " + vt.z );
      insertPoint( vt );
      pts.add( vt );
      for ( Cave3DShot s1 : splays1 ) {
        Cave3DVector v1 = vf.plus( s1.toCave3DVector() );
        // Log.v("Cave3D", "insert point 1 " + v1.x + " " + v1.y + " " + v1.z );
        insertPoint( v1 );
        pts.add( v1 );
      }
      // Log.v("Cave3D", "insert point F " + vf.x + " " + vf.y + " " + vf.z );
      insertPoint( vf );
      pts.add( vf );
      for ( Cave3DShot s2 : splays2 ) {
        Cave3DVector v2 = vt.plus( s2.toCave3DVector() );
        // Log.v("Cave3D", "insert point 2 " + v2.x + " " + v2.y + " " + v2.z );
        insertPoint( v2 );
        pts.add( v2 );
      }
    } catch ( RuntimeException e ) {
      vf.dump();
      vt.dump();
      for ( Cave3DShot s1 : splays1 ) {
        vf.plus( s1.toCave3DVector() ).dump();
      }
      for ( Cave3DShot s2 : splays2 ) {
        vt.plus( s2.toCave3DVector() ).dump();
      }
      Log.e("Cave3D", "ERROR " + e.getMessage() );
      throw e;
    }
    makeConcave( pts, 0.5f, 0.1f, 0.000001f );
  }

  
  void addPoint( Cave3DVector p ) { insertPoint( p ); }

  void addTriangle( CWTriangle t ) { mFace.add( t ); }

  // int getNrVertex() { return mVertex.size(); }
  // int getNrSide()   { return mSide.size(); }
  // int getNrFace()   { return mFace.size(); }

  private Cave3DVector getCenter()
  {
    Cave3DVector ret = new Cave3DVector();
    for( CWPoint p : mVertex ) { ret.x += p.x; ret.y += p.y; ret.z += p.z; }
    ret.times( 1.0f / mVertex.size() );
    return ret;
  }
  
  // -------------------------------------------------------------------

  private static float volume( Cave3DVector p0, Cave3DVector p1, Cave3DVector p2, Cave3DVector p3 )
  {
    // return p0.x * ( p1.y * p2.z + p3.y * p1.z + p2.y * p3.z - p1.y * p3.z - p3.y * p2.z - p2.y * p1.z ) 
    //      - p0.y * ( p1.x * p2.z + p3.x * p1.z + p2.x * p3.z - p1.x * p3.z - p3.x * p2.z - p2.x * p1.z ) 
    //      + p0.z * ( p1.x * p2.y + p3.x * p1.y + p2.x * p3.y - p1.x * p3.y - p3.x * p2.y - p2.x * p1.y ) 
    //      - (p1.x * (p2.y*p3.z - p2.z*p3.y) + p1.y * (p2.z*p3.x - p2.x*p3.z) + p1.z * (p2.x*p3.y - p2.y*p3.z));
    Cave3DVector u1 = p1.minus(p0);
    Cave3DVector u2 = p2.minus(p0);
    Cave3DVector u3 = p3.minus(p0);
    return u1.cross(u2).dot(u3);
  }

  private void addVertex( CWPoint v )
  {
    if ( v == null || mVertex.contains(v) ) return;
    mVertex.add( v );
  }
  
  private CWPoint getVertexByTag( int tag )
  {
    for ( CWPoint v : mVertex ) if ( v.mCnt == tag ) return v;
    return null;
  }

  private void addSide( CWSide s )
  {
    if ( s == null || mSide.contains( s ) ) return;
    mSide.add( s );
  }
  
  private CWSide getSideByTag( int tag )
  {
    for ( CWSide s : mSide ) if ( s.mCnt == tag ) return s;
    return null;
  }

  private CWSide getSide( CWPoint p1, CWPoint p2 )
  {
    for ( CWSide s : mSide ) {
      if ( s.p1 == p1 && s.p2 == p2 ) return s;
      if ( s.p1 == p2 && s.p2 == p1 ) return s;
    }
    CWSide s0 = new CWSide( p1 , p2 );
    mSide.add( s0 );
    return s0;
  }
  
  private CWTriangle addTriangle( CWPoint p1, CWPoint p2, CWPoint p3 )
  {
    CWSide s1 = getSide( p2, p3 );
    CWSide s2 = getSide( p3, p1 );
    CWSide s3 = getSide( p1, p2 );
    CWTriangle t = new CWTriangle( p1, p2, p3, s1, s2, s3 );
    mFace.add( t );
    s1.setTriangle( t );
    s2.setTriangle( t );
    s3.setTriangle( t );
    addVertex( p1 );
    addVertex( p2 );
    addVertex( p3 );
    return t;
  }
  
  private CWTriangle getTriangleByTag( int tag )
  {
    for ( CWTriangle t : mFace ) if ( t.mCnt == tag ) return t;
    return null;
  }

  private void removeVertex( CWPoint v )  { mVertex.remove( v ); }
  private void removeSide( CWSide s )     { mSide.remove( s ); }
  private void removeFace( CWTriangle t ) { mFace.remove( t ); }
  
  // -------------------------------------------------------------------
  private void addTo( List<CWPoint> yes, List<CWPoint> no, CWPoint v ) 
  {
    if ( v.areAllTrianglesOutside() ) {
      if ( ! yes.contains(v) ) yes.add( v );
    } else {
      if ( ! no.contains(v) ) no.add( v );
    }
  }
  private void addTo( List<CWSide> yes, List<CWSide> no, CWSide s ) 
  {
    if ( s.areTrianglesOutside() ) {
      if ( ! yes.contains(s) ) yes.add( s );
    } else {
      if ( ! no.contains(s) ) no.add( s );
    }
  }
  
  private void insertPoint( Cave3DVector p )
  {
    // Log.v("Cave3D", mVertex.size() + " insert point " + p.x + " " + p.y + " " + p.z );
    if ( mVertex.size() < 4 ) {
      mVertex.add( new CWPoint( p.x, p.y, p.z ) );
      if ( mVertex.size() == 4 ) {
        CWPoint p0 = mVertex.get(0);
        CWPoint p1 = mVertex.get(1);
        CWPoint p2 = mVertex.get(2);
        CWPoint p3 = mVertex.get(3);
        // positive volume for points inside the tetrahedron
        // face triangles must point inside
        if ( volume( p0, p1, p2, p3 ) < 0 ) {
          p1 = mVertex.get(2);
          p2 = mVertex.get(1);
        }
        
        /**                 1      3 is above the screen
         *                / | \
         *  face-3       /  |--\------- T0.s3 = T2.s3
         *  T3.s3 ------/   3   \------ T0.s2 = T3.s1
         *             /  ,' `,--\----- T0.s1 = T1.s3
         *  T1.s1 ----/-,'     `, \
           *           0 ----------- 2
         *                T3.s2 = T1.s2
         *
         *                face-2
         */
        addTriangle( p0, p1, p2 ); //  0--1--2-[3]->
        addTriangle( p1, p3, p2 ); // [0] 1--2--3--<
        addTriangle( p2, p3, p0 ); //  0-[1]-2--3-->
        addTriangle( p3, p1, p0 ); //  0--1-[2]-3--<
      // dump();
      }
      return;
    }
   
    // int nv = mVertex.nv();
    ArrayList<CWTriangle> faceToRemove = new ArrayList<CWTriangle>();
    for ( CWTriangle t : mFace ) {
      if ( t.setOutside( p ) ) {
        faceToRemove.add( t );
      }
    }
    if ( faceToRemove.size() == 0 ) {
      // Log.v("Cave3D", "point is inside " + p.x + " " + p.y + " " + p.z );
      return;
    } else {
      // Log.v("Cave3D", "faces to remove " + faceToRemove.size() );
    }

    // dump();
    CWPoint vv = new CWPoint( p.x, p.y, p.z );

    ArrayList<CWPoint> vertexToRemove = new ArrayList<CWPoint>();
    ArrayList<CWPoint> vertexToKeep   = new ArrayList<CWPoint>();
    ArrayList<CWSide> sideToRemove    = new ArrayList<CWSide>();
    ArrayList<CWSide> sideToKeep      = new ArrayList<CWSide>();

    for ( CWTriangle t : faceToRemove ) {
      addTo( vertexToRemove, vertexToKeep, t.v1 );
      addTo( vertexToRemove, vertexToKeep, t.v2 );
      addTo( vertexToRemove, vertexToKeep, t.v3 );
      addTo( sideToRemove, sideToKeep, t.s1 );
      addTo( sideToRemove, sideToKeep, t.s2 );
      addTo( sideToRemove, sideToKeep, t.s3 );
    }
    // Log.v("Cave3D", "to remove T: " + faceToRemove.size() 
    //   + " V: " + vertexToRemove.size() + " / " + vertexToKeep.size() 
    //   + " S: " + sideToRemove.size()   + " / " + sideToKeep.size() );
   
    if ( sideToKeep.size() > 2 ) {

      addVertex( vv );
      
      CWSide s0 = sideToKeep.get(0);
      CWPoint p0 = s0.p1;
      CWPoint p2 = s0.p2;
      // check if need to reverse the border
      boolean reverse = false;
      CWPoint p1 = sideToKeep.get(1).p1;
      if ( p1 == p0 || p1 == p2 ) p1 = sideToKeep.get(1).p2;
      if ( volume( p0, p2, p1, vv ) > 0 ) { 
        // if VV is on the positive side of the border, reverse the border
        p1 = p2; p2 = p0; p0 = p1;
        reverse = true;
      }
      
      // Log.v("Cave3D", "Border " + p0.mCnt + " " + p1.mCnt + " p2 " + p2.mCnt );
      // s0.dump();
      int size = sideToKeep.size();
      for ( int todo = 1; todo < size; ++todo ) {
        int k = todo;
        for ( ; k<size; ++k ) {
          CWSide s = sideToKeep.get(k);
          if ( s.contains(p2) ) {
            if ( k != todo ) {
              sideToKeep.set( k, sideToKeep.get(todo) );
              sideToKeep.set( todo, s );
            }
            // s.dump();
            p2 = s.otherPoint( p2 );
            break;
          }
        }
        if ( k == size ) {
          Log.e("Cave3D", "WARNING next side not found");
          // dump();
          // Log.v("Cave3D", "sides to keep " + sideToKeep.size() );
          // for ( int j=0; j<sideToKeep.size(); ++j ) sideToKeep.get(j).dump();
          // Log.v("Cave3D", "sides to remove " + sideToRemove.size() );
          // for ( int j=0; j<sideToRemove.size(); ++j ) sideToRemove.get(j).dump();
          // Log.v("Cave3D", "vertexs to keep " + vertexToKeep.size() );
          // for ( int j=0; j<vertexToKeep.size(); ++j ) vertexToKeep.get(j).dump();
          // Log.v("Cave3D", "vertexs to remove " + vertexToRemove.size() );
          // for ( int j=0; j<vertexToRemove.size(); ++j ) vertexToRemove.get(j).dump();
          // Log.v("Cave3D", "faces to remove " + faceToRemove.size() );
          // for ( int j=0; j<faceToRemove.size(); ++j ) faceToRemove.get(j).dump();

          throw new RuntimeException("side not found");
        }
      }
      
      // s0 = sideToKeep.get(0);
      p0 = s0.p1;
      p2 = s0.p2;
      if ( reverse ) {
        p0 = s0.p2;
        p2 = s0.p1;
      }
      addTriangle( p0, p2, vv );
      for ( int todo = 1; todo < sideToKeep.size(); ++todo ) {
        p0 = p2;
        p2 = sideToKeep.get(todo).otherPoint( p0 );
        addTriangle( p0, p2, vv );
      }
      
      // now remove sides and triangles
      // Log.v("Cave3D", "removing V " + vertexToRemove.size() + " S " + sideToRemove.size() + " T " + faceToRemove.size() );
      for ( CWSide s2 : sideToRemove )     { removeSide( s2 ); }
      for ( CWTriangle f1 : faceToRemove ) { 
        for ( CWPoint v1 :vertexToKeep ) v1.removeTriangle( f1 );
        removeFace( f1 );
      }
      for ( CWPoint v1 : vertexToRemove )  { removeVertex( v1 ); }
    }

    // remove small area triangles
    // Log.v("Cave3D", "small area V " + mVertex.size() + " S " + mSide.size() + " T " + mFace.size() );
    // dump();
    // Log.v("Cave3D", "check small area. nr T " + mFace.size() );
    boolean repeat = true;
    while ( repeat ) {
      repeat = false;
      for ( CWTriangle t : mFace ) {
        // Log.v("Cave3D", "Triangle " + t.mCnt + " area " + t.area() );
        if ( t.area() < 0.01f ) {
          float d12 = t.v1.distance( t.v2 );
          float d13 = t.v1.distance( t.v3 );
          float d23 = t.v3.distance( t.v2 );
          if ( d12 < d13 ) {
            if ( d12 < d23 ) {
              reduce( t, t.v2, t.v1, t.v3, t.s3, t.s1, t.s2 );
            } else {
              reduce( t, t.v3, t.v2, t.v1, t.s1, t.s2, t.s3 );
            }
          } else {
            if ( d13 < d23 ) {
              reduce( t, t.v1, t.v3, t.v2, t.s2, t.s3, t.s1 );
            } else {
              reduce( t, t.v3, t.v2, t.v1, t.s1, t.s2, t.s3 );
            }
          }
          repeat = true;
          break;
        }
      }
    }
    
    // Log.v("Cave3D", "insert point done");
    // dump();
    // checkConsistency();
  }

  private void reduce( CWTriangle t, CWPoint p2, CWPoint p1, CWPoint p3, CWSide s3, CWSide s1, CWSide s2  )
  {
    // Log.v("Cave3D", "reduce triangle " + t.mCnt + " " + p2.mCnt + " " + p1.mCnt + " " + p3.mCnt );

    CWTriangle t2 = s3.otherTriangle(t);
    CWTriangle tn = s1.otherTriangle(t);
	  
    CWSide s4 = null;
    CWSide s5 = null;
    if ( t2.s1 == s3 )      { s4 = t2.s2; s5 = t2.s3; }
    else if ( t2.s2 == s3 ) { s4 = t2.s3; s5 = t2.s1; }
    else if ( t2.s3 == s3 ) { s4 = t2.s1; s5 = t2.s2; }
	  
    CWPoint p4 = s4.otherPoint(p1);
	  
    CWTriangle t3 = s5.otherTriangle(t2);
    // Log.v("Cave3D", " T2/3 " + t2.mCnt + " " + t3.mCnt +  " TN " + tn.mCnt
    //    + " S4/5 " + s4.mCnt + " " + s5.mCnt + " P4 " + p4.mCnt );
    
    p3.removeTriangle(t);
    p1.removeTriangle(t);
    p1.removeTriangle(t2);
    p4.removeTriangle(t2);
	  
    for ( CWTriangle tt : p2.mTriangle ) {
      if ( tt == t2 || tt == t ) continue;
      if ( tt == t3 ) {
        // change side s5 --> s4
        if ( tt.s1 == s5 )      { tt.s1 = s4; }
        else if ( tt.s2 == s5 ) { tt.s2 = s4; }
        else if ( tt.s3 == s5 ) { tt.s3 = s4; }
      }
      if ( tt == tn ) {
        // change side s1 --> s2
        if ( tt.s1 == s1 )      { tt.s1 = s2; }
        else if ( tt.s2 == s1 ) { tt.s2 = s2; }
        else if ( tt.s3 == s1 ) { tt.s3 = s2; }
      }
      // change vertex p2 --> p1
      if ( tt.v1 == p2 )      { tt.v1 = p1; }
      else if ( tt.v2 == p2 ) { tt.v2 = p1; }
      else if ( tt.v3 == p2 ) { tt.v3 = p1; }
      tt.s1.replace( p2, p1 );
      tt.s2.replace( p2, p1 );
      tt.s3.replace( p2, p1 );
      tt.rebuildTriangle();
    }

    // Log.v("Cave3D", "removing T " + t.mCnt + " " + t2.mCnt 
    //   + " S " + s1.mCnt + " " + s3.mCnt + " " + s5.mCnt + " P " + p2.mCnt );
    mFace.remove( t );
    mFace.remove( t2 );
    mSide.remove( s1 );
    mSide.remove( s3 );
    mSide.remove( s5 );
    mVertex.remove( p2 );
    
    // Log.v("Cave3D", "after reducing");
    // dump( );
    // orderPointTriangles();
  }
  
  // private void checkConsistency()
  // {
  //   for ( CWPoint p : mVertex ) {
  //     for ( CWTriangle t : mFace ) {
  //       if ( t.contains( p ) ) continue;
  //       float vol = t.volume( p );
  //       if ( vol < 0 ) {
  //         Log.v("Cave3D", "Inconsistent T " + t.mCnt + " with V " + p.mCnt + " vol " + vol );
  //       }
  //     }
  //   }
  // }

  // boolean checkPoint( Cave3DVector v )
  // {
  //   // Log.v("Cave3D", "Check vector " + v.x + " " + v.y + " " + v.z );
  //   float totvol = 0;
  //   boolean ret = true;
  //   for ( CWTriangle t : mFace ) {
  //     float vol = t.volume( v );
  //     if ( vol <= -0.001f ) {
  //       // Log.v("Cave3D", "neg vol " + vol );
  //       // t.dump(); 
  //       ret = false;
  //     }
  //     totvol += vol;
  //   }
  //   if ( ret ) {
  //     totvol = Math.round( totvol*100 )/100;
  //     float angle = solidAngle( v, 0.0001f );
  //     Log.v("Cave3D", "Volume " + totvol + " " + ret + " angle " + angle );
  //   }
  //   return ret;
  // } 

  void dump( )
  {
    Log.v("Cave3D", "Convex hull vertices " + mVertex.size() 
        + " sides " + mSide.size() 
        + " faces " + mFace.size() );
    for ( CWPoint v :  mVertex ) v.dump( );
    for ( CWSide s : mSide )     s.dump( );
    for ( CWTriangle f : mFace ) f.dump( );
  }
  
  // void serialize( PrintStream out )
  // {
  //   out.println(mVertex.size() + " " + mSide.size() + " " + mFace.size() );
  //   for ( CWPoint v :  mVertex ) v.serialize( out );
  //   for ( CWSide s : mSide ) s.serialize( out );
  //   for ( CWTriangle f : mFace ) f.serialize( out );
  // }
  
  float computeDiameter() 
  {
    float diam = 0;
    for ( int k1 = 0; k1 < mVertex.size(); ++k1 ) {
      CWPoint p1 = mVertex.get(k1);
      for ( int k2 = k1+1; k2 < mVertex.size(); ++k2 ) {
        float d = p1.distance( mVertex.get( k2 ) );
        if ( d > diam ) diam = d;
      }
    }
    return diam;
  }

  // thr concavity threshold
  // eps point "coincidence" threshold
  private void makeConcave( ArrayList< Cave3DVector > pts, float distance_thr, float angle_thr, float eps )
  {
    ArrayList<Cave3DVector> insidePts = new ArrayList<Cave3DVector>();
    for ( Cave3DVector p : pts ) {
      if ( ! isVertex( p, eps ) ) insidePts.add( p );
    }
    // Log.v("Cave3D", "Make concave: inside points " + insidePts.size() );
    if ( insidePts.size() == 0 ) return;

    float diameter = computeDiameter();
    while ( true ) {
      Cave3DVector p0 = null;
      CWTriangle   t0 = null;
      float d0 = diameter;
      for ( Cave3DVector p : insidePts ) {
        for ( CWTriangle t : mFace ) {
          if ( t.hasPointAbove(p) ) {
            float d = t.distance(p);
            if ( d < d0 ) {
              p0 = p;
              t0 = t;
              d0 = d;
            }
          }
        }
      }
      if ( d0 > distance_thr ) break;
      float a0 = t0.maxAngleOfPoint( p0 );
      if ( a0 > angle_thr ) break;
      // Log.v("Cave3D", "concavity T " + t0.mCnt + " P " + p0.x + " " + p0.y + " " + p0.z );
      // TODO replace CWTriangle t0 with 3 Triangles in apex CWPoint p0
      // for the three sides s of t0:
      //   check if instead of CWTriangle with s and p0 
      //   should split the triangle on the other side of s 
      CWTriangle t1 = t0.s1.otherTriangle( t0 );
      CWTriangle t2 = t0.s2.otherTriangle( t0 );
      CWTriangle t3 = t0.s3.otherTriangle( t0 );
      CWPoint v0 = new CWPoint( p0.x, p0.y, p0.z );
      mVertex.add( v0 );
      dropTriangle( t0 );

      CWTriangle t31 = addTriangle( v0, t0.v1, t0.v2 ); // side t0.s3 
      CWTriangle t11 = addTriangle( v0, t0.v2, t0.v3 ); // side t0.s1 
      CWTriangle t21 = addTriangle( v0, t0.v3, t0.v1 ); // side t0.s2 
      CWPoint p3 = t3.oppositePointOf( t0.s3 );
      CWPoint p1 = t1.oppositePointOf( t0.s1 );
      CWPoint p2 = t2.oppositePointOf( t0.s2 );
      if ( v0.squareDistance(p3) < t0.v1.squareDistance(t0.v2) ) {
        reformDiamond( v0, p3, t0.v1, t0.v2, t3, t31, t0.s3 );
      }
      if ( v0.squareDistance(p1) < t0.v2.squareDistance(t0.v3) ) {
        reformDiamond( v0, p1, t0.v2, t0.v3, t1, t11, t0.s1 );
      }
      if ( v0.squareDistance(p2) < t0.v3.squareDistance(t0.v1) ) {
        reformDiamond( v0, p2, t0.v3, t0.v1, t2, t21, t0.s2 );
      }
      insidePts.remove( p0 );
    }
  }

  private void reformDiamond( CWPoint p0, CWPoint p3, CWPoint v1, CWPoint v2, CWTriangle t3, CWTriangle t31, CWSide s3 ) 
  {
    dropTriangle( t3 );
    dropTriangle( t31 );
    mSide.remove( s3 );
    addTriangle( v2, v1, p3 );
    addTriangle( v1, v2, p0 );
  }

  private void dropTriangle( CWTriangle t )
  {
    t.v1.removeTriangle( t );
    t.v2.removeTriangle( t );
    t.v3.removeTriangle( t );
    t.s1.removeTriangle( t );
    t.s2.removeTriangle( t );
    t.s3.removeTriangle( t );
    mFace.remove( t );
  }
  
  void orderPointTriangles()
  {
    for ( CWPoint p : mVertex ) p.orderTriangles();
  }
  
  private boolean isVertex( Cave3DVector p, float eps )
  {
    for ( CWPoint v : mVertex ) if ( v.distance(p) < eps ) return true;
    return false;
  }

  
  // private boolean isPointOnSurface( Cave3DVector p, float eps )
  // {
  //   for ( CWTriangle t : mFace ) if ( t.isInside( p ) ) return true;
  //   return false
  // }

  /**
   * @param eps surface uncertainty
   */
  float solidAngle( Cave3DVector p, float eps )
  {
    if ( isVertex(p, 0.0001f) ) return 0;
    if ( isOnSurface(p, eps) ) return 0;
    float ret = 0;
    for ( CWTriangle f : mFace ) ret += f.solidAngle(p);
    if ( Math.abs( ret ) < 0.001 ) return 0;
    return (float)Math.round(0.25 * ret / Math.PI);
  }

  /** check if a point is inside this CW
   * @param p   point
   * @param eps
   */
  boolean isPointInside( Cave3DVector p, float eps ) { return solidAngle(p, eps) > 0.4f; }

  /** check if a point is on the surface of this CW (ie, inside a face)
   * @param p   point
   * @param eps
   */
  boolean isOnSurface( Cave3DVector v, float eps )
  {
    for ( CWTriangle t : mFace ) {
      if ( t.isPointInside(v,eps) ) return true;
    }
    return false;
  }
 
 
  // points of the other CW that are inside this CW 
  List<CWPoint> computeInsidePoints( CWConvexHull cv, float eps )
  { 
    ArrayList<CWPoint> ret = new ArrayList<CWPoint>();
    for ( CWPoint p : cv.mVertex ) if ( isPointInside( p, eps ) ) ret.add( p );
    return ret;
  }
  
  // **********************************************************************
  
  /**
   * @param cv2    the other CW
   */
  ArrayList<CWIntersection> computeIntersection( CWConvexHull cv2 ) 
  {
    ArrayList<CWIntersection> ret = new ArrayList<CWIntersection>();
    
    for ( CWTriangle tb : cv2.mFace ) {
      for ( CWTriangle ta : mFace ) {
      Cave3DVector v = ta.intersectionBasepoint( tb );
      Cave3DVector n = ta.intersectionDirection( tb );
      CWLinePoint lpa1 = new CWLinePoint();
      CWLinePoint lpa2 = new CWLinePoint();
      CWLinePoint lpb1 = new CWLinePoint();
      CWLinePoint lpb2 = new CWLinePoint();
      if ( ta.intersectionPoints(v, n, lpa1, lpa2) && tb.intersectionPoints(v, n, lpb1, lpb2) ) {
        if ( lpa1.mAlpha > lpa2.mAlpha ) {
          CWLinePoint lpa = lpa1; lpa1= lpa2; lpa2 = lpa;
        }
        if ( lpb1.mAlpha > lpb2.mAlpha ) {
            CWLinePoint lpb = lpb1; lpb1= lpb2; lpb2 = lpb;
          }
        // now a1 < a2 and b1 < b2
        if ( lpa1.mAlpha < lpb1.mAlpha ) {
          if ( lpa2.mAlpha < lpb1.mAlpha ) { // a1 < a2 < b1 < b2
            // no intersection
          } else if ( lpa2.mAlpha < lpb2.mAlpha ) { // a1 < b1 < a2 < b2
            CWIntersection ii = new CWIntersection( 1, ta, tb, v, n );
            ii.mV1 = lpb1;
            ii.mV2 = lpa2;
            ret.add( ii );
          } else { // lpa2.mAlpha > lpb2.mAlpha
              CWIntersection ii = new CWIntersection( 2, ta, tb, v, n );
              ii.mV1 = lpb1;
              ii.mV2 = lpb2;
              ret.add( ii );
          }
        } else {
          if ( lpb2.mAlpha < lpa1.mAlpha ) { // b1 < b2 < a1 < a2
              // no intersection
            } else if ( lpb2.mAlpha < lpa2.mAlpha ) { // b1 < a1 < b2 < a2
              CWIntersection ii = new CWIntersection( 1, tb, ta, v, n );
              ii.mV1 = lpa1;
              ii.mV2 = lpb2;
              ret.add( ii );
            } else { // lpa2.mAlpha > lpb2.mAlpha
                CWIntersection ii = new CWIntersection( 2, tb, ta, v, n );
              ii.mV1 = lpa1;
              ii.mV2 = lpa2;
              ret.add( ii );
            }
        }
      }
      }  
    }
    return ret;
  }

  
  //**********************************************************************

  /** shrink a bit the convex hull by moving inside points that are on the surface on another cv
   *  
   * @param cv the other cw
   * @param delta   amount to move towards the center
   * @return true if at least one point has moved
   */
/*
  private boolean regularizePoints( CWConvexHull cv, float delta, float eps )
  {
    boolean ret = false;
    Cave3DVector c1 = getCenter();
    for ( CWPoint p : mVertex ) {
      if ( cv.isOnSurface( p, eps ) ) {
        Cave3DVector dp = c1.minus(p);
        dp.times( delta / dp.length() );
        p.add( dp );
        ret = true;
      }
    }
    return ret;
  }
  
  private boolean regularizeSides( CWConvexHull cv, float delta, float eps )
  {
    boolean ret = false;
    for ( CWSide s1 : mSide ) {
      Cave3DVector d1 = s1.p2.minus( s1.p1 );
      float d11 = d1.dot( d1 );
      for ( CWSide s2 : cv.mSide ) {
        Cave3DVector q = s1.p1.minus( s2.p1 );
        Cave3DVector d2 = s2.p2.minus( s2.p1 );
        float d22 = d2.dot( d2 );
        float d12 = d1.dot( d2 );
        float det = d11 * d22 - d12 * d12;
        float q1 = q.dot( d1 );
        float q2 = q.dot( d2 );
        float a = ( -d22 * q1 + d12 * q2 )/det;
        float b = ( -d12 * q1 + d11 * q2 )/det;
        if ( a < 0 || a > 1 || b < 0 || b > 1 ) continue;
        Cave3DVector d = new Cave3DVector( q.x + a*d1.x - b*d2.x, q.y + a*d1.y - b*d2.y, q.z + a*d1.z - b*d2.z );
        
        if ( d.length() < delta ) {
          Cave3DVector n = d1.cross( d2 );
          n.normalized();
          Cave3DVector dn = n.cross(d);
          // Log.v("Cave3D", "DN " + dn.x + " " + dn.y + " " + dn.z );
          n.mul( delta/2 );
          if ( n.dot(d) >= 0.0f ) {
            s1.p1.add( n ); s1.p2.add( n ); // N.B. d1 is unchanged
            s2.p1.sub( n ); s2.p2.sub( n );
          } else {
            s1.p1.sub( n ); s1.p2.sub( n );
            s2.p1.add( n ); s2.p2.add( n );
          }
          ret = true;
        }
      }
    }
    return ret;
  }
  
  private static boolean regularizePoints( CWConvexHull cv1, CWConvexHull cv2, float delta, float eps )
  {
    return cv1.regularizePoints(cv2, delta, eps ) || cv2.regularizePoints(cv1, delta, eps );
  }
  
  private static boolean regularizeSides( CWConvexHull cv1, CWConvexHull cv2, float delta, float eps )
  {
    return cv1.regularizeSides(cv2, delta, eps ) || cv2.regularizeSides(cv1, delta, eps );
  }
  
  static void regularize( CWConvexHull cv1, CWConvexHull cv2, float delta, float eps )
  {
    while ( regularizePoints( cv1, cv2, delta, eps ) || regularizeSides( cv1, cv2, delta, eps  ) ) ;
  }
  
*/  
  

  private float computeVolume()
  {
    if ( mVertex.size() < 4 ) return 0.0f;
    Cave3DVector cc = getCenter();
    float vol = 0;
    for ( CWTriangle t : mFace ) {
      vol += t.volume( cc );
    }
    return vol;
  }

}
