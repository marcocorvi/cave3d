/** @file ShpExporter.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Shapefile exporter
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import java.io.File;
import java.io.IOException;


public class ShpExporter
{
  private static final String TAG = "Cave3D SHP";

  ArrayList< CWFacet > mFacets;
  ArrayList< Cave3DTriangle > mTriangles; // powercrust triangles

  Cave3DVector[] mVertex; // triangle vertices
  Cave3DVector mMin;
  Cave3DVector mMax;
  float xoff, yoff, zoff; // offset to have positive coords values
  float scale;       // scale factor

  ShpExporter()
  {
    mFacets = new ArrayList< CWFacet >();
    mTriangles = null;
    mVertex    = null; 
    resetMinMax();
  }

  private void resetMinMax()
  {
    xoff = 0;
    yoff = 0;
    zoff = 0;
    scale = 1.0f;
    mMin = new Cave3DVector();
    mMax = new Cave3DVector();
  }

  void add( CWFacet facet ) { mFacets.add( facet ); }

  void add( CWPoint v1, CWPoint v2, CWPoint v3 )
  {
     mFacets.add( new CWFacet( v1, v2, v3 ) );
  }

  // ---------------------------------------------------------------------------

  // filepath    filename with no ending '/'
  boolean exportASCII( String filename, Cave3DParser data, boolean b_legs, boolean b_splays, boolean b_walls )
  {

    boolean ret = true;
    ArrayList<File> files = new ArrayList<File>();

    String filepath = filename.replace(".shz", "" );

    File path = new File(filepath);
    if ( path.exists() ) {
      Log.w( TAG, "Export error: file exists" );
      return false;
    }
    path.mkdirs();
    if ( true )     ret &= exportStations( filepath, files, data.getStations() );
    if ( b_legs )   ret &= exportShots( filepath, files, data.getShots(), "leg" );
    if ( b_splays ) ret &= exportShots( filepath, files, data.getSplays(), "splay" );
    if ( b_walls ) {
      ret &= exportFacets( filepath, files, mFacets );
      if ( mTriangles != null ) ret &= exportTriangles( filepath, files, mTriangles );
    }

    if ( ret ) {
      Archiver zipper = new Archiver( );
      zipper.compressFiles( filename, files );
    }
    deleteDir( path ); // delete temporary shapedir

    return true;
  }

  private boolean exportStations( String filepath, List<File> files, List< Cave3DStation> stations )
  {
    // Log.v( TAG, "Export stations " + stations.size() );
    boolean ret = false;
    try {
      ShpPointz shp = new ShpPointz( filepath + "/station",  files );
      // shp.setYYMMDD( info.date );
      ret = shp.writeStations( stations );
    } catch ( IOException e ) {
      Log.w( TAG, "Failed station export: " + e.getMessage() );
    }
    return ret;
  }
    
  private boolean exportShots( String filepath, List<File> files, List< Cave3DShot> shots, String name )
  {
    // Log.v( TAG, "Export " + name + " " + shots.size() );
    boolean ret = false;
    try {
      ShpPolylinez shp = new ShpPolylinez( filepath + "/" + name, files );
      // shp.setYYMMDD( info.date );
      ret = shp.writeShots( shots, name );
    } catch ( IOException e ) {
      Log.w( TAG, "Failed " + name + " export: " + e.getMessage() );
    }
    return ret;
  }

  private boolean exportFacets( String filepath, List<File> files, List< CWFacet > facets )
  {
    // Log.v( TAG, "Export facets " + facets.size() );
    boolean ret = false;
    try {
      ShpPolygonz shp = new ShpPolygonz( filepath + "/facet", files );
      // shp.setYYMMDD( info.date );
      ret = shp.writeFacets( facets );
    } catch ( IOException e ) {
      Log.w( TAG, "Failed facet export: " + e.getMessage() );
    }
    return ret;
  }

  private boolean exportTriangles( String filepath, List<File> files, List< Cave3DTriangle > triangles )
  {
    // Log.v( TAG, "Export triangles " + triangles.size() );
    boolean ret = false;
    try {
      ShpPolygonz shp = new ShpPolygonz( filepath + "/triangle", files );
      // shp.setYYMMDD( info.date );
      ret = shp.writeTriangles( mTriangles );
    } catch ( IOException e ) {
      Log.w( TAG, "Failed triangle export: " + e.getMessage() );
    }
    return ret;
  }

  static void deleteDir( File dir )
  {
    if ( dir != null && dir.exists() ) {
      File[] files = dir.listFiles();
      if ( files != null ) {
        for (File file : files ) {
          if (file.isFile()) {
            if ( ! file.delete() ) Log.w( TAG, "File delete failed " + file.getName() ); 
          }
        }
      }
      if ( ! dir.delete() ) Log.w( TAG, "Dir delete failed " + dir.getName() );
    }
  }

}

