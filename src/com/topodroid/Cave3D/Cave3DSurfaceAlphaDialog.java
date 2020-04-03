/* @file Cave3DSurfaceAlphaDialog.java
 *
 * @author marco corvi
 * @date mar 2020
 *
 * @brief Cave3D DEM surface alpha dialog
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

// import android.util.Log;

import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.EditText;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.graphics.Paint;

public class Cave3DSurfaceAlphaDialog extends Dialog 
                              implements View.OnClickListener
{
  // private static final String TAG = "Cave3D INFO";
  private Context mContext;

  private SeekBar mETalpha;
  private Button mBtnOk;
  private Button mBtnLoad;
  private CheckBox mCBproj;
  private EditText mDemFile;

  private Cave3D mCave3D;
  // private Cave3DRenderer mRenderer;


  public Cave3DSurfaceAlphaDialog( Context ctx, Cave3D cave3D )
  {
    super( ctx );
    mContext = ctx;
    mCave3D  = cave3D;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cave3d_surface_alpha_dialog);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    mETalpha = ( SeekBar ) findViewById(R.id.alpha);
    // mETalpha.setText( Integer.toString( Cave3DRenderer.surfacePaint.getAlpha() ) );
    mETalpha.setProgress( Cave3DRenderer.surfacePaint.getAlpha() );

    mDemFile = (EditText) findViewById( R.id.dem_file );
    String name = (mCave3D.mDEMname != null )? mCave3D.mDEMname : "--";
    mDemFile.setText( String.format( mContext.getResources().getString( R.string.dem_file ), name ) );

    mCBproj = (CheckBox) findViewById( R.id.projection );
    mCBproj.setChecked( mCave3D.getSurfaceLegs() );

    RadioButton rb;
    Paint.Style style = Cave3DRenderer.surfacePaint.getStyle();
    if ( style == Paint.Style.STROKE ) {
      rb = (RadioButton) findViewById( R.id.stroke );
      rb.setChecked( true );
    } else if ( style == Paint.Style.FILL ) {
      rb = (RadioButton) findViewById( R.id.fill );
      rb.setChecked( true );
    } else if ( style == Paint.Style.FILL_AND_STROKE ) {
      rb = (RadioButton) findViewById( R.id.stroke_fill );
      rb.setChecked( true );
    }

    mBtnOk = (Button) findViewById( R.id.button_ok );
    mBtnOk.setOnClickListener( this );
    mBtnLoad = (Button) findViewById( R.id.dem_load );
    mBtnLoad.setOnClickListener( this );

    setTitle( R.string.title_surface_alpha );
  }

  public void onClick(View view)
  {
    if ( view.getId() == R.id.dem_load ) {
      (new Cave3DDEMDialog( mContext, mCave3D )).show();
    } else if ( view.getId() == R.id.button_ok ) {
      mCave3D.setSurfaceLegs( mCBproj.isChecked() );

      // Log.v( TAG, "onClick()" );
      int alpha = mETalpha.getProgress();
      if ( 0 < alpha && alpha < 256 ) Cave3DRenderer.surfacePaint.setAlpha( alpha );

      RadioButton rb = (RadioButton) findViewById( R.id.stroke );
      if ( rb.isChecked() ) {
        Cave3DRenderer.surfacePaint.setStyle( Paint.Style.STROKE );
      } else {
        rb = (RadioButton) findViewById( R.id.fill );
        if ( rb.isChecked() ) {
          Cave3DRenderer.surfacePaint.setStyle( Paint.Style.FILL );
        } else {
          Cave3DRenderer.surfacePaint.setStyle( Paint.Style.FILL_AND_STROKE );
        }
      }
    }
    dismiss();
  }  

}

