/** @file Cave3DCS.java
 *
 * @author marco corvi
 * @date mav 2020
 *
 * @brief Cave3D coordinate system
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.util.Log;

public class Cave3DCS
{
  String name; // CS name
  // String proj4; // proj4 syntax CS description

  public Cave3DCS( String nm )
  {
    name = nm;
  }

  public boolean equals( Cave3DCS cs ) { return (cs != null) && name.equals( cs.name ); }

}

