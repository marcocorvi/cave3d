/* @file FractalDialog.java
 *
 * @author marco corvi
 * @date mar 2018
 *
 * @brief Cave3D fractal counts dialog
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.CheckBox;
import android.widget.RadioButton;

// import android.util.Log;

public class FractalDialog extends Dialog 
                           implements View.OnClickListener
{
  // private static final String TAG = "Cave3D FRACTAL";

  private Cave3D   mCave3d;
  private Context  mContext;
  private CheckBox mCBsplays;
  private Button mBtnOk;
  private ImageView mImage;
  // private Button mBtnClose;
  private EditText mCell;
  private static int mCellSide = 2;

  private Cave3DRenderer mRenderer;
  private RadioButton mRBtotal;
  private RadioButton mRBnghb;

  public FractalDialog( Context context, Cave3D cave3d, Cave3DRenderer renderer )
  {
    super( context );
    mContext  = context;
    mCave3d   = cave3d;
    mRenderer = renderer;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.fractal_dialog);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    TextView tv = ( TextView ) findViewById(R.id.fractal_count);
    tv.setText( FractalResult.countsString() );

    tv = (TextView) findViewById( R.id.fractal_computer );
    tv.setText( (FractalResult.computer != null)? "fractal computer is running" : "fractal computer is idle" );

    mBtnOk = (Button) findViewById( R.id.fractal_ok );
    mBtnOk.setOnClickListener( this );

    mCell = (EditText) findViewById( R.id.fractal_cell );
    mCell.setText( Integer.toString( mCellSide ) );

    mCBsplays = (CheckBox) findViewById( R.id.fractal_splays );
    mImage    = (ImageView) findViewById( R.id.fractal_dims );

    mImage.setImageBitmap( FractalResult.makeImage() );

    mRBtotal = (RadioButton) findViewById( R.id.fractal_count_total );
    mRBnghb  = (RadioButton) findViewById( R.id.fractal_count_nghb  );

    setTitle( R.string.fractal_title );
  }

  @Override
  public void onClick(View view)
  {
    // Log.v( TAG, "onClick()" );
    mCellSide = Integer.parseInt( mCell.getText().toString() );
    int mode = FractalComputer.COUNT_TOTAL;
    if ( mRBnghb.isChecked() ) mode = FractalComputer.COUNT_NGHB;
    int ret = FractalResult.compute( mContext, mCave3d, mRenderer, mCBsplays.isChecked(), mCellSide, mode );
    dismiss();
  }

}

