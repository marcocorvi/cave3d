/** @file Cave3DProjection.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief a normal plane projection vector
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

class Cave3DProjection 
{
  Cave3DShot   shot;
  Cave3DVector vector;  // 3D vector (absolute coords)
  // Cave3DVector proj3d;  // plane projection (absolute coords)
  Cave3DVector proj;    // plane projection (relative coords: origin at the station first, at the hull center later)
  float angle;         // angle with the reference projection

  /** 
   * @param s   splay shot
   * @param n   normal to the plane
   */
  Cave3DProjection( Cave3DStation st, Cave3DShot s, Cave3DVector normal )
  {
    shot   = s;
    angle  = 0.0f;
    if ( shot != null ) {
      vector = s.toCave3DVector();
      float vn = vector.dot( normal );
      proj = vector.minus( normal.times(vn) );  // P = V - (V*N) N

      // proj3d = new Cave3DVector( st.e + proj.x, st.n + proj.y, st.z + proj.z );

      // make vector in absolute ref system
      if ( Cave3D.mUseSplayVector ) { // if using splay vectors
        vector.x += st.e;
        vector.y += st.n;
        vector.z += st.z;
      } else {
        vector.x = st.e + proj.x;
        vector.y = st.n + proj.y;
        vector.z = st.z + proj.z;
      }
    } else {
      proj   = new Cave3DVector( 0, 0, 0 );
      vector = new Cave3DVector( st.e, st.n, st.z );
    }
  }
}
