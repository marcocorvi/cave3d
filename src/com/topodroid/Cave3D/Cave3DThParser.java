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

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.util.Log;

public class Cave3DThParser extends Cave3DParser
{

  public Cave3DThParser( Cave3D cave3d, String filename ) throws Cave3DParserException
  {
    super( cave3d );

    if ( readFile( filename, "", false, 0.0f, 1.0f, 1.0f, 1.0f ) ) {
      processShots();
      setShotSurveys();
      setSplaySurveys();
      setStationDepths();

      // System.out.println("Shots    " + shots.size() );
      // System.out.println("Stations " + stations.size() );
      // System.out.println("Bounds N: " + nmin + " " + nmax );
      // System.out.println("       E: " + emin + " " + emax );
      // System.out.println("       Z: " + zmin + " " + zmax );
      // Log.v( TAG, "Shots    " + shots.size() );
      // Log.v( TAG, "Stations " + stations.size() );
      // Log.v( TAG, "Bounds N: " + nmin + " " + nmax );
      // Log.v( TAG, "       E: " + emin + " " + emax );
      // Log.v( TAG, "       Z: " + zmin + " " + zmax );
      // for ( Cave3DFix f : fixes ) {
      //   Log.v("Cave3D", "FIX " + f.name + " " + f.e + " " + f.n );
      // }

      if ( stations.size() > 0 ) {
        do_render = true;
      }
    } else {

    }
  }

  /** read input file
   * @param usd
   * @param sd
   * @param ul units of length (as multiple of 1 meter)
   * @param ub units of bearing (as multiple of 1 degree)
   * @param uc units of clino
   */
  private boolean readFile( String filename, String basepath,
                            boolean usd, float sd,
                            float ul, float ub, float uc )
                  throws Cave3DParserException
  {
    String path = basepath;
    // Log.v("Cave3D", "basepath <" + basepath + ">");
    // Log.v("Cave3D", "filename <" + filename + ">");

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
      // Log.v( TAG, "reading file " + filename + " dir " + dirname );

      FileReader fr = new FileReader( filename );
      BufferedReader br = new BufferedReader( fr );
      String line = br.readLine();
      int cnt = 1;
      // Log.v(TAG, cnt + ":" + line );
      while ( line != null ) {
        line = line.trim();
        int pos = line.indexOf( '#' );
        if ( pos >= 0 ) {
          line = line.substring( 0, pos );
        }
        if ( line.length() > 0 ) {
          String[] vals = line.split( " " );
          // Log.v("Cave3D", "[" + vals.length + "] >>" + line + "<<" );
          // for (int j=0; j<vals.length; ++j ) Log.v("Cave3D", "    " + vals[j] );
          if ( vals.length > 0 ) {
            int idx = nextIndex( vals, -1 );
            String cmd = vals[idx];
            if ( cmd.equals("survey") ) {
              idx = nextIndex( vals, idx );
              if ( idx < vals.length ) {
                survey_pos[ks] = path.length();
                path = path + "." + vals[idx];
                // Log.v( "Cave3D", "SURVEY " + path );
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
                    Log.e( "Cave3D", "centerline declination number format exception" );
                  }
                }
              } else if ( cmd.equals("data") ) {
                // data normal from to length compass clino ...
                // TODO
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
                      } catch ( NumberFormatException e ) { }
                    }
                  } 
                }
              } else if ( cmd.equals("fix") ) { // ***** fix station east north Z
                // Log.v( TAG, "command fix");
                idx = nextIndex( vals, idx );
                if ( idx < vals.length ) {
                  String name;
                  int index = vals[idx].indexOf('@');
                  if ( index > 0 ) {
                    name = vals[idx].substring(0,index) + "@" + path + "." + vals[1].substring(index+1);
                  } else {
                    name = vals[idx] + "@" + path;
                  }
                  // Log.v( TAG, "command fix " + name );
                  try { 
                    idx = nextIndex( vals, idx );
                    if ( idx < vals.length ) {
                      float x = Float.parseFloat( vals[idx] );
                      // Log.v( TAG, " fix x " + x );
                      idx = nextIndex( vals, idx );
                      if ( idx < vals.length ) {
                        float y = Float.parseFloat( vals[idx] );
                        // Log.v( TAG, " fix y " + y );
                        idx = nextIndex( vals, idx );
                        if ( idx < vals.length ) {
                          float z = Float.parseFloat( vals[idx] );
                          // Log.v( TAG, " fix z " + z + " adding fix ");
	                  fixes.add( new Cave3DFix( name, x, y, z ) );
                        }
                      }
                    }
                  } catch ( NumberFormatException e ) {
                    Log.e("Cave3D", "fix station number format exception");
                  }
                }
              } else if ( vals.length >= 5 ) {
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
                            from = from + "@" + path;
                            to = null;
                            splays.add( new Cave3DShot( from, to, len, ber, cln ) );
                          } else {
                            from = from + "@" + path;
                            to   = to + "@" + path;
                            // StringWriter sw = new StringWriter();
                            // PrintWriter pw = new PrintWriter( sw );
                            // pw.format("%s %s %.2f %.1f %.1f", from, to, len, ber, cln );
                            // Log.v("Cave3D", sw.getBuffer().toString() );
                            shots.add( new Cave3DShot( from, to, len, ber, cln ) );
                          }
                        }
                      }
                    }
                  } catch ( NumberFormatException e ) {
                    Log.e("Cave3D", "shot data number format exception");
                  }
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
                              // Log.v( TAG, "Surface " + e1 + "-" + n1 + " " + e2 + "-" + n2 + " " + c1 + "x" + c2);
                            }
                          }
                        }
                      }
                    }
                  }
                } catch ( NumberFormatException e ) {
                  Log.e("Cave3D", "surface grid metadata number format exception");
                }
                // and read grid data
                if ( mSurface != null ) {
                  mSurface.readGridData( units_grid, grid_flip, br );
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
                  Log.e("Cave3D", "surface grid units number format exception");
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
                Log.e("Cave3D", "survey declination number format exception");
              }
            } else if ( cmd.equals("input") ) {
              idx = nextIndex( vals, idx );
              if ( idx < vals.length ) {
                filename = vals[idx];
                // Log.v( "Cave3D", "FILE " + filename );
                if ( filename.endsWith( ".th" ) ) {
                  if ( ! readFile( dirname + '/' + filename, 
                                   path,
                                   use_survey_declination, survey_declination,
                                   units_len, units_ber, units_cln ) ) {
                    return false;
                  }
                } else {
                  Log.e(TAG, "Input file <" + filename + "> has no .th extension");
                }
              }
            } else if ( cmd.equals("equate") ) {
              idx = nextIndex( vals, idx );
              if ( idx < vals.length ) {
                String from, to;
                int index = vals[idx].indexOf('@');
                if ( index > 0 ) {
                  from = vals[idx].substring(0,index) + "@" + path + "." + vals[idx].substring(index+1);
                } else {
                  from = vals[idx] + "@" + path;
                }
                while ( idx < vals.length ) {
                  idx = nextIndex( vals, idx );
                  if ( idx < vals.length ) {
                    index = vals[idx].indexOf('@');
                    if ( index > 0 ) {
                      to = vals[idx].substring(0,index) + "@" + path + "." + vals[idx].substring(index+1);
                    } else {
                      to = vals[idx] + "@" + path;
                    }
                    // StringWriter sw = new StringWriter();
                    // PrintWriter pw = new PrintWriter( sw );
                    // pw.format("EQUATE %s %s 0.00 0.0 0.0", from, to );
                    // Log.v("Cave3D", sw.getBuffer().toString() );
                    // Log.v(TAG, "Equate " + from + " " + to );
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
                Log.e( TAG, filename + ":" + cnt + " negative survey level" );
              } else {
                path = path.substring(0, survey_pos[ks]); // return to previous survey_pos in path
                // Log.v("Cave3D", "endsurvey PATH " + path );
                in_survey = ( ks > 0 );
              }
            }
          }
        }
        line = br.readLine();
        ++ cnt;
        // Log.v(TAG, cnt + ":" + line );
      }
    } catch ( IOException e ) {
      // TODO
      Log.e(TAG, "exception " + e.toString() );
      throw new Cave3DParserException();
    }
    // Log.v(TAG, "done readFile " + filename );

    return ( shots.size() > 0 );
  }

}
