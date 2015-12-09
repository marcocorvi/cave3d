/* @file Cave3DStationDialog.java
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

import java.io.StringWriter;
import java.io.PrintWriter;

import android.os.Bundle;
import android.app.Dialog;
// import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.graphics.*;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

// import android.util.Log;

public class Cave3DStationDialog extends Dialog 
                                 implements View.OnClickListener
{
    private Button mBtDistance;
    private TextView mTvSurface;

    private Cave3DView mCave3Dview;
    private Cave3DRenderer mRenderer;
    private Cave3DStation  mStation;
    private Cave3DSurface  mSurface;

    public Cave3DStationDialog( Context context, Cave3DView cave3Dview, Cave3DStation st, Cave3DSurface surface )
    {
      super(context);
      mCave3Dview = cave3Dview;
      mStation  = st;
      mSurface  = surface;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cave3d_station_dialog);

        TextView tv = ( TextView ) findViewById(R.id.st_name);
        tv.setText( mStation.name );

        StringWriter sw1 = new StringWriter();
        PrintWriter  pw1 = new PrintWriter( sw1 );
        StringWriter sw2 = new StringWriter();
        PrintWriter  pw2 = new PrintWriter( sw2 );
        StringWriter sw3 = new StringWriter();
        PrintWriter  pw3 = new PrintWriter( sw3 );
        pw1.format( "E %.2f", mStation.e );
        pw2.format( "N %.2f", mStation.n );
        pw3.format( "Z %.2f", mStation.z );

        tv = ( TextView ) findViewById(R.id.st_east);
        tv.setText( sw1.getBuffer().toString() );
        tv = ( TextView ) findViewById(R.id.st_north);
        tv.setText( sw2.getBuffer().toString() );
        tv = ( TextView ) findViewById(R.id.st_vert);
        tv.setText( sw3.getBuffer().toString() );

        mBtDistance = (Button) findViewById( R.id.st_distance );
        mBtDistance.setOnClickListener( this );

        mTvSurface  = (TextView) findViewById( R.id.st_surface );
        if ( mSurface != null ) {
          double zs = mSurface.computeZ( mStation.e, mStation.n );
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter( sw );
          pw.format("Depth %.1f", zs - mStation.z );
          mTvSurface.setText( sw.getBuffer().toString() );
        }
        

        setTitle( R.string.STATION );
    }

    public void onClick(View v)
    {
      // Log.v( TAG, "onClick()" );
      Button b = (Button)v;
      if ( b == mBtDistance ) {
        mCave3Dview.startStationDistance( mStation );
      }  
      dismiss();
    }
}

