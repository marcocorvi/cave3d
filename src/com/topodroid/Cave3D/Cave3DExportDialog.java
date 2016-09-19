/* @file Cave3DExportDialog.java
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

import java.io.File;
import java.io.FileFilter;

import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

import android.os.Bundle;
import android.app.Dialog;
// import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.graphics.*;
import android.view.View;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.CheckBox;
// import android.widget.ToggleButton;
// import android.widget.RadioButton;
import android.widget.Toast;

import android.util.Log;

public class Cave3DExportDialog extends Dialog 
                                implements View.OnClickListener
                                         , AdapterView.OnItemClickListener
{
    private Button mBtnOk;

    private Context mContext;
    private Cave3D  mCave3D;
    private Cave3DRenderer mRenderer;

    private EditText mETfilename;
    private Button   mButtonOK;
    private CheckBox mBinary;
    private CheckBox mAscii;

    private CheckBox mSplay;
    private CheckBox mWalls;
    private CheckBox mSurface;

    // private RadioButton mDebug;
    private String   mDirname;
    private TextView mTVdir;

    private ListView mList;
    private ArrayAdapter<String> mArrayAdapter;

    public Cave3DExportDialog( Context context, Cave3D cave3D, Cave3DRenderer renderer )
    {
      super( context );
      mContext  = context;
      mCave3D   = cave3D;
      mRenderer = renderer;
      mDirname  = mCave3D.mAppBasePath;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.cave3d_export_dialog);

      mTVdir      = (TextView) findViewById( R.id.dirname );
      mETfilename = (EditText) findViewById( R.id.filename );
      mList       = (ListView) findViewById( R.id.list );
      mList.setOnItemClickListener( this );
      mList.setDividerHeight( 2 );
      mArrayAdapter = new ArrayAdapter<String>( mContext, R.layout.message );
      mList.setAdapter( mArrayAdapter );

      mButtonOK = (Button) findViewById( R.id.button_ok );
      mTVdir.setOnClickListener( this );
      mButtonOK.setOnClickListener( this );

      mBinary = (CheckBox) findViewById( R.id.binary );
      mAscii  = (CheckBox) findViewById( R.id.ascii );
      // mDebug  = (RadioButton) findViewById( R.id.debug );
      mBinary.setChecked( true );

      mBinary.setOnClickListener( this );
      mAscii.setOnClickListener( this );
      // mDebug.setOnClickListener( this );

      mSplay   = (CheckBox) findViewById( R.id.splay );
      mWalls   = (CheckBox) findViewById( R.id.walls );
      mSurface = (CheckBox) findViewById( R.id.surface );

      mSplay.setVisibility( View.GONE );
      mWalls.setVisibility( View.GONE );
      mSurface.setVisibility( View.GONE );

      updateList( mDirname );
      setTitle( R.string.EXPORT );
    }

    private void updateList( String dirname )
    {
      int len = dirname.length();
      while ( len > 1 && dirname.charAt(len-1) == '/' ) {
        dirname = dirname.substring( 0, len-1 );
        -- len;
      }
      // Log.v("Cave3D", "DIR <" + dirname + ">" );

      mDirname = dirname;
      len = dirname.length();
      if ( len > 30 ) {
        dirname = "..." + dirname.substring( len-30 );
      }
      mTVdir.setText( dirname );

      mArrayAdapter.clear();
      File dir = new File( mDirname );
      File[] files = dir.listFiles( new FileFilter() { 
        public boolean accept( File pathname ) {
          if ( pathname.getName().startsWith(".") ) return false;
          return true;
        } } );

      if ( files == null || files.length == 0 ) {
        Toast.makeText( mContext, R.string.no_files, Toast.LENGTH_SHORT ).show();
        return;
      }
      ArrayList<String> dirs  = new ArrayList<String>();
      ArrayList<String> names = new ArrayList<String>();
      for ( File f : files ) {
        if ( f.isDirectory() ) {
          // if ( f.getName().equals( "." ) continue;
          dirs.add( new String( f.getName() ) );
        }
      }
      for ( File f : files ) {
        if ( ! f.isDirectory() ) {
          names.add( new String( f.getName() ) );
        }
      }
    
      Comparator<String> cmp = new Comparator<String>() {
            @Override
            public int compare( String s1, String s2 ) { return s1.compareToIgnoreCase( s2 ); }
      };
      if ( dirs.size() > 0 ) { // sort files by name (alphabetical order)
        Collections.sort( dirs, cmp );
        for ( int k=0; k<dirs.size(); ++k ) mArrayAdapter.add( dirs.get(k) + " /" );
      }
      if ( names.size() > 0 ) { // sort files by name (alphabetical order)
        Collections.sort( names, cmp );
        for ( int k=0; k<names.size(); ++k ) mArrayAdapter.add( names.get(k) );
      }

      mList.setAdapter( mArrayAdapter );
      mList.invalidate();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
      String item = ((TextView) view).getText().toString().trim();
      String[] vals = item.split(" ");
      if ( vals.length == 1 ) {
        mETfilename.setText( vals[0] );
      } else {
        updateList( mDirname + "/" + vals[0] );
      }
    }

    @Override
    public void onClick(View v)
    {
      // Log.v( TAG, "onClick()" );
      if ( v.getId() == R.id.button_ok ) {
        String filename = mETfilename.getText().toString();
        if ( filename == null || filename.length() == 0 ) {
          mETfilename.setError( "no filename" );
          return;
        }
        String pathname = mDirname + "/" + filename;
        boolean splays  = mSplay.isChecked();
        boolean walls   = mWalls.isChecked();
        boolean surface = mSurface.isChecked();
        if ( mBinary.isChecked() ) {
          mRenderer.exportModel( 0, pathname, splays, walls, surface );
        } else if ( mAscii.isChecked() ) {
          mRenderer.exportModel( 1, pathname, splays, walls, surface );
        } else {
          mRenderer.exportModel( 2, pathname, splays, walls, surface );
        }
      } else if ( v.getId() == R.id.dirname ) {
        int pos = mDirname.lastIndexOf('/');
        if ( pos > 1 ) {
          updateList( mDirname.substring(0, pos ) );
        }
        return;
      } else if ( v.getId() == R.id.binary ) {
        if ( mBinary.isChecked() ) mAscii.setChecked( false );
        return;
      } else if ( v.getId() == R.id.ascii ) {
        if ( mAscii.isChecked() ) mBinary.setChecked( false );
        return;
      }
      dismiss();
    }
}

