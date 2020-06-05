/** @file Cave3DShot.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D shot
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

// import android.util.Log;

public class Cave3DShot
{
  private static final double DEG2RAD = (Math.PI/180);

  static final long FLAG_SURVEY     =  0; // flags
  static final long FLAG_SURFACE    =  1;
  static final long FLAG_DUPLICATE  =  2;
  static final long FLAG_COMMENTED  =  4; // lox-flag NOT_VISIBLE
  // static final long FLAG_NO_PLAN    =  8;
  // static final long FLAG_NO_PROFILE = 16;
  // static final long FLAG_BACKSHOT   = 32;

  String from;
  String to;
  Cave3DStation from_station;
  Cave3DStation to_station;  // null for splay shots
  double len, ber, cln;      // radians
  Cave3DSurvey mSurvey;
  int mSurveyNr;
  boolean used = false;
  long mFlag;
  long mMillis;

  public boolean isSurvey()    { return mFlag == FLAG_SURVEY; }
  public boolean isSurface()   { return (mFlag & FLAG_SURFACE)    == FLAG_SURFACE; }
  public boolean isDuplicate() { return (mFlag & FLAG_DUPLICATE)  == FLAG_DUPLICATE; }
  public boolean isCommented() { return (mFlag & FLAG_COMMENTED)  == FLAG_COMMENTED; } 

  // b/c in degrees
  public Cave3DShot( String f, String t, double l, double b, double c, long flag, long millis )
  {
    from = f;
    to   = t;
    len = l;
    ber = b * DEG2RAD;
    cln = c * DEG2RAD;
    used = false;
    from_station = null;
    to_station   = null;
    mSurvey = null;
    mSurveyNr = 0;
    mFlag = flag;
    mMillis = millis;
  }

  // used for cave pathlength between stations
  // b,c radians
  public Cave3DShot( Cave3DStation f, Cave3DStation t, double l, double b, double c, long flag, long millis )
  {
    from = (f!=null)? f.name : null;
    to   = (t!=null)? t.name : null;
    len = l;
    ber = b;
    cln = c;
    used = false;
    from_station = f;
    to_station   = t;
    mSurvey   = null;
    mSurveyNr = 0;
    mFlag = flag;
    mMillis = millis;
  }

  boolean hasSurvey() { return mSurvey != null; }

  void setSurvey( Cave3DSurvey survey ) { mSurvey = survey; }
  Cave3DSurvey getSurvey() { return mSurvey; }

  /* dot product 
   * ( cc1 * cb1, cc1 * sb1, sc1 ) * ( cc2 * cb2, cc2 * sb2, sc2 )
   *   = cc1 * cc2 * cos(b1-b2) + sc1 * sc2
   */
  double dotProduct( Cave3DShot sh )
  {
    return Math.cos( ber - sh.ber ) * Math.cos( cln ) * Math.cos( sh.cln ) + Math.sin( cln ) * Math.sin( sh.cln );
  }

  public Cave3DStation getStationFromStation( Cave3DStation st ) 
  {
    if ( st.name.equals( from ) ) {
      double dz = len * Math.sin( cln );
      double dh = len * Math.cos( cln );
      return new Cave3DStation( to, 
                          st.x + (dh * Math.sin(ber)),
                          st.y + (dh * Math.cos(ber)),
                          st.z + (dz) );
    } else if ( st.name.equals( to ) ) {
      double dz = len * Math.sin( cln );
      double dh = len * Math.cos( cln );
      return new Cave3DStation( from,
                          st.x - (dh * Math.sin(ber)),
                          st.y - (dh * Math.cos(ber)),
                          st.z - (dz) );
    } else {
      return null;
    }
  }

  // average depth of the shot
  double depth()
  {
    if ( to_station == null ) return 0.0f;
    return (from_station.depth + to_station.depth)/2;
  }

  // return the 3D vector (E, N, Up )
  Vector3D toVector3D() 
  {
    double h = len * Math.cos(cln);
    return new Vector3D( (h * Math.sin(ber)), (h * Math.cos(ber)), (len * Math.sin(cln)) );
  }

  // makes sense only for splays
  Vector3D toPoint3D()
  {
    int sign = 1;
    Cave3DStation st = from_station;
    if ( st == null ) {
      st = to_station;
      sign = -1;
    }
    if ( st == null ) return null;
    double h = sign * len * Math.cos(cln);
    return new Vector3D( st.x + (h * Math.sin(ber)), st.y + (h * Math.cos(ber)), st.z + sign * (len * Math.sin(cln)) );
  }

  Cave3DStation getOtherStation( Cave3DStation st )
  {
    if ( st == from_station ) return to_station;
    if ( st == to_station )   return from_station;
    return null;
  }

}

