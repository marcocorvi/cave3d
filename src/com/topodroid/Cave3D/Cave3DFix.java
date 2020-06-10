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
    return isWGS84()? Geodetic.meridianRadiusApprox( latitude ) : 1.0;
  }

  double getWEradius() 
  {
    return isWGS84()? Geodetic.parallelRadiusApprox( latitude ) : 1.0;
  }

}
