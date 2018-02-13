/* @file Cave3DSurveysDialog.java
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

import java.util.ArrayList;

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

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

// import android.util.Log;

public class Cave3DSurveysDialog extends Dialog 
                            implements OnItemClickListener
                            // , View.OnClickListener
{
  // private Button mBtnOk;

  private Cave3D mCave3D;
  private Cave3DRenderer mRenderer;

  private ArrayAdapter<String> mArrayAdapter;
  private ListView mList;


  public Cave3DSurveysDialog( Cave3D cave3D, Cave3DRenderer renderer )
  {
    super(cave3D);
    mCave3D = cave3D;
    mRenderer = renderer;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cave3d_surveys_dialog);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

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

    setTitle( R.string.SURVEYS );
  }

  // @Override
  // public void onClick(View view)
  // {
  //   // Log.v( TAG, "onClick()" );
  //   dismiss();
  // }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
      CharSequence item = ((TextView) view).getText();
      String name = item.toString();
      Cave3DSurvey survey = mRenderer.getSurvey( name );
      if ( survey != null ) {
        ( new Cave3DSurveyDialog( mCave3D, survey ) ).show();
      } else {
        // TODO Toast.makeToast( );
      }
    }

}

