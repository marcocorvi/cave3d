/** @file Cave3DDatParser.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D compass file parser
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

public class Cave3DDatParser extends Cave3DParser
{
  private static final String TAG = "Cave3D DAT";

  static final int FLIP_NONE       = 0;
  static final int FLIP_HORIZONTAL = 1;
  static final int FLIP_VERTICAL   = 2;

  static final int DATA_NONE      = 0;
  static final int DATA_NORMAL    = 1;
  static final int DATA_DIMENSION = 2;


  public Cave3DDatParser( Cave3D cave3d, String filename ) throws Cave3DParserException
  {
    super( cave3d, filename );

    if ( filename.endsWith(".mak") ) {
      readFile( filename );
    } else {
      readFile( filename, null, 0.0f, 0.0f, 0.0f );
    }
    processShots();
    setShotSurveys();
    setSplaySurveys();
    setStationDepths();
  }

  /** read input MAK file
   */
  private boolean readFile( String filename )
                  throws Cave3DParserException
  {
    if ( ! checkPath( filename ) ) return false;

    int linenr = 0;

    try {
      String dirname = "./";
      int i = filename.lastIndexOf('/');
      if ( i > 0 ) dirname = filename.substring(0, i+1);
      // Log.v( TAG, "reading MAK file " + filename + " dir " + dirname );

      FileReader fr = new FileReader( filename );
      BufferedReader br = new BufferedReader( fr );
      ++linenr;
      String line = br.readLine();
      // Log.v(TAG, linenr + ":" + line );
      while ( line != null ) {
        // line = line.trim();
        if ( line.startsWith( "#" ) ) {
          i = line.lastIndexOf( ',' );
	  String file = line.substring(1,i);

          ++linenr; line = br.readLine();
          line = line.trim();
          if ( line.length() == 0 ) continue; // no georeference
	  i = line.indexOf( '[' );
          if ( i <= 0 ) continue; // missing station name
	  String station = line.substring(0,i);
	  int j = line.indexOf( ']' );
          if ( j <= i+3 ) continue; // bad syntax
	  String data = line.substring( i+3, j );
          // Log.v(TAG, "++ " + linenr + ": " + station + " - " + data );
          String[] vals = data.split( "," );
          if ( vals.length >= 3 ) {
            try {
              int idx = nextIndex( vals, -1 );
	      float x = Float.parseFloat( vals[idx] );
              idx = nextIndex( vals, idx );
	      float y = Float.parseFloat( vals[idx] );
              idx = nextIndex( vals, idx );
	      float z = Float.parseFloat( vals[idx] );
	      readFile( dirname + file, station, x, y, z );
	    } catch ( NumberFormatException e ) {
	      Log.e( TAG, "Error file " + filename + ":" + linenr );
	    }
	  }
	}
      
        ++linenr; line = br.readLine();
        // Log.v(TAG, linenr + ":" + line );
      }
    } catch ( IOException e ) {
      Log.e( TAG, "I/O error " + e.getMessage() );
      throw new Cave3DParserException( filename, linenr );
    }
    // Log.v(TAG, "done readFile " + filename );

    return ( shots.size() > 0 );
  }

  /** read input DAT file
   */
  private boolean readFile( String filename, String station, float x, float y, float z )
                  throws Cave3DParserException
  {
    if ( ! checkPath( filename ) ) return false;

    int linenr = 0;
    // Log.v( TAG, "DAT file <" + filename + "> station " + station );
    Cave3DCS cs = null;
    // int in_data = 0; // 0 none, 1 normal, 2 dimension

    int[] survey_pos = new int[50]; // FIXME max 50 levels
    int ks = 0;
    boolean in_survey = false;

    float declination = 0.0f;
    float units_len = 0.3048f; // foot to meter
    float units_ber = 1;
    float units_cln = 1;
    int idx;

    float length, bearing, clino, left, up, down, right, back_bearing, back_clino;

    String survey = null;

    try {
      String dirname = "./";
      int i = filename.lastIndexOf('/');
      if ( i > 0 ) {
        dirname = filename.substring(0, i+1);
	survey  = "@" + filename.substring(i+1);
      } else {
        survey = "@" + filename;
      }
      survey.replace(".dat", "");
      // Log.v( TAG, "reading file " + filename + " dir " + dirname );

      FileReader fr = new FileReader( filename );
      BufferedReader br = new BufferedReader( fr );
      ++linenr;
      String line = br.readLine();
      // Log.v(TAG, linenr + ":" + line );
      int cnt_shot = 0;
      while ( line != null ) {
        line = line.trim();
        // "SURVEY NAME" not used
        // "SURVEY DATE" not used
        // "SURVEY TEAM" not used
	if ( line.startsWith( "DECLINATION:" ) ) {
          String[] vals = splitLine( line );
          idx = nextIndex( vals, -1 );
          idx = nextIndex( vals, idx );
	  try {
            declination = Float.parseFloat( vals[idx] );
	    // Log.v( TAG, "declination " + declination );
	  } catch ( NumberFormatException e ) { }
	} else if ( line.contains("FROM") && line.contains("TO" ) ) {
          ++linenr; line = br.readLine();
          // Log.v(TAG, linenr + ":" + line );
	  for ( ; ; ) {
	    if ( line.length() == 0 ) {
              ++linenr; line = br.readLine();
              // Log.v(TAG, linenr + ":" + line );
              continue;
	    }
	    if ( line.charAt(0) == 0x0c ) {
              // Log.v( TAG, "formfeed");
              break; // formfeed
	    }
            String[] vals = splitLine( line );
	    if ( vals.length >= 5 ) { // FROM TO LEN BEAR INC L U D R FLAGS COMMENT
              idx = nextIndex( vals, -1 );
              String f0   = vals[idx];
	      String from = vals[idx] + survey;
	      if ( station == null ) station = f0;
              idx = nextIndex( vals, idx );
              String to   = vals[idx] + survey;
	      try {
                idx = nextIndex( vals, idx );
                length = Float.parseFloat( vals[idx] ) * units_len;
                idx = nextIndex( vals, idx );
                bearing = Float.parseFloat( vals[idx] );
                idx = nextIndex( vals, idx );
                clino = Float.parseFloat( vals[idx] );
                left  = -999;
                up    = -999;
                down  = -999;
                right = -999;
                if ( vals.length >= 9 ) {
                  idx = nextIndex( vals, idx );
		  left = Float.parseFloat( vals[idx] ); // LEFT
                  idx = nextIndex( vals, idx );
		  up = Float.parseFloat( vals[idx] ); // UP
                  idx = nextIndex( vals, idx );
		  down = Float.parseFloat( vals[idx] ); // DOWN
                  idx = nextIndex( vals, idx );
		  right = Float.parseFloat( vals[idx] ); // RIGHT
                  if ( vals.length >= 11 ) {
                    idx = nextIndex( vals, idx );
                    if (vals[idx].startsWith("#")) {
                      // mFlag = vals[idx];
                      // if ( k < kmax ) mComment = TDUtil.concat( vals, k );
                    } else if ( bearing < -900 || clino < -900 ) {
                      back_bearing = Float.parseFloat(vals[idx]) + 180;
                      if ( back_bearing >= 360 ) back_bearing -= 360;
                      if ( bearing < -900 ) {
                        bearing = back_bearing;
                      } else if ( back_bearing >= 0 && back_bearing <= 360 ) {
                        if ( Math.abs( bearing - back_bearing ) > 180 ) {
                          bearing = ( bearing + back_bearing + 360 ) / 2;
                          if ( bearing >= 360 ) bearing -= 360;
                        } else {
                          bearing = ( bearing + back_bearing ) / 2;
                        }
                      }
                      idx = nextIndex( vals, idx );
                      back_clino = Float.parseFloat(vals[idx]);
                      if ( clino < -900 ) {
                        clino = - back_clino;
                      } else if ( back_clino >= -90 && back_clino <= 90 ) {
                        clino = ( clino - back_clino ) / 2;
                      }
                 
                      // if ( vals.length >= 12 ) {
                      //   idx = nextIndex( vals, idx );
                      //   if (vals[idx].startsWith("#")) {
                      //     // mFlag = vals[idx];
                      //     // if ( k < kmax ) mComment = TDUtil.concat( vals, k );
                      //   }
                      // }
                    }
                  }
                }
                if ( bearing >= 360 ) bearing -= 360;
                else if ( bearing < 0 ) bearing += 360;

                bearing += declination;
                shots.add( new Cave3DShot( from, to, length, bearing, clino ) );
                ++ cnt_shot;
		if ( left > 0 ) splays.add( new Cave3DShot( from, f0+"-L"+survey, left, bearing-90, 0 ) );
		if ( up > 0 ) splays.add( new Cave3DShot( from, f0+"-U"+survey, up, bearing, 90 ) );
		if ( down > 0 ) splays.add( new Cave3DShot( from, f0+"-D"+survey, down, bearing, -90 ) );
		if ( right > 0 ) splays.add( new Cave3DShot( from, f0+"-R"+survey, right, bearing+90, 0 ) );

	      } catch ( NumberFormatException e ) { }
	    }
            ++linenr; line = br.readLine();
            // Log.v(TAG, linenr + ":" + line );
	  }
	}
        ++linenr; line = br.readLine();
        // Log.v(TAG, linenr + ":" + line );
      }
      if ( station != null ) {
        // Log.v( TAG, "add fix station " +  station + survey );
	fixes.add( new Cave3DFix( station+survey, x, y, z, cs ) );
      }
    } catch ( IOException e ) {
      Log.e( TAG, "I/O error " + e.getMessage() );
      throw new Cave3DParserException( filename, linenr );
    }
    // Log.v(TAG, "shots " + shots.size() );
    return ( shots.size() > 0 );
  }

  private void setShotSurveys()
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

  private void setSplaySurveys()
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

  private void processShots()
  {
    if ( shots.size() == 0 ) return;
    if ( fixes.size() == 0 ) {
      // Log.v( TAG, "shots " + shots.size() + " fixes " + fixes.size() );
      Cave3DShot sh = shots.get( 0 );
      fixes.add( new Cave3DFix( sh.from, 0.0f, 0.0f, 0.0f, null ) );
    }
 
    int mLoopCnt = 0;
    Cave3DFix f0 = fixes.get( 0 );
    // Log.v( TAG, "Process Shots. Fix " + f0.name + " " + f0.e + " " + f0.n + " " + f0.z );

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

    computeBoundingBox();
    // // bounding box
    // emin = emax = stations.get(0).e;
    // nmin = nmax = stations.get(0).n;
    // zmin = zmax = stations.get(0).z;
    // for ( Cave3DStation s : stations ) {
    //   if ( nmin > s.n )      nmin = s.n;
    //   else if ( nmax < s.n ) nmax = s.n;
    //   if ( emin > s.e )      emin = s.e;
    //   else if ( emax < s.e ) emax = s.e;
    //   if ( zmin > s.z )      zmin = s.z;
    //   else if ( zmax < s.z ) zmax = s.z;
    // }
  }

  static int nextIndex( String[] vals, int idx )
  {
    ++idx;
    while ( idx < vals.length && vals[idx].length() == 0 ) ++idx;
    return idx;
  }

  static int prevIndex( String[] vals, int idx )
  {
    --idx;
    while ( idx >= 0 && vals[idx].length() == 0 ) --idx;
    return idx;
  }

}
