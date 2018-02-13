/* @file Cave3DWallsDialog.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D walls construction dialog
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

public class Cave3DWallsDialog extends Dialog 
                              implements View.OnClickListener
{
    private Button mBtnOk;

    private Context mContext;
    private Cave3D  mCave3D;
    private Cave3DRenderer mRenderer;

    private Button   mButtonOK;
    private CheckBox mCBconvexhull;
    private CheckBox mCBpowercrust;
    private CheckBox mCBconvexhullNo;
    private CheckBox mCBpowercrustNo;


    public Cave3DWallsDialog( Context context, Cave3D cave3D, Cave3DRenderer renderer )
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
      setContentView(R.layout.cave3d_walls_dialog);
      getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

      mButtonOK = (Button) findViewById( R.id.button_ok );
      mButtonOK.setOnClickListener( this );

      mCBconvexhull   = (CheckBox) findViewById( R.id.convexhull );
      mCBpowercrust   = (CheckBox) findViewById( R.id.powercrust );
      mCBconvexhullNo = (CheckBox) findViewById( R.id.convexhull_no );
      mCBpowercrustNo = (CheckBox) findViewById( R.id.powercrust_no );

      setTitle( R.string.walls_title );
    }

    @Override
    public void onClick(View v)
    {
      // Log.v( TAG, "onClick()" );
      if ( v.getId() == R.id.button_ok ) {
        if ( mCBconvexhull.isChecked() ) {
          mRenderer.makeConvexHull( mCBconvexhullNo.isChecked() );
        } 
        if ( mCBpowercrust.isChecked() ) {
          mRenderer.makePowercrust( mCBpowercrustNo.isChecked() );
        }
        // mCave3D.refresh();
      }
      dismiss();
    }
}

