/* @file DialogSurveyManager.java
 *
 * @author marco corvi
 * @date apr 2021
 *
 * @brief BT survey manager
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import com.topodroid.in.ParserBluetooth;

import java.io.File;

import android.util.Log;

class BluetoothSurveyManager
{
  static BluetoothSurvey getSurvey( String name ) 
  {
    if ( name == null || name.length() == 0 ) {
      Log.v("Cave3D", "BT survey manager get survey: null name");
      return null;
    }
    if ( ! Cave3DFile.hasBluetoothSurvey( name ) ) {
      Log.v("Cave3D", "BT survey manager get: survey does not exist " + name );
      return null;
    }
    Log.v("Cave3D", "BT survey manager get: survey " + name );
    BluetoothSurvey bt_survey = new BluetoothSurvey( name, name );
    bt_survey.deserialize( Cave3DFile.getBluetoothFilename( name ), true ); // deserialze header_only
    return bt_survey;
  }

  static BluetoothSurvey createSurvey( String name )
  {
    if ( name == null || name.length() == 0 ) {
      Log.v("Cave3D", "BT survey manager create survey: null name");
      return null;
    }
    if ( Cave3DFile.hasBluetoothSurvey( name ) ) {
      Log.v("Cave3D", "BT survey manager create: survey exists " + name );
      return null;
    }
    Log.v("Cave3D", "BT survey manager create: survey new " + name );
    BluetoothSurvey bt_survey = new BluetoothSurvey( name, name );
    // saveSurvey( bt_survey );
    return bt_survey;
  }

  static void renameSurvey( BluetoothSurvey bt_survey, String new_filename )
  {
    if ( new_filename == null || new_filename.length() == 0 ) return;
    String old_filename = bt_survey.getFilename();
    if ( new_filename.equals( old_filename ) ) {
      Log.v("Cave3D", "BT survey manager rename survey: file unchanged " + new_filename );
      return;
    }
    File new_file = new File( Cave3DFile.getBluetoothFilename( new_filename ) );
    if ( new_file.exists() ) {
      Log.v("Cave3D", "BT survey manager rename survey: file exists " + new_filename );
      return;
    }
    File old_file = new File( Cave3DFile.getBluetoothFilename( old_filename ) );
    if ( old_file.renameTo( new_file ) ) {
      Log.v("Cave3D", "BT survey manager rename survey: " + old_filename + " --> " + new_filename );
      bt_survey.setNickname( new_filename );
    }
  }

  static boolean saveSurvey( BluetoothSurvey bt_survey )
  {
    if ( bt_survey == null ) {
      Log.v("Cave3D", "BT survey manager save survey: null");
      return false;
    }
    Log.v("Cave3D", "BT survey manager save survey " + bt_survey.getFilename() );
    return bt_survey.serialize( Cave3DFile.getBluetoothFilename( bt_survey.getFilename() ) );
  }

  static boolean loadSurvey( BluetoothSurvey bt_survey )
  {
    if ( bt_survey == null ) {
      Log.v("Cave3D", "BT survey manager retrieve survey: null");
      return false;
    }
    Log.v("Cave3D", "BT survey manager retrieve data "  + bt_survey.getFilename() );
    return bt_survey.deserialize( Cave3DFile.getBluetoothFilename( bt_survey.getFilename() ), false ); // deserialize all
  }

}
