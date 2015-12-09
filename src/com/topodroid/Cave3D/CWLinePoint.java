/** @file CWLinePoint.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief CW triangles intersection line-point
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

// import java.io.PrintStream;

public class CWLinePoint extends Cave3DVector 
{
  float mAlpha;  // line abscissa
  CWSide mSide;    // side to which the point belongs
  CWTriangle mTri; // triangle to which the side belongs
  
  CWLinePoint()
  {
    super(0,0,0);
    mAlpha = 0;
    mSide = null;
    mTri  = null;
  }
  
  CWLinePoint( float a, CWSide s, CWTriangle t, float x, float y, float z )
  {
    super( x,y,z);
    mAlpha = a;
    mSide = s;
    mTri  = t;
  }
  
  CWLinePoint( float a, CWSide s, CWTriangle t, Cave3DVector v )
  {
    super(v.x, v.y, v.z );
    mAlpha = a;
    mSide = s;
    mTri  = t;
  }
  
  void copy( float a, CWSide s, CWTriangle t, Cave3DVector v )
  {
    mAlpha = a;
    mSide = s;
    mTri  = t;
    copy( v );
  }
  
  // void dump( PrintStream out )
  // {
  //   CWTriangle t = mSide.otherTriangle( mTri );
  //   Log.v("Cave3D", "LP " + mTri.mCnt + "/" + mSide.mCnt + "/" + t.mCnt );
  // }
}
