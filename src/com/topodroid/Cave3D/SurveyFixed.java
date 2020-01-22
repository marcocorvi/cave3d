/* @file SurveyFixed.java
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

class SurveyFixed
{
  String station;
  double mLongitude;
  double mLatitude;
  double mAltitude;
  double mAltimetric;

  SurveyFixed( String name )
  {
    station = name;
  }
}
