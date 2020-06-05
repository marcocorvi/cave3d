/* @file SurveyFixed.java
 *
 * @author marco corvi
 * @date may 2012
 *
 * @brief survey fixed point - as in TopoDroid database
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

class SurveyFixed
{
  String station;
  double mLongitude;
  double mLatitude;
  double mAltitude;
  double mAltimetric;
  String mCsName = null;
  double mCsLongitude;
  double mCsLatitude;
  double mCsAltitude;

  void log()
  { 
    Log.v("TopoGL","fix " + station + " " + mLongitude + " " + mLatitude + " <" + ((mCsName != null)? mCsName : "null" ) + ">" );
  }

  SurveyFixed( String name )
  {
    station = name;
  }

  boolean hasCS() { return ( mCsName != null ) && ( mCsName.length() > 0 ); }
}
