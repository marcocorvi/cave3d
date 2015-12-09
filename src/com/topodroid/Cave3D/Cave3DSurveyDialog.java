/* @file Cave3DSurveyDialog.java
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
import android.widget.Button;
import android.widget.TextView;

// import android.util.Log;

public class Cave3DSurveyDialog extends Dialog 
                            // implements OnItemClickListener
                            // , View.OnClickListener
{
  // private Button mBtnOk;

  private Cave3D mCave3D;
  private Cave3DSurvey mSurvey;

  public Cave3DSurveyDialog( Cave3D cave3D, Cave3DSurvey survey )
  {
    super(cave3D);
    mCave3D = cave3D;
    mSurvey = survey;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cave3d_survey_dialog);

    TextView tv;
    tv = (TextView) findViewById( R.id.survey_legs );
    tv.setText( Integer.toString( mSurvey.mNrShots ) );
    tv = (TextView) findViewById( R.id.survey_legs_length );
    tv.setText( Integer.toString( (int)(mSurvey.mLenShots) ) );
    tv = (TextView) findViewById( R.id.survey_splays );
    tv.setText( Integer.toString( mSurvey.mNrSplays ) );
    tv = (TextView) findViewById( R.id.survey_splays_length );
    tv.setText( Integer.toString( (int)(mSurvey.mLenSplays) ) );

    setTitle( mSurvey.name );
  }

  // @Override
  // public void onClick(View view)
  // {
  //   // Log.v( TAG, "onClick()" );
  //   dismiss();
  // }

}

