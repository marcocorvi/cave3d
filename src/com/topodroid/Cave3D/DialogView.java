/* @file DialogView.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D drawing infos dialog
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

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;

class DialogView extends Dialog 
                 implements View.OnClickListener
{
  private Button mBtnOk;

  // private Context mContext;
  private TopoGL  mApp;
  private GlRenderer mRenderer;

  private Button   mButtonOK;
  private Button   mButtonCancel;
  private Button mRBtop;
  private Button mRBeast;
  private Button mRBnorth;
  private Button mRBwest;
  private Button mRBsouth;
  private CheckBox mCBzoom;

  public DialogView( Context context, TopoGL app, GlRenderer renderer )
  {
    super( context );
    // mContext  = context;
    mApp      = app;
    mRenderer = renderer;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cave3d_view_dialog);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    mButtonOK = (Button) findViewById( R.id.button_ok );
    mButtonCancel = (Button) findViewById( R.id.button_cancel );
    mButtonOK.setOnClickListener( this );
    mButtonCancel.setOnClickListener( this );

    mRBtop   = (Button) findViewById( R.id.view_top );
    mRBeast  = (Button) findViewById( R.id.view_east );
    mRBnorth = (Button) findViewById( R.id.view_north );
    mRBwest  = (Button) findViewById( R.id.view_west );
    mRBsouth = (Button) findViewById( R.id.view_south );
    mRBtop.setOnClickListener( this );
    mRBeast.setOnClickListener( this );
    mRBnorth.setOnClickListener( this );
    mRBwest.setOnClickListener( this );
    mRBsouth.setOnClickListener( this );

    mCBzoom  = (CheckBox) findViewById( R.id.view_zoom  );
    mCBzoom.setChecked( true );

    setTitle( R.string.view_title );
  }

  @Override
  public void onClick(View v)
  {
    // Log.v( TAG, "onClick()" );
    int id = v.getId();
    if ( id == R.id.button_cancel ) {
      /* nothing */
    } else {
      if ( id == R.id.view_top ) {
        mRenderer.setAngles( 180, -90 );
      } else if ( id == R.id.view_north ) {
        mRenderer.setAngles(   0, 0 );
      } else if ( id == R.id.view_west ) {
        mRenderer.setAngles( 270, 0 );
      } else if ( id == R.id.view_south ) {
        mRenderer.setAngles( 180, 0 );
      } else if ( id == R.id.view_east ) {
        mRenderer.setAngles(  90, 0 );
      }
      if ( mCBzoom.isChecked() ) mRenderer.zoomOne();
      mApp.refresh();
    }
    dismiss();
  }
}

