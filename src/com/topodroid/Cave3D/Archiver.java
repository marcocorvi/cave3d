/** @file Archiver.java
 *
 * @author marco corvi
 * @date june 2012
 *
 * @brief Cave3D shapefile export archiver
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.util.List;

class Archiver
{
  private static final String TAG = "Cave3D ZIP";

  static final int BUF_SIZE = 4096;
  byte[] data; 

  Archiver( ) 
  {
    data = new byte[ BUF_SIZE ];
  }

  boolean compressFiles( String zipname, List<File> files )
  {
    ZipOutputStream zos = null;
    boolean ret = true;
    try {
      zos = new ZipOutputStream( new BufferedOutputStream( new FileOutputStream( zipname ) ) );
      for ( File file : files ) ret &= addEntry( zos, file );
      // for ( File file : files ) TDUtil.deleteFile( file );
    } catch ( FileNotFoundException e ) {
    // } catch ( IOException e ) {
    //   // FIXME
    } finally {
      if ( zos != null ) try { zos.close(); } catch ( IOException e ) { 
        ret = false;
        // Log.w( TAG, "compress close error");
      }
    }
    return ret;
  }

  private boolean addEntry( ZipOutputStream zos, File name )
  {
    if ( name == null || ! name.exists() ) return false;
    boolean ret = false;
    BufferedInputStream bis = null;
    try { 
      // Log.w( TAG, "add file " + name.getPath() );
      bis = new BufferedInputStream( new FileInputStream( name ), BUF_SIZE );
      ZipEntry entry = new ZipEntry( name.getName() );
      int cnt;
      zos.putNextEntry( entry );
      while ( (cnt = bis.read( data, 0, BUF_SIZE )) != -1 ) {
        zos.write( data, 0, cnt );
      }
      zos.closeEntry( );
      ret = true;
    } catch (FileNotFoundException e ) {
      Log.w( TAG, "File Not Found " + e.getMessage() );
    } catch ( IOException e ) {
      Log.w( TAG,  "IO exception " + e.getMessage() );
    } finally {
      if ( bis != null ) try { bis.close(); } catch (IOException e ) { /* ret = false; */ }
    }
    // Log.v( "DistoX-ZIP", "zip add file " + name.getPath() + " return " + ret );
    return ret;
  }
}
