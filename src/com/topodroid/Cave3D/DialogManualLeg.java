/* @file DialogManualLeg.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D legs visibility dialog
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;

import android.graphics.*;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

class DialogManualLeg extends Dialog 
                  implements View.OnClickListener
{
  private Context mContext;
  private TopoGL  mParent;

  private CheckBox mCBsurface;
  private CheckBox mCBduplicate;
  private CheckBox mCBcommented;

  private EditText mETlength;
  private EditText mETbearing;
  private EditText mETclino;

  public DialogManualLeg( Context context, TopoGL parent )
  {
    super( context );
    mContext  = context;
    mParent   = parent;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_manual_leg);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    Button buttonOK     = (Button) findViewById( R.id.button_ok );
    Button buttonCancel = (Button) findViewById( R.id.button_cancel );
    buttonOK.setOnClickListener( this );
    buttonCancel.setOnClickListener( this );

    mCBsurface   = (CheckBox) findViewById( R.id.surface );
    mCBduplicate = (CheckBox) findViewById( R.id.duplicate );
    mCBcommented = (CheckBox) findViewById( R.id.commented );

    mCBsurface.setChecked(   false );
    mCBduplicate.setChecked( false );
    mCBcommented.setChecked( false );

    mETlength   = (EditText) findViewById( R.id.length );
    mETbearing  = (EditText) findViewById( R.id.bearing );
    mETclino    = (EditText) findViewById( R.id.clino );

    setTitle( R.string.title_manual_leg );
  }

  @Override
  public void onClick(View v)
  {
    // Log.v( TAG, "onClick()" );
    if ( v.getId() == R.id.button_ok ) {
      boolean surface   = mCBsurface.isChecked();
      boolean duplicate = mCBduplicate.isChecked();
      boolean commented = mCBcommented.isChecked();

      try {
        double length  = (mETlength.getText()  == null)? 0 : Double.parseDouble( mETlength.getText().toString() );
        double bearing = (mETbearing.getText() == null)? 0 : Double.parseDouble( mETbearing.getText().toString() );
        double clino   = (mETclino.getText()   == null)? 0 : Double.parseDouble( mETclino.getText().toString() );
        if ( length <= 0  ) {
          Log.v("Cave3D", "Length out of range: " + length );
        } else if ( bearing >= 0 && bearing < 360 ) {
          Log.v("Cave3D", "Azimuth out of range: " + bearing );
        } else if ( clino >= -90 && clino <= 90 ) {
          Log.v("Cave3D", "Clino out of range: " + clino );
        } else {
          mParent.handleManualLeg( length, bearing, clino, surface, duplicate, commented );
        }
      } catch ( NumberFormatException e ) {
        // TODO
        Log.v("Cave3D", "Error " + e.getMessage() );
      }
    }
    dismiss();
  }
}

