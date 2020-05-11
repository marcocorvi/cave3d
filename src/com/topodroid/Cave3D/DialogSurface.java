/* @file DialogSurface.java
 *
 * @author marco corvi
 * @date mar 2020
 *
 * @brief DEM surface alpha dialog
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

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.EditText;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.graphics.Paint;

class DialogSurface extends Dialog 
                    implements View.OnClickListener
{
  private Context mContext;

  private SeekBar mETalpha;
  // private Button mBtnLoadDEM;
  // private Button mBtnLoadTexture;
  private CheckBox mCBproj;
  private CheckBox mCBtexture;
  // private EditText mDemFile;
  // private EditText mTextureFile;

  private TopoGL mApp;
  // private Cave3DRenderer mRenderer;


  public DialogSurface( Context ctx, TopoGL app )
  {
    super( ctx );
    mContext = ctx;
    mApp  = app;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cave3d_surface_alpha_dialog);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    mETalpha = ( SeekBar ) findViewById(R.id.alpha);
    mETalpha.setProgress( (int)(GlSurface.mAlpha * 255) );

    EditText mDemFile = (EditText) findViewById( R.id.dem_file );
    
    if ( mApp.mDEMname != null ) {
      mDemFile.setText( String.format( mContext.getResources().getString( R.string.dem_file ), mApp.mDEMname ) );
    } else {
      mDemFile.setVisibility( View.GONE );
    }

    EditText mTextureFile = (EditText) findViewById( R.id.texture_file );
    Button btn_texture = (Button)findViewById( R.id.texture_load );
    if ( mApp.hasSurface() ) {
      if ( mApp.mTextureName != null ) {
        mTextureFile.setText( String.format( mContext.getResources().getString( R.string.texture_file ), mApp.mTextureName ) );
      } else {
        mTextureFile.setVisibility( View.GONE );
      }
      btn_texture.setOnClickListener( this );
    } else {
      mTextureFile.setVisibility( View.GONE );
      btn_texture.setVisibility( View.GONE );
    }

    mCBproj = (CheckBox) findViewById( R.id.projection );
    mCBtexture = (CheckBox) findViewById( R.id.texture );
    mCBproj.setChecked( GlModel.surfaceLegsMode );
    mCBtexture.setChecked( GlModel.surfaceTexture );

    findViewById( R.id.button_ok ).setOnClickListener( this );
    findViewById( R.id.button_cancel ).setOnClickListener( this );
    findViewById( R.id.dem_load ).setOnClickListener( this );

    setTitle( R.string.title_surface_alpha );
  }

  public void onClick(View view)
  {
    if ( view.getId() == R.id.dem_load ) {
      (new DialogDEM( mContext, mApp )).show();
    } else if ( view.getId() == R.id.texture_load ) {
      (new DialogTexture( mContext, mApp )).show();
    } else if ( view.getId() == R.id.button_ok ) {
      GlModel.surfaceLegsMode = mCBproj.isChecked();
      GlModel.surfaceTexture  = mCBtexture.isChecked();

      // Log.v( "TopoGL-ALPHA, "onClick()" );
      int alpha = mETalpha.getProgress();
      if ( 0 < alpha && alpha < 256 ) GlSurface.setAlpha( alpha/255.0f );

    // } else if ( view.getId() == R.id.button_cancel ) {
    //   // nothing
    }
    dismiss();
  }  

}

