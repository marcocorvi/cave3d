/* @file Cave3DFileOpener.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D file opener activity: get user input filename
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

// import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

public class  Cave3DOpenFileDialog extends Activity
                            implements OnItemClickListener
{
  // private static final String TAG = "Cave3D FILE";

  // private ArrayAdapter<String> mArrayAdapter;
  private ArrayList< MyFileItem > mItems;
  private MyFileAdapter mArrayAdapter;
  private ListView mList;

  class MyFilenameFilter implements FilenameFilter
  {
    public boolean accept( File dir, String name ) {
      if ( name.endsWith( ".th" ) ) return true;
      if ( name.endsWith( "thconfig" ) ) return true;
      if ( name.endsWith( "tdconfig" ) ) return true;
      if ( name.endsWith( ".lox" ) ) return true;
      if ( name.endsWith( ".mak" ) ) return true;
      if ( name.endsWith( ".dat" ) ) return true;
      if ( name.endsWith( ".tro" ) ) return true;
      return false;
    }
  }

  class MyDirnameFilter implements FilenameFilter
  {
    public boolean accept( File dir, String name ) {
      File file = new File( dir, name );
      if ( file.isDirectory() && ! name.startsWith(".") ) return true;
      return false;
    }
  }

  @Override
  public void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );

    setContentView(R.layout.openfile);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    setTitle( R.string.select_file );
    mList = (ListView) findViewById( R.id.list );

    // mArrayAdapter = new ArrayAdapter<String>( this, R.layout.message );
    mList.setOnItemClickListener( this );
    mList.setDividerHeight( 2 );
 
    mItems = new ArrayList< MyFileItem >();
    mArrayAdapter = new MyFileAdapter( this, this, mList, R.layout.message, mItems );
    updateList( Cave3D.mAppBasePath );
    mList.setAdapter( mArrayAdapter );
  }

  private void updateList( String basedir )
  {
    File dir = new File( basedir );
    if ( dir.exists() ) {
      String[] dirs  = dir.list( new MyDirnameFilter() );
      String[] files = dir.list( new MyFilenameFilter() );
      mArrayAdapter.clear();
      mArrayAdapter.add( "..", true );
      if ( dirs != null ) {
        for ( String item : dirs ) {
          mArrayAdapter.add( item, true );
        }
      }
      if ( files != null ) {
        for ( String item : files ) {
          mArrayAdapter.add( item, false );
        }
      }
    } else {
      // should never comes here
      Toast.makeText( this, R.string.warning_no_cwd, Toast.LENGTH_LONG ).show();
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id)
  {
    CharSequence item = ((TextView) view).getText();
    String name = item.toString();
    if ( name.startsWith("+ ") ) {
      name = name.substring( 2 );
    }
    if ( name.equals("..") ) {
      File dir = new File( Cave3D.mAppBasePath );
      Cave3D.mAppBasePath = dir.getParent();
      updateList( Cave3D.mAppBasePath );
      return;
    }
    File file = new File( Cave3D.mAppBasePath, name );
    if ( file.isDirectory() ) {
      Cave3D.mAppBasePath += "/" + name;
      updateList( Cave3D.mAppBasePath );
      return;
    }
    // Log.v( TAG, "FILE " + name );
    Intent intent = new Intent();
    intent.putExtra( "com.topodroid.Cave3D.filename", name );
    setResult( Activity.RESULT_OK, intent );
    // } else {
    //   setResult( RESULT_CANCELED );
    // }
    finish();
  }
    

}
