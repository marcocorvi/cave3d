/** @file ParserBluetooth.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief parser for bluetooth surveys
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

/* the bluetooth parser contains only one survey
 */
public class ParserBluetooth extends TglParser
{
  static final int FLIP_NONE       = 0;
  static final int FLIP_HORIZONTAL = 1;
  static final int FLIP_VERTICAL   = 2;

  static final int DATA_NONE      = 0;
  static final int DATA_NORMAL    = 1;
  static final int DATA_DIMENSION = 2;

  int mLastStationNr  = 0;
  int mSurveyId = 1;
  Cave3DStation mLastStation;
  // Cave3DSurvey mSurvey; // this is surveys.get(0)

  public ParserBluetooth( TopoGL app, String filename, String name ) throws ParserException
  {
    super( app, filename, name );
    Log.v("Cave3D", "BT Parser cstr - filename: " + filename );
  }

  public void initialize()
  {
    if ( surveys.size() == 0 || stations.size() == 0 ) {
      initializeEmpty();
      return;
    }
    Log.v("Cave3D", "BT Parser init: surveys " + surveys.size() + " stations " + stations.size() + " fix " + fixes.size() );
    assert( surveys.size() == 1 );
    // mSurvey = surveys.get( 0 );
    mLastStation   = stations.get( stations.size() - 1 );
    mLastStationNr = Integer.parseInt( mLastStation.name );
    if ( fixes.size() == 0 ) {
      Cave3DStation st = stations.get( 0 );
      fixes.add( new Cave3DFix( st.name, st.x, st.y, st.z, null ) );
    }

    mCaveLength   = 0;
    emin = mLastStation.x; emax = mLastStation.x;
    nmin = mLastStation.y; nmax = mLastStation.y;
    zmin = mLastStation.z; zmax = mLastStation.z;
    for ( Cave3DStation st : stations ) {
      if ( st.x < emin ) { emin = st.x; } else if ( st.x > emax ) { emax = st.x; }
      if ( st.y < nmin ) { nmin = st.y; } else if ( st.y > nmax ) { nmax = st.y; }
      if ( st.z < zmin ) { zmin = st.z; } else if ( st.z > zmax ) { zmax = st.z; }
    }
    x0 = (emax + emin)/2;
    y0 = (nmax + nmin)/2;
    z0 = (zmax + zmin)/2;
    
    Log.v("Cave3D", "BT Parser init: center " + x0 + " " + y0 + " " + z0 );
    x0 = y0 = z0 = 0; // model center
  }
        

  private void initializeEmpty()
  {
    Log.v("Cave3D", "BT Parser init empty - name " + mName );
    // mSurvey = new Cave3DSurvey( mName );
    // surveys.add( mSurvey );
    surveys.add( new Cave3DSurvey( mName, mSurveyId, -1 ) ); // survey-id 1, parent survey -1 (none)

    String name = String.format("%d", mLastStationNr );
    mLastStation = new Cave3DStation( name, 0.0f, 0.0f, 0.0f, mLastStationNr, mSurveyId, 0, "" );
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
  private Cave3DStation makeStation( String name, double e, double n, double z, int id, int sid )
  {
    // double x0 = mLastStation.x + e;
    // double y0 = mLastStation.y + z;
    // double z0 = mLastStation.z - n;
    double x0 = mLastStation.x + e;
    double y0 = mLastStation.y + n;
    double z0 = mLastStation.z + z;
    updateBBox( x0, y0, z0 );
    Log.v("Cave3D", "last station <" + mLastStation.name + "> " + mLastStation.x + " " + mLastStation.y + " " + mLastStation.z );
    Log.v("Cave3D", "BT parser - make station <" + name + "> " + x0 + " " + y0 + " " + z0 );
    return new Cave3DStation( name, x0, y0, z0, id, sid, 0, "" );
  }

  // FIXME BLUETOOTH_CCORDS
  // static Vector3D bluetoothToVector( Vector3D w ) { return new Vector3D( w.x, w.y, w.z ); }
  public static Vector3D bluetoothToVector( Vector3D w ) { return new Vector3D( w.x, w.z, -w.y ); }


  public Cave3DShot addSplay( double d, double b, double c, double e, double n, double z )
  {
    Cave3DStation station = makeStation( "-", e, n, z, -1, mSurveyId );
    Cave3DStation from = (mStartStation == null)? mLastStation : mStartStation;

    Cave3DShot shot = new Cave3DShot( from, station, d, b, c, 0, System.currentTimeMillis() );
    splays.add( shot );
    // mSurvey.addSplay( shot );
    surveys.get(0).addSplay( shot );
    Log.v("Cave3D", "BT parser splay: " + d + " " + b + " " + c );
    // Log.v("Cave3D", "BT parser last station " + mLastStation.name + " " + mLastStation.x + " " + mLastStation.y + " " + mLastStation.z );
    return shot;
  }

  public Cave3DShot addLeg( double d, double b, double c, double e, double n, double z )
  {
    ++ mLastStationNr;
    Cave3DStation from = (mStartStation == null)? mLastStation : mStartStation;
    mStartStation = null;

    String to = String.format("%d", mLastStationNr );
    Cave3DStation station = makeStation( to, e, n, z, mLastStationNr, mSurveyId );
    Cave3DShot shot = new Cave3DShot( from, station, d, b, c, 0, System.currentTimeMillis() );
    shots.add( shot );
    stations.add( station );
    // mSurvey.addShot( shot );
    // mSurvey.addStation( station );
    surveys.get(0).addShot( shot );
    surveys.get(0).addStation( station );

    mLastStation = station;
    mCaveLength += d;
    Log.v("Cave3D", String.format("BT parser added leg: %.2f %.1f %.1f to %.2f %.2f %.2f ", d, b, c, e, n, z ) );
    Log.v("Cave3D", "BT parser last station " + mLastStation.name + " " + mLastStation.x + " " + mLastStation.y + " " + mLastStation.z );
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
