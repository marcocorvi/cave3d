/** @file Cave3DCS.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D fixed station
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.util.Log;

public class Cave3DCS
{
  private static final String TAG = "Cave3D";

  /** coordinate system
   */
  String name; // CS name
  // String proj4; // proj4 syntax CS description

  public Cave3DCS( String nm )
  {
    name = nm;
  }

}

