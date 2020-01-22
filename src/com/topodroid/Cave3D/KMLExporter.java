/** @file KMLExporter.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Walls KML exporter
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

import java.io.FileWriter;
import java.io.PrintWriter;
// import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class KMLExporter
{
  private static final String TAG = "Cave3D KML";

  ArrayList<CWFacet> mFacets;
  float lat, lng, asl;
  float s_radius, e_radius;
  Cave3DStation zero;
  ArrayList< Cave3DTriangle > mTriangles;

  KMLExporter()
  {
    mFacets = new ArrayList< CWFacet >();
    mTriangles = null;
  }

  void add( CWFacet facet ) { mFacets.add( facet ); }

  void add( CWPoint v1, CWPoint v2, CWPoint v3 )
  {
     mFacets.add( new CWFacet( v1, v2, v3 ) );
  }

  static float EARTH_RADIUS1 = (float)(6378137 * Math.PI / 180.0f); // semimajor axis [m]
  static float EARTH_RADIUS2 = (float)(6356752 * Math.PI / 180.0f);

  private boolean getGeolocalizedData( Cave3DParser data, float decl, float asl_factor )
  {
    // Log.v( TAG, "get geoloc. data. Decl " + decl );
    List< Cave3DFix > fixes = data.getFixes();
    if ( fixes.size() == 0 ) return false;

    Cave3DFix origin = null;
    for ( Cave3DFix fix : fixes ) {
      if ( fix.cs == null ) continue;
      if ( ! fix.cs.name.equals("long-lat") ) continue;
      for ( Cave3DStation st : data.getStations() ) {
        if ( st.name.equals( fix.name ) ) {
          origin = fix;
          zero   = st;
          break;
        }
      }
      if ( origin != null ) break;
    }
    if ( origin == null ) {
      // Log.v( TAG, "no geolocalized origin");
      return false;
    }

    // origin has coordinates ( e, n, z ) these are assumed lat-long
    // altitude is assumed wgs84
    lat = (float)origin.n;
    lng = (float)origin.e;
    asl = (float)origin.z; // KML uses Geoid altitude (unless altitudeMode is set)
    // Log.v( TAG, "origin " + lat + " N " + lng + " E " + asl );
    float alat = ( lat > 0 )? lat : - lat;

    s_radius = ((90 - alat) * EARTH_RADIUS1 + alat * EARTH_RADIUS2)/90;
    e_radius = s_radius * (float)Math.cos( alat * Math.PI / 180.0 );

    s_radius = 1 / s_radius;
    e_radius = 1 / e_radius;

    return true;
  }

  boolean exportASCII( String filename, Cave3DParser data, boolean do_splays, boolean do_walls, boolean do_station )
  {
    String name = "Cave3D";
    boolean ret = true;

    // Log.v( TAG, "export as KML " + filename );
    if ( ! getGeolocalizedData( data, 0.0f, 1.0f ) ) { // FIXME declination 0.0f
      Log.w( TAG, "no geolocalized station");
      return false;
    }

    List< Cave3DStation> stations = data.getStations();
    List< Cave3DShot>    shots    = data.getShots();
    List< Cave3DShot>    splays   = data.getSplays();

    // now write the KML
    try {
      FileWriter fw = new FileWriter( filename );
      PrintWriter pw = new PrintWriter( fw );

      pw.format(Locale.US, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      pw.format(Locale.US, "<kml xmlnx=\"http://www.opengis.net/kml/2.2\">\n");
      pw.format(Locale.US, "<Document>\n");

      pw.format(Locale.US, "<name>%s</name>\n", name );
      pw.format(Locale.US, "<description>%s</description>\n", name );

      pw.format(Locale.US, "<Style id=\"centerline\">\n");
      pw.format(Locale.US, "  <LineStyle>\n");
      pw.format(Locale.US, "    <color>ff0000ff</color>\n"); // AABBGGRR
      pw.format(Locale.US, "    <width>2</width>\n");
      pw.format(Locale.US, "  </LineStyle>\n");
      pw.format(Locale.US, "  <LabelStyle>\n");
      pw.format(Locale.US, "     <color>ff0000ff</color>\n"); // AABBGGRR
      pw.format(Locale.US, "     <colorMode>normal</colorMode>\n");
      pw.format(Locale.US, "     <scale>1.0</scale>\n");
      pw.format(Locale.US, "  </LabelStyle>\n");
      pw.format(Locale.US, "</Style>\n");

      pw.format(Locale.US, "<Style id=\"splay\">\n");
      pw.format(Locale.US, "  <LineStyle>\n");
      pw.format(Locale.US, "    <color>ff66cccc</color>\n"); // AABBGGRR
      pw.format(Locale.US, "    <width>1</width>\n");
      pw.format(Locale.US, "  </LineStyle>\n");
      pw.format(Locale.US, "  <LabelStyle>\n");
      pw.format(Locale.US, "     <color>ff66cccc</color>\n"); // AABBGGRR
      pw.format(Locale.US, "     <colorMode>normal</colorMode>\n");
      pw.format(Locale.US, "     <scale>0.5</scale>\n");
      pw.format(Locale.US, "  </LabelStyle>\n");
      pw.format(Locale.US, "</Style>\n");

      pw.format(Locale.US, "<Style id=\"station\">\n");
      pw.format(Locale.US, "  <IconStyle><Icon></Icon></IconStyle>\n");
      pw.format(Locale.US, "  <LabelStyle>\n");
      pw.format(Locale.US, "     <color>ffff00ff</color>\n"); // AABBGGRR
      pw.format(Locale.US, "     <colorMode>normal</colorMode>\n");
      pw.format(Locale.US, "     <scale>1.0</scale>\n");
      pw.format(Locale.US, "  </LabelStyle>\n");
      pw.format(Locale.US, "  <LineStyle>\n");
      pw.format(Locale.US, "    <color>ffff00ff</color>\n"); // AABBGGRR
      pw.format(Locale.US, "    <width>1</width>\n");
      pw.format(Locale.US, "  </LineStyle>\n");
      pw.format(Locale.US, "</Style>\n");
      
      pw.format(Locale.US, "<Style id=\"wall\">\n");
      pw.format(Locale.US, "  <IconStyle><Icon></Icon></IconStyle>\n");
      pw.format(Locale.US, "  <LineStyle>\n");
      pw.format(Locale.US, "    <color>9900ccff</color>\n"); // AABBGGRR
      pw.format(Locale.US, "    <width>1</width>\n");
      pw.format(Locale.US, "  </LineStyle>\n");
      pw.format(Locale.US, "  <PolyStyle>\n");
      // pw.format(Locale.US, "    <color>9900ccff</color>\n"); // AABBGGRR
      pw.format(Locale.US, "    <color>9900ccff</color>\n"); // AABBGGRR
      pw.format(Locale.US, "    <colorMode>normal</colorMode>\n"); 
      pw.format(Locale.US, "    <fill>1</fill>\n"); 
      pw.format(Locale.US, "    <outline>1</outline>\n"); 
      pw.format(Locale.US, "  </PolyStyle>\n");
      pw.format(Locale.US, "</Style>\n");

      if ( do_station ) {
        for ( Cave3DStation st : stations ) {
          float e = lng + (st.e - zero.e) * e_radius;
          float n = lat + (st.n - zero.n) * s_radius;
          float z = asl + (st.z - zero.z);
          pw.format(Locale.US, "<Placemark>\n");
          pw.format(Locale.US, "  <name>%s</name>\n", st.name );
          pw.format(Locale.US, "  <styleUrl>#station</styleUrl>\n");
          pw.format(Locale.US, "  <MultiGeometry>\n");
            pw.format(Locale.US, "  <Point id=\"%s\">\n", st.name );
            pw.format(Locale.US, "    <coordinates>%f,%f,%f</coordinates>\n", e, n, z );
            pw.format(Locale.US, "  </Point>\n");
          pw.format(Locale.US, "  </MultiGeometry>\n");
          pw.format(Locale.US, "</Placemark>\n");
        }
      }

      pw.format(Locale.US, "<Placemark>\n");
      pw.format(Locale.US, "  <name>centerline</name>\n" );
      pw.format(Locale.US, "  <styleUrl>#centerline</styleUrl>\n");
      pw.format(Locale.US, "  <MultiGeometry>\n");
      pw.format(Locale.US, "    <altitudeMode>absolute</altitudeMode>\n");
      for ( Cave3DShot sh : shots ) {
        Cave3DStation sf = sh.from_station;
        Cave3DStation st = sh.to_station;
        float ef = lng + (sf.e - zero.e) * e_radius;
        float nf = lat + (sf.n - zero.n) * s_radius;
        float zf = asl + (sf.z - zero.z);
        float et = lng + (st.e - zero.e) * e_radius;
        float nt = lat + (st.n - zero.n) * s_radius;
        float zt = asl + (st.z - zero.z);
        pw.format(Locale.US, "    <LineString id=\"%s-%s\"> <coordinates>\n", sf.name, st.name );
        // pw.format(Locale.US, "      <tessellate>1</tessellate>\n"); //   breaks the line up in small chunks
        // pw.format(Locale.US, "      <extrude>1</extrude>\n"); // extends the line down to the ground
        pw.format(Locale.US, "        %f,%f,%f %f,%f,%f\n", ef, nf, zf, et, nt, zt );
        pw.format(Locale.US, "    </coordinates> </LineString>\n");
      }
      pw.format(Locale.US, "  </MultiGeometry>\n");
      pw.format(Locale.US, "</Placemark>\n");

      if ( do_splays ) {
        pw.format(Locale.US, "<Placemark>\n");
        pw.format(Locale.US, "  <name>splays</name>\n" );
        pw.format(Locale.US, "  <styleUrl>#splay</styleUrl>\n");
        pw.format(Locale.US, "  <MultiGeometry>\n");
        pw.format(Locale.US, "    <altitudeMode>absolute</altitudeMode>\n");
        for ( Cave3DShot sp : splays ) {
          Cave3DStation sf = sp.from_station;
          Cave3DVector v = sp.toCave3DVector();
          float ef = lng + (sf.e - zero.e) * e_radius;
          float nf = lat + (sf.n - zero.n) * s_radius;
          float zf = asl + (sf.z - zero.z);
          float et = lng + (sf.e + v.x - zero.e) * e_radius;
          float nt = lat + (sf.n + v.y - zero.n) * s_radius;
          float zt = asl + (sf.z + v.z - zero.z);
          pw.format(Locale.US, "    <LineString> <coordinates>\n" );
          // pw.format(Locale.US, "      <tessellate>1</tessellate>\n"); //   breaks the line up in small chunks
          // pw.format(Locale.US, "      <extrude>1</extrude>\n"); // extends the line down to the ground
          pw.format(Locale.US, "        %f,%f,%f %f,%f,%f\n", ef, nf, zf, et, nt, zt );
          pw.format(Locale.US, "    </coordinates> </LineString>\n");
        }
        pw.format(Locale.US, "  </MultiGeometry>\n");
        pw.format(Locale.US, "</Placemark>\n");
      }

      if ( do_walls ) {
        pw.format(Locale.US, "<Placemark>\n");
        pw.format(Locale.US, "  <name>walls</name>\n" );
        pw.format(Locale.US, "  <styleUrl>#wall</styleUrl>\n");
        pw.format(Locale.US, "  <altitudeMode>absolute</altitudeMode>\n");
        pw.format(Locale.US, "  <MultiGeometry>\n");
        for ( CWFacet facet : mFacets ) {
          float e1 = lng + (facet.v1.x - zero.e) * e_radius;
          float n1 = lat + (facet.v1.y - zero.n) * s_radius;
          float z1 = asl + (facet.v1.z - zero.z);
          float e2 = lng + (facet.v2.x - zero.e) * e_radius;
          float n2 = lat + (facet.v2.y - zero.n) * s_radius;
          float z2 = asl + (facet.v2.z - zero.z);
          float e3 = lng + (facet.v3.x - zero.e) * e_radius;
          float n3 = lat + (facet.v3.y - zero.n) * s_radius;
          float z3 = asl + (facet.v3.z - zero.z);
          pw.format(Locale.US, "    <Polygon>\n");
          pw.format(Locale.US, "      <outerBoundaryIs> <LinearRing> <coordinates>\n");
          pw.format(Locale.US, "             %f,%f,%.3f\n", e1,n1,z1);
          pw.format(Locale.US, "             %f,%f,%.3f\n", e2,n2,z2);
          pw.format(Locale.US, "             %f,%f,%.3f\n", e3,n3,z3);
          pw.format(Locale.US, "      </coordinates> </LinearRing> </outerBoundaryIs>\n");
          pw.format(Locale.US, "    </Polygon>\n");
          pw.format(Locale.US, "    <LineString> <coordinates>\n");
          pw.format(Locale.US, "             %f,%f,%.3f %f,%f,%.3f", e1,n1,z1, e2,n2,z2 );
          pw.format(Locale.US, "    </coordinates> </LineString>\n");
          pw.format(Locale.US, "    <LineString> <coordinates>\n");
          pw.format(Locale.US, "             %f,%f,%.3f %f,%f,%.3f", e2,n2,z2, e3,n3,z3 );
          pw.format(Locale.US, "    </coordinates> </LineString>\n");
          pw.format(Locale.US, "    <LineString> <coordinates>\n");
          pw.format(Locale.US, "             %f,%f,%.3f %f,%f,%.3f", e3,n3,z3, e1,n1,z1 );
          pw.format(Locale.US, "    </coordinates> </LineString>\n");
        }
        if ( mTriangles != null ) {
          for ( Cave3DTriangle t : mTriangles ) {
            pw.format(Locale.US, "    <Polygon>\n");
            pw.format(Locale.US, "      <outerBoundaryIs> <LinearRing> <coordinates>\n");
            for ( int k = 0; k < t.size; ++k ) {
              float e1 = lng + (t.vertex[k].x - zero.e) * e_radius;
              float n1 = lat + (t.vertex[k].y - zero.n) * s_radius;
              float z1 = asl + (t.vertex[k].z - zero.z);
              pw.format(Locale.US, "             %f,%f,%.3f\n", e1,n1,z1);
            }
            pw.format(Locale.US, "      </coordinates> </LinearRing> </outerBoundaryIs>\n");
            pw.format(Locale.US, "    </Polygon>\n");
            float e0 = lng + (t.vertex[t.size-1].x - zero.e) * e_radius;
            float n0 = lat + (t.vertex[t.size-1].y - zero.n) * s_radius;
            float z0 = asl + (t.vertex[t.size-1].z - zero.z);
            for ( int k = 0; k < t.size; ++k ) {
              float e1 = lng + (t.vertex[k].x - zero.e) * e_radius;
              float n1 = lat + (t.vertex[k].y - zero.n) * s_radius;
              float z1 = asl + (t.vertex[k].z - zero.z);
              pw.format(Locale.US, "    <LineString> <coordinates>\n");
              pw.format(Locale.US, "             %f,%f,%.3f %f,%f,%.3f", e0,n0,z0, e1,n1,z1 );
              pw.format(Locale.US, "    </coordinates> </LineString>\n");
              e0 = e1;
              n0 = n1;
              z0 = z1;
            }
          }
        }
        pw.format(Locale.US, "  </MultiGeometry>\n");
        pw.format(Locale.US, "</Placemark>\n");
      }

      pw.format(Locale.US, "</Document>\n");
      pw.format(Locale.US, "</kml>\n");
      fw.flush();
      fw.close();
      return true;
    } catch ( IOException e ) {
      Log.w( TAG, "I/O error " + e.getMessage() );
      return false;
    }
  }

}

