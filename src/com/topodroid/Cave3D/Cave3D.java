/* @file Cave3D.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D main activity
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.io.StringWriter;
import java.io.PrintWriter;

import java.util.ArrayList;

import android.os.Environment;
import android.app.Activity;
import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Build;

import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import android.graphics.drawable.BitmapDrawable;

import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;

import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ZoomControls;
import android.widget.ZoomButton;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences.Editor;

import android.util.DisplayMetrics;

import android.net.Uri;

public class Cave3D extends Activity
                    implements OnZoomListener
                    , OnClickListener
                    , OnItemClickListener
                    // , View.OnTouchListener
                    , OnSharedPreferenceChangeListener
{
  final static boolean ANDROID_10 = ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.P );

  private static final String TAG = "Cave3D";
  static String VERSION = "";

  private static final int REQUEST_OPEN_FILE = 1;
  static boolean mUseSplayVector = true;

  final static String APP_BASE_PATH = 
    ANDROID_10 ? Environment.getExternalStorageDirectory().getAbsolutePath()
               : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
  static String mAppBasePath  = APP_BASE_PATH;

  static int mCheckPerms = -1;

  // -----------------------------------------------------
  // PREFERENCES

  private SharedPreferences prefs;

  static int mSelectionRadius = 20;
  static int mTextSize        = 20;
  static int mButtonSize      = 1;
  static boolean mAllSplay    = true;
  static boolean mGridAbove   = false;
  static float   mMinClino    = 0;
  static boolean mPreprojection = true;
  static boolean mSplitTriangles = true;
  static boolean mSplitRandomize = true;
  static boolean mSplitStretch   = false;
  static float mSplitRandomizeDelta = 0.1f; // meters
  static float mSplitStretchDelta   = 0.1f;
  static float mPowercrustDelta     = 0.1f; // meters
  // static boolean mWallConvexHull = false;
  // static boolean mWallPowercrust = false;
  // static boolean mWallDelaunay   = false;
  // static boolean mWallHull       = false;

  static final String CAVE3D_BASE_PATH        = "CAVE3D_BASE_PATH";
  static final String CAVE3D_TEXT_SIZE        = "CAVE3D_TEXT_SIZE";
  static final String CAVE3D_BUTTON_SIZE      = "CAVE3D_BUTTON_SIZE";
  static final String CAVE3D_SELECTION_RADIUS = "CAVE3D_SELECTION_RADIUS";
  static final String CAVE3D_GRID_ABOVE       = "CAVE3D_GRID_ABOVE";
  static final String CAVE3D_NEG_CLINO        = "CAVE3D_NEG_CLINO";
  static final String CAVE3D_ALL_SPLAY        = "CAVE3D_ALL_SPLAY";
  static final String CAVE3D_PREPROJECTION    = "CAVE3D_PREPROJECTION";
  static final String CAVE3D_SPLIT_TRIANGLES  = "CAVE3D_SPLIT_TRIANGLES";
  static final String CAVE3D_SPLIT_RANDOM     = "CAVE3D_SPLIT_RANDOM";
  static final String CAVE3D_SPLIT_STRETCH    = "CAVE3D_SPLIT_STRETCH";
  // static final String CAVE3D_CONVEX_HULL      = "CAVE3D_CONVEX_HULL";
  // static final String CAVE3D_POWERCRUST       = "CAVE3D_POWERCRUST";
  static final String CAVE3D_POWERCRUST_DELTA = "CAVE3D_POWERCRUST_DELTA";
  // static final String CAVE3D_DELAUNAY         = "CAVE3D_DELAUNAY";
  // static final String CAVE3D_HULL             = "CAVE3D_HULL";

  public void onSharedPreferenceChanged( SharedPreferences sp, String k ) 
  {
    if ( k.equals( CAVE3D_BASE_PATH ) ) { 
      mAppBasePath = sp.getString( k, APP_BASE_PATH );
      // Log.v( TAG, "SharedPref change: path " + mAppBasePath );
      if ( mAppBasePath == null ) mAppBasePath = APP_BASE_PATH;
    } else if ( k.equals( CAVE3D_TEXT_SIZE ) ) {
      try {
        mTextSize = Integer.parseInt( sp.getString( k, "20" ) );
        Cave3DRenderer.setStationPaintTextSize( mTextSize );
      } catch ( NumberFormatException e ) {
      }
    } else if ( k.equals( CAVE3D_BUTTON_SIZE ) ) {
      try {
        mButtonSize = Integer.parseInt( sp.getString( k, "1" ) );
        resetButtonBar();
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
    } else if ( k.equals( CAVE3D_NEG_CLINO ) ) { 
      mMinClino = sp.getBoolean( k, false ) ? - Cave3DRenderer.PIOVERTWO : 0;
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
    } else if ( k.equals( CAVE3D_POWERCRUST_DELTA ) ) { 
      try {
        mPowercrustDelta = Float.parseFloat( sp.getString( k, "0.1" ) );
      } catch ( NumberFormatException e ) { }
      
    // } else if ( k.equals( CAVE3D_CONVEX_HULL ) ) { 
    //   mWallConvexHull = sp.getBoolean( k, true );
    // } else if ( k.equals( CAVE3D_POWERCRUST ) ) { 
    //   mWallPowercrust = sp.getBoolean( k, false );
    //   setMenuAdapter( getResources() );
    // } else if ( k.equals( CAVE3D_DELAUNAY ) ) { 
    //   mWallDelaunay = sp.getBoolean( k, false );
    // } else if ( k.equals( CAVE3D_HULL ) ) { 
    //   mWallHull = sp.getBoolean( k, false );
    }
  }

  private void loadPreferences( SharedPreferences sp )
  {
    float r;
    mAppBasePath = sp.getString( CAVE3D_BASE_PATH, APP_BASE_PATH );
    if ( mAppBasePath == null ) mAppBasePath = APP_BASE_PATH;
    try {
      mTextSize = Integer.parseInt( sp.getString( CAVE3D_TEXT_SIZE, "20" ) );
    } catch ( NumberFormatException e ) {
      mTextSize = 20;
    }
    try {
      mButtonSize = Integer.parseInt( sp.getString( CAVE3D_BUTTON_SIZE, "1" ) );
    } catch ( NumberFormatException e ) {
      mButtonSize = 1;
    }
    try {
      mSelectionRadius = Integer.parseInt( sp.getString( CAVE3D_SELECTION_RADIUS, "20" ) );
    } catch ( NumberFormatException e ) {
      mSelectionRadius = 20;
    }
    mGridAbove      = sp.getBoolean( CAVE3D_GRID_ABOVE, false );
    mMinClino       = sp.getBoolean( CAVE3D_NEG_CLINO, false ) ? - Cave3DRenderer.PIOVERTWO : 0;
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
    } catch ( NumberFormatException e ) {
      mSplitStretchDelta = 0.1f;
      mSplitStretch = true;
    }
    // mWallConvexHull = sp.getBoolean( CAVE3D_CONVEX_HULL, true );
    // mWallPowercrust = sp.getBoolean( CAVE3D_POWERCRUST,  false );
    // mWallDelaunay   = sp.getBoolean( CAVE3D_DELAUNAY,    false );
    // mWallHull       = sp.getBoolean( CAVE3D_HULL,        false );
  }

  // -----------------------------------------------------------

  String mFilename; // opened filename
  public static float mScaleFactor   = 1.0f;
  public static float mDisplayWidth  = 200f;
  public static float mDisplayHeight = 320f;

  private Cave3DView mView;
  // private DrawingSurface mDrawingSurface;
  private boolean mIsNotMultitouch;

  private Cave3DRenderer mRenderer;

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

  // ---------------------------------------------------------
  // DIMENSIONS

  static int setListViewHeight( Context context, HorizontalListView listView )
  {
    int size = getScaledSize( context );
    LayoutParams params = listView.getLayoutParams();
    params.height = size + 10;
    listView.setLayoutParams( params );
    return size;
  }

  // default button size
  static int getScaledSize( Context context )
  {
    return (int)( 42 * mButtonSize * context.getResources().getSystem().getDisplayMetrics().density );
  }

  static int getDefaultSize( Context context )
  {
    return (int)( 42 * context.getResources().getSystem().getDisplayMetrics().density );
  }

  boolean isMultitouch()
  {
    return getPackageManager().hasSystemFeature( PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH );
  }

  void toast( int r, boolean loong )
  {
    if ( loong ) {
      Toast.makeText( this, r, Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText( this, r, Toast.LENGTH_SHORT).show();
    }
  }
  void toast( int r ) { toast( r, false ); }

  void toast( int r, String str, boolean loong )
  {
    String msg = String.format( getResources().getString( r ), str );
    if ( loong ) {
      Toast.makeText( this, msg, Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText( this, msg, Toast.LENGTH_SHORT).show();
    }
  }
  void toast( int r, String str ) { toast( r, str, false ); }

  void toast( int r, int n1, int n2 )
  {
    String msg = String.format( getResources().getString( r ), n1, n2 );
    Toast.makeText( this, msg, Toast.LENGTH_SHORT).show();
  }

  // ---------------------------------------------------------------
  // MENU

  Button     mMenuImage;
  ListView   mMenu;
  MyMenuAdapter mMenuAdapter = null;
  boolean    onMenu = false;

  int menus[] = {
    R.string.menu_open,       // 0
    R.string.menu_export,
    R.string.menu_info,
    R.string.menu_ico,
    R.string.menu_rose,
    R.string.menu_viewpoint,  // 5
    R.string.menu_alpha,
    R.string.menu_reset,
    R.string.menu_wall,       // 7
    R.string.menu_options,
    // R.string.menu_fractal, // FRACTAL
    R.string.menu_help
  };

  void setMenuAdapter( Resources res )
  {
    int size = getScaledSize( this );
    MyButton.setButtonBackground( this, mMenuImage, size, R.drawable.iz_menu );

    // mMenuAdapter = new ArrayAdapter<String>(this, R.layout.menu );
    mMenuAdapter = new MyMenuAdapter( this, this, mMenu, R.layout.menu, new ArrayList< MyMenuItem >() );
    for ( int k=0; k<menus.length; ++k ) {
      mMenuAdapter.add( res.getString( menus[k] ) );
    }
    mMenu.setAdapter( mMenuAdapter );
    mMenu.invalidate();
  }

  private void closeMenu()
  {
    mMenu.setVisibility( View.GONE );
    mMenuAdapter.resetBgColor();
    onMenu = false;
  }

  private void handleMenu( int pos ) 
  {
    closeMenu();
    // Toast.makeText(this, item.toString(), Toast.LENGTH_SHORT).show();
    int p = 0;
    if ( p++ == pos ) { // OPEN
      openFile();
    } else if ( p++ == pos ) { // EXPORT
      new Cave3DExportDialog( this, this, mRenderer ).show();
    } else if ( p++ == pos ) { // INFO
      if ( mFilename != null ) (new Cave3DInfoDialog(this, mRenderer)).show();
    } else if ( p++ == pos ) { // ICO
      if ( mFilename != null ) (new Cave3DIcoDialog(this, mRenderer)).show();
    } else if ( p++ == pos ) { // ROSE
      if ( mFilename != null ) (new Cave3DRoseDialog(this, mRenderer)).show();
    } else if ( p++ == pos ) { // VIEWPOINT
      if ( mFilename != null ) {
        new Cave3DViewDialog( this, this, mRenderer ).show();
      }
    } else if ( p++ == pos ) { // SURFACE ALPHA
      (new Cave3DSurfaceAlphaDialog( this, this )).show();
    } else if ( p++ == pos ) { // RESET
      if ( mFilename != null ) mRenderer.resetGeometry();
    } else if ( p++ == pos ) { // DO_WALLS
      new Cave3DWallsDialog( this, this, mRenderer ).show();
    } else if ( p++ == pos ) { // OPTIONS
      startActivity( new Intent( this, Cave3DPreferences.class ) );
    // } else if ( p++ == pos ) { // FRACTAL
    //   new FractalDialog( this, this, mRenderer ).show();
    } else if ( p++ == pos ) { // HELP
      ( new HelpDialog(this) ).show();	    
    }
  }

  @Override 
  public void onItemClick(AdapterView<?> parent, View view, int pos, long id)
  {
    if ( mMenu == (ListView)parent ) { // MENU
      handleMenu( pos );
    }
  }

  // ---------------------------------------------------------
  // BUTTONS

  HorizontalListView mListView;
  HorizontalButtonView mButtonView1;
  MyButton mButton1[];
  static int mNrButton1 = 8;
  static int izons[] = {
    R.drawable.iz_move,
    R.drawable.iz_station,
    R.drawable.iz_splays,
    R.drawable.iz_wall_no,
    R.drawable.iz_planview_no,
    R.drawable.iz_surface_no,
    R.drawable.iz_color,
    R.drawable.iz_frame,
    // secondary bitmaps
    R.drawable.iz_wall,
    R.drawable.iz_planview,
    R.drawable.iz_surface,
    // R.drawable.iz_view
  };
  int BTN_MOVE     = 0;
  int BTN_WALL     = 3;
  int BTN_PLANVIEW = 4;
  int BTN_SURFACE  = 5;
  BitmapDrawable mBMmove;
  BitmapDrawable mBMturn;
  BitmapDrawable mBMhull;
  BitmapDrawable mBMdelaunay;
  BitmapDrawable mBMpowercrust;
  // BitmapDrawable mBMconvex;
  BitmapDrawable mBMwallNo;
  BitmapDrawable mBMplanviewNo;
  BitmapDrawable mBMsurfaceNo;
  BitmapDrawable mBMwall;
  BitmapDrawable mBMplanview;
  BitmapDrawable mBMsurface;

  private void resetButtonBar()
  {
    int size = setListViewHeight( this, mListView );
    mButton1 = new MyButton[ mNrButton1 ];
    mButton1[0] = new MyButton( this, this, size, izons[0], 0 );
    mButton1[1] = new MyButton( this, this, size, izons[1], 0 );
    mButton1[2] = new MyButton( this, this, size, izons[2], 0 );
    mButton1[3] = new MyButton( this, this, size, izons[3], 0 );
    mButton1[4] = new MyButton( this, this, size, izons[4], 0 );
    mButton1[5] = new MyButton( this, this, size, izons[5], 0 );
    mButton1[6] = new MyButton( this, this, size, izons[6], 0 );
    mButton1[7] = new MyButton( this, this, size, izons[7], 0 );

    mBMmove = mButton1[BTN_MOVE].mBitmap;
    mBMturn = MyButton.getButtonBackground( this, size, R.drawable.iz_turn );
    // mBMconvex = mButton1[BTN_WALL].mBitmap;

    mBMwallNo     = mButton1[ BTN_WALL     ].mBitmap;
    mBMplanviewNo = mButton1[ BTN_PLANVIEW ].mBitmap;
    mBMsurfaceNo  = mButton1[ BTN_SURFACE  ].mBitmap;
    mBMwall       = MyButton.getButtonBackground( this, size, R.drawable.iz_wall );
    mBMplanview   = MyButton.getButtonBackground( this, size, R.drawable.iz_planview );
    mBMsurface    = MyButton.getButtonBackground( this, size, R.drawable.iz_surface );

    setButtonSurface();
    setButtonWall();

    // mButtonView1 = new HorizontalImageButtonView( mButton1 );
    mButtonView1 = new HorizontalButtonView( mButton1 );
    mListView.setAdapter( mButtonView1.mAdapter );
  }

  void setButtonSurface()
  {
    if ( mRenderer == null ) {
      mButton1[ BTN_SURFACE ].setOnClickListener( null );
      return;
    }
    if ( mRenderer.hasSurface() ) {
      mButton1[ BTN_SURFACE ].setBackgroundDrawable( mBMsurface );
      mButton1[ BTN_SURFACE ].setOnClickListener( this );
    } else {
      mButton1[ BTN_SURFACE ].setBackgroundDrawable( mBMsurfaceNo );
      mButton1[ BTN_SURFACE ].setOnClickListener( null );
    }
  }

  void setButtonWall() 
  {
    if ( mRenderer == null ) {
      mButton1[ BTN_WALL ].setOnClickListener( null );
      mButton1[ BTN_PLANVIEW ].setOnClickListener( null );
      return;
    }
    if ( mRenderer.hasWall() ) {
      mButton1[ BTN_WALL ].setBackgroundDrawable( mBMwall );
      mButton1[ BTN_WALL ].setOnClickListener( this );
      if ( mRenderer.hasPlanview() ) {
        mButton1[ BTN_PLANVIEW ].setBackgroundDrawable( mBMplanview );
        mButton1[ BTN_PLANVIEW ].setOnClickListener( this );
      } else {
        mButton1[ BTN_PLANVIEW ].setBackgroundDrawable( mBMplanviewNo );
        mButton1[ BTN_PLANVIEW ].setOnClickListener( null );
      }
    } else {
      mButton1[ BTN_WALL ].setBackgroundDrawable( mBMwallNo );
      mButton1[ BTN_WALL ].setOnClickListener( null );
      mButton1[ BTN_PLANVIEW ].setBackgroundDrawable( mBMplanviewNo );
      mButton1[ BTN_PLANVIEW ].setOnClickListener( null );
    }
  }

  @Override
  public void onClick(View view)
  { 
    if ( onMenu ) {
      closeMenu();
      return;
    }
    Button b0 = (Button)view;
    if ( b0 == mMenuImage ) {
      if ( mMenu.getVisibility() == View.VISIBLE ) {
        mMenu.setVisibility( View.GONE );
        onMenu = false;
      } else {
        mMenu.setVisibility( View.VISIBLE );
        onMenu = true;
      }
      return;
    }

    int k1 = 0;
    if ( b0 == mButton1[k1++] ) { // MOVE - TURN
      setModeText( mode+1 );
    } else if ( b0 == mButton1[k1++] ) { // STATIONS
      if ( mRenderer != null ) mRenderer.toggleDoStations();
    } else if ( b0 == mButton1[k1++] ) { // SPLAYS
      if ( mRenderer != null ) mRenderer.toggleDoSplays();
    } else if ( b0 == mButton1[k1++] ) { // WALLS
      if ( mRenderer != null ) setWallButton( mRenderer.toggleWallMode() );
    } else if ( b0 == mButton1[k1++] ) { // PLANVIEW
      if ( mRenderer != null ) mRenderer.toggleDoPlanview();
    } else if ( b0 == mButton1[k1++] ) { // SURFACE
      if ( mRenderer != null ) mRenderer.toggleDoSurface();
    } else if ( b0 == mButton1[k1++] ) { // COLOR
      if ( mFilename != null ) mRenderer.toggleColorMode();
    } else if ( b0 == mButton1[k1++] ) { // FRAME
      if ( mFilename != null ) mRenderer.toggleFrameMode();
    // } else if ( b0 == mButton1[k1++] ) { // VIEWS
    //   if ( mFilename != null ) {
    //     new Cave3DViewDialog( this, this, mRenderer ).show();
    //   }
    }
  }

  // -------------------------------------------------------------------

  public void onActivityResult( int request, int result, Intent data ) 
  {
    switch ( request ) {
      case REQUEST_OPEN_FILE:
        if ( result == Activity.RESULT_OK ) {
          String filename = data.getExtras().getString( "com.topodroid.Cave3D.filename" );
          if ( filename != null && filename.length() > 0 ) {
            // Log.v( TAG, "path " + mAppBasePath + " file " + filename );
            doOpenFile( mAppBasePath + "/" + filename );
          }
        }
        break;
    }
  }

  void showTitle( double clino, double phi )
  {
    if ( mFilename != null ) {
      setTitle( String.format( getResources().getString(R.string.title), mFilename, -clino, 360-phi ) );
    } else {
      setTitle( "C A V E _ 3 D ");
    }
  }

  private boolean doOpenSurvey( String survey, String base )
  {
    mAppBasePath = base;
    mFilename = survey;
    boolean ret = mRenderer.initRendering( this, survey, base );
    // Log.v( TAG, "do open survey: " + (ret? "true" : "false" ) );
    return true;
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
          // modeBtn.setText( getResources().getString( R.string.btn_move ) );
          mButton1[BTN_MOVE].setBackgroundDrawable( mBMmove );
          break;
        case MODE_ROTATE:
          // modeBtn.setText( getResources().getString( R.string.btn_rotate ) );
          mButton1[BTN_MOVE].setBackgroundDrawable( mBMturn );
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
/*
    switch ( wall_mode ) {
      case Cave3DRenderer.WALL_HULL:
        Toast.makeText( this, "wall mode HULL", Toast.LENGTH_SHORT ).show();
        // mButton1[BTN_WALL].setBackgroundDrawable( mBMhull );
        break;
      case Cave3DRenderer.WALL_POWERCRUST:
        Toast.makeText( this, "wall mode POWERCRUST", Toast.LENGTH_SHORT ).show();
        // mButton1[BTN_WALL].setBackgroundDrawable( mBMpowercrust );
        break;
      case Cave3DRenderer.WALL_DELAUNAY:
        Toast.makeText( this, "wall mode DELAUNAY", Toast.LENGTH_SHORT ).show();
        // mButton1[BTN_WALL].setBackgroundDrawable( mBMdelaunay );
        break;
      case Cave3DRenderer.WALL_CW:
        Toast.makeText( this, "wall mode CONVEX", Toast.LENGTH_SHORT ).show();
        // mButton1[BTN_WALL].setBackgroundDrawable( mBMconvex );
        break;
      default:
        Toast.makeText( this, "wall mode NONE", Toast.LENGTH_SHORT ).show();
    }
*/
  }

  @Override
  public void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );
    // Log.v( TAG, "Cave3D::onCreate");
    try {
      VERSION = getPackageManager().getPackageInfo( getPackageName(), 0 ).versionName;
    } catch ( NameNotFoundException e ) {
      e.printStackTrace(); // FIXME
    }
    FeatureChecker.createPermissions( this, this );

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
    // Log.v( TAG, "display " + mDisplayWidth + " " + mDisplayHeight + " scale " + mScaleFactor + " density " + density );
    
    mListView = (HorizontalListView) findViewById(R.id.listview);
    resetButtonBar();

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
      mView.setZoomControl( mZoomBtnsCtrl, ZOOM_Y, mIsNotMultitouch );
    }

    // mView.setOnTouchListener(this);
    // mView.setOnLongClickListener(this);
    // mView.setBuiltInZoomControls(true);

    // Log.v( TAG, "Cave3D::onCreate view is " + ( (mView==null)? "null" : mView.toString() ) );
    mRenderer = mView.getRenderer();
    
    setModeText( MODE_ROTATE );
    setWallButton( mRenderer.wall_mode );

    mMenuImage = (Button) findViewById( R.id.handle );
    mMenuImage.setOnClickListener( this );
    mMenu = (ListView) findViewById( R.id.menu );
    mMenuAdapter = null;
    setMenuAdapter( getResources() );
    closeMenu();

    mCheckPerms = FeatureChecker.checkPermissions( this );
    Cave3DPerms perms_dialog = null;
    if ( mCheckPerms != 0 ) {
      perms_dialog = new Cave3DPerms( this, mCheckPerms );
      perms_dialog.show();
    }

    if ( mCheckPerms >= 0 ) {
      boolean file_dialog = true;
      Bundle extras = getIntent().getExtras();
      if ( extras != null ) {
        // the uri string is the absolute basepath
        // String uri_str = extras.getString( "BASE_URI" );
        // Uri uri = Uri.parse( uri_str );
        // Log.v("DistoX-URI", "Cave3D " + uri.toString() );

        String name = extras.getString( "INPUT_FILE" );
        // Log.v( "Cave3D-EXTRA", "TopoDroid filename " + name );
        if ( name != null ) {
          if ( doOpenFile( name ) ) {
            file_dialog = false;
          } else {
            Log.e( TAG, "Cannot open input file " + name );
          }
        } else {
          name = extras.getString( "INPUT_SURVEY" );
          String base = extras.getString( "SURVEY_BASE" );
          // Log.v( "Cave3D-EXTRA", "open input survey " + name + " base " + base );
          if ( name != null ) {
            if ( doOpenSurvey( name, base ) ) {
              file_dialog = false;
            } else {
              Log.e( TAG, "Cannot open input survey " + name );
            }
          } else {
            Log.e( TAG, "No input file or survey");
          }
        }          
      }
      if ( file_dialog ) { 
        openFile();
      }
    } else {
      Log.e( "Cave3D-PARM", "finishing activity ... perms " + mCheckPerms );
      if ( perms_dialog != null ) perms_dialog.dismiss();
      finish();
    }
  }

  void refresh() { mView.refresh(); }

  @Override
  protected synchronized void onPause() 
  { 
    if ( mIsNotMultitouch ) mZoomBtnsCtrl.setVisible(false);
    // mZoomBtnsCtrl.setVisible(false);
    // Log.v("Cave3D", "on pause" );
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Log.v("Cave3D", "on resume" );
  }


  /* FIXME-23 */
  @Override
  public void onRequestPermissionsResult( int code, final String[] perms, int[] results )
  {
    // Log.v( "Cave3D-PERM", "req code " + code + " results length " + results.length );
    if ( code == FeatureChecker.REQUEST_PERMISSIONS ) {
      if ( results.length > 0 ) {
	for ( int k = 0; k < results.length; ++ k ) {
	  FeatureChecker.GrantedPermission[k] = ( results[k] == PackageManager.PERMISSION_GRANTED );
	  // Log.v( "Cave3D-PERM", "perm " + k + " perms " + perms[k] + " result " + results[k] );
	}
      }
    }
    // Log.v( "Cave3D-PERM", "must restart " + FeatureChecker.MustRestart );
    // if ( ! FeatureChecker.MustRestart ) {
    //   Cave3DAlertDialog.makeAlert( this, getResources(), R.string.perm_required,
    //     new DialogInterface.OnClickListener() {
    //       @Override public void onClick( DialogInterface dialog, int btn ) { finish(); }
    //     }
    //   );
    // }
  }
  /* */

  boolean getSurfaceLegs() { return mRenderer.getSurfaceLegs(); }
  void    setSurfaceLegs( boolean show ) { mRenderer.toggleSurfaceLegs( show ); }

}
