/** @file CGALExporter.java
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

import android.util.Log;

public class CGALExporter
{
  // ArrayList<CWFacet> mFacets;
  // float lat, lng, asl;
  float s_radius = 1;
  float e_radius = 1;
  Cave3DStation zero = new Cave3DStation( "", 0, 0, 0 );

  // CGALExporter() { }

  boolean exportASCII( String filename, Cave3DParser data, boolean do_splays, boolean do_walls, boolean do_station )
  {
    // String name = "Cave3D";
    // Log.v("DistoX", "export as CGAL " + filename );

    List< Cave3DStation> stations = data.getStations();
    List< Cave3DShot>    shots    = data.getShots();
    List< Cave3DShot>    splays   = data.getSplays();

    // now write the KML
    try {
      FileWriter fw = new FileWriter( filename );
      PrintWriter pw = new PrintWriter( fw );

      int nst = stations.size();
      int nsp = splays.size();
      pw.format(Locale.US, "OFF\n");
      pw.format(Locale.US, "%d 0 0\n", (nst+nsp) );
      pw.format(Locale.US, "\n");

      for ( Cave3DStation st : stations ) {
        float e = (st.e - zero.e) * e_radius;
        float n = (st.n - zero.n) * s_radius;
        float z = (st.z - zero.z);
        int cnt = 0;
        for ( Cave3DShot sp : splays ) {
          if ( st == sp.from_station ) ++cnt;
        }
        pw.format(Locale.US, "# %s %d\n", st.name, cnt );
        pw.format(Locale.US, "%.2f %.2f %.2f\n", e, n, z );
        for ( Cave3DShot sp : splays ) {
          if ( st == sp.from_station ) {
            Cave3DVector v = sp.toCave3DVector();
            e = (st.e + v.x - zero.e) * e_radius;
            n = (st.n + v.y - zero.n) * s_radius;
            z = (st.z + v.z - zero.z);
            pw.format(Locale.US, "%.2f %.2f %.2f\n", e, n, z );
          }
        }  
      }
      fw.flush();
      fw.close();
      return true;
    } catch ( IOException e ) {
      // TDLog.Error( "Failed KML export: " + e.getMessage() );
      return false;
    }
  }

}


