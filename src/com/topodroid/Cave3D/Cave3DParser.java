/** @file Cave3DParser.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D therion file parser and model
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.util.Log;

public class Cave3DParser
{
  protected static final String TAG = "Cave3D Parser";

  boolean do_render; // whether ready to render
  Cave3D mCave3D;

  protected ArrayList< Cave3DSurvey > surveys;
  protected ArrayList< Cave3DFix > fixes;
  protected ArrayList< Cave3DStation > stations;
  protected ArrayList< Cave3DShot > shots;   // centerline shots
  protected ArrayList< Cave3DShot > splays;  // splay shots
  public float emin, emax, nmin, nmax, zmin, zmax;
  protected Cave3DSurface mSurface;
  protected float mCaveLength;

  // public float getEmin() { return emin; }
  // public float getEmax() { return emax; }
  // public float getNmin() { return nmin; }
  // public float getNmax() { return nmax; }
  // public float getVmin() { return zmin; }
  // public float getVmax() { return zmax; }

  public int getStationNumber() { return stations.size(); }
  public int getShotNumber()    { return shots.size(); }
  public int getSplayNumber()   { return splays.size(); }
  public int getSurveySize()    { return surveys.size(); }

  public ArrayList< Cave3DSurvey >  getSurveys() { return surveys; }
  public ArrayList< Cave3DShot >    getShots() { return shots; }
  public ArrayList< Cave3DShot >    getSplays() { return splays; }
  public ArrayList< Cave3DStation > getStations() { return stations; }
  public ArrayList< Cave3DFix >     getFixes() { return fixes; }

  public Cave3DSurface getSurface() { return mSurface; }

  void setStationsPathlength( float len ) 
  {
    for ( Cave3DStation st : stations ) st.setPathlength( len );
  }

  Cave3DSurvey getSurvey( String name )
  {
    if ( name == null ) return null;
    for ( Cave3DSurvey s : surveys ) if ( name.equals( s.name ) ) return s;
    return null;
  }

  Cave3DSurvey getSurvey( int id )
  {
    if ( id < 0 ) return null;
    for ( Cave3DSurvey s : surveys ) if ( s.mId == id ) return s;
    return null;
  }

  Cave3DStation getStation( int id ) 
  {
    if ( id < 0 ) return null;
    for ( Cave3DStation s : stations ) if ( s.mId == id ) return s;
    return null;
  }

  Cave3DStation getStation( String name ) 
  {
    if ( name == null ) return null;
    for ( Cave3DStation s : stations ) if ( name.equals( s.short_name ) ) return s;
    return null;
  }


  float getCaveDepth() { return zmax - zmin; }
  float getCaveLength() { return mCaveLength; }

  public float[] getStationVertices()
  {
    float v[] = new float[ 3 * stations.size() ];
    int k = 0;
    int k3 = 0;
    for ( Cave3DStation s : stations ) {
      s.vertex = k++;
      v[k3++] = s.e; // X horizontal
      v[k3++] = s.n; // Y vertical
      v[k3++] = s.z; // Z depth
    }
    return v;
  }
 
  /** 3D vertices of the centerline shots
   */
  // public float[] getShotVertices()
  // {
  //   float v[] = new float[ 3 * 2 * shots.size() ];
  //   int k = 0;
  //   for ( Cave3DShot sh : shots ) {
  //     Cave3DStation sf = sh.from_station;
  //     Cave3DStation st = sh.to_station;
  //     // Log.v(TAG, "getShotVertices shot " + sh.from + " " + sh.to );
  //     if ( sf != null && st != null ) {
  //       v[k++] = sf.e;
  //       v[k++] = sf.z;
  //       v[k++] = sf.n;
  //       v[k++] = st.e;
  //       v[k++] = st.z;
  //       v[k++] = st.n;
  //     } else {
  //       if ( sf == null ) Log.e( TAG, " shot has null from station" );
  //       if ( st == null ) Log.e( TAG, " shot has null to station" );
  //     }
  //   }
  //   return v;
  // }


  /** 3D vertices of the splay shots
   */
  // public float[] getSplayVertices()
  // {
  //   float v[] = new float[ 3 * 2 * splays.size() ];
  //   int k = 0;
  //   for ( Cave3DShot sh : splays ) {
  //     Cave3DStation sf = sh.from_station;
  //     Cave3DStation st = sh.to_station;
  //     // Log.v(TAG, "getShotVertices shot " + sh.from + " " + sh.to );
  //     if ( sf != null ) {
  //       v[k++] = sf.e;
  //       v[k++] = sf.z;
  //       v[k++] = sf.n;
  //       v[k++] = st.e;
  //       v[k++] = st.z;
  //       v[k++] = st.n;
  //     } else {
  //       if ( sf == null ) Log.e( TAG, " shot has null from station" );
  //     }
  //   }
  //   return v;
  // }

  ArrayList< Cave3DShot > getLegAt( Cave3DStation station )
  {
    ArrayList< Cave3DShot > ret = new ArrayList< Cave3DShot >();
    for ( Cave3DShot shot : shots ) {
      if ( shot.from_station == station ) {
        ret.add( shot );
      } else if ( shot.to_station == station ) {
        double b = shot.ber + Math.PI;
        if ( b > 2*Math.PI ) b -= 2*Math.PI;
        ret.add( new Cave3DShot( null, null, shot.len, (float)b, -shot.cln) );
      }
    }
    return ret;
  }

  /** get the legs at "station"
   */
  ArrayList< Cave3DShot > getLegsAt( Cave3DStation station )
  {
    // Log.v("Cave3D", "get legs at " + station.name );
    ArrayList< Cave3DShot > ret = new ArrayList< Cave3DShot >();
    for ( Cave3DShot shot : shots ) { // add survey legs too1
      if ( shot.from_station == station ) {
        ret.add( shot );
      } else if ( shot.to_station == station ) {
        ret.add( shot );
      }
    }
    return ret;
  }

  /** get the legs at "station"
   *  the legs that do not connect "other" are reversed
   *  Therefore the return has legs
   *      station - other
   *      xxx - station
   */
  ArrayList< Cave3DShot > getLegsAt( Cave3DStation station, Cave3DStation other )
  {
    // Log.v("Cave3D", "get legs at " + station.name + " other " + other.name );
    ArrayList< Cave3DShot > ret = new ArrayList< Cave3DShot >();
    for ( Cave3DShot shot : shots ) { // add survey legs too1
      if ( shot.from_station == station ) {
        if ( shot.to_station == other ) {
          // Log.v("Cave3D", "add direct shot " + shot.from + "-" + shot.to );
          ret.add( shot );
        } else {
          // Log.v("Cave3D", "add other shot " + shot.to + "-" + shot.from );
          double b = shot.ber + Math.PI;
          if ( b > 2*Math.PI ) b -= 2*Math.PI;
          ret.add( new Cave3DShot( null, null, shot.len, (float)b, -shot.cln) ); // stations not important
        }
      } else if ( shot.to_station == station ) {
        if ( shot.from_station == other ) {
          // Log.v("Cave3D", "add reversed shot " + shot.to + "-" + shot.from );
          double b = shot.ber + Math.PI;
          if ( b > 2*Math.PI ) b -= 2*Math.PI;
          ret.add( new Cave3DShot( null, null, shot.len, (float)b, -shot.cln) ); // stations not important
        } else {
          // Log.v("Cave3D", "add other shot " + shot.from + "-" + shot.to );
          ret.add( shot );
        }
      }
    }
    return ret;
  }

  ArrayList< Cave3DShot > getSplayAt( Cave3DStation station, boolean also_legs )
  {
    ArrayList< Cave3DShot > ret = new ArrayList< Cave3DShot >();
    for ( Cave3DShot splay : splays ) {
      if ( splay.from_station == station ) {
        ret.add( splay );
      }
    }
    if ( also_legs ) {
      for ( Cave3DShot shot : shots ) { // add survey legs too1
        if ( shot.from_station == station ) {
          ret.add( shot );
        } else if ( shot.to_station == station ) {
          double b = shot.ber + Math.PI;
          if ( b > 2*Math.PI ) b -= 2*Math.PI;
          ret.add( new Cave3DShot( null, null, shot.len, (float)b, -shot.cln) ); // stations not important
        }
      }
    }
    return ret;
  }

  protected void setStationDepths( )
  {
    float deltaz = zmax - zmin + 0.001f;
    int k = 0;
    for ( Cave3DStation s : stations ) {
      s.depth = (zmax - s.z)/deltaz; // Z depth
      // Log.v(TAG, "Depth " + s.name + " " + s.depth ); 
    }  
    if ( stations.size() > 0 ) do_render = true;
  }

  public Cave3DParser( Cave3D cave3d ) 
  {
    do_render = false;

    // Log.v("Cave3D", "parsing " + filename );

    mCave3D = cave3d;
    mSurface = null;

    fixes  = new ArrayList< Cave3DFix >();
    shots  = new ArrayList< Cave3DShot >();
    splays = new ArrayList< Cave3DShot >();
    surveys = new ArrayList< Cave3DSurvey >();
    stations = new ArrayList< Cave3DStation >();

  }

  protected void computeBoundingBox()
  {
    if ( stations.size() == 0 ) return;
    emin = emax = stations.get(0).e;
    nmin = nmax = stations.get(0).n;
    zmin = zmax = stations.get(0).z;
    for ( Cave3DStation s : stations ) {
      if ( nmin > s.n )      nmin = s.n;
      else if ( nmax < s.n ) nmax = s.n;
      if ( emin > s.e )      emin = s.e;
      else if ( emax < s.e ) emax = s.e;
      if ( zmin > s.z )      zmin = s.z;
      else if ( zmax < s.z ) zmax = s.z;
    }
  }

}
