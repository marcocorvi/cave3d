/* @file DBlock.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief DistoX survey data as from TopoDroid database
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

// import java.lang.Long;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Locale;

class DBlock
{
  long   mId;
  long   mMillis;
  long   mSurveyId;
  String mFrom;    // N.B. mfrom and mTo must be not null
  String mTo;
  double mLength;   // meters
  double mBearing;  // degrees
  double mClino;    // degrees
  int   mFlag;

  // used by PocketTopo parser only
  DBlock( long id, long sid, long millis, String f, String t, double d, double b, double c, int flag )
  {
    // assert( f != null && t != null );
    mId   = id;
    mSurveyId = sid;
    mMillis = millis;
    // mName = "";
    mFrom = f;
    mTo   = t;
    mLength  = d;
    mBearing = b;
    mClino   = c;
    mFlag    = flag;
  }

  DBlock()
  {
    mId = 0;
    mMillis = 0;
    mSurveyId = 0;
    // mName = "";
    mFrom = "";
    mTo   = "";
    mLength = 0.0f;
    mBearing = 0.0f;
    mClino = 0.0f;
    mFlag  = 0;
  }

  void setBlockName( String from, String to )
  {
    if ( from == null || to == null ) {
      Log.e( "TopoGL", "Error DBlock::setName() either from or to is null");
      return;
    }
    mFrom = from.trim();
    mTo   = to.trim();
  }

}

