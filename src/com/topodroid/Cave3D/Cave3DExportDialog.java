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
import android.view.ViewGroup.LayoutParams;
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
    private CheckBox mStlBinary;
    private CheckBox mStlAscii;
    private CheckBox mKmlAscii;
    private CheckBox mCgalAscii;
    private CheckBox mLasBinary;
    private CheckBox mDxfAscii;

    private CheckBox mSplay;
    private CheckBox mWalls;
    private CheckBox mSurface;
    private CheckBox mStation;
    private CheckBox mOverwrite;

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
      getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

      mTVdir      = (TextView) findViewById( R.id.dirname );
      mETfilename = (EditText) findViewById( R.id.filename );
      String name = mRenderer.getName();
      if ( name != null ) mETfilename.setText( name );

      mList       = (ListView) findViewById( R.id.list );
      mList.setOnItemClickListener( this );
      mList.setDividerHeight( 2 );
      mArrayAdapter = new ArrayAdapter<String>( mContext, R.layout.message );
      mList.setAdapter( mArrayAdapter );

      mButtonOK = (Button) findViewById( R.id.button_ok );
      mTVdir.setOnClickListener( this );
      mButtonOK.setOnClickListener( this );

      mStlBinary = (CheckBox) findViewById( R.id.stl_binary );
      mStlAscii  = (CheckBox) findViewById( R.id.stl_ascii );
      mKmlAscii  = (CheckBox) findViewById( R.id.kml_ascii );
      mCgalAscii  = (CheckBox) findViewById( R.id.cgal_ascii );
      mLasBinary  = (CheckBox) findViewById( R.id.las_binary );
      mDxfAscii   = (CheckBox) findViewById( R.id.dxf_ascii );
      // mDebug  = (RadioButton) findViewById( R.id.debug );
      mStlBinary.setChecked( true );

      mStlBinary.setOnClickListener( this );
      mStlAscii.setOnClickListener( this );
      mKmlAscii.setOnClickListener( this );
      mCgalAscii.setOnClickListener( this );
      mLasBinary.setOnClickListener( this );
      mDxfAscii.setOnClickListener( this );
      // mDebug.setOnClickListener( this );

      mSplay   = (CheckBox) findViewById( R.id.splay );
      mWalls   = (CheckBox) findViewById( R.id.walls );
      mSurface = (CheckBox) findViewById( R.id.surface );
      mStation = (CheckBox) findViewById( R.id.station );
      mOverwrite = (CheckBox) findViewById( R.id.overwrite );

      // mSplay.setVisibility( View.GONE );
      // mWalls.setVisibility( View.GONE );
      // mSurface.setVisibility( View.GONE );

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
        boolean station = mStation.isChecked();
        boolean overwrite = mOverwrite.isChecked();
        if ( mStlBinary.isChecked() ) {
          mRenderer.exportModel( ModelType.STL_BINARY, pathname, splays, walls, surface, overwrite );
        } else if ( mStlAscii.isChecked() ) {
          mRenderer.exportModel( ModelType.STL_ASCII, pathname, splays, walls, surface, overwrite );
        } else if ( mKmlAscii.isChecked() ) {
          mRenderer.exportModel( ModelType.KML_ASCII, pathname, splays, walls, station, overwrite );
        } else if ( mCgalAscii.isChecked() ) {
          mRenderer.exportModel( ModelType.CGAL_ASCII, pathname, splays, walls, station, overwrite );
        } else if ( mLasBinary.isChecked() ) {
          mRenderer.exportModel( ModelType.LAS_BINARY, pathname, splays, walls, station, overwrite );
        } else if ( mDxfAscii.isChecked() ) {
          Log.v("Cave3D", "export DXF" );
          mRenderer.exportModel( ModelType.DXF_ASCII, pathname, splays, walls, station, overwrite );
        } else {
          mRenderer.exportModel( ModelType.SERIAL, pathname, splays, walls, surface, overwrite );
        }
      } else if ( v.getId() == R.id.dirname ) {
        int pos = mDirname.lastIndexOf('/');
        if ( pos > 1 ) {
          updateList( mDirname.substring(0, pos ) );
        }
        return;
      } else if ( v.getId() == R.id.stl_binary ) {
        if ( mStlBinary.isChecked() ) {
          mStlAscii.setChecked( false );
          mKmlAscii.setChecked( false );
          mCgalAscii.setChecked( false );
          mLasBinary.setChecked( false );
          mDxfAscii.setChecked( false );
        }
        return;
      } else if ( v.getId() == R.id.stl_ascii ) {
        if ( mStlAscii.isChecked() ) {
          mStlBinary.setChecked( false );
          mKmlAscii.setChecked( false );
          mCgalAscii.setChecked( false );
          mLasBinary.setChecked( false );
          mDxfAscii.setChecked( false );
        }
        return;
      } else if ( v.getId() == R.id.kml_ascii ) {
        if ( mKmlAscii.isChecked() ) {
          mStlBinary.setChecked( false );
          mStlAscii.setChecked( false );
          mCgalAscii.setChecked( false );
          mLasBinary.setChecked( false );
          mDxfAscii.setChecked( false );
        }
        return;
      } else if ( v.getId() == R.id.cgal_ascii ) {
        if ( mCgalAscii.isChecked() ) {
          mStlBinary.setChecked( false );
          mStlAscii.setChecked( false );
          mKmlAscii.setChecked( false );
          mLasBinary.setChecked( false );
          mDxfAscii.setChecked( false );
        }
        return;
      } else if ( v.getId() == R.id.las_binary ) {
        if ( mLasBinary.isChecked() ) {
          mStlBinary.setChecked( false );
          mStlAscii.setChecked( false );
          mKmlAscii.setChecked( false );
          mCgalAscii.setChecked( false );
          mDxfAscii.setChecked( false );
        }
        return;
      } else if ( v.getId() == R.id.dxf_ascii ) {
        if ( mDxfAscii.isChecked() ) {
          mStlBinary.setChecked( false );
          mStlAscii.setChecked( false );
          mKmlAscii.setChecked( false );
          mCgalAscii.setChecked( false );
          mLasBinary.setChecked( false );
        }
        return;
      }
      dismiss();
    }
}

