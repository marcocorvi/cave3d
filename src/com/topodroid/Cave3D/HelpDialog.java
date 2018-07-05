/* @file HelpDialog.java
 *
 * @author marco corvi
 * @date jul 2018
 *
 * @brief help dialog (from TopoDroid)
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.os.Bundle;
import android.app.Dialog;
// import android.app.Activity;
import android.content.Context;
// import android.content.Intent;

import android.widget.Button;
import android.widget.TextView;

import android.view.View;
import android.view.View.OnClickListener;

// import android.util.Log;

class HelpDialog extends Dialog
                 implements OnClickListener
{
  HelpDialog( Context context )
  {
    super( context ); 
  }

  @Override
  public void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );
    setContentView( R.layout.help_dialog );
  }

  @Override 
  public void onClick( View v ) 
  {
    dismiss();
  }

}

