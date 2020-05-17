/** @file OsmFactory.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D OSM file parser
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Canvas;

// import org.xmlpull.v1.XmlPullParserFactory;
// import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

public class OsmFactory
{
  class Path
  {
    ArrayList< Point2D > nodes;
    
    Path()
    {
      nodes = new ArrayList< Point2D >();
    }
    
    int size() { return nodes.size(); }

    void add( Point2D pt ) { nodes.add( pt ); }

    void draw( Canvas canvas, Paint paint )
    {
      if ( size() < 2 ) return;
      Point2D p1 = nodes.get( 0 );
      for ( int k=1; k<size(); ++k ) {
        Point2D p2 = nodes.get( k );
        // draw line p1-p2
        canvas.drawLine( p1.x, p1.y, p2.x, p2.y, paint );
        p1 = p2;
      }
    }
  }

  class Way
  {
    int color;
    ArrayList< Path > paths;
    Path path; 
    
    Way()
    {
      color = 0xffff00ff; // violet
      paths = new ArrayList< Path >();
      path  = null;
    }

    int size()
    {
      int ret = 0;
      for ( Path p : paths ) ret += p.size();
      return ret;
    }

    void append( Point2D pt )
    {
      if ( path == null ) {
        path = new Path();
        paths.add( path );
      }
      path.add( pt );
    }

    void closePath() { path = null; }

    void draw( Canvas canvas ) 
    {
      Paint paint = new Paint();
      paint.setColor( color );
      for ( Path p : paths ) p.draw( canvas, paint );
    }
      
  }

  // --------------------------------------------------------------------

  Cave3DFix mOrigin;
  float x1, y1, x2, y2;
  float s_radius;
  float e_radius;
  // XmlPullParserFactory xmlParserFactory;
  int width;
  int height;
  float mXres, mYres;

  OsmFactory( double xx1, double yy1, double xx2, double yy2, Cave3DFix origin )
  {
    x1 = (float)( xx1 );
    y1 = (float)( yy1 );
    x2 = (float)( xx2 );
    y2 = (float)( yy2 );
    mOrigin = origin;
    // Log.v("TopoGL-OSM", "origin " + origin.longitude + " " + origin.latitude + " X " + origin.x + " " + origin.y + " " + origin.z );
    float PI_180 = (float)(Math.PI / 180);
    float alat = (float)origin.latitude;
    float a = ( alat < 0 )? -alat : alat;
    // KML radius is already multiplied by PI/180
    s_radius = ((90 - a) * ExportKML.EARTH_RADIUS1 + a * ExportKML.EARTH_RADIUS2)/90;
    e_radius = s_radius * (float)Math.cos( alat * PI_180 );
    // xmlParserFactory = XmlPullParserFactory.newInstance();
    mXres = mYres = 0.5f;
    width  = (int)( (x2 - x1 )/mXres );
    height = (int)( (y2 - y1 )/mYres );
    // Log.v("TopoGL-OSM", "bitmap " + width + "x" + height );
  }

  private float m2x( float m ) { return (float)(mOrigin.x + ( m - mOrigin.longitude ) * e_radius); }
  private float p2y( float p ) { return (float)(mOrigin.y + ( p - mOrigin.latitude  ) * s_radius); }
  private float x2m( float x ) { return (float)(mOrigin.longitude + ( x - mOrigin.x ) / e_radius); }
  private float y2p( float y ) { return (float)(mOrigin.latitude  + ( y - mOrigin.y ) / s_radius); }

  private String getValue( String line, int pos )
  {
    int end = line.indexOf( '"', pos );
    return line.substring( pos, end );
  }

  private float getFloat( String line, int pos )
  {
    try {
      return Float.parseFloat( getValue( line, pos ) );
    } catch ( NumberFormatException e ) { }
    return 0;
  }

  public Bitmap getBitmap( String path )
  {
    Bitmap bitmap = null;
    float m1 = x2m( x1 );
    float m2 = x2m( x2 );
    float p1 = y2p( y1 );
    float p2 = y2p( y2 );
    // Log.v("TopoGL-OSM", "bounds M " + m1 + " " + m2 + " P " + p1 + " " + p2 );

    for ( ; ; ) {
      bitmap = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 );
      if ( bitmap != null ) break;
      mXres *= 2;
      mYres *= 2;
      width /= 2;
      height /= 2;
    }
    // Log.v("TopoGL-OSM", "bitmap " + width + "x" + height + " res " + mXres );
    Canvas canvas = new Canvas( bitmap );
    canvas.drawColor( 0xffffffff );

    boolean inNode = false;
    String nodeId = null;
    float lat = 0;
    float lon = 0;
    Way way = null;

    HashMap<String, Point2D> nodes = new HashMap<>();

    try {
      FileReader fis = new FileReader( path );
      BufferedReader br = new BufferedReader( fis );
      String line;
      while ( ( line = br.readLine().trim() ) != null ) {
        if ( line.startsWith( "<?xml" ) ) continue;
        if ( line.startsWith( "<osm" ) ) continue;
        if ( line.startsWith( "</osm" ) ) break;
        if ( line.startsWith( "<node" ) ) {
          inNode = true;
        } else if ( line.startsWith( "</node" ) ) {
          inNode = false;
        } else if ( line.startsWith( "<way" ) ) {
          way = new Way();
        } else if ( line.startsWith( "</way>" ) ) {
          way.draw( canvas );
          way = null;
        } else if ( line.startsWith( "<relation" ) ) {
        } else if ( line.startsWith( "</relation" ) ) {
        }
        if ( inNode ) { // get id, lat, lon
          int idx = line.indexOf( " id=" );
          if ( idx > 0 ) nodeId = getValue( line, idx+5 );
          idx = line.indexOf( " lat=" );
          if ( idx > 0 ) lat = getFloat( line, idx+6 );
          idx = line.indexOf( " lon=" );
          if ( idx > 0 ) lon = getFloat( line, idx+6 );
        }
        if ( inNode && line.indexOf( "/>" ) >= 0 ) {
          if ( nodeId != null && lon >= m1 && lon <= m2 && lat >= p1 && lat <= p2 ) {
            float x = (m2x( lon ) - x1) / mXres; // bitmap coordinates
            float y = height - 1 - (p2y( lat ) - y1) / mYres;
            nodes.put( nodeId, new Point2D( x, y ) ); 
          }       
          inNode = false;
          nodeId = null;
          lat = 0;
          lon = 0;
        }
        if ( way != null ) {
          if ( line.startsWith( "<nd " ) ) {
            int idx = line.indexOf( " ref=" );
            if ( idx > 0 ) {
              String ref = getValue( line, idx+6 );
              Point2D pt = nodes.get( ref );
              if ( pt != null ) { 
                way.append( pt );
              } else {
                way.closePath();
              }
            }
          } else if ( line.startsWith( "<tag " ) ) {
            int idx = line.indexOf( " k=" );
            if ( idx > 0 ) { 
              String value = getValue( line, idx+4 );
              if ( value.equals( "waterway" ) ) {
                way.color = 0xff0066ff;
              } else if ( value.equals( "highway" ) ) {
                way.color = 0xff000000;
              } else if ( value.equals( "building" ) ) {
                way.color = 0xff000000;
              } else if ( value.equals( "amenity" ) ) {
                way.color = 0xffcccccc;
              } else if ( value.equals( "leisure" ) ) {
                way.color = 0xffffcc00;
              } else if ( value.equals( "boundary" ) ) {
                way.color = 0xffcc6666;
              } else if ( value.equals( "landuse" ) ) {
                way.color = 0xff00ff66;
              } else if ( value.equals( "place" ) ) {
                way.color = 0xffccff33;
              } else if ( value.equals( "power" ) ) {
                way.color = 0xffff0000;
              }
            }
          }
        }
      }
    } catch ( IOException e ) { 
    }
    return bitmap;
  }

}
