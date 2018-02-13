/* @file Cave3DViewDialog.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D drawing infos dialog
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.os.Bundle;
import android.app.Dialog;
// import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.graphics.*;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnClickListener;
// import android.widget.EditText;
import android.widget.Button;
// import android.widget.TextView;
import android.widget.CheckBox;
// import android.widget.ToggleButton;
import android.widget.RadioButton;
import android.widget.Toast;

import android.util.Log;

public class Cave3DViewDialog extends Dialog 
                              implements View.OnClickListener
{
    private Button mBtnOk;

    private Context mContext;
    private Cave3D  mCave3D;
    private Cave3DRenderer mRenderer;

    private Button   mButtonOK;
    private RadioButton mRBtop;
    private RadioButton mRBeast;
    private RadioButton mRBnorth;
    private RadioButton mRBwest;
    private RadioButton mRBsouth;
    // private CheckBox mCBpoint;
    private CheckBox mCBzoom;


    public Cave3DViewDialog( Context context, Cave3D cave3D, Cave3DRenderer renderer )
    {
      super( context );
      mContext  = context;
      mCave3D   = cave3D;
      mRenderer = renderer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.cave3d_view_dialog);
      getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

      mButtonOK = (Button) findViewById( R.id.button_ok );
      mButtonOK.setOnClickListener( this );

      mRBtop   = (RadioButton) findViewById( R.id.view_top );
      mRBeast  = (RadioButton) findViewById( R.id.view_east );
      mRBnorth = (RadioButton) findViewById( R.id.view_north );
      mRBwest  = (RadioButton) findViewById( R.id.view_west );
      mRBsouth = (RadioButton) findViewById( R.id.view_south );

      // mCBpoint = (CheckBox) findViewById( R.id.view_point );
      mCBzoom  = (CheckBox) findViewById( R.id.view_zoom  );

      setTitle( R.string.view_title );
    }

    @Override
    public void onClick(View v)
    {
      // Log.v( TAG, "onClick()" );
      if ( v.getId() == R.id.button_ok ) {
        // if ( mCBpoint.isChecked() ) {
          if ( mRBtop.isChecked() ) {
            mRenderer.setAngles( 0, Cave3DRenderer.PIOVERTWO );
          } else if ( mRBeast.isChecked() ) {
            mRenderer.setAngles( Cave3DRenderer.PIOVERTWO, 0 );
          } else if ( mRBnorth.isChecked() ) {
            mRenderer.setAngles( Cave3DRenderer.PI, 0 );
          } else if ( mRBwest.isChecked() ) {
            mRenderer.setAngles( Cave3DRenderer.THREEPIOVERTWO, 0 );
          } else if ( mRBsouth.isChecked() ) {
            mRenderer.setAngles( Cave3DRenderer.TWOPI, 0 );
          }
        // }
        if ( mCBzoom.isChecked() ) {
          mRenderer.zoomOne();
        }
        mCave3D.refresh();
      }
      dismiss();
    }
}

