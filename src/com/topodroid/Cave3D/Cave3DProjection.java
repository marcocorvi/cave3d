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
  Vector3D vector;  // 3D vector (absolute coords)
  // Vector3D proj3d;  // plane projection (absolute coords)
  Vector3D proj;    // plane projection (relative coords: origin at the station first, at the hull center later)
  float angle;         // angle with the reference projection

  /** 
   * @param s   splay shot
   * @param n   normal to the plane
   */
  Cave3DProjection( Cave3DStation st, Cave3DShot s, Vector3D normal )
  {
    shot   = s;
    angle  = 0.0f;
    if ( shot != null ) {
      vector = s.toVector3D();
      float vn = vector.dotProduct( normal );
      proj = vector.difference( normal.scaledBy(vn) );  // P = V - (V*N) N

      // proj3d = new Vector3D( st.x + proj.x, st.y + proj.y, st.z + proj.z );

      // make vector in absolute ref system
      if ( TopoGL.mUseSplayVector ) { // if using splay vectors
        vector.x += st.x;
        vector.y += st.y;
        vector.z += st.z;
      } else {
        vector.x = st.x + proj.x;
        vector.y = st.y + proj.y;
        vector.z = st.z + proj.z;
      }
    } else {
      proj   = new Vector3D( 0, 0, 0 );
      vector = new Vector3D( st.x, st.y, st.z );
    }
  }
}
