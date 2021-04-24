/* @file DialogSurveyList.java
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

import java.util.Date;
import java.util.Arrays;
import java.util.ArrayList;

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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;

class DialogSurveyList extends Dialog
                     implements OnItemClickListener, OnClickListener
{
  private Context mContext;
  private TopoGL  mApp;

  // private ArrayAdapter<String> mArrayAdapter;
  // private ArrayList< MyFileItem > mItems;
  private MyFileAdapter mArrayAdapter;
  private ListView mList;
  private EditText mName;
  private Button   mNew;

  DialogSurveyList( Context ctx, TopoGL app )
  {
    super( ctx );
    mContext = ctx;
    mApp     = app;
  }

  @Override
  public void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );

    setContentView(R.layout.survey_list);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    setTitle( R.string.survey_list );
    mList = (ListView) findViewById( R.id.list );
    mName = (EditText) findViewById( R.id.name );
    mNew  = (Button) findViewById( R.id.btn_new );

    SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd:hh:mm" );
    mName.setText( sdf.format( new Date() ) );
    mNew.setOnClickListener( this );

    // mArrayAdapter = new ArrayAdapter<String>( this, R.layout.message );
    mList.setOnItemClickListener( this );
    mList.setDividerHeight( 2 );
 
    // mItems = new ArrayList< MyFileItem >();
    // mArrayAdapter = new MyFileAdapter( mContext, this, mList, R.layout.message, mItems );
    // if ( ! updateList( mApp.mAppBasePath ) ) {
    //   dismiss();
    // } else {
    //   mList.setAdapter( mArrayAdapter );
    // }
  }

  private boolean updateList( String basedir )
  {
    return false;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id)
  {
    CharSequence item = ((TextView) view).getText();
    if ( item != null ) {
      mApp.openBluetoothSurvey( item.toString() );
    }
    dismiss();
  }

  @Override
  public void onClick( View view )
  {
    if ( view.getId() == R.id.btn_new ) {
      String name= mName.getText().toString();
      if ( name != null && name.length() > 0 ) {
        mApp.openBluetoothSurvey( name );
      }
    }
    dismiss();
  }

}
