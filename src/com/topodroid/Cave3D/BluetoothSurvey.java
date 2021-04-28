/* @file BluetoothSurvey.java
 *
 * @author marco corvi
 * @date apr 2021
 *
 * @brief BT survey struct
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import com.topodroid.in.ParserBluetooth;
import com.topodroid.Cave3D.TglParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

import android.util.Log;

class BluetoothSurvey
{
  private String mName;
  private String mNickname; // filename
  private ParserBluetooth mParser;

  // This is a bit messy:
  //   BluetoothSurvey needs a ParserBluetooth
  //   ParserBluetooth needs the app
  //   and the BluetoothSurvey is created by the BluetoothSurveyManager
  BluetoothSurvey( String name, String nick ) 
  {
    Log.v("Cave3D", "BT survey from name " + name + " " + nick );
    mName     = name;
    mNickname = nick;
    mParser   = null;
  }

  String getName() { return mName; }

  String getFilename() { return mNickname; }

  String getNickname() { return mNickname; }
  void setNickname( String nick ) { mNickname = nick; }

  void setParser( ParserBluetooth parser ) 
  { 
    mParser = parser;
    if ( mParser != null ) {
      boolean ret = BluetoothSurveyManager.loadSurvey( this );
      Log.v("cave3D", "BT survey load survey " + ret );
      mParser.initialize();
    }
  }

  ParserBluetooth getParser( ) { return mParser; }

  boolean hasParser() { return mParser != null; }

  Cave3DShot addLeg( double d, double b, double c, double e, double n, double z )
  { return (mParser == null)? null : mParser.addLeg( d, b, c, e, n, z ); }

  Cave3DShot addSplay( double d, double b, double c, double e, double n, double z )
  { return (mParser == null)? null : mParser.addSplay( d, b, c, e, n, z ); }

  Cave3DStation getLastStation() 
  { return (mParser == null)? null : mParser.getLastStation(); }

  void saveSurvey() 
  {
    boolean ret = BluetoothSurveyManager.saveSurvey( this );
    Log.v("cave3D", "BT survey save survey " + ret );
  }

  boolean serialize( String filepath )
  {
    Log.v("Cave3D", "BT survey serialize " + filepath );
    try { 
      FileOutputStream fos = Cave3DFile.getFileOutputStream( filepath );
      BufferedOutputStream bos = new BufferedOutputStream ( fos );
      DataOutputStream dos = new DataOutputStream( bos );
      serialize( dos );
      if ( mParser != null ) mParser.serialize( dos );
      dos.close();
      fos.close();
    } catch ( FileNotFoundException e ) {
      Log.e("Cave3D", "Export Data file: " + e.getMessage() );
      return false;
    } catch ( IOException e ) {
      Log.e("Cave3D", "Export Data i/o: " + e.getMessage() );
      return false;
    }
    return true;
  }


  boolean deserialize( String filepath, boolean header_only )
  {
    Log.v("Cave3D", "Parser deserialize " + filepath );
    try { 
      FileInputStream fis = Cave3DFile.getFileInputStream( filepath );
      BufferedInputStream bis = new BufferedInputStream ( fis );
      DataInputStream dis = new DataInputStream( bis );
      deserialize( dis );
      if ( (! header_only) && mParser != null ) mParser.deserialize( dis );
      dis.close();
      fis.close();
    } catch ( FileNotFoundException e ) {
      Log.e("Cave3D", "Import Data file: " + e.getMessage() );
      return false;
    } catch ( IOException e ) {
      Log.e("Cave3D", "Import Data i/o: " + e.getMessage() );
      return false;
    }
    return true;
  }

  private void serialize( DataOutputStream dos ) throws IOException
  {
    dos.writeUTF( mName );
    Log.v("Cave3D", "BT survey serialized: " + mName );
  }

  private void deserialize( DataInputStream dis ) throws IOException
  {
    mName = dis.readUTF( );
    Log.v("Cave3D", "BT survey deserialized: " + mName );
  }

}
