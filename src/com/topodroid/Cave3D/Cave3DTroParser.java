/** @file Cave3DTroParser.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D VisualTopo file parser
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

public class Cave3DTroParser extends Cave3DParser
{
  static final int FLIP_NONE       = 0;
  static final int FLIP_HORIZONTAL = 1;
  static final int FLIP_VERTICAL   = 2;

  static final int DATA_NONE      = 0;
  static final int DATA_NORMAL    = 1;
  static final int DATA_DIMENSION = 2;

  float declination = 0.0f;
  boolean dmb = false; // whether bearing is DD.MM
  boolean dmc = false;
  float ul = 1;  // units factor [m]
  float ub = 1;  // dec.deg
  float uc = 1;  // dec.deg
  int dirw = 1;  // width direction
  int dirb = 1;  // bearing direction
  int dirc = 1;  // clino direction

  public Cave3DTroParser( Cave3D cave3d, String filename ) throws Cave3DParserException
  {
    super( cave3d, filename );

    readFile( filename );
    processShots();
    setShotSurveys();
    setSplaySurveys();
    setStationDepths();
  }


  private float angle( float value, float unit, boolean dm )
  {
    if ( dm ) {
      int sign = 1;
      if ( value < 0 ) { sign = -1; value = -value; }
      int iv = (int)value;
      return sign * ( iv + (value-iv)*0.6f ); // 0.6 = 60/100
    }
    return value * unit;
  }

  /** read input TRO file
   */
  private boolean readFile( String filename )
                  throws Cave3DParserException
  {
    if ( ! checkPath( filename ) ) return false;

    int linenr = 0;
    // Log.v("Cave3D", "DAT file <" + filename + "> station " + station );
    Cave3DCS cs = null;
    // int in_data = 0; // 0 none, 1 normal, 2 dimension

    // String survey = null; // UNUSED


    boolean splayAtFrom = true;
    String comment = "";

    String line = null;
    try {
      // String dirname = "./";
      // int i = filename.lastIndexOf('/');
      // if ( i > 0 ) {
      //   dirname = filename.substring(0, i+1);
      //   survey  = "@" + filename.substring(i+1);
      // } else {
      //   survey = "@" + filename;
      // }
      // survey.replace(".tro", "");
      // Log.v( TAG, "reading file " + filename + " dir " + dirname );

      FileReader fr = new FileReader( filename );
      BufferedReader br = new BufferedReader( fr );

      int cnt_shot = 0;
      int cnt_splay = 0;
      ++linenr;
      line = br.readLine();
      while ( line != null ) {
        line = line.trim();
        // Log.v("Cave3D", "LINE: " + line );
        if ( line.startsWith("[Configuration]") ) break;

        int pos = line.indexOf(";");
        if ( pos >= 0 ) {
          comment = (pos+1<line.length())? line.substring( pos+1 ) : "";
          line    = line.substring( 0, pos );
          comment = comment.trim();
        } else {
          comment = "";
        }

        if ( line.length() == 0 ) {    // comment
        } else {
          String[] vals = splitLine( line ); 
          int idx = nextIndex( vals, -1 );
          if ( line.startsWith("Version") ) {
            // IGNORE
          } else if ( line.startsWith("Trou") ) { // cave name and crs are not handled
            // String[] params = line.substring(5).split(",");
            // if ( params.length > 0 ) {
            //   String name = params[0].replaceAll(" ","_");
            //   try { // TODO coordinates
            //     float x = Float.parseFloat( params[1] );
            //     float y = Float.parseFloat( params[2] );
            //     // float z = Float.parseFloat( params[3] );
            //     String vt_cs = params[3]; // ccords system: must be present in VisualTopo registry
            //     fixes.add( new Cave3DFix( name, x, y, z, cs ) ); // FIXME
            //   } catch ( NumberFormatException e ) { }
            // }
          } else if ( vals[idx].equals("Param") ) {
            for ( int k = idx+1; k < vals.length; ++k ) {
              if ( vals[k].equals("Deca") ) {
                if ( ++k < vals.length ) {
                  ub = 1;
                  dmb = false;
                  if ( vals[k].equals("Deg") ) {
                    dmb = true;
                  } else if ( vals[k].equals("Gra" ) ) {
                    ub = 0.9f; // 360/400
                  } else { // if ( vals[k].equals("Degd" ) 
                    /* nothing */
                  }
                }
              } else if ( vals[k].equals("Clino") ) {
                if ( ++k < vals.length ) {
                  uc = 1;
                  dmc = false;
                  if ( vals[k].equals("Deg") ) {
                    dmc = true;
                  } else if ( vals[k].equals("Gra" ) ) {
                    uc = 0.9f; // 360/400
                  } else { // if ( vals[k].equals("Degd" ) 
                    /* nothing */
                  }
                }
              } else if ( vals[k].startsWith("Dir") || vals[k].startsWith("Inv") ) {
                String[] dirs = vals[k].split(",");
                if ( dirs.length == 3 ) {
                  dirb = ( dirs[0].equals("Dir") )? 1 : -1;
                  dirc = ( dirs[1].equals("Dir") )? 1 : -1;
                  dirw = ( dirs[2].equals("Dir") )? 1 : -1;
                }
              } else if ( vals[k].equals("Inc") ) {
                // FIXME splay at next station: Which ???
                splayAtFrom = false;
              } else if ( vals[k].equals("Dep") ) {
                splayAtFrom = true;
              } else if ( vals[k].equals("Arr") ) {
                splayAtFrom = false;
              } else if ( vals[k].equals("Std") ) {
                // standard colors; ignore
              } else if ( k == 5 ) {
                try {
                  declination = angle( Float.parseFloat( vals[k] ), 1, true );
                } catch ( NumberFormatException e ) { }
              } else {
                // ignore colors
              }
            }
          } else if ( vals[idx].equals("Entree") ) { // entrance station
          } else if ( vals[idx].equals("Club") ) {  // team and caving club
            // IGNORE mTeam = line.substring(5);
          } else if ( vals[idx].equals("Couleur") ) { 
            // IGNORE
          } else if ( vals[idx].equals("Surface") ) {
            // IGNORE
          } else { // survey data
            if ( vals.length >= 5 ) {
              String from = vals[idx];
              idx = nextIndex( vals, idx );
              String to   = vals[idx];
              if ( ! from.equals( to ) ) {
                boolean splay = ( to.equals( "*" ) );

                try {
                  idx = nextIndex( vals, idx );
                  float len = Float.parseFloat(vals[idx]) * ul;
                  idx = nextIndex( vals, idx );
                  float ber = angle( Float.parseFloat(vals[idx]), ub, dmb);
                  idx = nextIndex( vals, idx );
                  float cln = angle( Float.parseFloat(vals[idx]), uc, dmc); 
                  if ( splay ) {
                    splays.add( new Cave3DShot( from, from + cnt_splay, len, ber, cln ) );
                    ++ cnt_splay;

                  } else {
                    String station = ( (splayAtFrom || splay )? from : to );
                    shots.add( new Cave3DShot( from, to, len, ber, cln ) );
                    ++ cnt_shot;

                    idx = nextIndex( vals, idx );
	            len = vals[idx].equals("*")? -1 : Float.parseFloat(vals[idx]) * ul; 
	            if ( len > 0 ) splays.add( new Cave3DShot( station, station+"-L", len, ber-90, 0 ) );
                    
                    idx = nextIndex( vals, idx );
	            len = vals[idx].equals("*")? -1 : Float.parseFloat(vals[idx]) * ul; 
	            if ( len > 0 ) splays.add( new Cave3DShot( station, station+"-R", len, ber+90, 0 ) );
                    idx = nextIndex( vals, idx );
	            len = vals[idx].equals("*")? -1 : Float.parseFloat(vals[idx]) * ul; 
	            if ( len > 0 ) splays.add( new Cave3DShot( station, station+"-U", len, ber, 90 ) );
                    
                    idx = nextIndex( vals, idx );
	            len = vals[idx].equals("*")? -1 : Float.parseFloat(vals[idx]) * ul; 
	            if ( len > 0 ) splays.add( new Cave3DShot( station, station+"-D", len, ber, -90 ) );
                    
                  }
                } catch ( NumberFormatException e ) {
                  Log.e("Cave3D", "ERROR " + linenr + ": " + line + " " + e.getMessage() );
                }
              }
            }
          }
        }
        ++linenr;
        line = br.readLine();
      }

    } catch ( IOException e ) {
      Log.e(TAG, "I/O ERROR " + e.getMessage() );
      throw new Cave3DParserException( filename, linenr );
    }
    // Log.v("Cave3D-VT", "shots " + shots.size() + " splays " + splays.size() );
    return ( shots.size() > 0 );
  }

  // ------------------------------------------------------------

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
      Cave3DShot sh = shots.get( 0 );
      fixes.add( new Cave3DFix( sh.from, 0.0f, 0.0f, 0.0f, null ) );
      // Log.v( "Cave3D-VT", "shots " + shots.size() + " no fixes. starts at " + sh.from );
    }
 
    int mLoopCnt = 0;
    Cave3DFix f0 = fixes.get( 0 );
    // Log.v( "Cave3D", "Process Shots. Fix " + f0.name + " " + f0.e + " " + f0.n + " " + f0.z );

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
