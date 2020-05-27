/** @file ParserSketch.java
 *
 * @author marco corvi
 * @date apr 2020
 *
 * @brief TopoDroid Sketch-export parser
 *
 * Usage:
 *    ParserSketch Sketch = new ParserSketch( filename );
 *    if ( Sketch.valid() ) { // use data
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.util.ArrayList;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

class ParserSketch 
{
  private boolean mValid;    // header is valid
  String mFilename; // Sketch filename
  String mName;
  int mType; // 1 PLAN, 2 PROFILE
  int mAzimuth;
  ArrayList< SketchPoint > mPoints;
  ArrayList< SketchLine  > mLines;
  ArrayList< SketchLine  > mAreas;

  // ------------------------------------------------- LOG
  void log()
  {
    Log.v("TopoGL", "Sketch Pts " + mPoints.size() + " lines " + mLines.size() + " areas " + mAreas.size() );
    for ( SketchPoint pt : mPoints ) Log.v("TopoGL", "Pt " + pt.thname + " " + pt.x + " " + pt.y + " " + pt.z );
    for ( SketchLine  ln : mLines  ) Log.v("TopoGL", "Ln " + ln.thname + " " + ln.size() );
    for ( SketchLine  ln : mAreas  ) Log.v("TopoGL", "Ar " + ln.thname + " " + ln.size() );
  }

  // ------------------------------------------------- 

  ParserSketch( String filename )
  {
    mFilename = filename;
  }

  boolean valid() { return mValid; }

  boolean readData( )
  {
    mValid = false;
    mPoints = new ArrayList<>();
    mLines  = new ArrayList<>();
    mAreas  = new ArrayList<>();
    try {
      FileReader fr = new FileReader( mFilename );
      BufferedReader br = new BufferedReader( fr );
      String line = br.readLine();
      if ( line == null ) return false;
      line = line.trim().replaceAll( "\\s+", " " );
      String[] vals = line.split(" ");
      if ( vals.length < 7 ) return false; // SCRAP name type azimuth x y z
      mName = vals[1];
      mType = Integer.parseInt( vals[2] );
      mAzimuth = Integer.parseInt( vals[3] );
      float xoff = Float.parseFloat( vals[4] );
      float yoff = Float.parseFloat( vals[5] );
      float zoff = Float.parseFloat( vals[6] );
      while ( ( line = br.readLine() ) != null ) {
        line = line.trim().replaceAll( "\\s+", " " );
        vals = line.split(" ");
        if ( line.startsWith( "LINE" ) || line.startsWith( "AREA" ) ) {
          float red   = Float.parseFloat( vals[2] );
          float green = Float.parseFloat( vals[3] );
          float blue  = Float.parseFloat( vals[4] );
          float alpha = line.startsWith("AREA")? Float.parseFloat( vals[5] ) : 1.0f ;
          SketchLine ln = new SketchLine( vals[1], red, green, blue, alpha );
          while ( ( line = br.readLine() ) != null ) {
            if ( line.startsWith( "ENDLINE" ) ) {
              mLines.add( ln );
              break;
            }
            if ( line.startsWith( "ENDAREA" ) ) {
              mAreas.add( ln );
              break;
            }
            line = line.trim().replaceAll( "\\s+", " " );
            vals = line.split(" ");
            float x = xoff + Float.parseFloat( vals[0] );
            float y = yoff + Float.parseFloat( vals[1] );
            float z = zoff + Float.parseFloat( vals[2] );
            ln.insertPoint( x, y, z );
          }
        } else if ( line.startsWith( "POINT" ) ) {
          String th = vals[1];
          // azimuth = vals[2]
          float x = xoff + Float.parseFloat( vals[3] );
          float y = yoff + Float.parseFloat( vals[4] );
          float z = zoff + Float.parseFloat( vals[5] );
          mPoints.add( new SketchPoint( x, y, z, th ) );
        }
      }
      fr.close();
    } catch ( IOException e ) { }
    mValid = true;
    return true;
  }
            
}

