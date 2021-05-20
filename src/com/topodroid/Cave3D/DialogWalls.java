/* @file DialogWalls.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D walls construction dialog
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

// import android.util.Log;

import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;

import android.graphics.*;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.RadioButton;

class DialogWalls extends Dialog 
                  implements View.OnClickListener
{
  private Context mContext;
  private TopoGL  mApp;
  private TglParser mParser;

  private CheckBox mCBbubble;
  // private CheckBox mCBbubbleNo;

  private CheckBox mCBtube;
  // private CheckBox mCBtubeNo;

  private CheckBox mCBhull;
  // private CheckBox mCBhullNo;

  private CheckBox mCBconvexhull;
  // private CheckBox mCBconvexhullNo;

  private CheckBox mCBpowercrust;
  // private CheckBox mCBpowercrustNo;

  private CheckBox mCBplanProj;
  private CheckBox mCBprofileProj;
  private SeekBar mETalpha;

  public DialogWalls( Context context, TopoGL app, TglParser parser )
  {
    super( context );
    mContext = context;
    mApp     = app;
    mParser  = parser;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cave3d_walls_dialog);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    mETalpha = ( SeekBar ) findViewById(R.id.alpha);
    mETalpha.setProgress( (int)(GlWalls.getAlpha() * 255) );

    Button buttonOK     = (Button) findViewById( R.id.button_ok );
    Button buttonCancel = (Button) findViewById( R.id.button_cancel );
    // Button buttonSketch = (Button) findViewById( R.id.button_sketch );

    buttonOK.setOnClickListener( this );
    buttonCancel.setOnClickListener( this );
    // buttonSketch.setOnClickListener( this );

    mCBbubble   = (CheckBox) findViewById( R.id.bubble );
    // mCBbubbleNo = (CheckBox) findViewById( R.id.bubble_no );

    mCBtube   = (CheckBox) findViewById( R.id.tube );
    // mCBtubeNo = (CheckBox) findViewById( R.id.tube_no );

    mCBhull   = (CheckBox) findViewById( R.id.hull );
    // mCBhullNo = (CheckBox) findViewById( R.id.hull_no );

    mCBconvexhull   = (CheckBox) findViewById( R.id.convexhull );
    // mCBconvexhullNo = (CheckBox) findViewById( R.id.convexhull_no );

    mCBpowercrust   = (CheckBox) findViewById( R.id.powercrust );
    // mCBpowercrustNo = (CheckBox) findViewById( R.id.powercrust_no );

    if ( TglParser.WALL_BUBBLE < TglParser.WALL_MAX ) {
      mCBbubble.setOnClickListener( this );
    } else {
      mCBbubble.setVisibility( View.GONE );
      // mCBbubbleNo.setVisibility( View.GONE );
    }

    if ( TglParser.WALL_TUBE < TglParser.WALL_MAX && TglParser.mSplayUse == TglParser.SPLAY_USE_XSECTION ) {
      mCBtube.setOnClickListener( this );
    } else {
      mCBtube.setVisibility( View.GONE );
      // mCBtubeNo.setVisibility( View.GONE );
    }

    if ( TglParser.WALL_HULL < TglParser.WALL_MAX ) {
      mCBhull.setOnClickListener( this );
    } else {
      mCBhull.setVisibility( View.GONE );
      // mCBhullNo.setVisibility( View.GONE );
    }

    if ( TglParser.WALL_POWERCRUST < TglParser.WALL_MAX ) {
      mCBpowercrust.setOnClickListener( this );
    } else {
      mCBpowercrust.setVisibility( View.GONE );
      // mCBpowercrustNo.setVisibility( View.GONE );
    }

    mCBconvexhull.setOnClickListener( this );

    mCBplanProj     = (CheckBox) findViewById( R.id.cb_plan_proj );
    mCBprofileProj  = (CheckBox) findViewById( R.id.cb_profile_proj );
    mCBplanProj.setOnClickListener( this );
    mCBprofileProj.setOnClickListener( this );
    mCBplanProj.setChecked( GlModel.projMode == GlModel.PROJ_PLAN );
    mCBprofileProj.setChecked( GlModel.projMode == GlModel.PROJ_PROFILE );

    setTitle( R.string.walls_title );
  }

  @Override
  public void onClick(View v)
  {
    // Log.v( TAG, "onClick()" );
    if ( v.getId() == R.id.cb_plan_proj ) {
      mCBprofileProj.setChecked( false );
      return;
    } else if ( v.getId() == R.id.cb_profile_proj ) {
      mCBplanProj.setChecked( false );
      return;
    } else if ( v.getId() == R.id.bubble ) {
      // mCBbubble.setChecked( false );
      mCBtube.setChecked( false );
      mCBhull.setChecked( false );
      mCBconvexhull.setChecked( false );
      mCBpowercrust.setChecked( false );
      return;
    } else if ( v.getId() == R.id.tube ) {
      mCBbubble.setChecked( false );
      // mCBtube.setChecked( false );
      mCBhull.setChecked( false );
      mCBconvexhull.setChecked( false );
      mCBpowercrust.setChecked( false );
      return;
    } else if ( v.getId() == R.id.hull ) {
      mCBbubble.setChecked( false );
      mCBtube.setChecked( false );
      // mCBhull.setChecked( false );
      mCBconvexhull.setChecked( false );
      mCBpowercrust.setChecked( false );
      return;
    } else if ( v.getId() == R.id.convexhull ) {
      mCBbubble.setChecked( false );
      mCBtube.setChecked( false );
      mCBhull.setChecked( false );
      // mCBconvexhull.setChecked( false );
      mCBpowercrust.setChecked( false );
      return;
    } else if ( v.getId() == R.id.powercrust ) {
      mCBbubble.setChecked( false );
      mCBtube.setChecked( false );
      mCBhull.setChecked( false );
      mCBconvexhull.setChecked( false );
      // mCBpowercrust.setChecked( false );
      return;
    } else if ( v.getId() == R.id.button_ok ) {
      int alpha = mETalpha.getProgress();
      if ( 0 < alpha && alpha < 256 ) GlWalls.setAlpha( alpha/255.0f );

      if ( mParser != null ) {
        if ( mCBbubble.isChecked() ) {
          mParser.makeBubble( );
        } else if ( mCBtube.isChecked() ) {
          mParser.makeTube( );
        } else if ( mCBhull.isChecked() ) {
          mParser.makeHull( );
        } else if ( mCBconvexhull.isChecked() ) {
          mParser.makeConvexHull( );
        } else if ( mCBpowercrust.isChecked() ) {
          mParser.makePowercrust( );
        }
      }

      if ( mCBplanProj.isChecked() ) {
        GlModel.projMode = GlModel.PROJ_PLAN;
      } else if ( mCBprofileProj.isChecked() ) {
        GlModel.projMode = GlModel.PROJ_PROFILE;
      } else {
        GlModel.projMode = GlModel.PROJ_NONE;
      }
    // } else if ( v.getId() == R.id.button_sketch ) {
    //   (new DialogSketch( mApp, mApp )).show();
    // } else if ( v.getId() == R.id.button_cancel ) {
    }
    dismiss();
  }
}

