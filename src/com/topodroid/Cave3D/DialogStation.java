/* @file DialogStation.java
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

import java.io.StringWriter;
import java.io.PrintWriter;

import java.util.Locale;

import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import android.util.Log;

class DialogStation extends Dialog 
                    implements View.OnClickListener
{
  private Button mBtDistance;
  private Button mBtCenter;
  private TextView mTvSurface;

  private TglParser  mParser;
  // private GlRenderer mRenderer;
  private Cave3DStation  mStation;
  private DEMsurface  mSurface;

  public DialogStation( Context context, TglParser parser, String fullname, DEMsurface surface )
  {
    super(context);
    // mRenderer = renderer;
    mParser  = parser;
    mStation = mParser.getStation( fullname );
    mSurface = ( surface != null )? surface : parser.getSurface();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.cave3d_station_dialog);
      getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

      TextView tv = ( TextView ) findViewById(R.id.st_name);
      tv.setText( mStation.name );

      StringWriter sw1 = new StringWriter();
      PrintWriter  pw1 = new PrintWriter( sw1 );
      StringWriter sw2 = new StringWriter();
      PrintWriter  pw2 = new PrintWriter( sw2 );
      StringWriter sw3 = new StringWriter();
      PrintWriter  pw3 = new PrintWriter( sw3 );
      pw1.format(Locale.US, "E %.2f", mStation.x );
      pw2.format(Locale.US, "N %.2f", mStation.y );
      pw3.format(Locale.US, "Z %.2f", mStation.z );

      tv = ( TextView ) findViewById(R.id.st_east);
      tv.setText( sw1.getBuffer().toString() );
      tv = ( TextView ) findViewById(R.id.st_north);
      tv.setText( sw2.getBuffer().toString() );
      tv = ( TextView ) findViewById(R.id.st_vert);
      tv.setText( sw3.getBuffer().toString() );

      // Button btCenter = (Button) findViewById( R.id.st_center );
      // btCenter.setOnClickListener( this );
      // Button btDistance = (Button) findViewById( R.id.st_distance );
      // btDistance.setOnClickListener( this );

      Button btn_close = (Button) findViewById( R.id.btn_close );
      btn_close.setOnClickListener( this );

      mTvSurface  = (TextView) findViewById( R.id.st_surface );
      if ( mSurface != null ) {
        double zs = mSurface.computeZ( mStation.x, mStation.y );
        // Log.v("TopoGL-SURFACE", "Station " + mStation.z + " surface " + zs );
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter( sw );
        pw.format(Locale.US, "Depth %.1f", zs - mStation.z );
        mTvSurface.setText( sw.getBuffer().toString() );
      } else {
        mTvSurface.setVisibility( View.GONE );
      }

      setTitle( R.string.STATION );
  }

  @Override
  public void onClick(View v)
  {
  //   if ( v.getId() == R.id.st_distance ) {
  //     // mRenderer.toggleStationDistance( true );
  //     mParser.startStationDistance( mStation );
  //   // } else if ( v.getId() == R.id.st_center ) {
  //   //   mRenderer.centerStation( mStation );
  //   }  
    dismiss();
  }
}

