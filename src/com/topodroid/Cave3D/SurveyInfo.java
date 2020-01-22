/* @file SurveyInfo.java
 *
 * @author marco corvi
 * @date may 2012
 *
 * @brief TopoDroid survey info (name, date, comment etc)
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

// import android.util.Log;

class SurveyInfo
{
  final static float DECLINATION_MAX = 720;    // twice 360
  final static float DECLINATION_UNSET = 1080; // three times 360

  long id;
  String name;
  String date;
  String team;
  float  declination;

  boolean hasDeclination() { return declination < DECLINATION_MAX; }

  // get the declination or 0 if not-defined
  float getDeclination()
  {
    if ( declination < DECLINATION_MAX ) return declination;
    return 0;
  }

}
