/** @file Cave3DFix.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D fixed station
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

// import android.util.Log;

class Cave3DFix extends Vector3D
{
  // private static final String TAG = "Cave3D";

  /** fix station:
   * fix stations are supposed to be referred to the same coord system
   */
  Cave3DCS cs;
  String name;
  // float e, n, z; // north east, vertical (upwards)
  
  double longitude; // WGS84
  double latitude; 
  boolean hasWGS84;

  public Cave3DFix( String nm, float e0, float n0, float z0, Cave3DCS cs0, double lng, double lat )
  {
    super( e0, n0, z0 );
    name = nm;
    cs = cs0;
    longitude = lng;
    latitude  = lat;
    hasWGS84  = true;
  }

  public Cave3DFix( String nm, float e0, float n0, float z0, Cave3DCS cs0 )
  {
    super( e0, n0, z0 );
    name = nm;
    cs = cs0;
    longitude = 0;
    latitude  = 0;
    hasWGS84  = false;
  }

}
