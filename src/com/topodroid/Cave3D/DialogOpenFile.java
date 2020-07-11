/* @file DialogOpenFile.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief file opener dialog: get user input filename
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

import java.util.Arrays;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;

class DialogOpenFile extends Dialog
                     implements OnItemClickListener
{
  private Context mContext;
  private TopoGL  mApp;

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
      if ( name.endsWith( ".3d" ) ) return true;
      // if ( name.endsWith( ".srv" ) ) return true; // not implemented yet
      return false;
    }
  }

  class MyDirnameFilter implements FilenameFilter
  {
    public boolean accept( File dir, String name ) {
      File file = new File( dir, name );
      if ( file.isDirectory() && ! name.equals(".") ) return true;
      return false;
    }
  }

  DialogOpenFile( Context ctx, TopoGL app )
  {
    super( ctx );
    mContext = ctx;
    mApp     = app;
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
    mArrayAdapter = new MyFileAdapter( mContext, this, mList, R.layout.message, mItems );
    if ( ! updateList( mApp.mAppBasePath ) ) {
      dismiss();
    } else {
      mList.setAdapter( mArrayAdapter );
    }
  }

  private boolean updateList( String basedir )
  {
    if ( basedir == null ) return false;
    // Log.v("TopoGL", "update list files " + basedir );
    File dir = new File( basedir );
    if ( dir.exists() ) {
      String[] dirs  = dir.list( new MyDirnameFilter() );
      String[] files = dir.list( new MyFilenameFilter() );
      if ( dirs == null && files == null ) {
        if ( mApp != null ) mApp.uiToast( R.string.warning_empty_cwd, true );
        return false;
      }
      mArrayAdapter.clear();
      mArrayAdapter.add( "..", true );
      if ( dirs != null ) {
        Arrays.sort( dirs );
        for ( String item : dirs ) {
          mArrayAdapter.add( item, true );
        }
      }
      if ( files != null && files.length > 0 ) {
        Arrays.sort( files );
        for ( String item : files ) {
          mArrayAdapter.add( item, false );
        }
      } else {
        if ( mApp != null ) mApp.uiToast( R.string.warning_no_file, true );
      }
      return true;
    } else {
      // should never comes here
      if ( mApp != null ) mApp.uiToast( R.string.warning_no_cwd, true );
      return false;
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
      File dir = new File( mApp.mAppBasePath );
      String parent_dir = dir.getParent();
      if ( parent_dir != null ) {
        if ( updateList( parent_dir ) ) {
          mApp.mAppBasePath = parent_dir;
        }
      } else {
        if ( mApp != null ) mApp.uiToast( R.string.warning_no_parent, true );
      }
      return;
    }
    File file = new File( mApp.mAppBasePath, name );
    if ( file.isDirectory() ) {
      String dir = mApp.mAppBasePath + "/" + name;
      if ( updateList( dir ) ) {
        mApp.mAppBasePath = dir;
      }
      return;
    }
    // Intent intent = new Intent();
    // intent.putExtra( "com.topodroid.Cave3D.filename", name );
    // setResult( Activity.RESULT_OK, intent );
    mApp.doOpenFile( mApp.mAppBasePath + "/" + name, true ); // open asynchronous
    dismiss();
  }
    

}
