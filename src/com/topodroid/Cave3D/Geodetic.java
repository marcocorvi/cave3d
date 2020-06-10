/** @file Geodetic.java
 *
 * @author marco corvi
 * @date june 2020
 *
 * @brief TopoDroid geodetic data and functions
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 * ref. T. Soler, L.D. Hothem
 *      Coordinate systems usd in geodesy: basic definitions and concepts, 1988
 */
package com.topodroid.Cave3D;

// import android.util.Log;

class Geodetic
{
  static private final double EARTH_A = 6378137.0;
  static private final double EARTH_B = 6356752;
  static private final double EARTH_C = Math.sqrt( EARTH_A * EARTH_A - EARTH_B * EARTH_B );
  static private final double EARTH_E = EARTH_C / EARTH_A;
  static private final double EARTH_E2 = EARTH_E * EARTH_E;
  static private final double EARTH_1E2 = 1.0 - EARTH_E2; // (1- e^2)

  static double meridianRadiusExact( double latitude )
  {
    double s = Math.sin( latitude * Math.PI/180 );
    double W = Math.sqrt( 1 - EARTH_E2 * s * s );
    return (EARTH_A * EARTH_1E2 / W) * Math.PI/180.0;
  }

  static double parallelRadiusExact( double latitude )
  {
    double s = Math.sin( latitude * Math.PI/180 );
    double W = Math.sqrt( 1 - EARTH_E2 * s * s );
    return (EARTH_A / W) * Math.PI/180.0;
  }

  // ---------------------------------------------------
  // approximation

  static private final double EARTH_RADIUS1 = (6378137 * Math.PI / 180.0f); // semimajor axis [m]
  static private final double EARTH_RADIUS2 = (6356752 * Math.PI / 180.0f);

  static double meridianRadiusApprox( double latitude )
  {
    double alat = Math.abs( latitude );
    return ((90 - alat) * EARTH_RADIUS1 + alat * EARTH_RADIUS2)/90;
  }

  static double parallelRadiusApprox( double latitude )
  {
    double alat = Math.abs( latitude );
    double s_radius = ((90 - alat) * EARTH_RADIUS1 + alat * EARTH_RADIUS2)/90;
    return s_radius * Math.cos( alat * Math.PI / 180 );
  }

}
