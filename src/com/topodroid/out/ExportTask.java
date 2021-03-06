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

import android.util.Log;

import android.os.AsyncTask;
import android.content.Context;

import android.net.Uri;

public class ExportTask extends AsyncTask< Void, Void, Boolean >
{
  private TopoGL mApp;
  private TglParser mParser;
  private int mType;
  private Uri mUri;
  private String mPathname;
  private boolean mSplays;
  private boolean mStation;
  private boolean mSurface;
  private boolean mWalls;
  // private boolean mOverwrite;


  // public ExportTask( TopoGL app, TglParser parser, int type, String pathname, boolean splays, boolean station, boolean surface, boolean walls, boolean overwrite )
  // {
  //   mApp    = app;
  //   mParser = parser;
  //   mType = type;
  //   mPathname = pathname;
  //   mSplays   = splays;
  //   mStation  = station;
  //   mSurface  = surface;
  //   mWalls    = walls;
  //   mOverwrite = overwrite;
  // }

  public ExportTask( TopoGL app, TglParser parser, Uri uri, ExportData export )
  {
    mApp    = app;
    mParser = parser;
    mPathname = uri.getPath();
    mUri      = uri;
    mType     = export.mType;
    mSplays   = export.mSplays;
    mStation  = export.mStation;
    mSurface  = export.mSurface;
    mWalls    = export.mWalls;
    // mOverwrite = export.mOverwrite;
  }

  @Override
  public Boolean doInBackground( Void ... args )
  {
    try {
      switch ( mType ) {
        case ModelType.GLTF:
          return mApp.exportModel( mType, mUri, mSplays, mWalls, mStation );

        case ModelType.STL_BINARY:
          return mParser.exportModel( mType, mUri, mSplays, mWalls, mSurface );
        case ModelType.STL_ASCII:
          return mParser.exportModel( mType, mUri, mSplays, mWalls, mSurface );
        case ModelType.KML_ASCII:
          return mParser.exportModel( mType, mUri, mSplays, mWalls, mStation );
        case ModelType.CGAL_ASCII:
          return mParser.exportModel( mType, mUri, mSplays, mWalls, mStation );
        case ModelType.LAS_BINARY:
          return mParser.exportModel( mType, mUri, mSplays, mWalls, mStation );
        case ModelType.DXF_ASCII:
          return mParser.exportModel( mType, mUri, mSplays, mWalls, mStation );
        case ModelType.SHP_ASCII:
          return mParser.exportModel( mType, mUri, mSplays, mWalls, mStation );
        case ModelType.SERIAL:
          return mParser.exportModel( mType, mUri, mSplays, mWalls, mSurface );
        default:
          break;
      }
    } catch ( OutOfMemoryError e ) {
      Log.v("Cave3D", "Export task: Out of memory error" );
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
