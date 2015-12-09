/** @file CWIntersection.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief CW triangles intersection
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.io.PrintStream;

public class CWIntersection {
  static int cnt = 0;
  int mCnt;
  int mType;         // 1 one endpoint fron each triangle, 2 both endpoints from one triangle
  CWTriangle mTriA;    
  CWTriangle mTriB;
  Cave3DVector mV;       // base-point of intersection line
  Cave3DVector mN;       // direction of intersection line
  CWLinePoint mV1;
  CWLinePoint mV2;
  
  
  
  public CWIntersection( int type, CWTriangle ta, CWTriangle tb, Cave3DVector v, Cave3DVector n )
  {
    mCnt = cnt++;
    mType = type;
    mTriA = ta;
    mTriB = tb;
    mV = v; 
    mN = n;
    mV1 = null;
    mV2 = null;
  }
  
  void reverse()
  {
    CWLinePoint v = mV1; mV1 = mV2; mV2 = v;
    mV1.mAlpha *= -1;
    mV2.mAlpha *= -1;
    mN.reverse();
    CWTriangle t = mTriA; mTriA = mTriB; mTriB = t;
    mType += 2;
  }
  
  // void dump( int j )
  // {
  //   CWTriangle t1 = mV1.mSide.otherTriangle( mV1.mTri );
  //   CWTriangle t2 = mV2.mSide.otherTriangle( mV2.mTri );
  //     
  //   if ( j == 1 ) { // first 1 then 2
  //     Log.v("Cave3D", mCnt + " " + mType + ": " + mTriA.mCnt + "-" + mTriB.mCnt 
  //       + " " + t1.mCnt + "/" + mV1.mSide.mCnt + "/" + mV1.mTri.mCnt
  //       + " " + mV2.mTri.mCnt + "/" + mV2.mSide.mCnt + "/" + t2.mCnt
  //     );
  //   } else { // first 2 then 1
  //     Log.v("Cave3D", mCnt + " " + mType + ": " + mTriA.mCnt + "-" + mTriB.mCnt 
  //         + " " + mV2.mTri.mCnt + "/" + mV2.mSide.mCnt + "/" + t2.mCnt
  //         + " " + mV1.mTri.mCnt + "/" + mV1.mSide.mCnt + "/" + t1.mCnt
  //     );
  //   }
  //     // if ( mV1 != null ) mV1.dump( out );
  //     // if ( mV2 != null ) mV2.dump( out );
  // }
  
  // void serialize( PrintStream out )
  // {
  //   out.println( mV1.x + " " + mV1.y + " " + mV1.z + " " + mV2.x + " " + mV2.y + " " + mV2.z );
  // }

}
