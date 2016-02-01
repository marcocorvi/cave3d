/* @file Cave3D.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D main activity
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.io.StringWriter;
import java.io.PrintWriter;

import android.os.Environment;
import android.app.Activity;
import android.app.ActivityManager;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;


import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ZoomControls;
import android.widget.ZoomButton;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences.Editor;

import android.util.DisplayMetrics;

import android.util.Log;

public class Cave3D extends Activity
                    implements OnZoomListener
                    // , View.OnTouchListener
                    , OnSharedPreferenceChangeListener
{
  private static final String TAG = "Cave3D";

  private static final int REQUEST_OPEN_FILE = 1;
  static boolean mUseSplayVector = true;

  static String APP_BASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TopoDroid/th/";
  static String mAppBasePath  = APP_BASE_PATH;

  // -----------------------------------------------------
  // PREFERENCES

  private SharedPreferences prefs;

  static int mSelectionRadius = 20;
  static int mTextSize        = 20;
  static boolean mAllSplay    = true;
  static boolean mGridAbove   = false;
  static boolean mPreprojection = true;
  static boolean mSplitTriangles = true;
  static boolean mSplitRandomize = true;
  static boolean mSplitStretch   = false;
  static float mSplitRandomizeDelta = 0.1f; // meters
  static float mSplitStretchDelta   = 0.1f;

  static final String CAVE3D_BASE_PATH = "CAVE3D_BASE_PATH";
  static final String CAVE3D_TEXT_SIZE = "CAVE3D_TEXT_SIZE";
  static final String CAVE3D_SELECTION_RADIUS = "CAVE3D_SELECTION_RADIUS";
  static final String CAVE3D_GRID_ABOVE = "CAVE3D_GRID_ABOVE";
  static final String CAVE3D_ALL_SPLAY = "CAVE3D_ALL_SPLAY";
  static final String CAVE3D_PREPROJECTION = "CAVE3D_PREPROJECTION";
  static final String CAVE3D_SPLIT_TRIANGLES = "CAVE3D_SPLIT_TRIANGLES";
  static final String CAVE3D_SPLIT_RANDOM    = "CAVE3D_SPLIT_RANDOM";
  static final String CAVE3D_SPLIT_STRETCH   = "CAVE3D_SPLIT_STRETCH";

  public void onSharedPreferenceChanged( SharedPreferences sp, String k ) 
  {
    if ( k.equals( CAVE3D_BASE_PATH ) ) { 
      mAppBasePath = sp.getString( k, APP_BASE_PATH );
      Log.v("Cave3D", "SharedPref change: path " + mAppBasePath );
    } else if ( k.equals( CAVE3D_TEXT_SIZE ) ) {
      try {
        mTextSize = Integer.parseInt( sp.getString( k, "20" ) );
        Cave3DRenderer.setStationPaintTextSize( mTextSize );
      } catch ( NumberFormatException e ) {
      }
    } else if ( k.equals( CAVE3D_SELECTION_RADIUS ) ) { 
      try {
        mSelectionRadius = Integer.parseInt( sp.getString( k, "20" ) );
      } catch ( NumberFormatException e ) {
      }
    } else if ( k.equals( CAVE3D_GRID_ABOVE ) ) { 
      boolean b = sp.getBoolean( k, false );
      if ( b != mGridAbove ) {
        mGridAbove = b;
        mRenderer.precomputeProjectionsGrid();
      }
    } else if ( k.equals( CAVE3D_ALL_SPLAY ) ) { 
      mAllSplay = sp.getBoolean( k, true );
    } else if ( k.equals( CAVE3D_PREPROJECTION ) ) { 
      mPreprojection = sp.getBoolean( k, true );
    } else if ( k.equals( CAVE3D_SPLIT_TRIANGLES ) ) { 
      mSplitTriangles = sp.getBoolean( k, true );
    } else if ( k.equals( CAVE3D_SPLIT_RANDOM ) ) { 
      try {
        float r = Float.parseFloat( sp.getString( k, "0.1" ) );
        if ( r > 0.0001f ) {
          mSplitRandomizeDelta = r;
          mSplitRandomize = true;
        } else {
          mSplitRandomize = false;
        }
      } catch ( NumberFormatException e ) { }
    } else if ( k.equals( CAVE3D_SPLIT_STRETCH ) ) { 
      try {
        float r = Float.parseFloat( sp.getString( k, "0.1" ) );
        if ( r > 0.0001f ) {
          mSplitStretchDelta = r;
          mSplitStretch = true;
        } else {
          mSplitStretch = false;
        }
      } catch ( NumberFormatException e ) { }
    }
  }

  private void loadPreferences( SharedPreferences sp )
  {
    float r;
    mAppBasePath = sp.getString( CAVE3D_BASE_PATH, APP_BASE_PATH );
    try {
      mTextSize = Integer.parseInt( sp.getString( CAVE3D_TEXT_SIZE, "20" ) );
    } catch ( NumberFormatException e ) {
    }
    try {
      mSelectionRadius = Integer.parseInt( sp.getString( CAVE3D_SELECTION_RADIUS, "20" ) );
    } catch ( NumberFormatException e ) {
    }
    mGridAbove      = sp.getBoolean( CAVE3D_GRID_ABOVE, false );
    mAllSplay       = sp.getBoolean( CAVE3D_ALL_SPLAY, true );
    mPreprojection  = sp.getBoolean( CAVE3D_PREPROJECTION, true );
    mSplitTriangles = sp.getBoolean( CAVE3D_SPLIT_TRIANGLES, true );
    mSplitRandomize = false;
    try {
      r = Float.parseFloat( sp.getString( CAVE3D_SPLIT_RANDOM, "0.1" ) );
      if ( r > 0.0001f ) {
        mSplitRandomizeDelta = r;
        mSplitRandomize = true;
      }
    } catch ( NumberFormatException e ) { }
    mSplitStretch = false;
    try {
      r = Float.parseFloat( sp.getString( CAVE3D_SPLIT_STRETCH, "0.1" ) );
      if ( r > 0.0001f ) {
        mSplitStretchDelta = r;
        mSplitStretch = true;
      }
    } catch ( NumberFormatException e ) { }
  }

  // -----------------------------------------------------------

  String mFilename; // opened filename
  public float mScaleFactor   = 1.0f;
  public static float mDisplayWidth  = 200f;
  public static float mDisplayHeight = 320f;

  private Cave3DView mView;
  // private DrawingSurface mDrawingSurface;
  private boolean mIsNotMultitouch;

  private Cave3DRenderer mRenderer;
  private MenuItem mOpenFile;
  private MenuItem mExport;
  private MenuItem mColorMode;
  private MenuItem mFrameMode;
  // private MenuItem mZoomIn;
  private MenuItem mZoomOne;
  // private MenuItem mZoomOut;
  private MenuItem mReset;
  private MenuItem mOptions;
  // private MenuItem mWallMode;
  private MenuItem mInfo;
  private MenuItem mFiles;
  private MenuItem mIco;
  private MenuItem mRose;

  // private Button openBtn;
  private Button modeBtn;
  // private Button viewBtn;
  private Button stnsBtn;
  private Button wallBtn;
  private Button surfaceBtn;
  // private Button zoomInBtn;
  // private Button zoomOutBtn;
  private Button splayBtn;

  ZoomButtonsController mZoomBtnsCtrl;
  View mZoomView;
  ZoomControls mZoomCtrl;
  int ZOOM_Y = 280;

  private static final int TRY = 1;

  public static final int MODE_TRANSLATE = 0;
  public static final int MODE_ROTATE    = 1;
  // public static final int MODE_ZOOM      = 2;
  public static final int MODE_MAX       = 2;
  private int mode;
  private boolean supportsES2 = false;

  // public void zoomIn()  { mRenderer.zoomIn(); }
  // public void zoomOut() { mRenderer.zoomOut(); }
  public void zoomOne() { mRenderer.zoomOne(); }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    super.onCreateOptionsMenu( menu );
    Resources resources = getResources();
    mColorMode = menu.add( resources.getString( R.string.menu_color ) );
    mFrameMode = menu.add( resources.getString( R.string.menu_frame ) );
    mOptions   = menu.add( resources.getString( R.string.menu_options ) );
    mIco       = menu.add( resources.getString( R.string.menu_ico ) );
    mRose      = menu.add( resources.getString( R.string.menu_rose ) );
    // mWallMode  = menu.add( resources.getString( R.string.menu_wall ) );
    mInfo      = menu.add( resources.getString( R.string.menu_info ) );
    mFiles     = menu.add( resources.getString( R.string.menu_files ) );
    mOpenFile  = menu.add( resources.getString( R.string.menu_open ) );
    mExport    = menu.add( resources.getString( R.string.menu_export ) );
    mZoomOne   = menu.add( resources.getString( R.string.menu_zoom_one ) );
    mReset     = menu.add( resources.getString( R.string.menu_reset ) );
    return true;
  }

  
  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if ( item == mOpenFile ) {
      openFile();
    } else if ( item == mExport ) {
      new Cave3DExportDialog( this, this, mRenderer ).show();
    } else if ( item == mOptions ) {
      Intent intent = new Intent( this, Cave3DPreferences.class );
      startActivity( intent );
    } else if ( item == mColorMode ) {
      if ( mFilename != null )
        mRenderer.toggleColorMode();
    } else if ( item == mFrameMode ) {
      if ( mFilename != null )
        mRenderer.toggleFrameMode();
    } else if ( item == mZoomOne ) {
      if ( mFilename != null )
        zoomOne();
    } else if ( item == mReset ) {
      if ( mFilename != null )
        mRenderer.resetGeometry();
    // } else if ( item == mWallMode ) {
    //   setWallButton( mRenderer.toggleWallMode() );
    } else if ( item == mInfo ) {
      if ( mFilename != null )
        (new Cave3DInfoDialog(this, mRenderer)).show();
    } else if ( item == mFiles ) {
      if ( mFilename != null )
        (new Cave3DSurveysDialog(this, mRenderer)).show();
    } else if ( item == mIco  ) {
      if ( mFilename != null )
        (new Cave3DIcoDialog(this, mRenderer)).show();
    } else if ( item == mRose ) {
      if ( mFilename != null )
        (new Cave3DRoseDialog(this, mRenderer)).show();
    } else {
      return super.onOptionsItemSelected(item);
    }
    return true;
  }

  public void onActivityResult( int request, int result, Intent data ) 
  {
    switch ( request ) {
      case REQUEST_OPEN_FILE:
        if ( result == Activity.RESULT_OK ) {
          String filename = data.getExtras().getString( "com.topodroid.Cave3D.filename" );
          if ( filename != null && filename.length() > 0 ) {
            doOpenFile( mAppBasePath + "/" + filename );
          }
        }
        break;
    }
  }

  void showTitle( double clino, double phi )
  {
    if ( mFilename != null ) {
      setTitle( String.format( getResources().getString(R.string.title), mFilename, clino, phi ) );
    } else {
      setTitle( "C A V E _ 3 D ");
    }
  }

  private boolean doOpenFile( String filename )
  {
    mFilename = null;
    // mRenderer.setCave3D( this );
    if ( mRenderer.initRendering( this, filename ) ) {
      // setTitle( filename );
      int idx = filename.lastIndexOf( '/' );
      if ( idx >= 0 ) {
        mFilename = filename.substring( idx+1 );
      } else {
        mFilename = filename;
      }
    }
    return ( mFilename != null );
  }

  private void setModeText( int m )
  {
    mode = m % MODE_MAX;
    if ( TRY == 1 ) {
      switch ( mode ) {
        case MODE_TRANSLATE:
          modeBtn.setText( getResources().getString( R.string.btn_move ) );
          break;
        case MODE_ROTATE:
          modeBtn.setText( getResources().getString( R.string.btn_rotate ) );
          break;
      }
    }
    // mRenderer.setMode( mode );
    mView.setMode( mode );
  }

  private void openFile()
  {
    Intent openFileIntent = new Intent( Intent.ACTION_EDIT ).setClass( this, Cave3DOpenFileDialog.class );
    startActivityForResult( openFileIntent, REQUEST_OPEN_FILE );
  }

  @Override
  public boolean onSearchRequested()
  {
    // Bundle appData = new Bundle();
    // appData.putBoolean(SearchableActivity.JARGON, true);
    // startSearch(null, false, appData, false);
    // int grid     = mRenderer.getGrid();
    int surveys  = mRenderer.getNrSurveys();
    int shots    = mRenderer.getNrShots();
    int splays   = mRenderer.getNrSplays();
    int stations = mRenderer.getNrStations();
    int length   = (int)( mRenderer.getCaveLength() );
    int depth    = (int)( mRenderer.getCaveDepth() );

    StringWriter sw = new StringWriter();
    PrintWriter pw  = new PrintWriter( sw );
    Resources res = getResources();
    pw.format( res.getString( R.string.query ), surveys, stations, shots, splays, length, depth );
    Toast.makeText( this, sw.getBuffer().toString(), Toast.LENGTH_LONG ).show();
    return true;
 }
      

  public void onClick(View view)
  {
    switch (view.getId()){
      // case R.id.openBtn:
      //   openFile();
      //   break;
      // case R.id.viewBtn:
      //   // TODO
      //   Toast.makeText( this, "TODO onClick viewBtn", Toast.LENGTH_LONG ).show();
      //   break;
      case R.id.modeBtn:
        setModeText( mode+1 );
        break;
      // case R.id.zoomInBtn:
      //   zoomIn();
      //   break;
      // case R.id.zoomOutBtn:
      //   zoomOut();
      //   // Cave3DZoomDialog zoom_dialog = new Cave3DZoomDialog( this );
      //   // zoom_dialog.show();
      //   break;
      case R.id.splayBtn:
        if ( mRenderer != null ) {
          mRenderer.toggleDoSplays();
        }
        break;
      case R.id.stnsBtn:
        if ( mRenderer != null ) {
          mRenderer.toggleDoStations();
        }
        break;
      case R.id.wallBtn:
        if ( mRenderer != null ) {
          setWallButton( mRenderer.toggleWallMode() );
        }
        break;
      case R.id.surfaceBtn:
        if ( mRenderer != null ) {
          mRenderer.toggleDoSurface();
        }
        break;
    }
  }


  // @Override
  // public boolean onTouch( View view, MotionEvent motionEvent )
  // {
  //   // float x_canvas = motionEvent.getX();
  //   // float y_canvas = motionEvent.getY();
  //   // if ( y_canvas > ZOOM_Y ) {
  //   //   Log.v( TAG, "y_canvas zoom " + y_canvas );
  //   //   mZoomBtnsCtrl.setVisible( true );
  //   //   // mZoomCtrl.show( );
  //   //   return true;
  //   // }

  //   if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
  //   } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
  //   } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
  //   }
  //   return false;
  // }


  @Override
  public void onVisibilityChanged(boolean visible)
  {
      mZoomBtnsCtrl.setVisible( visible );
  }

  @Override
  public void onZoom( boolean zoomin )
  {
      if ( zoomin ) mRenderer.zoomIn();
      else          mRenderer.zoomOut();
  }

  private void setWallButton( int wall_mode )
  {
    if ( wall_mode == Cave3DRenderer.WALL_HULL ) {
      wallBtn.setTextColor( 0xff00ff00 );
    } else if ( wall_mode == Cave3DRenderer.WALL_DELAUNAY ) {
      wallBtn.setTextColor( 0xff0000ff );
    } else {
      wallBtn.setTextColor( 0xffcccccc );
    }
  }

  @Override
  public void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );
    // Log.v( TAG, "Cave3D::onCreate");

    mIsNotMultitouch = ! getPackageManager().hasSystemFeature( PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH );

    prefs = PreferenceManager.getDefaultSharedPreferences( this );
    loadPreferences( prefs );
    prefs.registerOnSharedPreferenceChangeListener( this );

    setContentView(R.layout.main);

    DisplayMetrics dm = getResources().getDisplayMetrics();
    float density  = dm.density;
    mDisplayWidth  = dm.widthPixels;
    mDisplayHeight = dm.heightPixels;
    mScaleFactor   = (mDisplayHeight / 320.0f) * density;
    ZOOM_Y = (int)mDisplayHeight - 100;
    // Log.v( TAG, "display " + mDisplayWidth + " " + mDisplayHeight + " scale " + mScaleFactor + " ZOOM_Y " + ZOOM_Y );
    
    // openBtn = (Button) findViewById(R.id.openBtn);
    modeBtn = (Button) findViewById(R.id.modeBtn);
    // viewBtn = (Button) findViewById(R.id.viewBtn);
    stnsBtn = (Button) findViewById(R.id.stnsBtn);
    wallBtn = (Button) findViewById(R.id.wallBtn);
    surfaceBtn = (Button) findViewById(R.id.surfaceBtn);
    // zoomInBtn = (Button) findViewById(R.id.zoomInBtn);
    // zoomOutBtn = (Button) findViewById(R.id.zoomOutBtn);
    splayBtn = (Button) findViewById(R.id.splayBtn);

    // modeBtn.setEnabled(true);
    // viewBtn.setEnabled(true);
    // stnsBtn.setEnabled(true);
    // wallBtn.setEnabled(true);
    // zoomInBtn.setEnabled(true);
    // zoomOutBtn.setEnabled(true);

    // openBtn.getBackground().setAlpha(192);
    modeBtn.getBackground().setAlpha(192);
    // viewBtn.getBackground().setAlpha(192);
    stnsBtn.getBackground().setAlpha(192);
    wallBtn.getBackground().setAlpha(192);
    surfaceBtn.getBackground().setAlpha(192);
    // FIXME surfaceBtn.setVisibility( View.GONE );

    // zoomInBtn.getBackground().setAlpha(192);
    // zoomOutBtn.getBackground().setAlpha(192);
    splayBtn.getBackground().setAlpha(192);


    mView = (Cave3DView) findViewById( R.id.caveView );
    // mZoom    = app.mScaleFactor;    // canvas zoom


    if ( mIsNotMultitouch ) {
        mZoomView = (View) findViewById(R.id.zoomView );
        mZoomBtnsCtrl = new ZoomButtonsController( mZoomView );
        mZoomBtnsCtrl.setOnZoomListener( this );
        mZoomBtnsCtrl.setVisible( true );
        mZoomBtnsCtrl.setZoomInEnabled( true );
        mZoomBtnsCtrl.setZoomOutEnabled( true );
        mZoomCtrl = (ZoomControls) mZoomBtnsCtrl.getZoomControls();
        // ViewGroup vg = mZoomBtnsCtrl.getContainer();
    }

    if ( mIsNotMultitouch ) {
      mView.setZoomControl( mZoomBtnsCtrl, ZOOM_Y, mIsNotMultitouch );
    }

    // mView.setOnTouchListener(this);
    // mView.setOnLongClickListener(this);
    // mView.setBuiltInZoomControls(true);

    // Log.v( TAG, "Cave3D::onCreate view is " + ( (mView==null)? "null" : mView.toString() ) );
    mRenderer = mView.getRenderer();
    
    setModeText( MODE_ROTATE );
    setWallButton( mRenderer.wall_mode );

    // onOptionsItemSelected( mOpenFile );

    Bundle extras = getIntent().getExtras();
    if ( extras != null ) {
      String name = extras.getString( "survey" );
      // Log.v("Cave3D", "TopoDroid filename " + name );
      if ( name != null ) {
        if ( ! doOpenFile( name ) ) {
          Log.e("Cave3D", "Cannot open TopoDroid file " + name );
          finish();
        }
      }
    } else {
      // Log.v("Cave3D", "No filename: openfile dialog" );
      openFile();
    }
  }

  @Override
  protected synchronized void onPause() 
  { 
    if ( mIsNotMultitouch ) mZoomBtnsCtrl.setVisible(false);
    // mZoomBtnsCtrl.setVisible(false);
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

}
