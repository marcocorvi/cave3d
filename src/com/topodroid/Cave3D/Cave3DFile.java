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

class Cave3DFile
{
  // android P (9) is API 28
  final static boolean NOT_ANDROID_10 = ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.P );
  final static boolean NOT_ANDROID_11 = ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q );

  static String EXTERNAL_STORAGE_PATH =  // app base path
    NOT_ANDROID_10 ? Environment.getExternalStorageDirectory().getAbsolutePath()
                   : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
                   // : "/sdcard";
                   // : null; 

  static String HOME_PATH = EXTERNAL_STORAGE_PATH;
                          // "/sdcard/Android/data/com.topodroid.Cave3D/files";
  static String mAppBasePath = HOME_PATH;
  static String SYMBOL_PATH = EXTERNAL_STORAGE_PATH + "/TopoDroid/symbol/point";
  static String C3D_PATH    = EXTERNAL_STORAGE_PATH + "/TopoDroid/c3d";
  static String BLUETOOTH_PATH = EXTERNAL_STORAGE_PATH + "/Cave3D";

  static void setAppBasePath( String base_path )
  {
    mAppBasePath = (base_path != null)? base_path : HOME_PATH;
  }

  // reset app base path
  static void checkAppBasePath( TopoGL app )
  {
    if ( EXTERNAL_STORAGE_PATH == null ) {
      EXTERNAL_STORAGE_PATH = app.getExternalFilesDir( null ).getPath();
    }
    mAppBasePath = EXTERNAL_STORAGE_PATH;
    // Log.v("TopoGL", "use base path " + mAppBasePath );

    File bt_dir = new File( BLUETOOTH_PATH );
    if ( ! bt_dir.exists() ) bt_dir.mkdirs();
  }

  static boolean hasC3dDir( ) { return new File( C3D_PATH ).exists(); }

  static boolean hasBluetoothSurvey( String name ) { return getBluetoothSurveyFile( name ).exists(); }

  static File getBluetoothSurveyFile( String name ) { return new File( getBluetoothFilename( name ) ); }
  
  static String getBluetoothFilename( String name ) { return BLUETOOTH_PATH + "/" + name; }

  static String getBluetoothDirname( ) { return BLUETOOTH_PATH; }

  static FileOutputStream getFileOutputStream( String path ) throws IOException, FileNotFoundException
  { return new FileOutputStream( path ); }

  static FileInputStream getFileInputStream( String path ) throws IOException, FileNotFoundException
  { return new FileInputStream( path ); }

}    