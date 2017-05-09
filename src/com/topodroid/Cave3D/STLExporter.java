/** @file STLExporter.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Walls STL exporter
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.util.ArrayList;

import java.io.FileWriter;
import java.io.PrintWriter;
// import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.util.Log;

public class STLExporter
{
  ArrayList<CWFacet> mFacets;
  float x, y, z; // offset to have positive coords values

  ArrayList< Cave3DTriangle > mTriangles; // powercrust triangles
  Cave3DVector[] mVertex; // triangle vertices

  STLExporter()
  {
    mFacets = new ArrayList< CWFacet >();
    x = 0;
    y = 0;
    z = 0;
    mTriangles = null;
    mVertex    = null;
  }

  void add( CWFacet facet ) { mFacets.add( facet ); }

  void add( CWPoint v1, CWPoint v2, CWPoint v3 )
  {
     mFacets.add( new CWFacet( v1, v2, v3 ) );
  }

  private void makePositiveCoords()
  {
    x = 0;
    y = 0;
    z = 0;
    for ( CWFacet facet : mFacets ) {
      if ( facet.v1.x < x ) x = facet.v1.x;
      if ( facet.v1.y < y ) y = facet.v1.y;
      if ( facet.v1.z < z ) z = facet.v1.z;
      if ( facet.v2.x < x ) x = facet.v2.x;
      if ( facet.v2.y < y ) y = facet.v2.y;
      if ( facet.v2.z < z ) z = facet.v2.z;
      if ( facet.v3.x < x ) x = facet.v3.x;
      if ( facet.v3.y < y ) y = facet.v3.y;
      if ( facet.v3.z < z ) z = facet.v3.z;
    }
    if ( mVertex != null ) {
      int len = mVertex.length;
      for ( int k=0; k<len; ++k ) {
        Cave3DVector v = mVertex[k];
        if ( v.x < x ) x = v.x;
        if ( v.y < y ) y = v.y;
        if ( v.z < z ) z = v.z;
      }
    }
    x = -x;
    y = -y;
    z = -z;
  }
  
  boolean exportASCII( String filename, boolean splays, boolean walls, boolean surface )
  {
    makePositiveCoords();
    String name = "Cave3D";
    boolean ret = true;
    FileWriter fw = null;
    try {
      fw = new FileWriter( filename );
      PrintWriter pw = new PrintWriter( fw );
      pw.format("solid %s\n", name );
      for ( CWFacet facet : mFacets ) {
        pw.format("  facet normal %.3f %.3f %.3f\n", facet.un.x, facet.un.y, facet.un.z );
        pw.format("    outer loop\n");
        pw.format("      vertex %.3f %.3f %.3f\n", x+facet.v1.x, y+facet.v1.y, z+facet.v1.z );
        pw.format("      vertex %.3f %.3f %.3f\n", x+facet.v2.x, y+facet.v2.y, z+facet.v2.z );
        pw.format("      vertex %.3f %.3f %.3f\n", x+facet.v3.x, y+facet.v3.y, z+facet.v3.z );
        pw.format("    endloop\n");
        pw.format("  endfacet\n");
      }
      if ( mTriangles != null ) {
        for ( Cave3DTriangle t : mTriangles ) {
          int size = t.size;
          Cave3DVector n = t.normal;
          pw.format("  facet normal %.3f %.3f %.3f\n", n.x, n.y, n.z );
          pw.format("    outer loop\n");
          for ( int k=0; k<size; ++k ) {
            Cave3DVector v = t.vertex[k];
            pw.format("      vertex %.3f %.3f %.3f\n", x+v.x, y+v.y, z+v.z );
          }
          pw.format("    endloop\n");
          pw.format("  endfacet\n");
        }
      }
      pw.format("endsolid %s\n", name );
    } catch ( FileNotFoundException e ) { 
      Log.e("Cave3D", "ERROR " + e.getMessage() );
      ret = false;
    } catch( IOException e ) {
      Log.e("Cave3D", "I/O ERROR " + e.getMessage() );
      ret = false;
    } finally {
      try {
        if ( fw != null ) fw.close();
      } catch ( IOException e ) {}
    }
    return ret;
  }

  private void intToByte( int i, byte[] b )
  {
    b[0] = (byte)(  i      & 0xff );
    b[1] = (byte)( (i>>8)  & 0xff );
    b[2] = (byte)( (i>>16) & 0xff );
    b[3] = (byte)( (i>>24) & 0xff );
  }

  private void floatToByte( float f, byte[] b )
  {
    int i = Float.floatToIntBits( f );
    b[0] = (byte)(  i      & 0xff );
    b[1] = (byte)( (i>>8)  & 0xff );
    b[2] = (byte)( (i>>16) & 0xff );
    b[3] = (byte)( (i>>24) & 0xff );
  }

  // int toIntLEndian( byte val[] ) 
  // {
  //   return val[0] | ( ((int)val[1]) << 8 ) | ( ((int)(val[2])) << 16 ) | ( ((int)(val[3])) << 24 );
  // }

  // float toFloatLEndian( byte val[] ) 
  // {
  //   return (float)( val[0] | ( ((int)val[1]) << 8 ) | ( ((int)(val[2])) << 16 ) | ( ((int)(val[3])) << 24 ) );
  // }

  boolean exportBinary( String filename, boolean splays, boolean walls, boolean surface ) 
  {
    makePositiveCoords();
    String name = "Cave3D";
    boolean ret = true;
    FileOutputStream fw = null;
    byte[] header = new byte[80];
    byte[] b4 = new byte[4];
    byte[] b2  = new byte[2];
    for (int k=0; k<80; ++k) header[k] = (byte)0;
    b2[0] = (byte)0;
    b2[1] = (byte)0;
    try {
      fw = new FileOutputStream( filename );
      BufferedOutputStream bw = new BufferedOutputStream( fw ); 
      bw.write( header, 0, 80 );
      int sz = mFacets.size();
      if ( mTriangles != null ) sz += mTriangles.size();
      intToByte( sz, b4 ); fw.write( b4 );
      for ( CWFacet facet : mFacets ) {
        floatToByte(   facet.un.x, b4 ); bw.write( b4, 0, 4 );
        floatToByte(   facet.un.y, b4 ); bw.write( b4, 0, 4 );
        floatToByte(   facet.un.z, b4 ); bw.write( b4, 0, 4 );
        floatToByte( x+facet.v1.x, b4 ); bw.write( b4, 0, 4 );
        floatToByte( y+facet.v1.y, b4 ); bw.write( b4, 0, 4 );
        floatToByte( z+facet.v1.z, b4 ); bw.write( b4, 0, 4 );
        floatToByte( x+facet.v2.x, b4 ); bw.write( b4, 0, 4 );
        floatToByte( y+facet.v2.y, b4 ); bw.write( b4, 0, 4 );
        floatToByte( z+facet.v2.z, b4 ); bw.write( b4, 0, 4 );
        floatToByte( x+facet.v3.x, b4 ); bw.write( b4, 0, 4 );
        floatToByte( y+facet.v3.y, b4 ); bw.write( b4, 0, 4 );
        floatToByte( z+facet.v3.z, b4 ); bw.write( b4, 0, 4 );
        bw.write( b2, 0, 2 );
      }
      if ( mTriangles != null ) {
        for ( Cave3DTriangle t : mTriangles ) {
          int size = t.size;
          Cave3DVector n = t.normal;
          floatToByte( n.x, b4 ); bw.write( b4, 0, 4 );
          floatToByte( n.y, b4 ); bw.write( b4, 0, 4 );
          floatToByte( n.z, b4 ); bw.write( b4, 0, 4 );
          for ( int k=0; k<size; ++k ) {
            Cave3DVector v = t.vertex[k];
            floatToByte( x+v.x, b4 ); bw.write( b4, 0, 4 );
            floatToByte( y+v.y, b4 ); bw.write( b4, 0, 4 );
            floatToByte( z+v.z, b4 ); bw.write( b4, 0, 4 );
            bw.write( b2, 0, 2 );
          }
        }
      }
    } catch ( FileNotFoundException e ) { 
      Log.e("Cave3D", "ERROR " + e.getMessage() );
      ret = false;
    } catch( IOException e ) {
      Log.e("Cave3D", "I/O ERROR " + e.getMessage() );
      ret = false;
    } finally {
      try {
        if ( fw != null ) fw.close();
      } catch ( IOException e ) {}
    }
    return ret;
  }

}

