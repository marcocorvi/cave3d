/* @file Cave3DStationDistanceDialog.java
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

import java.util.Locale;

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

// import android.util.Log;

public class Cave3DStationDistanceDialog extends Dialog 
                                 implements View.OnClickListener
{
    // private Cave3DView mCave3Dview;
    private Context mContext;
    private Cave3DStation  mStation1;
    private Cave3DStation  mStation2;
    private float mCavePathlength;

    public Cave3DStationDistanceDialog( Context context, Cave3DStation st1, Cave3DStation st2, float len )
    {
      super(context);
      mContext   = context;
      mStation1  = st1;
      mStation2  = st2;
      mCavePathlength = len;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cave3d_station_distance_dialog);
        getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

        TextView tv = ( TextView ) findViewById(R.id.st_name);
        tv.setText( mStation1.name + " - " + mStation2.name );

        double e = mStation1.e - mStation2.e;
        double n = mStation1.n - mStation2.n;
        double z = mStation1.z - mStation2.z;
        double d = Math.sqrt( e*e + n*n + z*z );

        tv = ( TextView ) findViewById(R.id.st_east);
        tv.setText( String.format(Locale.US, "E %1$.2f", e ) );
        tv = ( TextView ) findViewById(R.id.st_north);
        tv.setText( String.format(Locale.US, "N %1$.2f", n ) );
        tv = ( TextView ) findViewById(R.id.st_vert);
        tv.setText( String.format(Locale.US, "Z %1$.2f", z ) );
        tv = ( TextView ) findViewById(R.id.st_dist);
        tv.setText( String.format(Locale.US, mContext.getResources().getString(R.string.cave_distance), d ));

        tv = ( TextView ) findViewById(R.id.st_cave_pathlength);
        if ( mCavePathlength > 0 ) {
          tv.setText( String.format(Locale.US, mContext.getResources().getString(R.string.cave_length), mCavePathlength ));
        } else {
          tv.setVisibility( View.GONE );
        }

        setTitle( R.string.STATIONS_DISTANCE );
    }

    public void onClick(View v)
    {
      // Log.v( TAG, "onClick()" );
      dismiss();
    }
}

