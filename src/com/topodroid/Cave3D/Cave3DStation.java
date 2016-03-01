/** @file Cave3DStation.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D station
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.util.Log;

public class Cave3DStation
{
  private static final String TAG = "Cave3D";

  int vertex;     // index of vertex (coords) in the array of vertices 
                  // to get the coords use 3*vertex+0, 3*vertex+1, 3*vertex+2
  int mId;
  int mSid; // survey id
  String short_name;
  String name;
  float e, n, z;  // north east, vertical (upwards)
  float depth;    // depth from Zmax: positive and scaled in [0,1]
                  // 1.0 deepest
  int flag;       // station flag (not used)
  String comment; // not used

  private void setName( String nm )
  {
    name = nm;
    if ( name != null ) {
      int index = name.indexOf("@");
      if ( index > 0 ) {
        short_name = name.substring( 0, index );
      } else {
        short_name = name;
      }
    } else {
      short_name = "";
    }
  }

  public Cave3DStation( String nm, float e0, float n0, float z0 )
  {
    mId    = -1;
    mSid   = -1;
    vertex = -1;
    setName( nm );
    e = e0;
    n = n0;
    z = z0;
    flag    = 0;
    comment = null;
  }

  public Cave3DStation( String nm, float e0, float n0, float z0, int id, int sid, int fl, String cmt )
  {
    mId    = id;
    mSid   = sid;
    vertex = -1;
    setName( nm );
    e = e0;
    n = n0;
    z = z0;
    flag    = fl;
    comment = cmt;
  }

  Cave3DVector toCave3DVector() { return new Cave3DVector( e, n, z ); }

}

