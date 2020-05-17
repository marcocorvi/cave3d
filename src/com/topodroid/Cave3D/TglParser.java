/** @file TglParser.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D file parser 
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.Locale;

import android.widget.Toast;

import android.os.AsyncTask;

// import android.util.Log;

public class TglParser
{
  // private static final String TAG = "TopoGL Parser";

  public static final int WALL_NONE       = 0;
  public static final int WALL_CW         = 1;
  public static final int WALL_POWERCRUST = 2;
  public static final int WALL_DELAUNAY   = 3;
  public static final int WALL_HULL       = 4;
  public static final int WALL_MAX        = 3;

  boolean do_render; // whether ready to render
  TopoGL mApp;

  protected ArrayList< Cave3DSurvey >  surveys;
  protected ArrayList< Cave3DFix >     fixes;
  protected ArrayList< Cave3DStation > stations;
  protected ArrayList< Cave3DShot >    shots;   // centerline shots
  protected ArrayList< Cave3DShot >    splays;  // splay shots

  PowercrustComputer powercrustcomputer = null;
  ConvexHullComputer convexhullcomputer = null;

  // private ArrayList< CWConvexHull > walls   = null;
  // private ArrayList< CWBorder >     borders = null;

  DEMsurface mSurface;
  LoxBitmap  mBitmap = null;
  float mCaveLength;
  String mName; // file base name

  Cave3DStation mCenterStation = null;
  Cave3DStation mStartStation = null;
  protected Cave3DFix mOrigin = null; // coordinates of the origin station

  Cave3DFix getOrigin() { return mOrigin; }
  boolean hasOrigin() { return mOrigin != null; }

  // void setStartStation( Cave3DStation station ) { mStartStation = station; }
  void clearStartStation( ) { mStartStation = null; }
  void setStartStation( String fullname ) { mStartStation = getStation( fullname ); }

  // ----------------------------------------------------------------

  // TODO VECTOR
  protected float x0, y0, z0;       // model center coords (remain fixed)

  public float emin, emax, nmin, nmax, zmin, zmax; // survey bounds

  // ------------------------- LINE UTIL
  static Pattern pattern = Pattern.compile( "\\s+" );

  static String[] splitLine( String line )
  {
     return pattern.split(line); // line.split( "\\s+" );
  }
  // -------------------------------------

  String getName() { return mName; }

  void centerAtStation( Cave3DStation st ) { mCenterStation = st; }
   
  protected boolean mSurfaceFlipped = false; // somehow lox file cane be surface-flipped FIXME FIXME FIXME
  boolean surfaceFlipped() { return mSurfaceFlipped; }

  // public float getEmin() { return emin; }
  // public float getEmax() { return emax; }
  // public float getNmin() { return nmin; }
  // public float getNmax() { return nmax; }
  // public float getVmin() { return zmin; }
  // public float getVmax() { return zmax; }

  int getStationNumber() { return stations.size(); }
  int getShotNumber()    { return shots.size(); }
  int getSplayNumber()   { return splays.size(); }
  int getSurveyNumber()  { return surveys.size(); }

  ArrayList< Cave3DSurvey >  getSurveys()  { return surveys; }
  ArrayList< Cave3DShot >    getShots()    { return shots; }
  ArrayList< Cave3DShot >    getSplays()   { return splays; }
  ArrayList< Cave3DStation > getStations() { return stations; }
  ArrayList< Cave3DFix >     getFixes()    { return fixes; }

  Cave3DShot getShot( int k ) { return shots.get(k); }

  DEMsurface getSurface() { return mSurface; }
  boolean hasSurface() { return mSurface != null; }
  boolean hasWall() { return convexhullcomputer != null || ( powercrustcomputer != null && powercrustcomputer.hasTriangles() ); }
  boolean hasPlanview() { return powercrustcomputer != null && powercrustcomputer.hasPlanview(); }

  float getConvexHullVolume() { return ( convexhullcomputer == null )? 0 : convexhullcomputer.getVolume(); }
  float getPowercrustVolume() { return ( powercrustcomputer == null )? 0 : powercrustcomputer.getVolume(); }

  protected boolean checkPath( String path )
  {
    File file = new File( path );
    if ( ! file.exists() ) {
      Toast.makeText( mApp, mApp.getResources().getString( R.string.error_file_not_found ) + " " + path, Toast.LENGTH_LONG ).show();
      return false;
    }
    if ( ! file.canRead() ) {
      Toast.makeText( mApp, mApp.getResources().getString( R.string.error_file_not_readable ) + " " + path, Toast.LENGTH_LONG ).show();
      return false;
    }
    return true;
  }

  private void initStationsPathlength()
  {
    for ( Cave3DStation st : stations ) st.setPathlength( Float.MAX_VALUE, null );
  }

  TglMeasure computeCavePathlength( Cave3DStation s2 )
  { 
    Cave3DStation s1 = mStartStation;
    if ( s1 == null ) return null;
    if ( s2 == null || s2 == s1 ) return null;
    initStationsPathlength( );
    s1.setPathlength( 0, null );
    Stack<Cave3DStation> stack = new Stack<Cave3DStation>();
    stack.push( s1 );
    while ( ! stack.empty() ) {
      Cave3DStation s0 = stack.pop();
      ArrayList< Cave3DShot > legs = getLegsAt( s0, true );
      for ( Cave3DShot leg : legs ) {
        Cave3DStation s3 = leg.getOtherStation( s0 );
        if ( s3 != null ) {
          double d = s0.getPathlength() + leg.len;
          if ( s3.getPathlength() > d ) {
            s3.setPathlength( d, s0 );
            stack.push( s3 );
          }
        }
      }
    }
    return new TglMeasure( mApp.getResources(), s1, s2, s2.getFinalPathlength() );
  }

  Cave3DSurvey getSurvey( String name ) // get survey by the NAME
  {
    if ( name == null ) return null;
    for ( Cave3DSurvey s : surveys ) if ( name.equals( s.name ) ) return s;
    return null;
  }

  Cave3DSurvey getSurvey( int id ) // get survey by the ID
  {
    if ( id < 0 ) return null;
    for ( Cave3DSurvey s : surveys ) if ( s.mId == id ) return s;
    return null;
  }

  Cave3DStation getStation( int id ) // get station by the ID
  {
    if ( id < 0 ) return null;
    for ( Cave3DStation s : stations ) if ( s.mId == id ) return s;
    return null;
  }

  Cave3DStation getStation( String fullname ) // get station by the FULL_NAME
  {
    if ( fullname == null ) return null;
    for ( Cave3DStation s : stations ) if ( fullname.equals( s.name ) ) return s;
    return null;
  }

  float getCaveZMin()   { return zmin; }
  float getCaveDepth()  { return zmax - zmin; }
  float getCaveLength() { return mCaveLength; }

  float[] getStationVertices()
  {
    float v[] = new float[ 3 * stations.size() ];
    int k = 0;
    int k3 = 0;
    for ( Cave3DStation s : stations ) {
      s.vertex = k++;
      v[k3++] = s.x; // X horizontal
      v[k3++] = s.y; // Y vertical
      v[k3++] = s.z; // Z depth
    }
    return v;
  }

  float[] getSplaysEndpoints()
  {
    float v[] = new float[ 3 * splays.size() ];
    int k3=0;
    for ( Cave3DShot sp : splays ) {
      Vector3D vv = sp.toPoint3D();
      if ( vv != null ) {
        v[k3++] = vv.x;
        v[k3++] = vv.y;
        v[k3++] = vv.z;
      }
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

  /** get the legs at "station"
   * @param reverse   if true reverse leg if at to-station
   */
  ArrayList< Cave3DShot > getLegsAt( Cave3DStation station, boolean reverse )
  {
    ArrayList< Cave3DShot > ret = new ArrayList< Cave3DShot >();
    for ( Cave3DShot shot : shots ) {
      if ( shot.from_station == station ) {
        ret.add( shot );
      } else if ( shot.to_station == station ) {
        if ( reverse ) {
          double b = shot.ber + Math.PI;
          if ( b > 2*Math.PI ) b -= 2*Math.PI;
          ret.add( new Cave3DShot( station, shot.from_station, shot.len, (float)b, -shot.cln) );
        } else {
        ret.add( shot );
        }
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
    // Log.v( TAG, "get legs at " + station.name + " other " + other.name );
    ArrayList< Cave3DShot > ret = new ArrayList< Cave3DShot >();
    for ( Cave3DShot shot : shots ) { // add survey legs too1
      if ( shot.from_station == station ) {
        if ( shot.to_station == other ) {
          // Log.v( TAG, "add direct shot " + shot.from + "-" + shot.to );
          ret.add( shot );
        } else {
          // Log.v( TAG, "add other shot " + shot.to + "-" + shot.from );
          double b = shot.ber + Math.PI;
          if ( b > 2*Math.PI ) b -= 2*Math.PI;
          ret.add( new Cave3DShot( null, station, shot.len, (float)b, -shot.cln) ); // stations not important
        }
      } else if ( shot.to_station == station ) {
        if ( shot.from_station == other ) {
          // Log.v( TAG, "add reversed shot " + shot.to + "-" + shot.from );
          double b = shot.ber + Math.PI;
          if ( b > 2*Math.PI ) b -= 2*Math.PI;
          ret.add( new Cave3DShot( null, station, shot.len, (float)b, -shot.cln) ); // stations not important
        } else {
          // Log.v( TAG, "add other shot " + shot.from + "-" + shot.to );
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
          ret.add( new Cave3DShot( station, null, shot.len, (float)b, -shot.cln) ); // stations not important
        }
      }
    }
    return ret;
  }

  protected void setStationDepths( )
  {
    float deltaz = zmax - zmin + 0.001f;
    int k = 0;
    for ( Cave3DStation s : stations ) {
      s.depth = (zmax - s.z)/deltaz; // Z depth
      // Log.v(TAG, "Depth " + s.name + " " + s.depth ); 
    }  
    if ( stations.size() > 0 ) do_render = true;
  }

  public TglParser( TopoGL app, String filename ) 
  {
    mApp = app;
    do_render = false;
    mOrigin = null;
    // Toast.makeText( app, "Reading " + filename, Toast.LENGTH_SHORT ).show();

    // Log.v( TAG, "parsing " + filename );
    int pos = filename.lastIndexOf('/');
    if ( pos > 0 ) {
      mName = filename.substring(pos+1);
    } else {
      mName = filename;
    }
    pos = mName.lastIndexOf('.');
    if ( pos > 0 ) {
      mName = mName.substring(0,pos);
    }

    mSurface = null;

    fixes  = new ArrayList< Cave3DFix >();
    shots  = new ArrayList< Cave3DShot >();
    splays = new ArrayList< Cave3DShot >();
    surveys = new ArrayList< Cave3DSurvey >();
    stations = new ArrayList< Cave3DStation >();

  }

  protected void computeBoundingBox()
  {
    if ( stations.size() == 0 ) return;
    emin = emax = stations.get(0).x;
    nmin = nmax = stations.get(0).y;
    zmin = zmax = stations.get(0).z;
    for ( Cave3DStation s : stations ) {
      if ( nmin > s.y )      nmin = s.y;
      else if ( nmax < s.y ) nmax = s.y;
      if ( emin > s.x )      emin = s.x;
      else if ( emax < s.x ) emax = s.x;
      if ( zmin > s.z )      zmin = s.z;
      else if ( zmax < s.z ) zmax = s.z;
    }
    x0 = (emax + emin)/2;  // survey center point
    y0 = (nmax + nmin)/2;
    z0 = (zmax + zmin)/2;
  }

  // ---------------------------------------- EXPORT
  void exportModel( int type, String pathname, boolean b_splays, boolean b_walls, boolean b_surface, boolean overwrite )
  { 
      
    if ( type == ModelType.SERIAL ) { // serialization
      if ( ! pathname.endsWith(".txt") ) {
        pathname = pathname + ".txt";
      } 
      if ( ! checkFile(pathname, overwrite ) ) return;
      serializeWalls( pathname );
    } else {                          // model export 
      boolean ret = false;
      if ( type == ModelType.KML_ASCII ) { // KML export ASCII
        if ( ! pathname.endsWith(".kml") ) {
          pathname = pathname + ".kml";
        } 
        if ( ! checkFile(pathname, overwrite ) ) return;
        ExportKML kml = new ExportKML();
        if ( b_walls ) {
          if ( convexhullcomputer != null ) {
            for( CWConvexHull cw : convexhullcomputer.getWalls() ) {
              synchronized( cw ) {
                for ( CWTriangle f : cw.mFace ) kml.add( f );
              }
            }
          } else if ( powercrustcomputer != null && powercrustcomputer.hasTriangles() ) {
            kml.mTriangles = powercrustcomputer.getTriangles();
          }
        }
        ret = kml.exportASCII( pathname, this, b_splays, b_walls, b_surface );
      } else if ( type == ModelType.CGAL_ASCII ) { // CGAL export: only stations and splay-points
        if ( ! pathname.endsWith(".cgal") ) {
          pathname = pathname + ".cgal";
        }
        if ( ! checkFile(pathname, overwrite ) ) return;
        ExportCGAL cgal = new ExportCGAL();
        ret = cgal.exportASCII( pathname, this, b_splays, b_walls, b_surface );
      } else if ( type == ModelType.LAS_BINARY ) { // LAS v. 1.2
        if ( ! pathname.endsWith(".las") ) {
	  pathname = pathname + ".las";
        }
        if ( ! checkFile(pathname, overwrite ) ) return;
        ret = ExportLAS.exportBinary( pathname, this, b_splays, b_walls, b_surface );
      } else if ( type == ModelType.DXF_ASCII ) { // DXF
        if ( ! pathname.endsWith(".dxf") ) {
	  pathname = pathname + ".dxf";
        }
        if ( ! checkFile(pathname, overwrite ) ) return;
        ExportDXF dxf = new ExportDXF();
        if ( b_walls ) {
          if ( convexhullcomputer != null ) {
            for ( CWConvexHull cw : convexhullcomputer.getWalls() ) {
              synchronized( cw ) {
                for ( CWTriangle f : cw.mFace ) dxf.add( f );
              }
            }
          } else if ( powercrustcomputer == null || ! powercrustcomputer.hasTriangles() ) {
            if ( mApp != null ) mApp.toast(R.string.powercrust_dxf_not_supported, pathname, true );
            return;
          }
        }
        ret = dxf.exportAscii( pathname, this, true, b_splays, b_walls, true ); // true = version13
      } else if ( type == ModelType.SHP_ASCII ) { // SHP
        if ( ! pathname.endsWith(".shz") ) {
	  pathname = pathname + ".shz";
        }
        if ( ! checkFile(pathname, overwrite ) ) return;
        ExportSHP shp = new ExportSHP();
        if ( b_walls ) {
          if ( convexhullcomputer != null ) {
            for ( CWConvexHull cw : convexhullcomputer.getWalls() ) {
              synchronized( cw ) {
                for ( CWTriangle f : cw.mFace ) shp.add( f );
              }
            }
          } 
          if ( powercrustcomputer != null && powercrustcomputer.hasTriangles() ) {
            shp.mTriangles = powercrustcomputer.getTriangles();
            shp.mVertex    = powercrustcomputer.getVertices();
          }
        }
        ret = shp.exportASCII( pathname, this, true, b_splays, b_walls );

      } else {                                     // STL export ASCII or binary
        if ( ! pathname.endsWith(".stl") ) {
          pathname = pathname + ".stl";
        } 
        if ( ! checkFile(pathname, overwrite ) ) return;
        ExportSTL stl = new ExportSTL();
        if ( b_walls ) {
          if ( convexhullcomputer != null ) {
            for ( CWConvexHull cw : convexhullcomputer.getWalls() ) {
              synchronized( cw ) {
                for ( CWTriangle f : cw.mFace ) stl.add( f );
              }
            }
          } else if ( powercrustcomputer != null && powercrustcomputer.hasTriangles() ) {
            stl.mTriangles = powercrustcomputer.getTriangles();
            stl.mVertex    = powercrustcomputer.getVertices();
          }
        }

        if ( type == ModelType.STL_BINARY ) {
          ret = stl.exportBinary( pathname, b_splays, b_walls, b_surface );
        } else { // type == ModelType.STL_ASCII
          ret = stl.exportASCII( pathname, b_splays, b_walls, b_surface );
        }
      }
      if ( mApp != null ) { // CRASH here - this should not be necessary
        if ( ret ) {
          mApp.toast(R.string.ok_export, pathname);
        } else {
          mApp.toast(R.string.error_export_failed, pathname, true );
        }
      }
    }
  }

  private void serializeWalls( String filename )
  {
    // if ( Cave3D.mWallConvexHull ) {
      FileWriter fw = null;
      try {
        if ( convexhullcomputer != null ) {
          fw = new FileWriter( filename );
          PrintWriter out = new PrintWriter( fw );
          out.format(Locale.US, "E %d %d\n", convexhullcomputer.getWallsSize(), convexhullcomputer.getBordersSize() );
          for ( CWConvexHull wall : convexhullcomputer.getWalls() ) {
            wall.serialize( out );
          }
          for ( CWBorder border : convexhullcomputer.getBorders() ) {
            border.serialize( out );
          }
        } else if ( powercrustcomputer != null && powercrustcomputer.hasTriangles() ) {
          // TODO
        }
      } catch ( FileNotFoundException e ) { 
        if ( mApp != null ) mApp.toast( R.string.error_file_not_found, true );
      } catch ( IOException e ) {
        if ( mApp != null ) mApp.toast( R.string.error_io_exception, true );
      } finally {
        if ( fw != null ) {
          try {
            fw.flush();
            fw.close();
          } catch (IOException e ) { }
        }
      }
    // } else  {
    //   if ( mApp != null ) mApp.toast( "ConvexHull walls are disabled" );
    // }
  }

  boolean checkFile( String pathname, boolean overwrite )
  {
    // Log.v( "Cave3D-CHECK", "Check file " + pathname + " overwrite " + overwrite );
    if ( (new File(pathname)).exists() && ! overwrite ) {
      if ( mApp != null ) mApp.toast(R.string.warning_not_overwrite, pathname);
      return false;
    }
    return true;
  }

  protected float getGridSize()
  {
    float dx = emax - emin;
    float dy = nmax - nmin;
    float d = (float)Math.sqrt( dx*dx + dy*dy );
    float grid_size = d / 10;
    if ( grid_size > 50 ) { grid_size = 50; }
    else if ( grid_size > 20 ) { grid_size = 20; }
    else if ( grid_size > 10 ) { grid_size = 10; }
    else if ( grid_size >  5 ) { grid_size =  5; }
    else if ( grid_size >  2 ) { grid_size =  2; }
    else if ( grid_size >  1 ) { grid_size =  1; }
    else if ( grid_size > 0.5f ) { grid_size = 0.5f; }
    else if ( grid_size > 0.2f ) { grid_size = 0.2f; }
    else if ( grid_size > 0.1f ) { grid_size = 0.1f; }
    return grid_size;
  }

  // ------------------------ 3D MODEL: CONVEX HULL
  public void makeConvexHull( boolean clear )
  {
    // walls   = null;
    // borders = null;
    if ( ! clear ) {
      if ( shots == null ) return;
      if ( WALL_CW < WALL_MAX /* && Cave3D.mWallConvexHull */ ) {
        convexhullcomputer = new ConvexHullComputer( this, shots );
        (new AsyncTask<Void, Void, Boolean>() {
            public Boolean doInBackground( Void ... v ) {
              return convexhullcomputer.computeConvexHull( );
            }

            public void onPostExecute( Boolean b )
            {
              if ( ! b ) convexhullcomputer = null;
              if ( mApp != null ) mApp.notifyWall( WALL_CW, b );
            }
        }).execute();


      }
      // if ( mCave3D != null ) mCave3D.toast( "computing convex hull walls" );
    }
    // if ( mApp != null ) mApp.setButtonWall(); // private
  }

  /* // FIXME skip -------------------------- WALL_HULL triangles
  public void makeHull()
  {
    triangles_hull = null;
    if ( WALL_HULL < WALL_MAX ) {
      ArrayList< Cave3DHull > hulls = new ArrayList< Cave3DHull >();
      for ( Cave3DShot sh : shots ) {
        Cave3DStation sf = sh.from_station;
        Cave3DStation st = sh.to_station;
        if ( sf != null && st != null ) {
          ArrayList< Cave3DShot > legs1 = getLegsAt( sf, st );
          ArrayList< Cave3DShot > legs2 = getLegsAt( st, sf );
          ArrayList< Cave3DShot > splays1 = getSplayAt( sf, false );
          ArrayList< Cave3DShot > splays2 = getSplayAt( st, false );
          // Log.v( TAG, "splays at " + sf.name + " " + splays1.size() + " at " + st.name + " " + splays2.size() );
          // if ( splays1.size() > 0 && splays2.size() > 0 ) 
          {
            if ( WALL_HULL < WALL_MAX && Cave3D.mWallHull ) {
              hulls.add( new Cave3DHull( sh, splays1, splays2, sf, st ) );
            }
          }
        }
      }
      triangles_hull = new ArrayList< Triangle3D >();
      for ( Cave3DHull h : hulls ) {
        if ( h.triangles != null ) {
          for ( Triangle3D t : h.triangles ) {
            triangles_hull.add( t );
            // Log.v( TAG, t.toString() );
          }
        }
      }
    }
  }
  */

  /* // FIXME skip ------------------------- WALL_DELAUNAY triangles
  public void makeDelaunay()
  {
    triangles_delaunay = null;
    if ( WALL_DELAUNAY < WALL_MAX ) {
      triangles_delaunay = new ArrayList< Triangle3D >();
      for ( Cave3DStation st : stations ) {
        ArrayList< Cave3DShot > station_splays = .getSplayAt( st, false );
        int ns = station_splays.size();
        
        if ( ns >= 4 ) {
          Vector3D[] vec = new Vector3D[ns]; // vector of the splays at station "st"
          for ( int n=0; n<ns; ++n ) {
            Cave3DShot sh = station_splays.get( n );
            float h = sh.len * (float)Math.cos( sh.cln );
            vec[n] = new Vector3D( h * (float)Math.sin(sh.ber), h * (float)Math.cos(sh.ber), sh.len * (float)Math.sin(sh.cln) );
          }
          Cave3DDelaunay delaunay = new Cave3DDelaunay( vec );
          delaunay.insertTrianglesIn( triangles_delaunay, st );
        }
      }
    }
  }
  */


  // ------------------------ 3D MODEL: POWERCRUST
  public void makePowercrust( boolean clear )
  {
    if ( clear ) {
      powercrustcomputer = null;
    } else {
      if ( shots == null ) return;
      if ( WALL_POWERCRUST < WALL_MAX /* && Cave3D.mWallPowercrust */ ) {
        powercrustcomputer = new PowercrustComputer( this, stations, shots );
        (new AsyncTask<Void, Void, Boolean>() {
            public Boolean doInBackground( Void ... v ) {
              return powercrustcomputer.computePowercrust( );
            }

            public void onPostExecute( Boolean b )
            {
              if ( ! b ) powercrustcomputer = null;
              if ( mApp != null ) mApp.notifyWall( WALL_POWERCRUST, b );
            }
        }).execute();
      }
    }
    // if ( mApp != null ) mApp.setButtonWall(); // private
  }

}
