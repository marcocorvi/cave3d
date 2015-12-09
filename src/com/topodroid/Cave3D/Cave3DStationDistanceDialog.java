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

public class Cave3DStationDistanceDialog extends Dialog 
                                 implements View.OnClickListener
{
    // private Cave3DView mCave3Dview;
    private Cave3DStation  mStation1;
    private Cave3DStation  mStation2;

    public Cave3DStationDistanceDialog( Context context, Cave3DStation st1, Cave3DStation st2 )
    {
      super(context);
      mStation1  = st1;
      mStation2  = st2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cave3d_station_distance_dialog);

        TextView tv = ( TextView ) findViewById(R.id.st_name);
        tv.setText( mStation1.name + " - " + mStation2.name );

        double e = mStation1.e - mStation2.e;
        double n = mStation1.n - mStation2.n;
        double z = mStation1.z - mStation2.z;
        double d = Math.sqrt( e*e + n*n + z*z );

        StringWriter sw1 = new StringWriter();
        PrintWriter  pw1 = new PrintWriter( sw1 );
        StringWriter sw2 = new StringWriter();
        PrintWriter  pw2 = new PrintWriter( sw2 );
        StringWriter sw3 = new StringWriter();
        PrintWriter  pw3 = new PrintWriter( sw3 );
        StringWriter sw4 = new StringWriter();
        PrintWriter  pw4 = new PrintWriter( sw4 );
        pw1.format( "E %.2f", e );
        pw2.format( "N %.2f", n );
        pw3.format( "Z %.2f", z );
        pw4.format( "D %.2f", d );

        tv = ( TextView ) findViewById(R.id.st_east);
        tv.setText( sw1.getBuffer().toString() );
        tv = ( TextView ) findViewById(R.id.st_north);
        tv.setText( sw2.getBuffer().toString() );
        tv = ( TextView ) findViewById(R.id.st_vert);
        tv.setText( sw3.getBuffer().toString() );
        tv = ( TextView ) findViewById(R.id.st_dist);
        tv.setText( sw4.getBuffer().toString() );

        setTitle( R.string.STATIONS_DISTANCE );
    }

    public void onClick(View v)
    {
      // Log.v( TAG, "onClick()" );
      dismiss();
    }
}

