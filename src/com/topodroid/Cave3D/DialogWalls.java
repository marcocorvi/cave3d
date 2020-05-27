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
import android.widget.RadioButton;

class DialogWalls extends Dialog 
                  implements View.OnClickListener
{
  private Context mContext;
  private TopoGL  mApp;
  private TglParser mParser;

  private CheckBox mCBconvexhull;
  private CheckBox mCBpowercrust;
  private CheckBox mCBconvexhullNo;
  private CheckBox mCBpowercrustNo;

  private CheckBox mCBplanProj;
  private CheckBox mCBprofileProj;

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

    Button buttonOK     = (Button) findViewById( R.id.button_ok );
    Button buttonCancel = (Button) findViewById( R.id.button_cancel );
    // Button buttonSketch = (Button) findViewById( R.id.button_sketch );

    buttonOK.setOnClickListener( this );
    buttonCancel.setOnClickListener( this );
    // buttonSketch.setOnClickListener( this );

    mCBconvexhull   = (CheckBox) findViewById( R.id.convexhull );
    mCBpowercrust   = (CheckBox) findViewById( R.id.powercrust );
    mCBconvexhullNo = (CheckBox) findViewById( R.id.convexhull_no );
    mCBpowercrustNo = (CheckBox) findViewById( R.id.powercrust_no );

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
    } else if ( v.getId() == R.id.button_ok ) {
      if ( mCBconvexhull.isChecked() ) {
        mParser.makeConvexHull( mCBconvexhullNo.isChecked() );
      } 
      if ( mCBpowercrust.isChecked() ) {
        mParser.makePowercrust( mCBpowercrustNo.isChecked() );
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

