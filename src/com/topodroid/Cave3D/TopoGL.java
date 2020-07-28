/** @file TopoGL.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief 3D Topo-GL activity
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;

import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.graphics.RectF;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Menu;
import android.view.MenuItem;
// import android.view.MotionEvent;

import android.widget.Toast;
import android.widget.Button;
import android.widget.ListView;
import android.widget.CheckBox;
// import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.SharedPreferences.Editor;

import android.util.DisplayMetrics;
import android.graphics.drawable.BitmapDrawable;

import android.os.AsyncTask;

import android.net.Uri;

import android.opengl.GLSurfaceView;

public class TopoGL extends Activity 
                    implements OnClickListener
                    , OnLongClickListener
                    , OnItemClickListener
                    , OnSharedPreferenceChangeListener
                    , GPS.GPSListener
{
  // android P (9) is API 28
  final static boolean NOT_ANDROID_10 = ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.P );
  final static boolean NOT_ANDROID_11 = ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q );
  static String VERSION = "";

  // private static final int REQUEST_OPEN_FILE = 1;


  static String EXTERNAL_STORAGE_PATH =  // app base path
    NOT_ANDROID_11 ? Environment.getExternalStorageDirectory().getAbsolutePath()
                   : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
                   // : "/sdcard";
                   // : null; 

  static String HOME_PATH = EXTERNAL_STORAGE_PATH;
                          // "/sdcard/Android/data/com.topodroid.Cave3D/files";
  static String mAppBasePath = HOME_PATH;
  static String SYMBOL_PATH = EXTERNAL_STORAGE_PATH + "/TopoDroid/symbol/point";
  static String C3D_PATH    = EXTERNAL_STORAGE_PATH + "/TopoDroid/c3d";

  boolean doSketches = false;

  private BitmapDrawable mBMmeasureOn;
  private BitmapDrawable mBMmeasureOff;
  private BitmapDrawable mBMfixOn;;
  private BitmapDrawable mBMfixOff;

  // reset app base path
  void checkAppBasePath()
  {
    if ( EXTERNAL_STORAGE_PATH == null ) {
      EXTERNAL_STORAGE_PATH = getExternalFilesDir( null ).getPath();
    }
    mAppBasePath = EXTERNAL_STORAGE_PATH;
    // Log.v("TopoGL", "use base path " + mAppBasePath );
  }

  static int mCheckPerms = -1;

  // ---------------------------------
  String mFilename; // opened filename
  public static float mScaleFactor   = 1.0f;
  public static float mDisplayWidth  = 200f;
  public static float mDisplayHeight = 320f;

  private boolean mIsNotMultitouch;

  private boolean supportsES2 = false;

  String mDEMname = null;
  String mTextureName = null;

  static boolean mSelectStation = true;
  static boolean mHasC3d = false;

  // --------------------------------- OpenGL stuff
  private GlSurfaceView glSurfaceView;
  private GlRenderer mRenderer = null;

  private LinearLayout mLayout;
  // private TextView     mText;
  private boolean rendererSet = false;
  private TglParser mParser = null;

  private LinearLayout mLayoutStation;
  private Button mCurrentStation;
  Button mMeasureStation;
  Button mFixStation;
  boolean isMeasuring = false;
  boolean isFixed = false;

  // used also by DialogSurface
  boolean hasSurface() { return ( mRenderer != null ) && mRenderer.hasSurface(); }

  boolean withOsm() { return mParser != null && mParser.hasOrigin(); }

  GPS mGPS = null;

  // ---------------------------------------------------------------
  // LIFECYCLE

  @Override
  public void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
    // Log.v( "TopoGL", "on create: Not Android 10 " + NOT_ANDROID_10 + " 11 " + NOT_ANDROID_11 );

    checkPermissions();

    setContentView( R.layout.main );
    mLayout = (LinearLayout) findViewById( R.id.view_layout );
    // mText   = (TextView) findViewById( R.id.text );

    DisplayMetrics dm = getResources().getDisplayMetrics();
    float density  = dm.density;
    mDisplayWidth  = dm.widthPixels;
    mDisplayHeight = dm.heightPixels;
    mScaleFactor   = (mDisplayHeight / 320.0f) * density;
    // Log.v( "TopoGL", "display " + mDisplayWidth + " " + mDisplayHeight + " scale " + mScaleFactor + " density " + density );

    GlModel.setWidthAndHeight( mDisplayWidth, mDisplayHeight );
    
    mListView = (HorizontalListView) findViewById(R.id.listview);
    resetButtonBar();

    mLayoutStation = (LinearLayout) findViewById( R.id.layout_station );
    mCurrentStation = (Button) findViewById( R.id.current_station );
    mCurrentStation.setOnClickListener( this );
    // mCurrentStation.setOnLongClickListener( this );

    mMeasureStation = (Button) findViewById( R.id.measure_station );
    mFixStation = (Button) findViewById( R.id.fix_station );
    mMeasureStation.setOnClickListener( this );
    mFixStation.setOnClickListener( this );

    mLayoutStation.setVisibility( View.GONE );
    
    // setWallButton( mRenderer.wall_mode );

    mMenuImage = (Button) findViewById( R.id.handle );
    mMenuImage.setOnClickListener( this );
    mMenu = (ListView) findViewById( R.id.menu );
    mMenuAdapter = null;
    setMenuAdapter( getResources() );
    closeMenu();

    // glSurfaceView = (GLSurfaceView) findViewById( R.id.view );

    // setContentView(glSurfaceView);
    // Log.v("TopoGL", "on create mid");
    mParser = null; // new TglParser( this, filename );


    if ( mCheckPerms >= 0 ) {
      // Log.v("TopoGL", "check perms" );
      boolean file_dialog = true;
      Bundle extras = getIntent().getExtras();
      if ( extras != null ) {
        // the uri string is the absolute basepath
        // String uri_str = extras.getString( "BASE_URI" );
        // Uri uri = Uri.parse( uri_str );
        // Log.v("DistoX-URI", "Cave3D " + uri.toString() );

        String name = extras.getString( "INPUT_FILE" );
        if ( name != null ) { // used by TdManager
          // Log.v( "TopoGL-EXTRA", "TopoDroid filename " + name );
          file_dialog = false;
          doOpenFile( name, true ); // asynch
        } else {
          name = extras.getString( "INPUT_SURVEY" );
          String base = extras.getString( "SURVEY_BASE" );
          if ( name != null ) {
            // Log.v( "TopoGL-EXTRA", "open input survey " + name + " base " + base );
            if ( doOpenSurvey( name, base ) ) {
              doSketches = true;
              file_dialog = false;
            } else {
              Log.e( "TopoGL", "Cannot open input survey " + name );
            }
          } else {
            Log.e( "TopoGL", "No input file or survey");
          }
        }          
      }

      mGPS = new GPS( this );

      if ( file_dialog ) { 
        // Log.v("TopoGL", "open file dialog");
        (new DialogOpenFile( this, this )).show();
        // openFile();
      }
    } else {
      Log.e( "TopoGL-PERM", "finishing activity ... perms " + mCheckPerms );
      // if ( perms_dialog != null ) perms_dialog.dismiss();
      finish();
    }
    // Log.v("TopoGL", "on create mid");
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    if ( rendererSet ) {
      // Log.v("TopoGL", "on pause");
      if ( glSurfaceView != null ) glSurfaceView.onPause();
      if ( mRenderer != null ) mRenderer.unbindTextures();
    }
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    // Log.v("TopoGL", "on start");
    makeSurface();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    if ( rendererSet ) {
      // Log.v("TopoGL", "on resume");
      // glSurfaceView.setMinimumWidth( mLayout.getWidth() );
      // glSurfaceView.setMinimumHeight( mLayout.getHeight() );
      glSurfaceView.onResume();
      mRenderer.rebindTextures();
      // mRenderer.onSurfaceChanged( null, glSurfaceView.getWidth(), glSurfaceView.getHeight() );
      // glSurfaceView.requestRender();
    }
  }

  // -----------------------------------------------------------------
  // BACK PRESSED

  private boolean doubleBack = false;
  private Handler doubleBackHandler = new Handler();
  private Toast   doubleBackToast = null;

  private final Runnable doubleBackRunnable = new Runnable() {
    @Override 
    public void run() {
      doubleBack = false;
      if ( doubleBackToast != null ) doubleBackToast.cancel();
      doubleBackToast = null;
    }
  };

  @Override
  public void onBackPressed () // askClose
  {
    // TDLog.Log( TDLog.LOG_INPUT, "MainWindow onBackPressed()" );
    if ( onMenu ) {
      closeMenu();
      return;
    }
    if ( doubleBack ) {
      if ( doubleBackToast != null ) doubleBackToast.cancel();
      doubleBackToast = null;
      super.onBackPressed();
      return;
    }
    doubleBack = true;
    doubleBackToast = Toast.makeText( this, R.string.double_back, Toast.LENGTH_SHORT );
    doubleBackToast.show();
    doubleBackHandler.postDelayed( doubleBackRunnable, 1000 );
  }

  // ----------------------------------------------------------------

  void makeParser( String filename )
  {
    // Log.v("TopoGL", "parser " + filename );
    mParser = new TglParser( this, filename );
    if ( mRenderer != null ) mRenderer.setParser( mParser );
  }

  void setTheTitle( String str ) { setTitle( str ); }

  private void makeSurface()
  {
    if ( glSurfaceView != null ) return;
    // Log.v("TopoGL", "make surface");
    glSurfaceView = new GlSurfaceView(this, this);
    glSurfaceView.setMinimumWidth( mLayout.getWidth() );
    glSurfaceView.setMinimumHeight( mLayout.getHeight() );
    mLayout.addView( glSurfaceView );
    mLayout.invalidate();

    // Check if the system supports OpenGL ES 2.0.
    final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

    final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
    /*
    final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
     */
    // Even though the latest emulator supports OpenGL ES 2.0,
    // it has a bug where it doesn't set the reqGlEsVersion so
    // the above check doesn't work. The below will detect if the
    // app is running on an emulator, and assume that it supports
    // OpenGL ES 2.0.
    final boolean supportsEs2 =
        configurationInfo.reqGlEsVersion >= 0x20000
            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
             && (Build.FINGERPRINT.startsWith("generic")
              || Build.FINGERPRINT.startsWith("unknown")
              || Build.MODEL.contains("google_sdk")
              || Build.MODEL.contains("Emulator")
              || Build.MODEL.contains("Android SDK built for x86")));

    if (supportsEs2) {
        // toast("This device supports OpenGL ES 2.0.", true );
        // Request an OpenGL ES 2.0 compatible context.
        glSurfaceView.setEGLContextClientVersion(2);

        // Assign our renderer
        if ( mRenderer == null ) {
          GlModel model = new GlModel( this );
          mRenderer = new GlRenderer( this, model );
          if ( mParser != null ) mRenderer.setParser( mParser );
        }
        glSurfaceView.setRenderer( mRenderer );
        // glSurfaceView.setRenderMode( GLSurfaceView.RENDERMODE_WHEN_DIRTY );
        rendererSet = true;
    } else {
        /*
         * This is where you could create an OpenGL ES 1.x compatible
         * renderer if you wanted to support both ES 1 and ES 2. Since we're
         * not doing anything, the app will crash if the device doesn't
         * support OpenGL ES 2.0. If we publish on the market, we should
         * also add the following to AndroidManifest.xml:
         * 
         * <uses-feature android:glEsVersion="0x00020000"
         * android:required="true" />
         * 
         * This hides our app from those devices which don't support OpenGL
         * ES 2.0.
         */
        toast("This device does not support OpenGL ES 2.0.", true );
        return;
    }
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
    R.string.menu_reset,
    R.string.menu_viewpoint,  // 6
    R.string.menu_alpha,
    R.string.menu_wall,       // 8
    R.string.menu_sketch,     // 9
    R.string.menu_options,
    // R.string.menu_fractal, // FRACTAL
    R.string.menu_help
  };

  void setMenuAdapter( Resources res )
  {
    mHasC3d = new File( C3D_PATH ).exists();

    int size = getScaledSize( this );
    MyButton.setButtonBackground( this, mMenuImage, size, R.drawable.iz_menu );
    mMenuAdapter = new MyMenuAdapter( this, this, mMenu, R.layout.menu, new ArrayList< MyMenuItem >() );
    for ( int k=0; k<menus.length; ++k ) {
      if ( k == 9 && ! mHasC3d ) continue;
      mMenuAdapter.add( res.getString( menus[k] ) );
    }
    mMenu.setAdapter( mMenuAdapter );
    mMenu.invalidate();
  }

  // used by GlSurfaceView
  void closeMenu()
  {
    mMenu.setVisibility( View.GONE );
    mMenuAdapter.resetBgColor();
    onMenu = false;
  }

  private void handleMenu( int pos ) 
  {
    closeMenu();
    // toast(item.toString() );
    int p = 0;
    if ( p++ == pos ) { // OPEN
      (new DialogOpenFile( this, this )).show();
      // openFile();
    } else if ( p++ == pos ) { // EXPORT
      if ( mParser != null ) {
        new DialogExport( this, this, mParser ).show();
      } else {
        Toast.makeText( this, R.string.no_model, Toast.LENGTH_SHORT ).show();
      }
    } else if ( p++ == pos ) { // INFO
      if ( mParser != null ) {
        new DialogInfo(this, mParser, mRenderer).show();
      } else {
        Toast.makeText( this, R.string.no_model, Toast.LENGTH_SHORT ).show();
      }
    } else if ( p++ == pos ) { // ICO
      if ( mParser != null ) {
        new DialogIco(this, mParser).show();
      } else {
        Toast.makeText( this, R.string.no_model, Toast.LENGTH_SHORT ).show();
      }
    } else if ( p++ == pos ) { // ROSE
      if ( mParser != null ) {
        new DialogRose(this, mParser).show();
      } else {
        Toast.makeText( this, R.string.no_model, Toast.LENGTH_SHORT ).show();
      }
    } else if ( p++ == pos ) { // RESET
      GlModel.resetModes();
      GlNames.resetStations();
      if ( mRenderer != null ) mRenderer.resetTopGeometry();
      mSelectStation = true;
      resetButtons();
    } else if ( p++ == pos ) { // VIEWPOINT
      if ( mParser != null ) {
        if ( mRenderer != null ) new DialogView( this, this, mRenderer ).show();
      } else {
        Toast.makeText( this, R.string.no_model, Toast.LENGTH_SHORT ).show();
      }
    } else if ( p++ == pos ) { // SURFACE ALPHA
      if ( mParser != null ) {
        new DialogSurface( this, this ).show();
      } else {
        Toast.makeText( this, R.string.no_model, Toast.LENGTH_SHORT ).show();
      }
    } else if ( p++ == pos ) { // DO_WALLS
      if ( mParser != null ) {
        new DialogWalls( this, this, mParser ).show();
      } else {
        Toast.makeText( this, R.string.no_model, Toast.LENGTH_SHORT ).show();
      }
    } else if ( mHasC3d && p++ == pos ) { // SKETCH
      if ( mParser != null ) {
        if ( doSketches ) {
          if ( mRenderer != null ) new DialogSketches( this, this, mRenderer ).show();
        } else {
          Toast.makeText( this, R.string.no_topodroid_model, Toast.LENGTH_SHORT ).show();
        }
      } else {
        Toast.makeText( this, R.string.no_model, Toast.LENGTH_SHORT ).show();
      }
    } else if ( p++ == pos ) { // OPTIONS
      startActivity( new Intent( this, TopoGLPreferences.class ) );
    // } else if ( p++ == pos ) { // FRACTAL
    //   new FractalDialog( this, this, mRenderer ).show();
    } else if ( p++ == pos ) { // HELP
      new DialogHelp(this).show();	    
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

  HorizontalListView mListView = null;
  HorizontalButtonView mButtonView1;
  MyButton mButton1[];
  static int mNrButton1 = 8;
  static int izons[] = {
    R.drawable.iz_light,
    R.drawable.iz_orthogonal,
    R.drawable.iz_station_no_dot,
    R.drawable.iz_splays_no,
    R.drawable.iz_wall_no,
    R.drawable.iz_surface_no,
    R.drawable.iz_color,
    R.drawable.iz_frame_grid,
    // secondary bitmaps
    R.drawable.iz_wall,
    R.drawable.iz_perspective,
    R.drawable.iz_surface,
    // R.drawable.iz_view
  };
  int BTN_MOVE     = 0;
  int BTN_PROJECT  = 1;
  int BTN_STATION  = 2;
  int BTN_SPLAYS   = 3;
  int BTN_WALL     = 4;
  int BTN_SURFACE  = 5;
  int BTN_COLOR    = 6;
  int BTN_FRAME    = 7;
  BitmapDrawable mBMlight;
  BitmapDrawable mBMmove;
  BitmapDrawable mBMturn;
  BitmapDrawable mBMhull;

  BitmapDrawable mBMdelaunay;
  BitmapDrawable mBMpowercrust;
  // BitmapDrawable mBMconvex;
  BitmapDrawable mBMwallNo;
  BitmapDrawable mBMsurfaceNo;
  BitmapDrawable mBMwall;
  BitmapDrawable mBMperspective;
  BitmapDrawable mBMorthogonal;
  BitmapDrawable mBMsurface;

  BitmapDrawable mBMstationNo;
  BitmapDrawable mBMstationPoint;
  BitmapDrawable mBMstationName;
  // BitmapDrawable mBMstation;
  BitmapDrawable mBMstationNoDot;
  BitmapDrawable mBMstationPointDot;
  BitmapDrawable mBMstationNameDot;
  // BitmapDrawable mBMstationDot;

  BitmapDrawable mBMsplaysNo;
  BitmapDrawable mBMsplaysLine;
  BitmapDrawable mBMsplaysPoint;
  // BitmapDrawable mBMsplays;

  BitmapDrawable mBMcolorNo;
  BitmapDrawable mBMcolorSurvey;
  BitmapDrawable mBMcolorDepth;
  BitmapDrawable mBMcolorSurface;

  BitmapDrawable mBMframeNo;
  BitmapDrawable mBMframeGrid;
  BitmapDrawable mBMframeAxes;


  private void resetButtonBar()
  {
    if ( mListView == null ) return;
    int size = setListViewHeight( this, mListView );
    mButton1 = new MyButton[ mNrButton1 ];
    mButton1[0] = new MyButton( this, this, size, izons[0] );
    mButton1[1] = new MyButton( this, this, size, izons[1] );
    mButton1[2] = new MyButton( this, this, size, izons[2] );
    mButton1[3] = new MyButton( this, this, size, izons[3] );
    mButton1[4] = new MyButton( this, this, size, izons[4] );
    mButton1[5] = new MyButton( this, this, size, izons[5] );
    mButton1[6] = new MyButton( this, this, size, izons[6] );
    mButton1[7] = new MyButton( this, this, size, izons[7] );

    // mButton1[ 0 ].setOnLongClickListener( this );
    mButton1[ 1 ].setOnLongClickListener( this ); // projection params
    mButton1[ 2 ].setOnLongClickListener( this ); // stations
    mButton1[ 3 ].setOnLongClickListener( this ); // splays
    mButton1[ 6 ].setOnLongClickListener( this ); // surveys

    mBMlight = mButton1[BTN_MOVE].mBitmap;
    mBMturn = MyButton.getButtonBackground( this, size, R.drawable.iz_turn );
    mBMmove = MyButton.getButtonBackground( this, size, R.drawable.iz_move );
    // mBMconvex = mButton1[BTN_WALL].mBitmap;

    mBMorthogonal  = mButton1[ BTN_PROJECT  ].mBitmap;
    mBMperspective = MyButton.getButtonBackground( this, size, R.drawable.iz_perspective);

    mBMstationNoDot   = mButton1[ BTN_STATION ].mBitmap;
    mBMstationPointDot= MyButton.getButtonBackground( this, size, R.drawable.iz_station_point_dot );
    mBMstationNameDot = MyButton.getButtonBackground( this, size, R.drawable.iz_station_name_dot );
    // mBMstationDot     = MyButton.getButtonBackground( this, size, R.drawable.iz_station_dot );

    mBMstationNo   = MyButton.getButtonBackground( this, size, R.drawable.iz_station_no );
    mBMstationPoint= MyButton.getButtonBackground( this, size, R.drawable.iz_station_point );
    mBMstationName = MyButton.getButtonBackground( this, size, R.drawable.iz_station_name );
    // mBMstation     = MyButton.getButtonBackground( this, size, R.drawable.iz_station );

    mBMsplaysNo    = mButton1[ BTN_SPLAYS  ].mBitmap;
    mBMsplaysLine  = MyButton.getButtonBackground( this, size, R.drawable.iz_splays_line );
    mBMsplaysPoint = MyButton.getButtonBackground( this, size, R.drawable.iz_splays_point );

    mBMwallNo      = mButton1[ BTN_WALL     ].mBitmap;
    mBMwall        = MyButton.getButtonBackground( this, size, R.drawable.iz_wall );

    mBMsurfaceNo   = mButton1[ BTN_SURFACE  ].mBitmap;
    mBMsurface     = MyButton.getButtonBackground( this, size, R.drawable.iz_surface );

    mBMcolorNo     = mButton1[ BTN_COLOR   ].mBitmap;
    mBMcolorSurvey = MyButton.getButtonBackground( this, size, R.drawable.iz_color_survey );
    mBMcolorDepth  = MyButton.getButtonBackground( this, size, R.drawable.iz_color_depth );
    mBMcolorSurface= MyButton.getButtonBackground( this, size, R.drawable.iz_color_surface );

    mBMframeGrid   = mButton1[ BTN_FRAME   ].mBitmap;
    mBMframeNo     = MyButton.getButtonBackground( this, size, R.drawable.iz_frame_no );
    mBMframeAxes   = MyButton.getButtonBackground( this, size, R.drawable.iz_frame_axes );

    // mButtonView1 = new HorizontalImageButtonView( mButton1 );
    mButtonView1 = new HorizontalButtonView( mButton1 );
    mListView.setAdapter( mButtonView1.mAdapter );

    resetButtons();

    // mButton1[ BTN_WALL ].setOnClickListener( this );
    // mButton1[ BTN_SURFACE ].setOnClickListener( this );

    mBMmeasureOn  = MyButton.getButtonBackground( this, size, R.drawable.iz_measure_on );
    mBMmeasureOff = MyButton.getButtonBackground( this, size, R.drawable.iz_measure_off );
    mBMfixOn      = MyButton.getButtonBackground( this, size, R.drawable.iz_station_on );
    mBMfixOff     = MyButton.getButtonBackground( this, size, R.drawable.iz_station_off );
  }

  // only for BTN_STATION
  @Override 
  public boolean onLongClick( View v ) 
  {
    // if ( v.getId() == R.id.current_station ) {
    //   centerAtCurrentStation();
    //   return true;
    // }

    Button b = (Button) v;
    if ( b == mButton1[ BTN_PROJECT ] ) {
      if ( mRenderer.projectionMode != GlRenderer.PROJ_PERSPECTIVE ) return false;
      new DialogProjection( this, mRenderer ).show();
    } else if ( b == mButton1[ BTN_STATION ] ) {
      mSelectStation = ! mSelectStation;
      // Log.v("TopoGL", "on long click " + mSelectStation );
      setButtonStation();
      closeCurrentStation();
    } else if ( b == mButton1[ BTN_SPLAYS ] ) {
      new DialogLegs( this ).show();
    } else if ( b == mButton1[ BTN_COLOR ] ) {
      if ( mParser == null || mParser.getSurveyNumber() < 2 ) return false;
      new DialogSurveys( this, this, mParser.getSurveys() ).show();
    }
    return true;
  }

  private void resetButtons()
  {
    setButtonProjection();
    setButtonSurface();
    setButtonWall();
    setButtonMove();
    setButtonStation();
    setButtonSplays();
    setButtonColor();
    setButtonFrame();
  }

  private void setButtonStation()
  {
    if ( mSelectStation ) {
      switch ( GlNames.stationMode ) {
        case GlNames.STATION_NONE:
          mButton1[ BTN_STATION ].setBackgroundDrawable( mBMstationNoDot );
          break;
        case GlNames.STATION_POINT:
          mButton1[ BTN_STATION ].setBackgroundDrawable( mBMstationPointDot );
          break;
        case GlNames.STATION_NAME:
          mButton1[ BTN_STATION ].setBackgroundDrawable( mBMstationNameDot );
          break;
        // case GlNames.STATION_ALL:
        //   mButton1[ BTN_STATION ].setBackgroundDrawable( mBMstationDot );
        //   break;
      }
    } else {
      switch ( GlNames.stationMode ) {
        case GlNames.STATION_NONE:
          mButton1[ BTN_STATION ].setBackgroundDrawable( mBMstationNo );
          break;
        case GlNames.STATION_POINT:
          mButton1[ BTN_STATION ].setBackgroundDrawable( mBMstationPoint );
          break;
        case GlNames.STATION_NAME:
          mButton1[ BTN_STATION ].setBackgroundDrawable( mBMstationName );
          break;
        // case GlNames.STATION_ALL:
        //   mButton1[ BTN_STATION ].setBackgroundDrawable( mBMstation );
        //   break;
      }
    }
  }

  private void setButtonFrame()
  {
    if ( mRenderer == null ) return;
    switch ( GlModel.frameMode ) {
      case GlModel.FRAME_NONE:
        mButton1[ BTN_FRAME ].setBackgroundDrawable( mBMframeNo );
        break;
      case GlModel.FRAME_GRID:
        mButton1[ BTN_FRAME ].setBackgroundDrawable( mBMframeGrid );
        break;
      case GlModel.FRAME_AXES:
        mButton1[ BTN_FRAME ].setBackgroundDrawable( mBMframeAxes );
        break;
    }
  }

  private void setButtonColor()
  {
    if ( mRenderer == null ) return;
    switch ( mRenderer.getColorMode() ) {
      case GlLines.COLOR_NONE:
        mButton1[ BTN_COLOR ].setBackgroundDrawable( mBMcolorNo );
        break;
      case GlLines.COLOR_SURVEY:
        mButton1[ BTN_COLOR ].setBackgroundDrawable( mBMcolorSurvey );
        break;
      case GlLines.COLOR_DEPTH:
        mButton1[ BTN_COLOR ].setBackgroundDrawable( mBMcolorDepth );
        break;
      case GlLines.COLOR_SURFACE:
        mButton1[ BTN_COLOR ].setBackgroundDrawable( mBMcolorSurface );
        break;
    }
  }

  private void setButtonSplays()
  {
    switch ( GlModel.splayMode ) {
      case GlModel.DRAW_NONE:
        mButton1[ BTN_SPLAYS ].setBackgroundDrawable( mBMsplaysNo );
        break;
      case GlModel.DRAW_LINE:
        mButton1[ BTN_SPLAYS ].setBackgroundDrawable( mBMsplaysLine );
        break;
      case GlModel.DRAW_POINT:
        mButton1[ BTN_SPLAYS ].setBackgroundDrawable( mBMsplaysPoint );
        break;
    }
  }

  private void setButtonSurface()
  {
    mButton1[ BTN_SURFACE ].setBackgroundDrawable( GlModel.surfaceMode ? mBMsurface : mBMsurfaceNo );
  }

  private void setButtonWall() 
  {
    mButton1[ BTN_WALL ].setBackgroundDrawable( GlModel.wallMode ? mBMwall : mBMwallNo );
  }

  private void setButtonProjection()
  {
    mButton1[ BTN_PROJECT ].setBackgroundDrawable( 
     ( mRenderer != null && mRenderer.projectionMode == GlRenderer.PROJ_PERSPECTIVE )? mBMperspective : mBMorthogonal );
  }

  private void setButtonMove()
  {
    if ( GlSurfaceView.mLightMode ) {
      if ( hasSurface() ) {
        mButton1[BTN_MOVE].setBackgroundDrawable( mBMlight );
      } else {
        mButton1[BTN_MOVE].setBackgroundDrawable( mBMmove );
      }
    } else { // MODE_ROTATE:
      mButton1[BTN_MOVE].setBackgroundDrawable( mBMturn );
    }
  }

  void showCurrentStation( String text )
  {
    mCurrentStation.setText( text );
    mLayoutStation.setVisibility( View.VISIBLE );
    isMeasuring = false;
    isFixed = false;
    mMeasureStation.setBackground( mBMmeasureOff );
    mFixStation.setBackground( mBMfixOff );
  }
 
  void closeCurrentStation()
  {
    mLayoutStation.setVisibility( View.GONE );
    isMeasuring = false;
    isFixed = false;
    if ( mRenderer != null ) mRenderer.clearStationHighlight();
    if ( mParser   != null ) mParser.clearStartStation();
    GlNames.setHLcolorG( 0.0f );
  }

  boolean centerAtCurrentStation( )
  {
    boolean res = false;
    if ( mRenderer != null ) res = mRenderer.setCenter();
    // Toast.makeText( this, res ? R.string.center_set : R.string.center_clear, Toast.LENGTH_SHORT ).show();
    GlNames.setHLcolorG( res ? 0.5f : 0.0f );
    return res;
  }

  @Override
  public void onClick(View view)
  { 
    if ( onMenu ) {
      closeMenu();
      return;
    }
    int id = view.getId();
    if ( id == R.id.handle ) {
      if ( mMenu.getVisibility() == View.VISIBLE ) {
        mMenu.setVisibility( View.GONE );
        onMenu = false;
      } else {
        mMenu.setVisibility( View.VISIBLE );
        onMenu = true;
      }
      return;
    } 
    if ( id == R.id.current_station ) {
      closeCurrentStation();
      return;
    }

    Button b0 = (Button)view;
    int k1 = 0;
    if ( b0 == mButton1[k1++] ) { // MOVE - TURN
      GlSurfaceView.toggleLightMode();
      setButtonMove();
    } else if ( b0 == mButton1[k1++] ) { // PROJECTION
      if ( mRenderer != null ) {
        mRenderer.toggleProjectionMode();
        setButtonProjection();
        refresh(); // does not help
      }
    } else if ( b0 == mButton1[k1++] ) { // STATIONS
      GlNames.toggleStations();
      setButtonStation();
    } else if ( b0 == mButton1[k1++] ) { // SPLAYS
      GlModel.toggleSplays();
      setButtonSplays();
    } else if ( b0 == mButton1[k1++] ) { // WALLS
      GlModel.toggleWallMode();
      setButtonWall();
    } else if ( b0 == mButton1[k1++] ) { // SURFACE
      GlModel.toggleSurface();
      setButtonSurface();
      setButtonMove();
    } else if ( b0 == mButton1[k1++] ) { // COLOR
      if ( mRenderer != null ) {
        mRenderer.toggleColorMode();
        setButtonColor();
      }
    } else if ( b0 == mButton1[k1++] ) { // FRAME
      if ( mRenderer != null ) {
        mRenderer.toggleFrameMode();
        setButtonFrame();
      }
    } else if ( b0 == mMeasureStation ) {
      if ( isMeasuring ) {
        mMeasureStation.setBackground( mBMmeasureOff );
        isMeasuring = false;
      } else {
        mMeasureStation.setBackground( mBMmeasureOn );
        isMeasuring = true;
      }
    } else if ( b0 == mFixStation ) {
      if ( centerAtCurrentStation() ) {
        mFixStation.setBackground( mBMfixOn );
        isFixed = true;
      } else {
        mFixStation.setBackground( mBMfixOff );
        isFixed = false;
      }
    }
  }

  // -------------------------------------------------------------------

  // public void onActivityResult( int request, int result, Intent data ) 
  // {
  //   switch ( request ) {
  //     case REQUEST_OPEN_FILE:
  //       if ( result == Activity.RESULT_OK ) {
  //         String filename = data.getExtras().getString( "com.topodroid.Cave3D.filename" );
  //         if ( filename != null && filename.length() > 0 ) {
  //           // Log.v( "TopoGL", "path " + mAppBasePath + " file " + filename );
  //           doOpenFile( mAppBasePath + "/" + filename );
  //         }
  //       }
  //       break;
  //   }
  // }

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
    // checkAppBasePath();
    // mAppBasePath = base;
    mFilename = survey;
    boolean ret = initRendering( survey, base );
    // Log.v( "TopoGL", "do open survey: " + base + "/" + survey + " " + (ret? "true" : "false" ) );
    return true;
  }

  // always called asynch
  // asynch call returns always false
  // synch call return true if successful
  boolean doOpenFile( final String filename, boolean asynch )
  {
    mFilename = null;
    doSketches = false;
    // setTitle( filename );
    int idx = filename.lastIndexOf( '/' );
    String path = ( idx >= 0 )? filename.substring( idx+1 ) : filename;
    Toast.makeText( this, String.format( getResources().getString( R.string.reading_file ), path ), Toast.LENGTH_SHORT ).show();
    if ( asynch ) {
      (new AsyncTask<Void, Void, Boolean>() {
        @Override public Boolean doInBackground(Void ... v ) {
          return initRendering( filename );
        }
        @Override public void onPostExecute( Boolean b )
        {
          if ( b ) {
            mFilename = path;
            CWConvexHull.resetCounters();
            if ( mRenderer != null ) mRenderer.setParser( mParser );
          }
        }
      } ).execute();
      return false;
    } else { // synchronous
      if ( initRendering( filename ) ) {
        mFilename = path;
        CWConvexHull.resetCounters();
        if ( mRenderer != null ) mRenderer.setParser( mParser );
      }
    }
    return ( mFilename != null );
  }

  // private void openFile()
  // {
  //   Intent openFileIntent = new Intent( Intent.ACTION_EDIT ).setClass( this, DialogOpenFile.class );
  //   startActivityForResult( openFileIntent, REQUEST_OPEN_FILE );
  // }

  private void setWallButton( int wall_mode )
  {
/*
    switch ( wall_mode ) {
      case TglParser.WALL_NONE:
        toast( "wall mode NONE" );
        // mButton1[BTN_WALL].setBackgroundDrawable( mBMnone );
        break;
      case TglParser.WALL_CW:
        toast( "wall mode CONVEX_HULL" );
        // mButton1[BTN_WALL].setBackgroundDrawable( mBMconvex );
        break;
      case TglParser.WALL_POWERCRUST:
        toast( "wall mode POWERCRUST" );
        // mButton1[BTN_WALL].setBackgroundDrawable( mBMpowercrust );
        break;
      case TglParser.WALL_DELAUNAY:
        toast( "wall mode DELAUNAY" );
        // mButton1[BTN_WALL].setBackgroundDrawable( mBMdelaunay );
        break;
      case TglParser.WALL_HULL:
        toast( "wall mode HULL" );
        // mButton1[BTN_WALL].setBackgroundDrawable( mBMhull );
        break;
      default:
        toast( "wall mode NONE" );
    }
*/
  }

  // ------------------------------ SKETCH
  void openSketch( String pathname, String filename ) 
  {
    // Log.v("Cave3D-DEM", pathname );
    if ( ! pathname.endsWith( ".c3d" ) ) return;
    ParserSketch sketch = new ParserSketch( pathname );
    // final double dd = mDEMbuffer;
    (new AsyncTask<ParserSketch, Void, Boolean>() {
      ParserSketch my_sketch = null;

      public Boolean doInBackground( ParserSketch ... sketch ) 
      {
        my_sketch = sketch[0];
        my_sketch.readData( );
        return true;
      }

      public void onPostExecute( Boolean b )
      {
        // my_sketch.log();
        if ( b ) {
          if ( mRenderer != null ) mRenderer.notifySketch( my_sketch );
          toast( R.string.sketch_ok, true );
        } else {
          toast( R.string.sketch_failed, true );
        }
      }
    }).execute( sketch );
  }

  // ------------------------------ DEM
  void openDEM( String pathname, String filename ) 
  {
    // Log.v("Cave3D-DEM", pathname );
    ParserDEM dem = null;
    if ( pathname.endsWith( ".grid" ) ) {
      dem = new DEMgridParser( pathname, mDEMmaxsize );
    } else if ( pathname.endsWith( ".asc" ) || pathname.endsWith(".ascii") ) {
      Cave3DFix origin = mParser.getOrigin();
      // origin.log();
      double xunit = mParser.getWEradius(); // radius * PI/180
      double yunit = mParser.getSNradius(); // radius * PI/180
      // Log.v("TopoGL", "xunit " + xunit + " yunit " + yunit );
      dem = new DEMasciiParser( pathname, mDEMmaxsize, false, xunit, yunit ); // false: flip horz
    } else { 
      return;
    }
    if ( dem.valid() ) {
      mDEMname = filename;
      final double dd = mDEMbuffer;
      // Log.v("TopoGL-DEM", "BBox X " + mParser.emin + " " + mParser.emax + " Y " + mParser.nmin + " " + mParser.nmax + " Z " + mParser.zmin + " " + mParser.zmax );
      (new AsyncTask<ParserDEM, Void, Boolean>() {
        ParserDEM my_dem = null;

        public Boolean doInBackground( ParserDEM ... dem ) {
          my_dem = dem[0];
          my_dem.readData( mParser.emin - dd, mParser.emax + dd, mParser.nmin - dd, mParser.nmax + dd );
          return my_dem.valid();
        }

        public void onPostExecute( Boolean b )
        {
          if ( b ) {
            if ( mRenderer != null ) mRenderer.notifyDEM( my_dem );
            toast( R.string.dem_ok, true );
          } else {
            toast( R.string.dem_failed, true );
          }
        }
      }).execute( dem );

    }
  }

  // load a texture file (either GeoTIFF or OSM)
  void openTexture( String pathname, String filename )
  {
    if ( mRenderer == null ) return;
    final RectF  bounds = mRenderer.getSurfaceBounds();
    if ( bounds == null ) return;


    // Log.v("TopoGL", "texture " + pathname + " bbox " + bounds.left + " " + bounds.bottom + "  " + bounds.right + " " + bounds.top );

    mTextureName = filename;
    if ( filename.endsWith( ".osm" ) ) {
      loadTextureOSM( pathname, bounds );
    } else {
      loadTextureGeotiff( pathname, bounds );
    }
  }

  private void loadTextureGeotiff( final String pathname, final RectF bounds )
  {
    (new AsyncTask<String, Void, Boolean>() {
      Bitmap bitmap = null;

      public Boolean doInBackground( String ... files ) {
        String file = files[0];
        bitmap = (Bitmap)( TiffFactory.getBitmap( pathname, bounds.left, bounds.bottom, bounds.right, bounds.top ) );
        // if ( bitmap != null ) {
        //   Log.v("TopoGL", "texture " + file + " size " + bitmap.getWidth() + " " + bitmap.getHeight() );
        // }

        return (bitmap != null);
      }

      public void onPostExecute( Boolean b )
      {
        if ( b ) {
          if ( mRenderer != null ) mRenderer.notifyTexture( bitmap ); // FIXME do in doInBackground
          toast( R.string.texture_ok, true );
        } else {
          toast( R.string.texture_failed, true );
        }
      }
    }).execute( pathname );
  }

  private void loadTextureOSM( final String pathname, final RectF bounds )
  {
    (new AsyncTask<String, Void, Boolean>() {
      Bitmap bitmap = null;

      public Boolean doInBackground( String ... files ) {
        String file = files[0];
        Cave3DFix origin = mParser.getOrigin();
        if ( origin == null ) {
          Log.e("TopoGL", "OSM with null origin");
          return false;
        } 

        OsmFactory osm = new OsmFactory( bounds.left, bounds.bottom, bounds.right, bounds.top, origin );
        bitmap = osm.getBitmap( pathname );
        // if ( bitmap != null ) {
        //   Log.v("TopoGL", "texture " + file + " size " + bitmap.getWidth() + " " + bitmap.getHeight() );
        // }

        return (bitmap != null);
      }

      public void onPostExecute( Boolean b )
      {
        if ( b ) {
          if ( mRenderer != null ) mRenderer.notifyTexture( bitmap ); // FIXME do in doInBackground
          toast( R.string.texture_ok, true );
        } else {
          toast( R.string.texture_failed, true );
        }
      }
    }).execute( pathname );
  }

  // ---------------------------------------- PERMISSIONS
  // TglPerms perms_dialog = null;

  private void checkPermissions()
  {
    prefs = PreferenceManager.getDefaultSharedPreferences( this );
    loadPreferences( prefs );
    prefs.registerOnSharedPreferenceChangeListener( this );

    try {
      VERSION = getPackageManager().getPackageInfo( getPackageName(), 0 ).versionName;
    } catch ( NameNotFoundException e ) {
      e.printStackTrace(); // FIXME
    }
    FeatureChecker.createPermissions( this, this );

    mCheckPerms = FeatureChecker.checkPermissions( this );
    if ( mCheckPerms != 0 ) {
      // perms_dialog = new TglPerms( this, mCheckPerms );
      // perms_dialog.show();
      TglPerms.toast( this, mCheckPerms );
    }
  }

  @Override
  public void onRequestPermissionsResult( int code, final String[] perms, int[] results )
  {
    // Log.v( "TopoGL-PERM", "req code " + code + " results length " + results.length );
    if ( code == FeatureChecker.REQUEST_PERMISSIONS ) {
      if ( results.length > 0 ) {
	for ( int k = 0; k < results.length; ++ k ) {
	  FeatureChecker.GrantedPermission[k] = ( results[k] == PackageManager.PERMISSION_GRANTED );
	  // Log.v( "TopoGL-PERM", "perm " + k + " perms " + perms[k] + " result " + results[k] );
	}
      }
    }
  }

  // --------------------------------------- DIMENSIONS
  private static int setListViewHeight( Context context, HorizontalListView listView )
  {
    int size = getScaledSize( context );
    if ( listView != null ) {
      LayoutParams params = listView.getLayoutParams();
      params.height = size + 10;
      listView.setLayoutParams( params );
    }
    return size;
  }

  // default button size
  private static int getScaledSize( Context context )
  {
    return (int)( 42 * mButtonSize * context.getResources().getSystem().getDisplayMetrics().density );
  }

  // private static int getDefaultSize( Context context )
  // {
  //   return (int)( 42 * context.getResources().getSystem().getDisplayMetrics().density );
  // }

  // private boolean isMultitouch()
  // {
  //   return getPackageManager().hasSystemFeature( PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH );
  // }

  static final int DEM_SHRINK = 1;
  static final int DEM_CUT    = 2;

  

  // ---------------------------------- PREFERENCES
  private SharedPreferences prefs;

  static float mSelectionRadius = 1.0f;
  static int mButtonSize      = 1;
  // static boolean mPreprojection = true;
  static float mDEMbuffer  = 200;
  static int   mDEMmaxsize = 400;
  static int   mDEMreduce  = DEM_SHRINK;
  // static boolean mWallConvexHull = false;
  // static boolean mWallPowercrust = false;
  // static boolean mWallDelaunay   = false;
  // static boolean mWallHull       = false;
  static boolean mStationDialog  = false;
  // static boolean mUseSplayVector = true; // ??? Hull with 3D splays or 2D splay projections
  static boolean mMeasureToast   = false;
  static boolean mSplayProj      = false;
  static float   mSplayThr       = 0.5f;

  static final String CAVE3D_BASE_PATH        = "CAVE3D_BASE_PATH";
  static final String CAVE3D_TEXT_SIZE        = "CAVE3D_TEXT_SIZE";
  static final String CAVE3D_BUTTON_SIZE      = "CAVE3D_BUTTON_SIZE";
  static final String CAVE3D_SELECTION_RADIUS = "CAVE3D_SELECTION_RADIUS";
  static final String CAVE3D_STATION_TOAST    = "CAVE3D_STATION_TOAST";
  static final String CAVE3D_STATION_SIZE     = "CAVE3D_STATION_SIZE";
  static final String CAVE3D_STATION_POINTS   = "CAVE3D_STATION_POINTS";
  static final String CAVE3D_MEASURE_DIALOG   = "CAVE3D_MEASURE_DIALOG";
  static final String CAVE3D_GRID_ABOVE       = "CAVE3D_GRID_ABOVE";
  static final String CAVE3D_GRID_EXTENT      = "CAVE3D_GRID_EXTEND";
  static final String CAVE3D_NEG_CLINO        = "CAVE3D_NEG_CLINO";
  static final String CAVE3D_DEM_BUFFER       = "CAVE3D_DEM_BUFFER";
  static final String CAVE3D_DEM_MAXSIZE      = "CAVE3D_DEM_MAXSIZE";
  static final String CAVE3D_DEM_REDUCE       = "CAVE3D_DEM_REDUCE";
  // WALLS category
  static final String CAVE3D_ALL_SPLAY        = "CAVE3D_ALL_SPLAY";
  static final String CAVE3D_SPLAY_PROJ       = "CAVE3D_SPLAY_PROJ";
  static final String CAVE3D_SPLAY_THR        = "CAVE3D_SPLAY_THR";
  static final String CAVE3D_SPLIT_TRIANGLES  = "CAVE3D_SPLIT_TRIANGLES";
  static final String CAVE3D_SPLIT_RANDOM     = "CAVE3D_SPLIT_RANDOM";
  static final String CAVE3D_SPLIT_STRETCH    = "CAVE3D_SPLIT_STRETCH";
  static final String CAVE3D_POWERCRUST_DELTA = "CAVE3D_POWERCRUST_DELTA";
  // static final String CAVE3D_CONVEX_HULL      = "CAVE3D_CONVEX_HULL";
  // static final String CAVE3D_POWERCRUST       = "CAVE3D_POWERCRUST";
  // static final String CAVE3D_DELAUNAY         = "CAVE3D_DELAUNAY";
  // static final String CAVE3D_HULL             = "CAVE3D_HULL";

  public void onSharedPreferenceChanged( SharedPreferences sp, String k ) 
  {
    // checkAppBasePath();
    if ( k.equals( CAVE3D_BASE_PATH ) ) { 
      mAppBasePath = sp.getString( k, HOME_PATH );
      // Log.v( "TopoGL", "SharedPref change: path " + mAppBasePath );
      if ( mAppBasePath == null ) mAppBasePath = HOME_PATH;
    } else if ( k.equals( CAVE3D_TEXT_SIZE ) ) {
      try {
        int size = Integer.parseInt( sp.getString( k, "10" ) );
        GlNames.setTextSize( size );
      } catch ( NumberFormatException e ) { }
    } else if ( k.equals( CAVE3D_BUTTON_SIZE ) ) {
      try {
        int size = Integer.parseInt( sp.getString( k, "1" ) );
        if ( size > 0 ) mButtonSize = size;
      } catch ( NumberFormatException e ) { }
      resetButtonBar();
    } else if ( k.equals( CAVE3D_SELECTION_RADIUS ) ) { 
      try {
        float radius = Float.parseFloat( sp.getString( k, "1.0" ) );
        if ( radius > 0.0f ) mSelectionRadius = radius;
      } catch ( NumberFormatException e ) { }
    } else if ( k.equals( CAVE3D_STATION_TOAST ) ) { 
      mStationDialog = sp.getBoolean( k, false );
    } else if ( k.equals( CAVE3D_STATION_SIZE ) ) { 
      try {
        GlNames.setPointSize( Integer.parseInt( sp.getString( k, "8" ) ) );
      } catch ( NumberFormatException e ) { }
    } else if ( k.equals( CAVE3D_STATION_POINTS ) ) { 
      GlModel.mStationPoints = sp.getBoolean( k, false );
    } else if ( k.equals( CAVE3D_MEASURE_DIALOG ) ) { 
      mMeasureToast  = sp.getBoolean( k, false );
    } else if ( k.equals( CAVE3D_GRID_ABOVE ) ) { 
      GlModel.mGridAbove = sp.getBoolean( k, false );
    } else if ( k.equals( CAVE3D_GRID_EXTENT ) ) { 
      try {
        int extent = Integer.parseInt( sp.getString( k, "10" ) );
        if ( extent > 1 && extent < 100 ) GlModel.mGridExtent = extent;
      } catch ( NumberFormatException e ) { }
    } else if ( k.equals( CAVE3D_NEG_CLINO ) ) { 
      GlRenderer.mMinClino = sp.getBoolean( k, false ) ? 90: 0;
    } else if ( k.equals( CAVE3D_ALL_SPLAY ) ) { 
      GlModel.mAllSplay = sp.getBoolean( k, true );
    } else if ( k.equals( CAVE3D_SPLAY_PROJ ) ) { 
      mSplayProj = sp.getBoolean( k, false );
    } else if ( k.equals( CAVE3D_SPLAY_THR ) ) { 
      try {
        float buffer = Float.parseFloat( sp.getString( k, "0.5" ) );
        mSplayThr = buffer; 
      } catch ( NumberFormatException e ) { }

    } else if ( k.equals( CAVE3D_DEM_BUFFER ) ) { 
      try {
        float buffer = Float.parseFloat( sp.getString( k, "200" ) );
        if ( buffer >= 0 ) mDEMbuffer = buffer;
      } catch ( NumberFormatException e ) { }
    } else if ( k.equals( CAVE3D_DEM_MAXSIZE ) ) { 
      try {
        int size = Integer.parseInt( sp.getString( k, "400" ) );
        if ( size >= 50 ) mDEMmaxsize = size;
      } catch ( NumberFormatException e ) { }
    } else if ( k.equals( CAVE3D_DEM_REDUCE ) ) { 
      try {
        int reduce = Integer.parseInt( sp.getString( k, "1" ) );
        if ( reduce == 1 ) mDEMreduce = DEM_SHRINK;
        else               mDEMreduce = DEM_CUT;
      } catch ( NumberFormatException e ) { }

    // } else if ( k.equals( CAVE3D_PREPROJECTION ) ) { 
    //   mPreprojection = sp.getBoolean( k, true );
    } else if ( k.equals( CAVE3D_SPLIT_TRIANGLES ) ) { 
      GlModel.mSplitTriangles = sp.getBoolean( k, true );
    } else if ( k.equals( CAVE3D_SPLIT_RANDOM ) ) { 
      try {
        float r = Float.parseFloat( sp.getString( k, "0.1" ) );
        if ( r > 0.0001f ) {
          GlModel.mSplitRandomizeDelta = r;
          GlModel.mSplitRandomize = true;
        } else {
          GlModel.mSplitRandomize = false;
        }
      } catch ( NumberFormatException e ) { }
    } else if ( k.equals( CAVE3D_SPLIT_STRETCH ) ) { 
      try {
        float r = Float.parseFloat( sp.getString( k, "0.1" ) );
        if ( r > 0.0001f ) {
          GlModel.mSplitStretchDelta = r;
          GlModel.mSplitStretch = true;
        } else {
          GlModel.mSplitStretch = false;
        }
      } catch ( NumberFormatException e ) { }
    } else if ( k.equals( CAVE3D_POWERCRUST_DELTA ) ) { 
      try {
        float delta = Float.parseFloat( sp.getString( k, "0.1" ) );
        if ( delta > 0 ) GlModel.mPowercrustDelta = delta;
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
    checkAppBasePath();
    mAppBasePath = sp.getString( CAVE3D_BASE_PATH, HOME_PATH );
    if ( mAppBasePath == null ) mAppBasePath = HOME_PATH;
    try {
      int size = Integer.parseInt( sp.getString( CAVE3D_TEXT_SIZE, "10" ) );
      GlNames.setTextSize( size );
    } catch ( NumberFormatException e ) { }
    try {
      int size = Integer.parseInt( sp.getString( CAVE3D_BUTTON_SIZE, "1" ) );
      if ( size > 0 ) {
        mButtonSize = size;
        resetButtonBar();
      }
    } catch ( NumberFormatException e ) { }
    try {
      float radius = Float.parseFloat( sp.getString( CAVE3D_SELECTION_RADIUS, "1.0" ) );
      if ( radius > 0.0f ) mSelectionRadius = radius;
    } catch ( NumberFormatException e ) { }
    mStationDialog = sp.getBoolean( CAVE3D_STATION_TOAST, false );
    try {
      GlNames.setPointSize( Integer.parseInt( sp.getString( CAVE3D_STATION_SIZE, "8" ) ) );
    } catch ( NumberFormatException e ) { }
    GlModel.mStationPoints = sp.getBoolean( CAVE3D_STATION_POINTS, false );
    mMeasureToast  = sp.getBoolean( CAVE3D_MEASURE_DIALOG, false );
    GlModel.mGridAbove = sp.getBoolean( CAVE3D_GRID_ABOVE, false );
    try {
      int extent = Integer.parseInt( sp.getString( CAVE3D_GRID_EXTENT, "10" ) );
      if ( extent > 1 && extent < 1000 ) GlModel.mGridExtent = extent;
    } catch ( NumberFormatException e ) { }
    GlRenderer.mMinClino  = sp.getBoolean( CAVE3D_NEG_CLINO, false ) ? 90 : 0;
    GlModel.mAllSplay  = sp.getBoolean( CAVE3D_ALL_SPLAY, true );
    mSplayProj = sp.getBoolean( CAVE3D_SPLAY_PROJ, false );
    try {
      float buffer = Float.parseFloat( sp.getString( CAVE3D_SPLAY_THR, "0.5" ) );
      mSplayThr = buffer;
    } catch ( NumberFormatException e ) { }
    try {
      float buffer = Float.parseFloat( sp.getString( CAVE3D_DEM_BUFFER, "200" ) );
      if ( buffer >= 0 ) mDEMbuffer = buffer;
    } catch ( NumberFormatException e ) { }
    try {
      int size = Integer.parseInt( sp.getString( CAVE3D_DEM_MAXSIZE, "400" ) );
      if ( size >= 50 ) mDEMmaxsize = size;
    } catch ( NumberFormatException e ) { }
    try {
      int reduce = Integer.parseInt( sp.getString( CAVE3D_DEM_REDUCE, "1" ) );
      if ( reduce == 1 ) mDEMreduce = DEM_SHRINK;
      else               mDEMreduce = DEM_CUT;
    } catch ( NumberFormatException e ) { }

    // mPreprojection  = sp.getBoolean( CAVE3D_PREPROJECTION, true );
    GlModel.mSplitTriangles = sp.getBoolean( CAVE3D_SPLIT_TRIANGLES, true );
    GlModel.mSplitRandomize = false;
    try {
      r = Float.parseFloat( sp.getString( CAVE3D_SPLIT_RANDOM, "0.1" ) );
      if ( r > 0.0001f ) {
        GlModel.mSplitRandomizeDelta = r;
        GlModel.mSplitRandomize = true;
      }
    } catch ( NumberFormatException e ) { }
    GlModel.mSplitStretch = false;
    try {
      r = Float.parseFloat( sp.getString( CAVE3D_SPLIT_STRETCH, "0.1" ) );
      if ( r > 0.0001f ) {
        GlModel.mSplitStretchDelta = r;
        GlModel.mSplitStretch = true;
      }
    } catch ( NumberFormatException e ) {
      GlModel.mSplitStretchDelta = 0.1f;
      GlModel.mSplitStretch = true;
    }
    // mWallConvexHull = sp.getBoolean( CAVE3D_CONVEX_HULL, true );
    // mWallPowercrust = sp.getBoolean( CAVE3D_POWERCRUST,  false );
    // mWallDelaunay   = sp.getBoolean( CAVE3D_DELAUNAY,    false );
    // mWallHull       = sp.getBoolean( CAVE3D_HULL,        false );
  }

  // ---------------------------------------------------------------
  // TOAST

  void toast( String r, boolean loong )
  {
    if ( loong ) { Toast.makeText( this, r, Toast.LENGTH_LONG).show(); } else { Toast.makeText( this, r, Toast.LENGTH_SHORT).show(); }
  }

  void toast( int r, boolean loong )
  {
    if ( loong ) { Toast.makeText( this, r, Toast.LENGTH_LONG).show(); } else { Toast.makeText( this, r, Toast.LENGTH_SHORT).show(); }
  }

  void toast( int r ) { toast( r, false ); }
  void toast( String r ) { toast( r, false ); }

  void toast( int r, String str, boolean loong )
  {
    String msg = String.format( getResources().getString( r ), str );
    // Log.v("TopoGL", "toast " + msg );
    if ( loong ) { Toast.makeText( this, msg, Toast.LENGTH_LONG).show(); } else { Toast.makeText( this, msg, Toast.LENGTH_SHORT).show(); }
  }

  void toast( int r, String str ) { toast( r, str, false ); }

  void toast( int r, int n1, int n2 )
  {
    String msg = String.format( getResources().getString( r ), n1, n2 );
    Toast.makeText( this, msg, Toast.LENGTH_SHORT).show();
  }

  void uiToast( final String r, final boolean loong ) 
  {
    final Context ctx = this;
    runOnUiThread( new Runnable() {
        public void run() {
          if ( loong ) {
            Toast.makeText( ctx, r, Toast.LENGTH_LONG ).show();
          } else {
            Toast.makeText( ctx, r, Toast.LENGTH_SHORT ).show();
          }
        }
    } );
  }

  void uiToast( final int r, final boolean loong ) 
  {
    final Context ctx = this;
    runOnUiThread( new Runnable() {
        public void run() {
          if ( loong ) {
            Toast.makeText( ctx, r, Toast.LENGTH_LONG ).show();
          } else {
            Toast.makeText( ctx, r, Toast.LENGTH_SHORT ).show();
          }
        }
    } );
  }

  void uiToast( final int r, final String str, final boolean loong ) 
  {
    final Context ctx = this;
    String msg = String.format( getResources().getString( r ), str );
    runOnUiThread( new Runnable() {
        public void run() {
          if ( loong ) {
            Toast.makeText( ctx, msg, Toast.LENGTH_LONG ).show();
          } else {
            Toast.makeText( ctx, msg, Toast.LENGTH_SHORT ).show();
          }
        }
    } );
  }

  void uiToast( final String r, final String str, final boolean loong ) 
  {
    final Context ctx = this;
    String msg = String.format( r, str );
    runOnUiThread( new Runnable() {
        public void run() {
          if ( loong ) {
            Toast.makeText( ctx, msg, Toast.LENGTH_LONG ).show();
          } else {
            Toast.makeText( ctx, msg, Toast.LENGTH_SHORT ).show();
          }
        }
    } );
  }


  // ------------------------------------------------------------------

  private boolean initRendering( String survey, String base ) 
  {
    // Log.v("TopoGL", "init rendering " + survey + " base " + base );
    doSketches = false;
    try {
      mParser = new ParserTh( this, survey, base ); // survey data directly from TopoDroid database
      CWConvexHull.resetCounters();
      if ( mRenderer != null ) {
        mRenderer.clearModel();
        mRenderer.setParser( mParser );
      }
      doSketches = true;
      // Log.v( "TopoGL", "Station " + mParser.getStationNumber() + " shot " + mParser.getShotNumber() );
    } catch ( ParserException e ) {
      toast(R.string.error_parser_error, survey + " " + e.msg(), true );
      mParser = null;
      // Log.v( "TopoGL", "parser exception " + filename );
    }
    return (mParser != null);
  }

  private boolean initRendering( String filename )
  {
    // Log.v("TopoGL", "init rendering " + filename );
    doSketches = false;
    try {
      mParser = null;
      if ( mRenderer != null ) mRenderer.clearModel();
      // resetAllPaths();
      if ( filename.endsWith( ".tdconfig" ) ) {
        mParser = new ParserTh( this, filename ); // tdconfig files are saved with therion syntax
        doSketches = true;
      } else if ( filename.endsWith( ".th" ) || filename.endsWith( ".thconfig" ) ) {
        mParser = new ParserTh( this, filename );
      } else if ( filename.endsWith( ".lox" ) ) {
        mParser = new ParserLox( this, filename );
      } else if ( filename.endsWith( ".mak" ) || filename.endsWith( ".dat" ) ) {
        mParser = new ParserDat( this, filename );
      } else if ( filename.endsWith( ".tro" ) ) {
        mParser = new ParserTro( this, filename );
      } else if ( filename.endsWith( ".3d" ) ) {
        mParser = new Parser3d( this, filename );
      } else {
        return false;
      }
      // CWConvexHull.resetCounters();
      // if ( mRenderer != null ) mRenderer.setParser( mParser );
      // Log.v( "TopoGL", "Station " + mParser.getStationNumber() + " shot " + mParser.getShotNumber() + " splay " + mParser.getSplayNumber() + " surveys " + mParser.getSurveyNumber() );
    } catch ( ParserException e ) {
      // Log.e( "TopoGL", "parser exception " + e.msg() );
      uiToast(R.string.error_parser_error, e.msg(), true );
      mParser = null;
    }
    return (mParser != null);
  }

  // run on onPostExecute
  void notifyWall( int type, boolean result )
  {
    if (type == TglParser.WALL_CW ) {
      if ( result ) {
        toast( R.string.done_convexhull );
      } else {
        toast( R.string.fail_convexhull, true );
      }
    } else if ( type == TglParser.WALL_POWERCRUST ) {
      if ( result ) {
        toast(  R.string.done_powercrust );
      } else {
        toast( R.string.fail_powercrust, true );
      }
    } else if ( type == TglParser.WALL_HULL ) {
      if ( result ) {
        toast(  R.string.done_hull );
      } else {
        toast( R.string.fail_hull, true );
      }
    }
    if ( mRenderer != null ) mRenderer.notifyWall( type, result );
  }

  void refresh()
  {
    // Log.v("TopoGL", "refresh. mode " + mRenderer.projectionMode );
    if ( mRenderer != null ) setTheTitle( mRenderer.getAngleString() );
    if ( glSurfaceView != null ) { // neither of these help
      glSurfaceView.requestRender(); 
      // glSurfaceView.onTouchEvent( null );
    }
  }

  void loadSketch()
  {
    new DialogSketch( this, this ).show();
  }

  public void notifyLocation( double lng, double lat )
  {
    // Log.v("TopoGL-GPS", "notified location " + lng + " " + lat );
    // TODO
    // [1] convert to model CRS
    if ( mParser != null && mParser.hasWGS84() ) {
      double e = mParser.lngToEast( lng, lat );
      double n = mParser.latToNorth( lat );
      // Log.v("TopoGL-GPS", "has origin " + mParser.hasOrigin() + " location " + e + " " + n );
      // [2] get Z from surface
      // [3] mRenderer.setLocation( new Vector3D( e, n, z ) );
      addGPSpoint( e, n );
    }
  }

/* ------------
  final static int CRS_CONVERSION_REQUEST = 2;
  final static int CRS_INPUT_REQUEST = 3; 

  void doProj4Conversion( String cs_to, double lng, double lat )
  {
    double alt = 0;
    // if ( cs_to == null ) return;
    try {
      Intent intent = new Intent( "Proj4.intent.action.Launch" );
      // Intent intent = new Intent( Intent.ACTION_DEFAULT, "com.topodroid.Proj4.intent.action.Launch" );
      intent.putExtra( "version", "1.1" );      // Proj4 version
      intent.putExtra( "request", "CRS_CONVERSION_REQUEST" ); // Proj4 request
      intent.putExtra( "cs_from", "Long-Lat" ); // NOTE MUST USE SAME NAME AS Proj4
      intent.putExtra( "cs_to", cs_to ); 
      intent.putExtra( "longitude", lng );
      intent.putExtra( "latitude",  lat );
      intent.putExtra( "altitude",  alt );
      startActivityForResult( intent, CRS_CONVERSION_REQUEST );
    } catch ( ActivityNotFoundException e ) {
      // TODO TDToast.makeBad( R.string.no_proj4 );
    }
  }

  void getProj4Coords( )
  {
    try {
      Intent intent = new Intent( "Proj4.intent.action.Launch" );
      // Intent intent = new Intent( Intent.ACTION_DEFAULT, "com.topodroid.Proj4.intent.action.Launch" );
      intent.putExtra( "version", "1.1" );      // Proj4 version
      intent.putExtra( "request", "CRS_INPUT_REQUEST" ); // Proj4 request
      startActivityForResult( intent, CRS_INPUT_REQUEST );
    } catch ( ActivityNotFoundException e ) {
      // TODO TDToast.makeBad( R.string.no_proj4 );
    }
  }

  public void onActivityResult( int reqCode, int resCode, Intent intent )
  {
    // mApp.resetLocale(); // OK-LOCALE
    if ( resCode == Activity.RESULT_OK ) {
      if ( reqCode == CRS_CONVERSION_REQUEST ) {
        Bundle bundle = intent.getExtras();
        if ( bundle != null ) {
          String cs = bundle.getString( "cs_to" );
          double e  = bundle.getDouble( "longitude");
          double n = bundle.getDouble( "latitude");
          // double alt = bundle.getDouble( "altitude");
	  // long   n_dec = bundle.containsKey( "decimals" )? bundle.getLong( "decimals" ) : 2;
          addGPSpoint( e, n );
        }
      } else if ( reqCode == CRS_INPUT_REQUEST ) {
        Bundle bundle = intent.getExtras();
        if ( bundle != null ) {
          // bundle.getDouble( "longitude" )
          // bundle.getDouble( "latitude" )
          // bundle.getDouble( "altitude" )
        }
      }
    }
  }
------------ */

  void setGPSstatus( boolean status )
  {
    // Log.v("TopoGL-GPS", "set GPS status " + status );
    if ( mGPS == null ) return;
    if ( status ) {
      mGPS.setGPSon();
      mGPS.setListener( this );
    } else {
      mGPS.setGPSoff();
      mGPS.setListener( null );
    }
  }
  
  boolean getGPSstatus()
  {
    return mGPS != null && mGPS.mIsLocating;
  }

  void addGPSpoint( double e, double n )
  {
    double z = mRenderer.getDEM_Z( e, n );
    if ( z > 0 ) {
      z += 0.1; // add 0.1 meter
      // Log.v("TopoGL-GPS", "set location " + e + " " + n + " " + z );
      mRenderer.setLocation( new Vector3D( e, n, z ) );
    } else {
      Log.e("TopoGL-GPS", "location " + e + " " + n + " out of DEM" );
    }
  }

  void hideOrShow( List< Cave3DSurvey > surveys )
  {
    if ( mRenderer != null ) {
      mRenderer.hideOrShow( surveys );
    }
  }

}
