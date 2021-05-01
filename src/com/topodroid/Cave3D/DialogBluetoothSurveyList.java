/* @file DialogBluetoothSurveyList.java
 *
 * @author marco corvi
 * @date apr 2021
 *
 * @brief BT survey list dialog
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.io.File;
import java.io.FileFilter;

import java.util.Date;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import java.text.SimpleDateFormat;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;

class DialogBluetoothSurveyList extends Dialog
                     implements OnItemClickListener, OnClickListener, OnItemLongClickListener
{
  private Context mContext;
  private TopoGL  mApp;

  // private ArrayAdapter<String> mArrayAdapter;
  private List< String > mItems;
  private ListView mList;
  private EditText mName;
  private Button   mNew;

  DialogBluetoothSurveyList( Context ctx, TopoGL app )
  {
    super( ctx );
    mContext = ctx;
    mApp     = app;
  }

  @Override
  public void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );

    setContentView(R.layout.bluetooth_survey_list);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    setTitle( R.string.survey_list );
    mList = (ListView) findViewById( R.id.list );
    mName = (EditText) findViewById( R.id.name );
    mNew  = (Button) findViewById( R.id.btn_new );

    SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd:hh:mm:ss" );
    mName.setText( sdf.format( new Date() ) );
    mNew.setOnClickListener( this );

    // mArrayAdapter = new ArrayAdapter<String>( this, R.layout.message );
    mList.setOnItemClickListener( this );
    mList.setOnItemLongClickListener( this );
    mList.setDividerHeight( 2 );
 
    updateFileList( );
  }

  void updateFileList( )
  {
    String basedir = Cave3DFile.getBluetoothDirname();
    ArrayList<String> mItems = new ArrayList<>();
    File dir = new File( basedir );
    if ( dir.isDirectory() ) {
      File[] files = dir.listFiles( new FileFilter() {
        @Override public boolean accept( File file ) { return file.isFile(); }
      } );
      Log.v("Cave3D", "BT survey files " + files.length );
      for ( int k=0; k<files.length; ++k ) mItems.add( files[k].getName() );
    }
    ArrayAdapter< String > array_adapter = new ArrayAdapter<>( mContext, R.layout.message, mItems );
    mList.setAdapter( array_adapter );
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id)
  {
    CharSequence item = ((TextView) view).getText();
    if ( item != null ) {
      BluetoothSurvey bt_survey = BluetoothSurveyManager.getSurvey( item.toString() );
      mApp.openBluetoothSurvey( bt_survey );
    }
    dismiss();
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
  {
    CharSequence item = ((TextView) view).getText();
    if ( item != null ) {
      BluetoothSurvey bt_survey = BluetoothSurveyManager.getSurvey( item.toString() );
      (new DialogBluetoothSurveyEdit( mApp, this, bt_survey )).show();
    }
    // dismiss();
    return true;
  }

  @Override
  public void onClick( View view )
  {
    if ( view.getId() == R.id.btn_new ) {
      String name= mName.getText().toString();
      BluetoothSurvey bt_survey = BluetoothSurveyManager.createSurvey( name );
      if ( bt_survey != null ) {
        mApp.openBluetoothSurvey( bt_survey );
      } else {
        Log.v("Cave3D", "BTsurvey manager create null");
      }
    }
    dismiss();
  }

}
