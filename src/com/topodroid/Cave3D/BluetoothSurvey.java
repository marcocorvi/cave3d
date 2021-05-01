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
  final static boolean LOG = false;

  private String mName;
  private String mNickname; // filename
  private ParserBluetooth mParser;

  // This is a bit messy:
  //   BluetoothSurvey needs a ParserBluetooth
  //   ParserBluetooth needs the app
  //   and the BluetoothSurvey is created by the BluetoothSurveyManager
  BluetoothSurvey( String name, String nick ) 
  {
    if (LOG) Log.v("Cave3D", "BT survey from name " + name + " " + nick );
    mName     = name;
    mNickname = nick;
    mParser   = null;
  }

  String getName() { return mName; }

  String getFilename() { return mNickname; }

  String getNickname() { return mNickname; }
  void setNickname( String nick ) { mNickname = nick; }

  void setBluetoothParser( ParserBluetooth parser ) 
  { 
    mParser = parser;
    if ( mParser != null ) {
      boolean ret = BluetoothSurveyManager.loadSurvey( this );
      if (LOG) Log.v("cave3D", "BT survey load survey " + ret );
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
    if (LOG) Log.v("cave3D", "BT survey save survey " + ret );
  }

  boolean serialize( String filepath )
  {
    Log.v("Cave3D", "BT survey serialize " + filepath );
    try { 
      FileOutputStream fos = Cave3DFile.getFileOutputStream( filepath );
      BufferedOutputStream bos = new BufferedOutputStream ( fos );
      DataOutputStream dos = new DataOutputStream( bos );
      dos.write('V');
      dos.writeInt( TopoGL.VERSION_CODE );
      dos.write('H'); // header
      serialize( dos );
      if ( mParser != null ) {
        dos.write('P'); // parser
        mParser.serialize( dos );
      }
      dos.write('E'); // file end
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
    if (LOG) Log.v("Cave3D", "deserialize " + filepath );
    try { 
      FileInputStream fis = Cave3DFile.getFileInputStream( filepath );
      BufferedInputStream bis = new BufferedInputStream ( fis );
      DataInputStream dis = new DataInputStream( bis );
      int what = dis.read(); // 'V'
      int version = dis.readInt( );
      boolean done = false;
      while ( ! done ) {
        what = dis.read(); // 'H'
        switch (what) {
          case 'H':
            deserialize( dis, version );
            if ( header_only ) done = true;
            break;
          case 'P':
            if ( mParser != null ) {
              mParser.deserialize( dis, version );
            } else {
              Log.e("Cave3D", "deserializing data without the parser");
              done = true;
            }
            break;
          case 'E':
            done = true;
            break;
          default:
            Log.e("Cave3D", "Bluetooth deserialize error - tag " + what );
            done = true;
            break;
        }
      }
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
    dos.write('N'); // NAME
    dos.writeUTF( mName );
    dos.write('E'); // END
    if (LOG) Log.v("Cave3D", "BT survey serialized: " + mName );
  }

  private void deserialize( DataInputStream dis, int version ) throws IOException
  {
    int what = 0;
    boolean done = false;
    while ( ! done ) {
      what = dis.read(); 
      switch (what) {
        case 'N':
          mName = dis.readUTF( );
          break;
        case 'E':
          done = true;
          break;
        default:
          Log.e("Cave3D", "Bluetooth survey deserialize error - tag " + what );
          done = true;
          break;
      }
    }
    if (LOG) Log.v("Cave3D", "BT survey deserialized: " + mName );
  }

}
