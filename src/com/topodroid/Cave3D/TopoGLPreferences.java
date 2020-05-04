/* @file TopoGLPreferences.java
 *
 * @author marco corvi
 * @date jul 2014
 *
 * @brief Cave3D options dialog
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.CheckBoxPreference;
// import android.preference.EditTextPreference;
// import android.preference.ListPreference;
// import android.view.Menu;
// import android.view.MenuItem;

/**
 */
public class TopoGLPreferences extends PreferenceActivity 
{

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate( savedInstanceState );

    // Bundle extras = getIntent().getExtras();
    // if ( extras != null ) {
    //   mPrefCategory = extras.getInt( TopoGLPreferences.PREF_CATEGORY );
    // }

    addPreferencesFromResource(R.xml.preferences);

  }

}
