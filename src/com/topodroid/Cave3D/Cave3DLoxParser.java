/** @file Cave3DLoxParser.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D loch file parser 
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

public class Cave3DLoxParser extends Cave3DParser
{
  private static float RAD2DEG = (float)(180/Math.PI);

  public Cave3DLoxParser( Cave3D cave3d, String filename ) throws Cave3DParserException
  {
    super( cave3d );

    readfile( filename );

  }

  private void readfile( String filename ) throws Cave3DParserException
  {
    LoxFile lox = new LoxFile( filename );

    ArrayList< LoxSurvey > lox_surveys = lox.GetSurveys();
    for ( LoxSurvey survey : lox_surveys ) {
      surveys.add( new Cave3DSurvey( survey.name, survey.id, survey.pid ) );
    }

    ArrayList< LoxStation > lox_stations = lox.GetStations();
    ArrayList< LoxShot > lox_shots = lox.GetShots();
    // Log.v("Cave3D", "stations " + lox_stations.size() + " shots " + lox_shots.size() );

    for ( LoxStation st : lox_stations ) {
      Cave3DStation station = new Cave3DStation( st.name, (float)st.x, (float)st.y, (float)st.z,
                                                 st.id, st.sid, st.flag, st.comment );
      stations.add( station );
      // Cave3DSurvey survey = getSurvey( st.sid );
      // if ( survey != null ) survey.addStationInfo( station );
    }
    computeBoundingBox();
    // Log.v(TAG, "E " + emin + " " + emax + " N " + nmin + " " + nmax + " Z " + zmin + " " + zmax );

    for ( LoxShot sh : lox_shots ) {
      Cave3DStation f = getStation( sh.from );
      Cave3DStation t = getStation( sh.to );
      if ( f != null && t != null ) {
        float de = t.e - f.e;
        float dn = t.n - f.n;
        float dz = t.z - f.z;
        float len = (float)Math.sqrt( de*de + dn*dn + dz*dz );
        float ber = (float)Math.atan2( de, dn ) * RAD2DEG;
        if ( ber < 0 ) ber += 360;
        float dh = (float)Math.sqrt( de*de + dn*dn );
        float cln = (float)Math.atan2( dz, dh ) * RAD2DEG;
        Cave3DShot shot = new Cave3DShot( f.name, t.name, len, ber, cln );
        shot.from_station = f;
        shot.to_station   = t;
        shot.used = true;
        mCaveLength += len;

        Cave3DSurvey survey = getSurvey( sh.sid );
        if ( (sh.flag & LoxShot.FLAG_SPLAY) != 0 ) {
          splays.add( shot );
          if ( survey != null ) {
            // shot.survey = survey;
            // shot.surveyNr = survey.number;
            survey.addSplayInfo( shot );
          }
        } else {
          shots.add( shot );
          if ( survey != null ) {
            shot.survey = survey;
            shot.surveyNr = survey.number;
            survey.addShotInfo( shot );
          }
        }
      }
    }
    setStationDepths();

    LoxSurface surface = lox.GetSurface();
    if ( surface != null ) {
      double e1 = surface.East1();
      double n1 = surface.North1();
      int d1 = surface.Width();
      int d2 = surface.Height();
      double e2 = e1 + (d1-1) * surface.DimEast();
      double n2 = n1 + (d2-1) * surface.DimNorth();
      mSurface = new Cave3DSurface( e1, n1, e2, n2, d1, d2 );
      mSurface.setGridData( surface.Grid() );
    }
  }

}

