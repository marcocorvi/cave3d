/** @file ParserBluetooth.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief compass file parser
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.in;

import com.topodroid.Cave3D.TopoGL;
import com.topodroid.Cave3D.TglParser;
import com.topodroid.Cave3D.Cave3DFix;
import com.topodroid.Cave3D.Cave3DStation;
import com.topodroid.Cave3D.Cave3DSurvey;
import com.topodroid.Cave3D.Cave3DShot;
import com.topodroid.Cave3D.Vector3D;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.util.Log;

public class ParserBluetooth extends TglParser
{
  static final int FLIP_NONE       = 0;
  static final int FLIP_HORIZONTAL = 1;
  static final int FLIP_VERTICAL   = 2;

  static final int DATA_NONE      = 0;
  static final int DATA_NORMAL    = 1;
  static final int DATA_DIMENSION = 2;

  int mLastStationNr  = 0;
  Cave3DStation mLastStation;
  Cave3DSurvey mSurvey;


  public ParserBluetooth( TopoGL app, String filename ) throws ParserException
  {
    super( app, filename );
    // Log.v("TopoGL", "====== Bluetooth Parser ======");

    mSurvey = new Cave3DSurvey( filename );
    surveys.add( mSurvey );

    String name = String.format("%d", mLastStationNr );
    mLastStation = new Cave3DStation( name, 0.0f, 0.0f, 0.0f );
    stations.add( mLastStation );
    // mStartStation = null;

    fixes.add( new Cave3DFix( name, 0.0f, 0.0f, 0.0f, null ) );

    mCaveLength   = 0;
    x0 = y0 = z0 = 0; // model center
    emin = -10; emax = 10;
    nmin = -10; nmax = 10;
    zmin = -10; zmax = 10;
  }

  // FIXME BLUETOOTH_CCORDS
  private Cave3DStation makeStation( String name, double e, double n, double z )
  {
    // double x0 = mLastStation.x + e;
    // double y0 = mLastStation.y + z;
    // double z0 = mLastStation.z - n;
    double x0 = mLastStation.x + e;
    double y0 = mLastStation.y + n;
    double z0 = mLastStation.z + z;
    updateBBox( x0, y0, z0 );
    // Log.v("TopoGL", "last station <" + mLastStation.name + "> " + mLastStation.x + " " + mLastStation.y + " " + mLastStation.z );
    // Log.v("TopoGL", "station <" + name + "> " + x0 + " " + y0 + " " + z0 );
    return new Cave3DStation( name, x0, y0, z0 );
  }

  // FIXME BLUETOOTH_CCORDS
  // static Vector3D bluetoothToVector( Vector3D w ) { return new Vector3D( w.x, w.y, w.z ); }
  public static Vector3D bluetoothToVector( Vector3D w ) { return new Vector3D( w.x, w.z, -w.y ); }


  public Cave3DShot addSplay( double d, double b, double c, double e, double n, double z )
  {
    Cave3DStation station = makeStation( "-", e, n, z );

    Cave3DShot shot = new Cave3DShot( mLastStation, station, d, b, c, 0, System.currentTimeMillis() );
    splays.add( shot );
    mSurvey.addSplay( shot );
    // Log.v("TopoGL", "BT nr. splays " + splays.size() );
    // Log.v("TopoGL", "BT last station " + mLastStation.name + " " + mLastStation.x + " " + mLastStation.y + " " + mLastStation.z );
    return shot;
  }

  public Cave3DShot addLeg( double d, double b, double c, double e, double n, double z )
  {
    ++ mLastStationNr;
    String to = String.format("%d", mLastStationNr );
    Cave3DStation station = makeStation( to, e, n, z);
    Cave3DShot shot = new Cave3DShot( mLastStation, station, d, b, c, 0, System.currentTimeMillis() );
    shots.add( shot );
    stations.add( station );
    mSurvey.addShot( shot );
    mSurvey.addStation( station );

    mLastStation = station;
    mCaveLength += d;
    return shot;
  }

  public Cave3DStation getLastStation() { return mLastStation; }

  private void updateBBox( double x, double y, double z )
  {
    if ( x < emin ) { emin = x; } else if ( x > emax ) { emax = x; }
    if ( y < nmin ) { nmin = y; } else if ( y > nmax ) { nmax = y; }
    if ( z < zmin ) { zmin = z; } else if ( z > zmax ) { zmax = z; }
  }

}
