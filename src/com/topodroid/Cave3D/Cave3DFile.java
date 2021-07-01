/* @file Cave3DFile.java
 *
 * @author marco corvi
 * @date apr 2021
 *
 * @brief Cave3D file wrapper
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.os.Environment;
import android.os.Build;

import android.util.Log;

public class Cave3DFile
{
  // android P (9) is API 28
  final static boolean NOT_ANDROID_10 = ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.P );
  final static boolean NOT_ANDROID_11 = ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q );

  // static String EXTERNAL_STORAGE_PATH =  // app base path
  //   NOT_ANDROID_10 ? Environment.getExternalStorageDirectory().getAbsolutePath()
  //                  : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
  //                  // : "/sdcard";
  //                  // : null; 

  static String TOPODROID_PATH =
    Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.topodroid.DistoX/files";
  static String SYMBOL_PATH = TOPODROID_PATH + "/point";
  static String C3D_PATH    = TOPODROID_PATH + "/c3d";

  // static String mAppBasePath   = HOME_PATH;
  static String HOME_PATH = null;
    // Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/com.topodroid.Cave3D/files";
  static String BLUETOOTH_PATH = null; // HOME_PATH + "/surveys";
  static File BLUETOOTH_DIR = null;

  // static void setAppBasePath( String base_path )
  // {
  //   mAppBasePath = (base_path != null)? base_path : HOME_PATH;
  // }

  static String getHomePath()      { return HOME_PATH; }
  static String getBluetoothPath() { return BLUETOOTH_PATH; }
  static File   getBluetoothDir()  { return BLUETOOTH_DIR; }

  // reset app base path
  static void checkAppBasePath( TopoGL app )
  {
    HOME_PATH = app.getExternalFilesDir( null ).getPath();
    // Log.v("Cave3D", "check app base path. External files dir " + home_path );
    // File dir = new File( TOPODROID_PATH );
    // File[] files = dir.listFiles();
    // for ( File file : files ) Log.v("Cave3D", file.getPath() );

    // if ( EXTERNAL_STORAGE_PATH == null ) {
    //   EXTERNAL_STORAGE_PATH = home_path;
    // }
    // mAppBasePath = EXTERNAL_STORAGE_PATH;
    // // Log.v("TopoGL", "use base path " + mAppBasePath );

    BLUETOOTH_DIR = app.getExternalFilesDir( "surveys" );
    BLUETOOTH_PATH = BLUETOOTH_DIR.getPath();
    if ( ! BLUETOOTH_DIR.exists() ) BLUETOOTH_DIR.mkdirs();
  }

  static boolean hasC3dDir( ) { return new File( C3D_PATH ).exists(); }

  static boolean hasBluetoothSurvey( String name ) { return getBluetoothSurveyFile( name ).exists(); }

  static File getBluetoothSurveyFile( String name ) { return new File( BLUETOOTH_DIR, name ); }
  
  static String getBluetoothFilename( String name ) { return BLUETOOTH_PATH + "/" + name; }

  // static String getBluetoothDirname( ) { return BLUETOOTH_PATH; }

  static FileOutputStream getFileOutputStream( String path ) throws IOException, FileNotFoundException
  { return new FileOutputStream( path ); }

  static FileInputStream getFileInputStream( String path ) throws IOException, FileNotFoundException
  { return new FileInputStream( path ); }

  // -------------------------------------------------------------------------------
  // utils

  public static String getExtension( String filename )
  {
    int pos = filename.lastIndexOf(".");
    if ( pos < 0 ) return null;
    return filename.substring( pos+1 ).toLowerCase();
  }

  public static String getFilename( String filename )
  {
    int pos = filename.lastIndexOf("/");
    if ( pos >= 0 ) {
      return filename.substring( pos + 1 );
    }
    return filename;
  }

  public static String getMainname( String filename )
  {
    int pos = filename.lastIndexOf("/");
    int qos = filename.lastIndexOf(".");
    if ( pos >= 0 ) {
      if ( qos > pos ) {
        return filename.substring( pos + 1, qos );
      } else {
        return filename.substring( pos + 1 );
      }
    } else {
      if ( qos >= 0 ) {
        return filename.substring( 0, qos );
      } else {
        return filename;
      }
    }
  }

}    
