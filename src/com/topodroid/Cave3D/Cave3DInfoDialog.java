/* @file Cave3DInfoDialog.java
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
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

// import android.util.Log;

public class Cave3DInfoDialog extends Dialog 
                              implements View.OnClickListener
                              , OnItemClickListener
{
  private Button mBtnOk;

  private Cave3D mCave3D;
  private Cave3DRenderer mRenderer;

  private ArrayAdapter<String> mArrayAdapter;
  private ListView mList;


  public Cave3DInfoDialog( Cave3D cave3D, Cave3DRenderer renderer )
  {
    super(cave3D);
    mCave3D   = cave3D;
    mRenderer = renderer;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cave3d_info_dialog);

    TextView tv = ( TextView ) findViewById(R.id.info_grid);
    tv.setText( Integer.toString( mRenderer.getGrid() ) );

    tv = ( TextView ) findViewById(R.id.info_azimuth);
    tv.setText( Integer.toString( (int)(mRenderer.phi * 180.0 / Math.PI ) ) + "N  " +
                Integer.toString( (int)(mRenderer.clino * 180.0 /Math.PI) ) );

    tv = ( TextView ) findViewById(R.id.info_shots);
    tv.setText( Integer.toString( mRenderer.getNrShots() ) + " / " +
                Integer.toString( mRenderer.getNrSplays() ) );

    tv = ( TextView ) findViewById(R.id.info_stations);
    tv.setText( Integer.toString( mRenderer.getNrStations() ) );

    tv = ( TextView ) findViewById(R.id.info_surveys);
    tv.setText( Integer.toString( mRenderer.getNrSurveys() ) );

    tv = ( TextView ) findViewById(R.id.info_length);
    tv.setText( Integer.toString( (int)(mRenderer.getCaveLength()) ) );

    tv = ( TextView ) findViewById(R.id.info_depth);
    tv.setText( Integer.toString( (int)(mRenderer.getCaveDepth()) ) );

    tv = ( TextView ) findViewById(R.id.info_volume);
    tv.setText( Integer.toString( (int)(mRenderer.getCaveVolume()) ) );


    int nr = mRenderer.getNrSurveys();
    ListView mList = ( ListView ) findViewById(R.id.surveys_list );
    mArrayAdapter = new ArrayAdapter<String>( mCave3D, R.layout.message );
    ArrayList< Cave3DSurvey > surveys = mRenderer.getSurveys();
    if ( surveys != null ) {
      for ( Cave3DSurvey s : surveys ) {
        mArrayAdapter.add( s.name );
      }
    }
    mList.setAdapter( mArrayAdapter );
    mList.setOnItemClickListener( this );
    mList.setDividerHeight( 2 );

    setTitle( R.string.INFO );
  }

  public void onClick(View view)
  {
    // Log.v( TAG, "onClick()" );
    dismiss();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id)
  {
    CharSequence item = ((TextView) view).getText();
    String name = item.toString();
    Cave3DSurvey survey = mRenderer.getSurvey( name );
    if ( survey != null ) {
      ( new Cave3DSurveyDialog( mCave3D, survey ) ).show();
    } else {
      // TODO Toast.makeText( );
    }
  }

}

