/** @file Cave3DShot.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D shot
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.util.Log;

public class Cave3DShot
{
  private static final String TAG = "Cave3D";

  private static final float DEG2RAD = (float)(Math.PI/180);
  String from;
  String to;
  Cave3DStation from_station;
  Cave3DStation to_station;  // null for splay shots
  float len, ber, cln;      // radians
  Cave3DSurvey survey;
  int surveyNr;
  boolean used;

  public Cave3DShot( String f, String t, float l, float b, float c )
  {
    from = f;
    to   = t;
    len = l;
    ber = b * DEG2RAD;
    cln = c * DEG2RAD;
    from_station = null;
    to_station   = null;
    used = false;
    survey = null;
    surveyNr = 0;
  }

  public Cave3DStation getStationFromStation( Cave3DStation st ) 
  {
    if ( st.name.equals( from ) ) {
      float dz = len * (float)Math.sin( cln );
      float dh = len * (float)Math.cos( cln );
      return new Cave3DStation( to, 
                          st.e + dh * (float)Math.sin(ber),
                          st.n + dh * (float)Math.cos(ber),
                          st.z + dz );
    } else if ( st.name.equals( to ) ) {
      float dz = len * (float)Math.sin( cln );
      float dh = len * (float)Math.cos( cln );
      return new Cave3DStation( from,
                          st.e - dh * (float)Math.sin(ber),
                          st.n - dh * (float)Math.cos(ber),
                          st.z - dz );
    } else {
      return null;
    }
  }

  float depth()
  {
    if ( to_station == null ) return 0.0f;
    return (from_station.depth + to_station.depth)/2;
  }

  Cave3DVector toCave3DVector() 
  {
    float h = len * (float)Math.cos(cln);
    return new Cave3DVector( h * (float)Math.sin(ber), h * (float)Math.cos(ber), len * (float)Math.sin(cln) );
  }

}

