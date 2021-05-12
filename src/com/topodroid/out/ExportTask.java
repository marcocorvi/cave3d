/** @file ExportTask.java
 *
 * @author marco corvi
 * @date may 2021
 *
 * @brief export model task
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.out;

import com.topodroid.Cave3D.ModelType;
import com.topodroid.Cave3D.TglParser;
import com.topodroid.Cave3D.TopoGL;
import com.topodroid.Cave3D.R;

import android.os.AsyncTask;
import android.content.Context;

public class ExportTask extends AsyncTask< Void, Void, Boolean >
{
  private TopoGL mApp;
  private TglParser mParser;
  private int mType;
  private String mPathname;
  private boolean mSplays;
  private boolean mStation;
  private boolean mSurface;
  private boolean mWalls;
  private boolean mOverwrite;


  public ExportTask( TopoGL app, TglParser parser, int type, String pathname, boolean splays, boolean station, boolean surface, boolean walls, boolean overwrite )
  {
    mApp    = app;
    mParser = parser;
    mType = type;
    mPathname = pathname;
    mSplays   = splays;
    mStation  = station;
    mSurface  = surface;
    mWalls    = walls;
    mOverwrite = overwrite;
  }

  @Override
  public Boolean doInBackground( Void ... args )
  {
    switch ( mType ) {
      case ModelType.STL_BINARY:
        return mParser.exportModel( mType, mPathname, mSplays, mWalls, mSurface, mOverwrite );
      case ModelType.STL_ASCII:
        return mParser.exportModel( mType, mPathname, mSplays, mWalls, mSurface, mOverwrite );
      case ModelType.KML_ASCII:
        return mParser.exportModel( mType, mPathname, mSplays, mWalls, mStation, mOverwrite );
      case ModelType.CGAL_ASCII:
        return mParser.exportModel( mType, mPathname, mSplays, mWalls, mStation, mOverwrite );
      case ModelType.LAS_BINARY:
        return mParser.exportModel( mType, mPathname, mSplays, mWalls, mStation, mOverwrite );
      case ModelType.DXF_ASCII:
        return mParser.exportModel( mType, mPathname, mSplays, mWalls, mStation, mOverwrite );
      case ModelType.SHP_ASCII:
        return mParser.exportModel( mType, mPathname, mSplays, mWalls, mStation, mOverwrite );
      case ModelType.GLTF:
        return mApp.exportModel( mType, mPathname, mSplays, mWalls, mStation, mOverwrite );
      case ModelType.SERIAL:
        return mParser.exportModel( mType, mPathname, mSplays, mWalls, mSurface, mOverwrite );
      default:
        break;
    }
    return false;
  }

   @Override
  protected void onPostExecute( Boolean res )
  {
    if ( mApp != null ) { // CRASH here - this should not be necessary
      if ( res ) {
        mApp.toast(R.string.ok_export, mPathname, false);
      } else {
        mApp.toast(R.string.error_export_failed, mPathname, true );
      }
    }
  }

}
