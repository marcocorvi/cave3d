/* @file DialogBluetoothSurveyEdit.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D bluetooth survey properties edit
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

// import android.util.Log;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;

class DialogBluetoothSurveyEdit extends Dialog 
                                implements View.OnClickListener
{
  // private Button mBtnOk;

  private TopoGL mApp;
  private BluetoothSurvey mBtSurvey;
  private DialogBluetoothSurveyList mBtSurveyList;
  private EditText mEtNickname;
  private CheckBox mOverwrite;

  public DialogBluetoothSurveyEdit( TopoGL app, DialogBluetoothSurveyList survey_list, BluetoothSurvey bt_survey )
  {
    super( app );
    mApp          = app;
    mBtSurveyList = survey_list;
    mBtSurvey     = bt_survey;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.bluetooth_survey_edit_dialog);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    ((TextView) findViewById( R.id.name )).setText( mBtSurvey.getName() );
    mEtNickname = (EditText) findViewById( R.id.nickname );
    // if ( mBtSurvey.getNickname() != null ) 
    mEtNickname.setText( mBtSurvey.getNickname() );

    ((Button) findViewById( R.id.btn_cancel )).setOnClickListener( this );
    ((Button) findViewById( R.id.btn_save )).setOnClickListener( this );

    mOverwrite = (CheckBox) findViewById( R.id.overwrite );

    setTitle( R.string.bt_survey_edit );
  }

  @Override
  public void onClick(View view)
  {
    if ( view.getId() == R.id.btn_save ) {
      if ( mEtNickname.getText() != null ) {
        BluetoothSurveyManager.renameSurvey( mBtSurvey, mEtNickname.getText().toString(), mOverwrite.isChecked() );
        mBtSurveyList.updateFileList( );
      }
    }
    dismiss();
  }

}

