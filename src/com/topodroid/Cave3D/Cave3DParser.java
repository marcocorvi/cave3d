/** @file Cave3DParser.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D therion file parser and model
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.util.Log;

public class Cave3DParser
{
  protected static final String TAG = "Cave3D Parser";

  boolean do_render;
  Cave3D mCave3D;

  protected ArrayList< Cave3DSurvey > surveys;
  protected ArrayList< Cave3DFix > fixes;
  protected ArrayList< Cave3DStation > stations;
  protected ArrayList< Cave3DShot > shots;   // centerline shots
  protected ArrayList< Cave3DShot > splays;  // splay shots
  public float emin, emax, nmin, nmax, zmin, zmax;
  protected Cave3DSurface mSurface;

  // public float getEmin() { return emin; }
  // public float getEmax() { return emax; }
  // public float getNmin() { return nmin; }
  // public float getNmax() { return nmax; }
  // public float getVmin() { return zmin; }
  // public float getVmax() { return zmax; }

  public int getStationNumber() { return stations.size(); }
  public int getShotNumber()    { return shots.size(); }
  public int getSplayNumber()   { return splays.size(); }
  public int getSurveySize()    { return surveys.size(); }

  public ArrayList< Cave3DSurvey > getSurveys() { return surveys; }
  public ArrayList< Cave3DShot > getShots() { return shots; }
  public ArrayList< Cave3DShot > getSplays() { return splays; }
  public ArrayList< Cave3DStation > getStations() { return stations; }
  public ArrayList< Cave3DFix > getFixes() { return fixes; }

  public Cave3DSurface getSurface() { return mSurface; }

  Cave3DSurvey getSurvey( String name )
  {
    for ( Cave3DSurvey s : surveys ) {
      if ( name.equals( s.name ) ) return s;
    }
    return null;
  }

  float mCaveLength;

  float getCaveDepth() { return zmax - zmin; }
  float getCaveLength() { return mCaveLength; }

  public float[] getStationVertices()
  {
    float v[] = new float[ 3 * stations.size() ];
    int k = 0;
    int k3 = 0;
    for ( Cave3DStation s : stations ) {
      s.vertex = k++;
      v[k3++] = s.e; // X horizontal
      v[k3++] = s.n; // Y vertical
      v[k3++] = s.z; // Z depth
    }
    return v;
  }
 
  /** 3D vertices of the centerline shots
   */
  // public float[] getShotVertices()
  // {
  //   float v[] = new float[ 3 * 2 * shots.size() ];
  //   int k = 0;
  //   for ( Cave3DShot sh : shots ) {
  //     Cave3DStation sf = sh.from_station;
  //     Cave3DStation st = sh.to_station;
  //     // Log.v(TAG, "getShotVertices shot " + sh.from + " " + sh.to );
  //     if ( sf != null && st != null ) {
  //       v[k++] = sf.e;
  //       v[k++] = sf.z;
  //       v[k++] = sf.n;
  //       v[k++] = st.e;
  //       v[k++] = st.z;
  //       v[k++] = st.n;
  //     } else {
  //       if ( sf == null ) Log.e( TAG, " shot has null from station" );
  //       if ( st == null ) Log.e( TAG, " shot has null to station" );
  //     }
  //   }
  //   return v;
  // }


  /** 3D vertices of the splay shots
   */
  // public float[] getSplayVertices()
  // {
  //   float v[] = new float[ 3 * 2 * splays.size() ];
  //   int k = 0;
  //   for ( Cave3DShot sh : splays ) {
  //     Cave3DStation sf = sh.from_station;
  //     Cave3DStation st = sh.to_station;
  //     // Log.v(TAG, "getShotVertices shot " + sh.from + " " + sh.to );
  //     if ( sf != null ) {
  //       v[k++] = sf.e;
  //       v[k++] = sf.z;
  //       v[k++] = sf.n;
  //       v[k++] = st.e;
  //       v[k++] = st.z;
  //       v[k++] = st.n;
  //     } else {
  //       if ( sf == null ) Log.e( TAG, " shot has null from station" );
  //     }
  //   }
  //   return v;
  // }

  void setShotSurveys()
  {
    for ( Cave3DShot sh : shots ) {
      Cave3DStation sf = sh.from_station;
      Cave3DStation st = sh.to_station;
      sh.survey = null;
      if ( sf != null && st != null ) {
        String sv = sh.from;
        sv = sv.substring( 1 + sv.indexOf('@', 0) );
        for ( Cave3DSurvey srv : surveys ) {
          if ( srv.name.equals( sv ) ) {
            sh.survey = srv;
            sh.surveyNr = srv.number;
            srv.addShotInfo( sh );
            break;
          }
        }
        if ( sh.survey == null ) {
          Cave3DSurvey survey = new Cave3DSurvey(sv);
          sh.survey = survey;
          sh.surveyNr = survey.number;
          surveys.add( survey );
          survey.addShotInfo( sh );
        } 
      }
    }
  }

  void setSplaySurveys()
  {
    for ( Cave3DShot sh : splays ) {
      String sv = null;
      Cave3DStation sf = sh.from_station;
      if ( sf == null ) {
        sf = sh.to_station;
        sv = sh.to;
      } else {
        sv = sh.from;
      }
      if ( sf != null ) {
        sv = sv.substring( 1 + sv.indexOf('@', 0) );
        for ( Cave3DSurvey srv : surveys ) {
          if ( srv.name.equals( sv ) ) {
            // sh.survey = srv;
            // sh.surveyNr = srv.number;
            srv.addSplayInfo( sh );
            break;
          }
        }
      }
    }
    
  }

  ArrayList< Cave3DShot > getLegAt( Cave3DStation station )
  {
    ArrayList< Cave3DShot > ret = new ArrayList< Cave3DShot >();
    for ( Cave3DShot shot : shots ) {
      if ( shot.from_station == station ) {
        ret.add( shot );
      } else if ( shot.to_station == station ) {
        double b = shot.ber + Math.PI;
        if ( b > 2*Math.PI ) b -= 2*Math.PI;
        ret.add( new Cave3DShot( null, null, shot.len, (float)b, -shot.cln) );
      }
    }
    return ret;
  }

  /** get the legs at "station"
   *  the legs that do not connect "other" are reversed
   *  Therefore the return has legs
   *      station - other
   *      xxx - station
   */
  ArrayList< Cave3DShot > getLegsAt( Cave3DStation station, Cave3DStation other )
  {
    // Log.v("Cave3D", "get legs at " + station.name + " other " + other.name );
    ArrayList< Cave3DShot > ret = new ArrayList< Cave3DShot >();
    for ( Cave3DShot shot : shots ) { // add survey legs too1
      if ( shot.from_station == station ) {
        if ( shot.to_station == other ) {
          // Log.v("Cave3D", "add direct shot " + shot.from + "-" + shot.to );
          ret.add( shot );
        } else {
          // Log.v("Cave3D", "add other shot " + shot.to + "-" + shot.from );
          double b = shot.ber + Math.PI;
          if ( b > 2*Math.PI ) b -= 2*Math.PI;
          ret.add( new Cave3DShot( null, null, shot.len, (float)b, -shot.cln) ); // stations not important
        }
      } else if ( shot.to_station == station ) {
        if ( shot.from_station == other ) {
          // Log.v("Cave3D", "add reversed shot " + shot.to + "-" + shot.from );
          double b = shot.ber + Math.PI;
          if ( b > 2*Math.PI ) b -= 2*Math.PI;
          ret.add( new Cave3DShot( null, null, shot.len, (float)b, -shot.cln) ); // stations not important
        } else {
          // Log.v("Cave3D", "add other shot " + shot.from + "-" + shot.to );
          ret.add( shot );
        }
      }
    }
    return ret;
  }

  ArrayList< Cave3DShot > getSplayAt( Cave3DStation station, boolean also_legs )
  {
    ArrayList< Cave3DShot > ret = new ArrayList< Cave3DShot >();
    for ( Cave3DShot splay : splays ) {
      if ( splay.from_station == station ) {
        ret.add( splay );
      }
    }
    if ( also_legs ) {
      for ( Cave3DShot shot : shots ) { // add survey legs too1
        if ( shot.from_station == station ) {
          ret.add( shot );
        } else if ( shot.to_station == station ) {
          double b = shot.ber + Math.PI;
          if ( b > 2*Math.PI ) b -= 2*Math.PI;
          ret.add( new Cave3DShot( null, null, shot.len, (float)b, -shot.cln) ); // stations not important
        }
      }
    }
    return ret;
  }

  void setStationDepths( )
  {
    float deltaz = zmax - zmin + 0.001f;
    int k = 0;
    for ( Cave3DStation s : stations ) {
      s.depth = (zmax - s.z)/deltaz; // Z depth
      // Log.v(TAG, "Depth " + s.name + " " + s.depth ); 
    }
  }

  public Cave3DParser( Cave3D cave3d ) 
  {
    do_render = false;

    // Log.v("Cave3D", "parsing " + filename );

    mCave3D = cave3d;
    mSurface = null;

    fixes  = new ArrayList< Cave3DFix >();
    shots  = new ArrayList< Cave3DShot >();
    splays = new ArrayList< Cave3DShot >();
    surveys = new ArrayList< Cave3DSurvey >();
    stations = new ArrayList< Cave3DStation >();

  }

  protected void processShots()
  {
    if ( shots.size() == 0 ) return;
    if ( fixes.size() == 0 ) {
      // Log.v( TAG, "shots " + shots.size() + " fixes " + fixes.size() );
      Cave3DShot sh = shots.get( 0 );
      fixes.add( new Cave3DFix( sh.from, 0.0f, 0.0f, 0.0f ) );
    }
 
    int mLoopCnt = 0;
    Cave3DFix f0 = fixes.get( 0 );
    // Log.v( TAG, "fix " + f0.name + " " + f0.e + " " + f0.n );

    mCaveLength = 0.0f;

    for ( Cave3DFix f : fixes ) {
      boolean found = false;
      // Log.v( TAG, "checking fix " + f.name );
      for ( Cave3DStation s1 : stations ) {
        if ( f.name.equals( s1.name ) ) { found = true; break; }
      }
      if ( found ) { // skip fixed stations that are already included in the model
        // Log.v( TAG, "found fix " + f.name );
        continue;
      }
      // Log.v(TAG, "start station " + f.name + " N " + f.n + " E " + f.e + " Z " + f.z );
      stations.add( new Cave3DStation( f.name, f.e, f.n, f.z ) );
      // sh.from_station = s0;
    
      boolean repeat = true;
      while ( repeat ) {
        // Log.v(TAG, "scanning the shots");
        repeat = false;
        for ( Cave3DShot sh : shots ) {
          if ( sh.used ) continue;
          // Log.v(TAG, "check shot " + sh.from + " " + sh.to );
          // Cave3DStation sf = sh.from_station;
          // Cave3DStation st = sh.to_station;
          Cave3DStation sf = null;
          Cave3DStation st = null;
          for ( Cave3DStation s : stations ) {
            if ( sh.from.equals(s.name) ) {
              sf = s;
              if (  sh.from_station == null ) sh.from_station = s;
              else if ( sh.from_station != s ) Log.e( TAG, "shot " + sh.from + " " + sh.to + " from-station mismatch ");
            } 
            if ( sh.to.equals(s.name) )   {
              st = s;
              if (  sh.to_station == null ) sh.to_station = s;
              else if ( sh.to_station != s ) Log.e( TAG, "shot " + sh.from + " " + sh.to + " to-station mismatch ");
            }
            if ( sf != null && st != null ) break;
          }
          if ( sf != null && st != null ) {
            // Log.v( TAG, "unused shot " + sh.from + " " + sh.to + " : " + sf.name + " " + st.name );
            sh.used = true; // LOOP
            mCaveLength += sh.len;
            // make a fake station
            Cave3DStation s = sh.getStationFromStation( sf );
            stations.add( s );
            s.name = s.name + "-" + mLoopCnt;
            ++ mLoopCnt;
            sh.to_station = s;
          } else if ( sf != null && st == null ) {
            // Log.v( TAG, "unused shot " + sh.from + " " + sh.to + " : " + sf.name + " null" );
            Cave3DStation s = sh.getStationFromStation( sf );
            stations.add( s );
            sh.to_station = s;
            // Log.v(TAG, "add station " + sh.to_station.name + " N " + sh.to_station.n + " E " + sh.to_station.e + " Z " + sh.to_station.z );
            sh.used = true;
            mCaveLength += sh.len;
            repeat = true;
          } else if ( sf == null && st != null ) {
            // Log.v( TAG, "unused shot " + sh.from + " " + sh.to + " : null " + st.name );
            Cave3DStation s = sh.getStationFromStation( st );
            stations.add( s );
            sh.from_station = s;
            // Log.v(TAG, "add station " + sh.from_station.name + " N " + sh.from_station.n + " E " + sh.from_station.e + " Z " + sh.from_station.z );
            sh.used = true;
            mCaveLength += sh.len;
            repeat = true;
          } else {
            // Log.v( TAG, "unused shot " + sh.from + " " + sh.to + " : null null" );
          }
        }
      }
    } // for ( Cave3DFix f : fixes )

    // 3D splay shots
    for ( Cave3DShot sh : splays ) {
      if ( sh.used ) continue;
      if (  sh.from_station != null ) continue;
      // Log.v(TAG, "check shot " + sh.from + " " + sh.to );
      for ( Cave3DStation s : stations ) {
        if ( sh.from.equals(s.name) ) {
          sh.from_station = s;
          sh.used = true;
          sh.to_station = sh.getStationFromStation( s );
          break;
        }
      }
    }

    // bounding box
    emin = emax = stations.get(0).e;
    nmin = nmax = stations.get(0).n;
    zmin = zmax = stations.get(0).z;
    for ( Cave3DStation s : stations ) {
      if ( nmin > s.n )      nmin = s.n;
      else if ( nmax < s.n ) nmax = s.n;
      if ( emin > s.e )      emin = s.e;
      else if ( emax < s.e ) emax = s.e;
      if ( zmin > s.z )      zmin = s.z;
      else if ( zmax < s.z ) zmax = s.z;
    }
  }

  static int nextIndex( String[] vals, int idx )
  {
    ++idx;
     while ( idx < vals.length && vals[idx].length() == 0 ) ++idx;
     return idx;
  }
     
  static final int FLIP_NONE = 0;
  static final int FLIP_HORIZONTAL = 1;
  static final int FLIP_VERTICAL   = 2;

  static int parseFlip( String flip )
  {
    if ( flip.equals("horizontal") ) return FLIP_HORIZONTAL;
    if ( flip.equals("vertical") ) return FLIP_VERTICAL;
    return FLIP_NONE;
  }
    

}
