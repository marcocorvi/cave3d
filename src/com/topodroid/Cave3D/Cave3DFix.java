/** @file Cave3DFix.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D fixed station
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 * ref. T. Soler, L.D. Hothem
 *      Coordinate systems usd in geodesy: basic definitions and concepts, 1988
 */
package com.topodroid.Cave3D;

import android.util.Log;

class Cave3DFix extends Vector3D
{
  // private static final String TAG = "Cave3D";
  static private final double EARTH_RADIUS1 = (6378137 * Math.PI / 180.0f); // semimajor axis [m]
  static private final double EARTH_RADIUS2 = (6356752 * Math.PI / 180.0f);

  static private final double EARTH_A = 6378137;
  static private final double EARTH_B = 6356752;
  static private final double EARTH_C = Math.sqrt( EARTH_A * EARTH_A - EARTH_B * EARTH_B );
  static private final double EARTH_E = EARTH_C / EARTH_A;
  static private final double EARTH_E2 = EARTH_E * EARTH_E;
  static private final double EARTH_1E2 = 1.0 - EARTH_E2; // (1- e^2)

  /** fix station:
   * fix stations are supposed to be referred to the same coord system
   */
  Cave3DCS cs;
  String name;
  // double e, n, z; // north east, vertical (upwards)
  
  double longitude; // WGS84
  double latitude; 
  boolean hasWGS84;

  boolean hasCS() { return cs != null && cs.hasName(); }

  void log()
  {
    Log.v("TopoGL", "origin " + name + " CS " + cs.name + " " + longitude + " " + latitude );
  }

  public Cave3DFix( String nm, double e0, double n0, double z0, Cave3DCS cs0, double lng, double lat )
  {
    super( e0, n0, z0 );
    name = nm;
    cs = cs0;
    longitude = lng;
    latitude  = lat;
    hasWGS84  = true;
  }

  public Cave3DFix( String nm, double e0, double n0, double z0, Cave3DCS cs0 )
  {
    super( e0, n0, z0 );
    name = nm;
    cs = cs0;
    longitude = 0;
    latitude  = 0;
    hasWGS84  = false;
  }

  public boolean isWGS84() { return cs.isWGS84(); }

  double getSNradius() 
  { 
    if ( isWGS84() ) {
      // double s = Math.sin( latitude * Math.PI/180 );
      // double W = Math.sqrt( 1 - EARTH_E2 * s * s );
      // return EARTH_A * EARTH_1E2 / W;
      double alat = Math.abs( latitude );
      return ((90 - alat) * EARTH_RADIUS1 + alat * EARTH_RADIUS2)/90;
    }
    return 1.0;
  }

  double getWEradius() 
  {
    if ( isWGS84() ) {
      // double s = Math.sin( latitude * Math.PI/180 );
      // double W = Math.sqrt( 1 - EARTH_E2 * s * s );
      // return EARTH_A / W;
      double alat = Math.abs( latitude );
      double s_radius = ((90 - alat) * EARTH_RADIUS1 + alat * EARTH_RADIUS2)/90;
      return s_radius * Math.cos( alat * Math.PI / 180 );
    }
    return 1.0;
  }

}
