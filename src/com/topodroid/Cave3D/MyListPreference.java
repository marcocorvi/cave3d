/* @file MyListPreferences.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief option list (as in TopoDroid)
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.content.Context;
import android.preference.Preference;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.preference.Preference.OnPreferenceChangeListener;

/**
 */
public class MyListPreference extends ListPreference
{
  public MyListPreference( Context c, AttributeSet a ) 
  {
    super(c,a);
    init();
  }

  public MyListPreference( Context c )
  {
    super( c );
    init();
  }

  private void init()
  {
    setOnPreferenceChangeListener( new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange( Preference p, Object v ) 
      {
        p.setSummary( getEntry() );
        return true;
      }
    } );
  }

  @Override
  public CharSequence getSummary() { return super.getEntry(); }
}

