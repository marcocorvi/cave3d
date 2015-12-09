/** @file CWBorder.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief CW intersection border
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.util.ArrayList;
import java.util.List;

// import java.io.PrintStream;

public class CWBorder 
{
  CWConvexHull mCV1;
  CWConvexHull mCV2;
  ArrayList< CWIntersection > mInts;
  List<CWPoint> pts2in1;
  List<CWPoint> pts1in2;
  float mVolume;
  boolean hasVolume;
  
  CWBorder( CWConvexHull cv1, CWConvexHull cv2, float eps )
  {
    mCV1 = cv1;
    mCV2 = cv2;
    mInts = new ArrayList< CWIntersection >();
    pts2in1 = cv1.computeInsidePoints( cv2, eps ); // points of cv2 inside cv1
    pts1in2 = cv2.computeInsidePoints( cv1, eps );
    hasVolume = false;
  }

  float getVolume()
  {
    if ( ! hasVolume ) {
      mVolume = computeVolume( 0.00001f );
      hasVolume = true;
    }
    return mVolume;
  }


  int size() { return mInts.size(); }
  
  boolean makeBorder()
  {
    ArrayList<CWIntersection> ints = mCV1.computeIntersection( mCV2 );
    return sortIntersections( ints );
  }
  
  /** sort a list of intersection into this border
   */
  private boolean sortIntersections( ArrayList<CWIntersection> ints )
  {
    if ( ints.size() == 0 ) return false;
    mInts.clear();
    CWIntersection i1 = ints.get(0);
    ints.remove( 0 );
    CWLinePoint lp1 = i1.mV1;
    CWLinePoint lp2 = i1.mV2;
    CWTriangle ta = i1.mTriA; // triangle of mCV1
    CWTriangle tb = i1.mTriB; // triangle of mCV2
    
    mInts.add( i1 );
    // i1.dump( System.out, 1 );
    // lp1.dump( System.out );
    while ( ints.size() > 0 ) {
      CWSide s1 = lp2.mSide;
      CWTriangle t1 = s1.otherTriangle( lp2.mTri );
      CWTriangle t2 = ( lp2.mTri == ta )? tb : ta;
      // search intersection of t1-t2 with side s1/t1
      boolean found = false;
      for ( CWIntersection i2 : ints ) {
        if ( ( i2.mTriA == t1 && i2.mTriB == t2 ) || ( i2.mTriA == t2 && i2.mTriB == t1 ) ) {
          ta = i2.mTriA;
          tb = i2.mTriB;
          if ( i2.mV1.mSide == s1 && i2.mV1.mTri == t1 ) {
            found = true; 
          } else if ( i2.mV2.mSide == s1 && i2.mV2.mTri == t1 ) {
            found = true;
            i2.reverse();
          }
          if ( found ) {
            lp2 = i2.mV2;
            mInts.add( i2 );
            ints.remove( i2 );
            break;
          }
        }
      }
      if ( ! found ) break;
    }
    return ( mInts.size() > 0 );
  }
  
  // void dump( PrintStream out )
  // {
  //   for ( CWIntersection ii : mInts ) {
  //     ii.dump(out, 1);
  //   }
  // }
  
  // void serialize( PrintStream out )
  // {
  //   out.println( mInts.size() );
  //   for ( CWIntersection ii : mInts ) {
  //     ii.serialize(out);
  //   }
  // }

  private Cave3DVector getCenter()
  {
    if ( mInts.size() == 0 ) return null;
    Cave3DVector ret = new Cave3DVector();
    for ( CWIntersection ii : mInts ) {
      ret.x += ii.mV1.x + ii.mV2.x;
      ret.y += ii.mV1.y + ii.mV2.y;
      ret.z += ii.mV1.z + ii.mV2.z;
    }
    float div = 1.0f/(2.0f*mInts.size());
    ret.x *= div;
    ret.y *= div;
    ret.z *= div;
    return ret;
  }
  
  static private float volume( Cave3DVector v0, Cave3DVector v1, Cave3DVector v2, Cave3DVector v3 )
  {
    Cave3DVector u1 = v1.minus(v0);
    Cave3DVector u2 = v2.minus(v0);
    Cave3DVector u3 = v3.minus(v0);
    return (float)Math.abs( u1.cross(u2).dot(u3) );
  }

  /** Compute the volume (*6) of the triangles than enter the first CW
   *
   * @param t21  set of triangles of the second CW that enter the first
   * @param p21  points of the second CW inside the first
   * @return volume (*6)
   */
  private float computeVolumeOf( List<CWTriangle> t21, List<CWPoint> p21, Cave3DVector cc )
  {
    float vol =0;
    CWPoint pts[] = new CWPoint[3];
    for ( CWTriangle t2 : t21 ) {
      switch ( t2.countVertexIn( p21, pts ) ) {
        case 0: break;
        case 1:
          for ( CWIntersection ii : mInts ) {
            if ( ii.mV1.mTri == t2 || ii.mV2.mTri == t2 ) {
              vol += Math.abs( volume( cc, ii.mV1, ii.mV2, pts[0] ) );
            }
          }
          break;
        case 2:
          vol += t2.volume(cc);
          CWPoint v = t2.v1;
          if ( v == pts[0] || v == pts[1] ) v = t2.v2;
          if ( v == pts[0] || v == pts[1] ) v = t2.v3;
          for ( CWIntersection ii : mInts ) {
            if ( ii.mV1.mTri == t2 || ii.mV2.mTri == t2 ) {
              vol -= Math.abs( volume( cc, ii.mV1, ii.mV2, pts[0] ) );
            }
          }
          break;
        case 3:
          vol += (float)( Math.abs( t2.volume( cc ) ) );
          break;
      }
    }
    return vol;
  }
  
  /**
   * compute the volume (*6) of the intersection
   * @param eps
   * @return volume (*6)
   */
  private float computeVolume( float eps )
  {
    Cave3DVector cc = getCenter();
    if ( cc == null ) return 0;
    
    List<CWPoint> pts2in1 = mCV1.computeInsidePoints( mCV2, eps ); // points of cv2 inside cv1
    List<CWPoint> pts1in2 = mCV2.computeInsidePoints( mCV1, eps );
    // Log.v("Cave3D", "Pts1in2 " + pts1in2.size() + " Pts2in1 " + pts2in1.size() );
    
    if ( pts1in2.size() == 0 && pts2in1.size() == 0 ) return 0;
    float vol = 0;
    
    List< CWTriangle > t1in2 = new ArrayList<CWTriangle>();
    List< CWTriangle > t2in1 = new ArrayList<CWTriangle>();
    
    for ( CWPoint p2 : pts2in1 ) {
      for ( CWTriangle t2 : p2.mTriangle ) {
        if ( ! t2in1.contains(t2) ) t2in1.add( t2 );
      }
    }
    for ( CWPoint p1 : pts1in2 ) {
      for ( CWTriangle t1 : p1.mTriangle ) {
        if ( ! t1in2.contains(t1) ) t1in2.add( t1 );
      }
    }
   
    vol += computeVolumeOf( t1in2, pts1in2, cc );
    vol += computeVolumeOf( t2in1, pts2in1, cc );
	  
    return vol;
  }

}



