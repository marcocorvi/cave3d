/** @file MeasureComputer.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D station distances computer
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import android.os.AsyncTask;
// import android.content.Context;

import android.widget.Toast;

import java.util.ArrayList;

class MeasureComputer extends AsyncTask< Void, Void, Integer >
{
  float mX, mY;
  float[] mMVPMatrix;
  TglParser mParser;
  GlModel   mModel;
  TopoGL    mApp;
  ParserDEM mDEM;
  TglMeasure mMeasure;  // result of the measure
  String     mFullname; // station name

  MeasureComputer( TopoGL app, float x, float y, float[] MVPMatrix, TglParser parser, ParserDEM dem, GlModel model )
  {
    mX = x;
    mY = y;
    mMVPMatrix = MVPMatrix;
    mParser    = parser;
    mModel     = model;
    mApp       = app;
    mDEM       = dem;
  }

  @Override
  protected Integer doInBackground( Void ... v ) 
  {
    int ret = 0;
    mFullname = mModel.checkNames( mX, mY, mMVPMatrix, TopoGL.mSelectionRadius, (mParser.mStartStation == null) );

    if ( mFullname != null ) {
      // Log.v("TopoGL-STATION", mFullname );
      if ( mParser.mStartStation != null ) {
        if ( mApp.mMeasureStation.isChecked() ) {
          // Log.v("TopoGL-PATH", "distance/pathlength " + mParser.mStartStation.name + " " + mFullname );
          Cave3DStation station = mParser.getStation( mFullname );
          if ( station != null ) {
            if ( station != mParser.mStartStation ) {
              mMeasure = mParser.computeCavePathlength( station );
              if ( mMeasure.dcave > 0 && station.getPathPrevious() != null ) {
                ArrayList< Cave3DStation > path = new ArrayList< >();
                while ( station != null ) {
                  path.add( station );
                  station = station.getPathPrevious();
                }
                // Log.v("TopoGL-PATH", "path size " + path.size() );
                mModel.setPath( path );
              }
              ret = 1;
            } else {
              ret = 2;
            }
          } else { // null station
            ret = 3;
          }
        } else { // do not measure
          ret = 6;
        }
      } else { // parser start-station is null
        mModel.setPath( null );
        mParser.setStartStation( mFullname );
        ret = 4;
      }
    } else { // mFullname is null
      ret = 5;
    }
    return new Integer( ret );
  }

  @Override
  protected void onPostExecute( Integer result )
  {
    int val = result.intValue();
    if ( val == 1 ) {
      if ( TopoGL.mMeasureToast ) {
        Toast.makeText( mApp, mMeasure.getString(), Toast.LENGTH_LONG ).show();
      } else {
        (new DialogMeasure( mApp, mMeasure )).show();
      }
      mApp.refresh();
    } else if ( val == 2 ) {
      mApp.closeCurrentStation();
    // } else if ( val == 3 ) {
    //   Log.w("TopoGL-PATH", "null station" );
    //   // mModel.setPath( null );
    } else if ( val == 4 ) {
      if ( TopoGL.mStationDialog ) {
        (new DialogStation( mApp, mParser, mFullname, mDEM )).show();
      } else {
        Cave3DStation st = mParser.getStation( mFullname );
        if ( st != null ) {
          DEMsurface surface = (mDEM != null)? mDEM : mParser.getSurface();
          String msg = String.format("%s: E %.1f N %.1f H %.1f", st.short_name, st.x, st.y, st.z );
          if (surface != null) {
            double zs = surface.computeZ( st.x, st.y );
            if ( zs > -1000 ) {
              zs -= st.z;
              msg = msg + String.format("\nDepth %.1f", zs );
            }
          }
          mApp.showCurrentStation( msg );
          // Toast.makeText( mApp, msg, Toast.LENGTH_SHORT ).show();
        }
      }
    // } else if ( val == 5 ) {
    //   // mModel.setPath( null );
    //   // mParser.mStartStation = null;
    // } else if ( val == 6 ) {
    }
    GlRenderer.mMeasureCompute = false;
  }

}
