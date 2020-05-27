/* @file DialogSketch.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief file opener dialog: get TopoDroid sketch-export filename
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

// import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

import java.util.ArrayList;

import android.content.Context;
import android.app.Dialog;
import android.os.Bundle;

import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import android.view.View;
// import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;


class DialogSketch extends Dialog
                   implements OnItemClickListener
{
  private Context mContext;
  private TopoGL  mApp;
  private String  mBaseDir;

  private ArrayList< MyFileItem > mItems;
  private MyFileAdapter mArrayAdapter;
  private ListView mList;

  class MyFilenameFilter implements FilenameFilter
  {
    public boolean accept( File dir, String name ) {
      if ( name.endsWith( ".c3d" ) ) return true;  // Cave3D sketch file
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

  DialogSketch( Context ctx, TopoGL app )
  {
    super( ctx );
    mContext = ctx;
    mApp  = app;
    mBaseDir = mApp.mAppBasePath;
  } 

  @Override
  public void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );

    setContentView(R.layout.openfile);
    getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    setTitle( R.string.select_dem_file );
    mList = (ListView) findViewById( R.id.list );

    // mArrayAdapter = new ArrayAdapter<String>( this, R.layout.message );
    mList.setOnItemClickListener( this );
    mList.setDividerHeight( 2 );
 
    mItems = new ArrayList< MyFileItem >();
    mArrayAdapter = new MyFileAdapter( mContext, this, mList, R.layout.message, mItems );
    updateList( mBaseDir );
    mList.setAdapter( mArrayAdapter );
  }

  private void updateList( String basedir )
  {
    if ( basedir == null ) return;
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
      Toast.makeText( mContext, R.string.warning_no_cwd, Toast.LENGTH_LONG ).show();
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
      File dir = new File( mBaseDir );
      String parent_dir = dir.getParent();
      if ( parent_dir != null ) {
        mBaseDir = parent_dir;
        updateList( mBaseDir );
      } else {
        Toast.makeText( mContext, R.string.warning_no_parent, Toast.LENGTH_LONG ).show();
      }
      return;
    }
    File file = new File( mBaseDir, name );
    if ( file.isDirectory() ) {
      mBaseDir += "/" + name;
      updateList( mBaseDir );
      return;
    }
    mApp.openSketch( mBaseDir + "/" + name, name );
    dismiss();
  }

}
