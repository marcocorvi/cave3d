/** @file CWSide.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief face side
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.io.PrintStream;
import java.io.IOException;

import android.util.Log;

/**
 * oriented side (as seen from outside)
 * 
 *       /     \
 *      v       ^
 *     /    t2   \
 *   p1 ----->----P2
 *     \ <--t1-- /
 *      \       /
 */
public class CWSide
{
  static int cnt = 0;
  int mCnt;
  CWPoint p1, p2;
  Cave3DVector u12;
  CWTriangle t1; // oriented opposite to the side: points are p2-p1 in t1
  CWTriangle t2; // oriented as the side: points are p1-p2 in t2

  public CWSide( CWPoint p1, CWPoint p2 )
  {
    mCnt = cnt++;
    this.p1 = p1;
    this.p2 = p2;
    u12 = p2.minus(p1);
    u12.normalized();
    t1 = null;
    t2 = null;
  }
  
  public CWSide( int tag, CWPoint p1, CWPoint p2 )
  {
    mCnt = tag;
    if ( cnt <= tag ) cnt = tag+1;
    this.p1 = p1;
    this.p2 = p2;
    u12 = p2.minus(p1);
    u12.normalized();
    t1 = null;
    t2 = null;
  }

  boolean areTrianglesOutside()
  {
    if ( t1 != null && ! t1.isOutside() ) return false;
    if ( t2 != null && ! t2.isOutside() ) return false;
    return true;
  }
  
  CWTriangle otherTriangle( CWTriangle t )
  {
    if ( t == t1 ) return t2;
    if ( t == t2 ) return t1;
    return null;
  }

  // return the old triangle
  CWTriangle setTriangle( CWTriangle t )
  {
    CWTriangle ret = null;
    if ( ( t.v1 == p1 && t.v2 == p2 ) || ( t.v2 == p1 && t.v3 == p2 ) || ( t.v3 == p1 && t.v1 == p2 ) ) {
      // triangle oriented as side
      ret = t2;
      t2 = t;
    } else if ( ( t.v1 == p2 && t.v2 == p1 ) || ( t.v2 == p2 && t.v3 == p1 ) || ( t.v3 == p2 && t.v1 == p1 ) ) {
      // triangle oriented opposite to side
      ret = t1;
      t1 = t;
    }
    return ret;
  }

  void removeTriangle( CWTriangle t )
  {
    if ( t == t1 ) t1 = null;
    if ( t == t2 ) t2 = null;
  }

  void replace( CWPoint pold, CWPoint pnew )
  {
    if ( p1 == pold ) { 
      p1 = pnew; 
    } else if ( p2 == pold ) {
      p2 = pnew;
    }
  }
  
  boolean contains( CWPoint p ) { return p == p1 || p == p2; }
  
  CWPoint otherPoint( CWPoint p ) { return ( p == p1 )? p2 : ( p == p2)? p1 : null; }

  float cross( Cave3DVector v )
  {
    Cave3DVector vp1 = v.minus( p1 );
    vp1.normalized();
    return u12.cross( vp1 ).length();
  }

  void dump( )
  {
    Log.v("Cave3D", "CWSide " + mCnt + " P " + p1.mCnt + "-" + p2.mCnt 
                     + " T " + ((t1 == null)? "-" : t1.mCnt) + " " + ((t2 == null)? "-" : t2.mCnt)
    );
  }
  

  // void serialize( PrintStream out )
  // {
  //   Cave3DVector dp = p2.minus(p1);
  //   out.println(mCnt + " " + p1.mCnt + " " + p2.mCnt 
  //     + " " + ((t1 == null)? "-1" : t1.mCnt) + " " + ((t2 == null)? "-1" : t2.mCnt)
  //     + " DP " + dp.x + " " + dp.y + " " + dp.z
  //   );
  // }
}

