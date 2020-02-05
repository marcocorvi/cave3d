/** @file Cave3DThParser.java
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

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.List;

import android.widget.Toast;

public class Cave3DThParser extends Cave3DParser
{
  static final int SUCCESS       =  0;
  static final int ERR_NO_DB     = -1;
  static final int ERR_NO_SURVEY = -2;
  static final int ERR_NO_FILE   = -3;
  static final int ERR_NO_SHOTS  = -4;

  static final int TOPODROID_DB_VERSION = 42; // must agree with TopoDroid database version

  static final int FLIP_NONE       = 0;
  static final int FLIP_HORIZONTAL = 1;
  static final int FLIP_VERTICAL   = 2;

  static final int DATA_NONE      = 0;
  static final int DATA_NORMAL    = 1;
  static final int DATA_DIMENSION = 2;

  ArrayList< String > mMarks;

  DataHelper mData;

  Cave3DFix mOrigin = null;

  static int parseFlip( String flip )
  {
    if ( flip.equals("horizontal") ) return FLIP_HORIZONTAL;
    if ( flip.equals("vertical") ) return FLIP_VERTICAL;
    return FLIP_NONE;
  }

  private void processMarks()
  {
    for ( String mark : mMarks ) {
      String[] vals = mark.split(" ");
      int len = vals.length;
      len = prevIndex( vals, len );
      if ( len > 0 ) { // 0 must be "mark"
        int flag = parseFlag( vals[len] );
        int idx = nextIndex( vals, -1 );
        while ( idx < len && vals[idx].equals("mark") ) idx = nextIndex( vals, idx );
        while ( idx < len ) {
          Cave3DStation st = getStation( vals[idx] );
          if ( st != null ) st.flag = flag;
          idx = nextIndex( vals, idx );
        }
      }
    }
  }

  private int parseFlag( String str ) // parse Therion flag name
  {
    if ( str.equals( "fixed" ) ) {
      return Cave3DStation.FLAG_PAINTED;
    } else if ( str.equals( "painted" ) ) {
      return Cave3DStation.FLAG_PAINTED;
    }
    return Cave3DStation.FLAG_NONE;
  }

  public Cave3DThParser( Cave3D cave3d, String surveyname, String base ) throws Cave3DParserException
  {
    super( cave3d, surveyname );
    mMarks = new ArrayList< String >();

    String path = base + "distox14.sqlite";
    // Log.v( "Cave3D-TH", "Th parser DB " + path );
    mData = new DataHelper( cave3d, path, TOPODROID_DB_VERSION ); // FIXME DB VERSION

    StringWriter sw = new StringWriter();
    PrintWriter  pw = new PrintWriter( sw );
    pw.printf("Read survey " + surveyname + "\n");
    int res = readSurvey( surveyname, "", false, 0, pw );
    Toast.makeText( mCave3D, sw.toString(), Toast.LENGTH_LONG ).show();

    if ( res == SUCCESS ) {
      processShots();
      setShotSurveys();
      setSplaySurveys();
      setStationDepths();
      processMarks();
      // Log.v( "Cave3D-TH", "read survey " + surveyname );
    }
  }

  public Cave3DThParser( Cave3D cave3d, String filename ) throws Cave3DParserException
  {
    super( cave3d, filename );

    // Log.v( "Cave3D-TH", "Th parser file: " + filename );
    mMarks = new ArrayList< String >();
    int pos = filename.indexOf("thconfig");
    if ( pos >= 0 ) {
      String path = filename.substring(0, pos) + "distox14.sqlite";
      // Log.v( "Cave3D-TH", "DB " + path );
      mData = new DataHelper( cave3d, path, TOPODROID_DB_VERSION ); // FIXME DB VERSION
    } else {
      mData = null;
    }

    StringWriter sw = new StringWriter();
    PrintWriter  pw = new PrintWriter( sw );
    pw.printf("Read file " + filename + "\n");
    int res = readFile( filename, "", false, 0.0f, 1.0f, 1.0f, 1.0f, pw );
    Toast.makeText( mCave3D, sw.toString(), Toast.LENGTH_LONG ).show();

    if ( res == SUCCESS ) {
      processShots();
      setShotSurveys();
      setSplaySurveys();
      setStationDepths();
      processMarks();

      // System.out.println("Shots    " + shots.size() );
      // System.out.println("Stations " + stations.size() );
      // System.out.println("Bounds N: " + nmin + " " + nmax );
      // System.out.println("       E: " + emin + " " + emax );
      // System.out.println("       Z: " + zmin + " " + zmax );
      // Log.v( "Cave3D-TH", "Shots    " + shots.size() );
      // Log.v( "Cave3D-TH", "Stations " + stations.size() );
      // Log.v( "Cave3D-TH", "Bounds N: " + nmin + " " + nmax );
      // Log.v( "Cave3D-TH", "       E: " + emin + " " + emax );
      // Log.v( "Cave3D-TH", "       Z: " + zmin + " " + zmax );
      // for ( Cave3DFix f : fixes ) {
      //   Log.v( "Cave3D-TH", "FIX " + f.name + " " + f.e + " " + f.n );
      // }
    }
  }

  private String makeName( String in, String path )
  {
    int index = in.indexOf('@');
    if ( index > 0 ) {
      return in.substring(0,index) + "@" + path + "." + in.substring(index+1);
    } else {
      return in + "@" + path;
    }
  }

  int readSurvey( String surveyname, String basepath, boolean usd, float sd, PrintWriter pw ) 
                  throws Cave3DParserException
  {
    if ( mData == null ) {
      pw.printf( mCave3D.getResources().getString( R.string.no_database ) );
      return ERR_NO_DB; 
    }

    // Toast.makeText( mCave3D, "Reading " + surveyname, Toast.LENGTH_SHORT ).show();

    SurveyInfo info = mData.getSurveyInfo( surveyname );
    if ( info == null ) {
      pw.printf( String.format( mCave3D.getResources().getString( R.string.no_survey ), surveyname ) );
      return ERR_NO_SURVEY;
    }

    long sid = info.id;
    List< DBlock > blks = mData.getSurveyShots( sid, 0 );
    if ( blks.size() == 0 ) {
      pw.printf( String.format( mCave3D.getResources().getString( R.string.empty_survey ), surveyname ) );
      return ERR_NO_SHOTS;
    }

    boolean use_centerline_declination = false;
    float declination = 0.0f;
    if ( info.hasDeclination() ) {
      use_centerline_declination = true;
      declination = info.declination;
    } else if ( usd ) {
      declination = sd;
    }

    int[] survey_pos = new int[50]; // FIXME max 50 levels
    int ks = 0;
    String path = basepath;
    survey_pos[ks] = path.length();
    path = path + "." + surveyname;
    ++ks;

    for ( DBlock blk : blks ) {
      if ( blk.mFrom.length() > 0 ) {
        float ber = blk.mBearing + declination;
        if ( ber >= 360 ) ber -= 360; else if ( ber < 0 ) ber += 360;
        String from = makeName( blk.mFrom, path );
        if ( blk.mTo.length() > 0 ) {
          String to = makeName( blk.mTo, path );
          shots.add( new Cave3DShot( from, to, blk.mLength, ber, blk.mClino ) );
        } else {
          splays.add( new Cave3DShot( from, null, blk.mLength, ber, blk.mClino ) );
        }
      } else if ( blk.mTo.length() > 0 ) {
        String to = makeName( blk.mTo, path );
        float ber = 180 + blk.mBearing + declination;
        if ( ber >= 360 ) ber -= 360;
        splays.add( new Cave3DShot( to, null, blk.mLength, ber, -blk.mClino ) );
      }
    }

    List<SurveyFixed> fixeds = mData.getSurveyFixeds( sid );
    if ( fixeds != null && fixeds.size() > 0 ) {
      Cave3DCS cs = new Cave3DCS("WGS-84");
      float PI_180 = (float)(Math.PI / 180);
      for ( SurveyFixed fx : fixeds ) {
        String name = makeName( fx.station, path );
        float alat = (float)fx.mLatitude;
        if ( alat < 0 ) alat = -alat;

        float s_radius = ((90 - alat) * KMLExporter.EARTH_RADIUS1 + alat * KMLExporter.EARTH_RADIUS2)/90;
        float e_radius = s_radius * (float)Math.cos( alat * PI_180 );
        s_radius *= PI_180;
        e_radius *= PI_180;

        float x = (float)fx.mLongitude * e_radius;
        float y = (float)fx.mLatitude * s_radius;
        float z = (float)fx.mAltitude;
        if ( mOrigin == null ) {
          // Log.v( "Cave3D-TH", "Fix origin " + name + " " + x + " " + y + " " + z );
          mOrigin = new Cave3DFix( name, x, y, z, cs );
	  fixes.add( new Cave3DFix( name, 0, 0, 0, cs ) );
        } else {
          x -= mOrigin.e;
          y -= mOrigin.n;
          z -= mOrigin.z;
          // Log.v( "Cave3D-TH", "Fix relative " + name + " " + x + " " + y + " " + z );
	  fixes.add( new Cave3DFix( name, x, y, z, cs ) );
        }
      }
    }
    
    return SUCCESS;
  }
  
  /** read input file
   * @param usd
   * @param sd
   * @param ul units of length (as multiple of 1 meter)
   * @param ub units of bearing (as multiple of 1 degree)
   * @param uc units of clino
   */
  private int readFile( String filename, String basepath,
                        boolean usd, float sd,
                        float ul, float ub, float uc, PrintWriter pw )
                  throws Cave3DParserException
  {
    if ( ! checkPath( filename ) ) {
      pw.printf( String.format( mCave3D.getResources().getString( R.string.no_file ), filename ) );
      return ERR_NO_FILE;
    }

    // Toast.makeText( mCave3D, "Reading " + filename, Toast.LENGTH_SHORT ).show();

    String surveyname = "--";
    String path = basepath;
    int linenr = 0;
    // Log.v( "Cave3D-TH", "basepath <" + basepath + ">");
    // Log.v( "Cave3D-TH", "filename <" + filename + ">");
    Cave3DCS cs = null;
    int in_data = 0; // 0 none, 1 normal, 2 dimension

    int[] survey_pos = new int[50]; // FIXME max 50 levels
    int ks = 0;
    boolean in_surface = false;
    boolean in_centerline = false;
    boolean in_survey = false;
    boolean in_map = false;
    boolean use_centerline_declination = false;
    boolean use_survey_declination = usd;
    float centerline_declination = 0.0f;
    float survey_declination = sd;
    float units_len = ul;
    float units_ber = ub;
    float units_cln = uc;
    float units_grid = 1; // default units meter
    int grid_flip = FLIP_NONE;

    try {
      String dirname = "./";
      int i = filename.lastIndexOf('/');
      if ( i > 0 ) dirname = filename.substring(0, i+1);
      // Log.v( "Cave3D-TH", "reading file " + filename + " dir " + dirname );

      FileReader fr = new FileReader( filename );
      BufferedReader br = new BufferedReader( fr );
      ++linenr;
      String line = br.readLine();
      // Log.v("Cave3D-TH", linenr + ":" + line );
      while ( line != null ) {
        line = line.trim();
        int pos = line.indexOf( '#' );
        if ( pos >= 0 ) {
          line = line.substring( 0, pos );
        }
        if ( line.length() > 0 ) {
          String[] vals = splitLine( line );
          // Log.v( "Cave3D-TH", "[" + vals.length + "] >>" + line + "<<" );
          // for (int j=0; j<vals.length; ++j ) Log.v( "Cave3D-TH", "    " + vals[j] );

          if ( vals.length > 0 ) {
            int idx = nextIndex( vals, -1 );
            String cmd = vals[idx];
            if ( cmd.equals("survey") ) {
              idx = nextIndex( vals, idx );
              if ( idx < vals.length ) {
                surveyname = vals[idx];
                survey_pos[ks] = path.length();
                path = path + "." + vals[idx];
                // Log.v( "Cave3D-TH", "SURVEY " + path );
                ++ks;
                in_survey = true;
              }
            } else if ( in_map ) {
              if ( cmd.equals("endmap") ) {
                in_map = false;
              }
            } else if ( in_centerline ) {
              if ( cmd.equals("endcenterline") ) {
                in_centerline = false;
                use_centerline_declination = false;
                centerline_declination = 0.0f;
              } else if ( cmd.equals("date") ) {
                idx = nextIndex( vals, idx );
                if ( idx < vals.length ) {
                  String date = vals[idx];
                  // TODO
                }
              } else if ( cmd.equals("flags") ) { // skip
              } else if ( cmd.equals("team") ) { // skip
              } else if ( cmd.equals("extend") ) { // skip
              } else if ( cmd.equals("declination") ) { 
                idx = nextIndex( vals, idx );
                if ( idx < vals.length ) {
                  try {
                    float decl = Float.parseFloat( vals[idx] );
                    use_centerline_declination = true;
                    centerline_declination = decl;
                  } catch ( NumberFormatException e ) {
                    Log.e( "Cave3D-TH", "Number error: centerline declination number format exception" );
                  }
                }
              } else if ( cmd.equals("station") ) {
                // TODO
              } else if ( cmd.equals("mark") ) {
                mMarks.add( line );
              } else if ( cmd.equals("data") ) {
                in_data = 0;
                // data normal from to length compass clino ...
                idx = nextIndex( vals, idx );
                if ( idx < vals.length ) {
                  if ( vals[idx].equals("normal") ) {
                    in_data = DATA_NORMAL;
                  } else if ( vals[idx].equals("dimension") ) {
                    in_data = DATA_DIMENSION;
                  } else {
                    // TODO
                  }
                }
              } else if ( cmd.equals("units") ) {
                idx = nextIndex( vals, idx );
                if ( idx < vals.length ) {
                  // parse "units" command
                  boolean isLength  = false;
                  boolean isBearing = false;
                  boolean isClino   = false;
                  float factor = 1;
                  for ( ; idx < vals.length; ++idx ) {
                    if ( vals[idx].equals("length") || vals[idx].equals("tape") ) { 
                      isLength = true;
                    } else if ( vals[idx].equals("compass") || vals[idx].equals("bearing") ) { 
                      isBearing = true;
                    } else if ( vals[idx].equals("clino") ) {
                      isClino = true;
                    } else if ( vals[idx].equals("m") || vals[idx].startsWith("meter") ) {
                      if ( isLength ) ul = factor;
                    } else if ( vals[idx].equals("cm") || vals[idx].startsWith("centimeter") ) {
                      if ( isLength ) ul = factor/100;
                    } else if ( vals[idx].startsWith("degree") ) {
                      if ( isBearing ) ub = factor;
                      if ( isClino )   uc = factor;
                    } else if ( vals[idx].startsWith("grad") ) {
                      if ( isBearing ) ub = (factor*360)/400.0f;
                      if ( isClino )   uc = (factor*360)/400.0f;
                    } else if ( vals[idx].length() > 0 ) {
                      try {
                        factor = Float.parseFloat( vals[idx] );
                      } catch ( NumberFormatException e ) { 
                        Log.e( "Cave3D-TH", "Number error " + e.getMessage() );
                      }
                    }
                  } 
                }
              } else if ( cmd.equals("cs") ) { // ***** fix station east north Z
                idx = nextIndex( vals, idx );
                if ( idx < vals.length ) {
                  cs = new Cave3DCS( vals[idx] );
                }
              } else if ( cmd.equals("fix") ) { // ***** fix station east north Z
                // Log.v( "Cave3D-TH", "command fix");
                idx = nextIndex( vals, idx );
                if ( idx < vals.length ) {
                  String name = makeName( vals[idx], path );
                  // Log.v( "Cave3D-TH", "command fix " + name );
                  try { 
                    idx = nextIndex( vals, idx );
                    if ( idx < vals.length ) {
                      float x = Float.parseFloat( vals[idx] );
                      // Log.v( "Cave3D-TH", " fix x " + x );
                      idx = nextIndex( vals, idx );
                      if ( idx < vals.length ) {
                        float y = Float.parseFloat( vals[idx] );
                        // Log.v( "Cave3D-TH", " fix y " + y );
                        idx = nextIndex( vals, idx );
                        if ( idx < vals.length ) {
                          float z = Float.parseFloat( vals[idx] );
	                  fixes.add( new Cave3DFix( name, x, y, z, cs ) );
                          // Log.v( "Cave3D-TH", " adding fix " + x + " " + y + " " + z );
                        }
                      }
                    }
                  } catch ( NumberFormatException e ) {
                    Log.e( "Cave3D-TH", "Fix station error: " + e.getMessage() );
                  }
                }
              } else if ( vals.length >= 5 ) {
                if ( in_data == DATA_NORMAL ) {
                  String from = vals[idx];
                  idx = nextIndex( vals, idx );
                  if ( idx < vals.length ) {
                    String to = vals[idx]; 
                    try {
                      idx = nextIndex( vals, idx );
                      if ( idx < vals.length ) {
                        float len  = Float.parseFloat( vals[idx] ) * units_len;
                        idx = nextIndex( vals, idx );
                        if ( idx < vals.length ) {
                          float ber  = Float.parseFloat( vals[idx] ) * units_len;
                          if ( use_centerline_declination ) {
                            ber += centerline_declination;
                          } else if ( use_survey_declination ) {
                            ber += survey_declination;
                          }
                          idx = nextIndex( vals, idx );
                          if ( idx < vals.length ) {
                            float cln  = Float.parseFloat( vals[idx] ) * units_len;
                            // TODO add shot
                            if ( to.equals("-") || to.equals(".") ) {
                              // TODO splay shot
                              from = makeName( from, path );
                              to = null;
                              splays.add( new Cave3DShot( from, to, len, ber, cln ) );
                            } else {
                              from = makeName( from, path );
                              to   = makeName( to, path );
                              // StringWriter sw = new StringWriter();
                              // PrintWriter pw = new PrintWriter( sw );
                              // pw.format(Locale.US, "%s %s %.2f %.1f %.1f", from, to, len, ber, cln );
                              // Log.v("Cave3D-TH", sw.getBuffer().toString() );
                              shots.add( new Cave3DShot( from, to, len, ber, cln ) );
                            }
                          }
                        }
                      }
                    } catch ( NumberFormatException e ) {
                      Log.e("Cave3D-TH", "Shot data error: " + e.getMessage() );
                    }
                  }
                } else if ( in_data == DATA_DIMENSION ) {
                  // TODO
                }
              }            
            } else if ( in_surface ) {
              if ( cmd.equals("endsurface") ) {
                in_surface = false;
              } else if ( cmd.equals("grid") ) {
                grid_flip = FLIP_NONE;
                units_grid = 1;
                mSurface = null;

                try {
                  float e1, n1, e2, n2, d1, d2;
                  int c1, c2;
                  // parse grid metadata
                  idx = nextIndex( vals, idx );
                  if ( idx < vals.length ) {
                    e1 = Float.parseFloat( vals[idx] );
                    idx = nextIndex( vals, idx );
                    if ( idx < vals.length ) {
                      n1 = Float.parseFloat( vals[idx] );
                      idx = nextIndex( vals, idx );
                      if ( idx < vals.length ) {
                        d1 = Float.parseFloat( vals[idx] );
                        idx = nextIndex( vals, idx );
                        if ( idx < vals.length ) {
                          d2 = Float.parseFloat( vals[idx] );
                          idx = nextIndex( vals, idx );
                          if ( idx < vals.length ) {
                            c1 = Integer.parseInt( vals[idx] );
                            e2 = e1 + d1*(c1-1);
                            idx = nextIndex( vals, idx );
                            if ( idx < vals.length ) {
                              c2 = Integer.parseInt( vals[idx] );
                              n2 = n1 + d2*(c2-1);
                              mSurface = new Cave3DSurface( e1, n1, e2, n2, c1, c2 );
                              // Log.v( "Cave3D-TH", "Surface " + e1 + "-" + n1 + " " + e2 + "-" + n2 + " " + c1 + "x" + c2);
                            }
                          }
                        }
                      }
                    }
                  }
                } catch ( NumberFormatException e ) {
                  Log.e( "Cave3D-TH", "surface grid metadata " + e.getMessage() );
                }
                // and read grid data
                if ( mSurface != null ) {
                  mSurface.readGridData( units_grid, grid_flip, br, filename );
                }
              } else if ( cmd.equals("grid-flip") ) {
                // parse the flip-value
                idx = nextIndex( vals, idx );
                if ( idx < vals.length ) {
                  grid_flip = parseFlip( vals[idx] );
                }
              } else if ( cmd.equals("grid-units") ) {
                // parse the grid-units
                try {
                  idx = nextIndex( vals, idx );
                  if ( idx < vals.length ) {
                    float value = Float.parseFloat( vals[idx] );
                    idx = nextIndex( vals, idx );
                    if ( idx < vals.length ) {
                      // FIXME TODO
                      // units_grid = parseUnits( value, vals[idx] );
                    }
                  }
                } catch ( NumberFormatException e ) {
                  Log.e( "Cave3D-TH", "surface grid units " + e.getMessage() );
                }
              }
            } else if ( cmd.equals("declination") ) {
              try {
                idx = nextIndex( vals, idx );
                if ( idx < vals.length ) {
                  use_survey_declination = true;
                  survey_declination = Float.parseFloat( vals[idx] );
                }
              } catch ( NumberFormatException e ) {
                Log.e( "Cave3D-TH", "survey declination " + e.getMessage() );
              }
            } else if ( cmd.equals("input") ) {
              idx = nextIndex( vals, idx );
              if ( idx < vals.length ) {
                filename = vals[idx];
                // Log.v( "Cave3D-TH", "FILE " + filename );
                if ( filename.endsWith( ".th" ) ) {
                  int res = readFile( dirname + '/' + filename, 
                                   path,
                                   use_survey_declination, survey_declination,
                                   units_len, units_ber, units_cln, pw );
                  if ( res != SUCCESS ) {
                    Log.e( "Cave3D-TH", "read file " + filename + " failed. Error code " + res );
                    Toast.makeText( mCave3D, "Failed read file " + filename + " code " + res, Toast.LENGTH_LONG ).show();
                  }
                } else {
                  Log.e( "Cave3D-TH", "Input file <" + filename + "> has no .th extension");
                }
              }
            } else if ( cmd.equals("load") ) {
              idx = nextIndex( vals, idx );
              if ( idx < vals.length ) {
                filename = vals[idx]; // survey name
                // Log.v( "Cave3D-TH", "survey " + filename );
                if ( mData == null ) {
                  String base = null;
                  if ( dirname.endsWith( "tdconfig/" ) ) {
                    base = dirname.replace( "tdconfig/", "" );
                    i = base.lastIndexOf('/');
                    if ( i > 0 && i < base.length() ) base = base.substring(0, i+1);
                  } else {
                    base = dirname;
                  }
                  String db_path = base + "distox14.sqlite";
                  // Log.v( "Cave3D-TH", "DB " + db_path );
                  if ( (new File(db_path)).exists() ) {
                    mData = new DataHelper( mCave3D, db_path, TOPODROID_DB_VERSION );
                  }
                }
                int res = readSurvey( filename, path, use_survey_declination, survey_declination, pw );
                if ( res != SUCCESS ) {
                  Log.e( "Cave3D-TH", "read survey " + filename + " failed. Error code " + res );
                  Toast.makeText( mCave3D, "Failed read survey " + filename + " code " + res, Toast.LENGTH_LONG ).show();
                }
              }
            } else if ( cmd.equals("equate") ) {
              idx = nextIndex( vals, idx );
              if ( idx < vals.length ) {
                String from = makeName( vals[idx], path );
                while ( idx < vals.length ) {
                  idx = nextIndex( vals, idx );
                  if ( idx < vals.length ) {
		    String to = makeName( vals[idx], path );
                    // StringWriter sw = new StringWriter();
                    // PrintWriter pw = new PrintWriter( sw );
                    // pw.format(Locale.US, "EQUATE %s %s 0.00 0.0 0.0", from, to );
                    // Log.v( "Cave3D-TH", sw.getBuffer().toString() );
                    // Log.v( "Cave3D-TH", "Equate " + from + " " + to );
                    shots.add( new Cave3DShot( from, to, 0.0f, 0.0f, 0.0f ) );
                  }
                }
              }
            } else if ( cmd.equals("surface") ) {
              in_surface = true;
            } else if ( cmd.equals("centerline") ) {
              in_centerline = true;
            } else if ( cmd.equals("map") ) {
              in_map = true;
            } else if ( cmd.equals("endsurvey") ) {
              --ks;
              if ( ks < 0 ) {
                Log.e( "Cave3D-TH", filename + ":" + linenr + " negative survey level" );
              } else {
                path = path.substring(0, survey_pos[ks]); // return to previous survey_pos in path
                // Log.v( "Cave3D-TH", "endsurvey PATH " + path );
                in_survey = ( ks > 0 );
              }
            }
          }
        }
        ++linenr;
        line = br.readLine();
        // Log.v( "Cave3D-TH", linenr + ":" + line );
      }
    } catch ( IOException e ) {
      Log.e( "Cave3D-TH", "I/O error " + e.getMessage() );
      throw new Cave3DParserException( filename, linenr );
    }
    // Log.v( "Cave3D-TH", "Done readFile " + filename );

    if ( shots.size() <= 0 ) {
      pw.printf( String.format( mCave3D.getResources().getString( R.string.empty_survey ), surveyname ) );
      return ERR_NO_SHOTS;
    }
    return SUCCESS;
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
    // Log.v( "Cave3D-TH", "shots " + shots.size() + " fixes " + fixes.size() );

    if ( fixes.size() == 0 ) {
      Cave3DShot sh = shots.get( 0 );
      fixes.add( new Cave3DFix( sh.from, 0.0f, 0.0f, 0.0f, null ) );
    }
 
    int mLoopCnt = 0;
    Cave3DFix f0 = fixes.get( 0 );
    // Log.v( "Cave3D-TH", "Process Shots. Fix " + f0.name + " " + f0.e + " " + f0.n + " " + f0.z );

    mCaveLength = 0.0f;
    // Log.v( "Cave3D-TH", "shots " + shots.size() + " splays " + splays.size() + " fixes " + fixes.size() );

    int used_cnt = 0; // number of used shots
    for ( Cave3DFix f : fixes ) {
      boolean found = false;
      // Log.v( "Cave3D-TH", "checking fix " + f.name );
      for ( Cave3DStation s1 : stations ) {
        if ( f.name.equals( s1.name ) ) { found = true; break; }
      }
      if ( found ) { // skip fixed stations that are already included in the model
        // Log.v( "Cave3D-TH", "found fix " + f.name );
        continue;
      }
      // Log.v( "Cave3D-TH", "start station " + f.name + " N " + f.n + " E " + f.e + " Z " + f.z );
      stations.add( new Cave3DStation( f.name, f.e, f.n, f.z ) );
      // sh.from_station = s0;
    

      boolean repeat = true;
      while ( repeat ) {
        // Log.v( "Cave3D-TH", "scanning the shots");
        repeat = false;
        for ( Cave3DShot sh : shots ) {
          if ( sh.used ) continue;
          // Log.v( "Cave3D-TH", "check shot " + sh.from + " " + sh.to );
          // Cave3DStation sf = sh.from_station;
          // Cave3DStation st = sh.to_station;
          Cave3DStation sf = null;
          Cave3DStation st = null;
          for ( Cave3DStation s : stations ) {
            if ( sh.from.equals(s.name) ) {
              sf = s;
              if (  sh.from_station == null ) sh.from_station = s;
              else if ( sh.from_station != s ) Log.e( "Cave3D-TH", "shot " + sh.from + " " + sh.to + " from-station mismatch ");
            } 
            if ( sh.to.equals(s.name) )   {
              st = s;
              if (  sh.to_station == null ) sh.to_station = s;
              else if ( sh.to_station != s ) Log.e( "Cave3D-TH", "shot " + sh.from + " " + sh.to + " to-station mismatch ");
            }
            if ( sf != null && st != null ) break;
          }
          if ( sf != null && st != null ) {
            // Log.v( "Cave3D-TH", "unused shot " + sh.from + " " + sh.to + " : " + sf.name + " " + st.name );
            sh.used = true; // LOOP
	    ++ used_cnt;
            mCaveLength += sh.len;
            // make a fake station
            Cave3DStation s = sh.getStationFromStation( sf );
            stations.add( s );
            s.name = s.name + "-" + mLoopCnt;
            ++ mLoopCnt;
            sh.to_station = s;
            repeat = true; // unnecessary
          } else if ( sf != null && st == null ) {
            // Log.v( "Cave3D-TH", "unused shot " + sh.from + " " + sh.to + " : " + sf.name + " null" );
            Cave3DStation s = sh.getStationFromStation( sf );
            stations.add( s );
            sh.to_station = s;
            // Log.v( "Cave3D-TH", "add station TO " + sh.from + " " + sh.to + " " + sh.to_station.name );
            sh.used = true;
	    ++ used_cnt;
            mCaveLength += sh.len;
            repeat = true;
          } else if ( sf == null && st != null ) {
            // Log.v( "Cave3D-TH", "unused shot " + sh.from + " " + sh.to + " : null " + st.name );
            Cave3DStation s = sh.getStationFromStation( st );
            stations.add( s );
            sh.from_station = s;
            // Log.v( "Cave3D-TH", "add station FR " + sh.from + " " + sh.to + " " + sh.from_station.name  );
            sh.used = true;
	    ++ used_cnt;
            mCaveLength += sh.len;
            repeat = true;
          } else {
            // Log.v( "Cave3D-TH", "unused shot " + sh.from + " " + sh.to + " : null null" );
          }
        }
      }
    } // for ( Cave3DFix f : fixes )
    // Log.v( "Cave3D-TH", "used shot " + used_cnt + " loops " + mLoopCnt );
    // StringBuilder sb = new StringBuilder();
    // for ( Cave3DStation st : stations ) { sb.append(" "); sb.append( st.name ); }
    // Log.v( "Cave3D-TH", sb.toString() );

    // 3D splay shots
    for ( Cave3DShot sh : splays ) {
      if ( sh.used ) continue;
      if (  sh.from_station != null ) continue;
      // Log.v( "Cave3D-TH", "check shot " + sh.from + " " + sh.to );
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
