/** @file Cave3DSurvey.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D survey
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.util.Log;

public class Cave3DSurvey
{
  private static final String TAG = "Cave3D";
  private static int count = 0;
  int number;

  String name;
  int mNrShots;
  int mNrSplays;
  double mLenShots;
  double mLenSplays;

  Cave3DSurvey( String n )
  {
    name = n;
    number = count;
    ++ count;
    mNrShots = 0;
    mNrSplays = 0;
    mLenShots = 0.0;
    mLenSplays = 0.0;
  }

  void addShotInfo( Cave3DShot sh )
  {
    mNrShots ++;
    mLenShots += sh.len;
  }

  void addSplayInfo( Cave3DShot sh )
  {
    mNrSplays ++;
    mLenSplays += sh.len;
  }

}


