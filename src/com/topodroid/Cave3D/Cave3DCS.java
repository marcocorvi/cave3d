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

class Cave3DCS
{
  final static String WGS84 = "WGS-84";

  String name; // CS name
  // String proj4; // proj4 syntax CS description

  Cave3DCS( ) { name = WGS84; }
  Cave3DCS( String nm ) { name = nm; }

  boolean hasName() { return ( name != null ) && ( name.length() > 0 ); }

  boolean equals( Cave3DCS cs ) { return (cs != null) && equals( cs.name ); }
  boolean equals( String cs_name ) { return (cs_name != null) && ( cs_name.length() > 0 ) && name.equals( cs_name ); }

  boolean isWGS84() { return name.equals( WGS84 ); }

}

