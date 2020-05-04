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

import java.util.ArrayList;

import android.os.Environment;
import android.app.Activity;
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

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import android.widget.Toast;
import android.widget.Button;
import android.widget.ListView;
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
                    , OnItemClickListener
                    , OnSharedPreferenceChangeListener
{
  final static boolean ANDROID_10 = ( Build.VERSION.SDK_INT <= Build.VERSION_CODES.P );
  static String VERSION = "";

  // private static final int REQUEST_OPEN_FILE = 1;

  final static String APP_BASE_PATH = 
    ANDROID_10 ? Environment.getExternalStorageDirectory().getAbsolutePath()
               : Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
  static String mAppBasePath  = APP_BASE_PATH;

  static int mCheckPerms = -1;

  // ---------------------------------
  String mFilename; // opened filename
  public static float mScaleFactor   = 1.0f;
  public static float mDisplayWidth  = 200f;
  public static float mDisplayHeight = 320f;

  private boolean mIsNotMultitouch;

  private boolean supportsES2 = false;

  String mDEMname = null;
  // --------------------------------- OpenGL stuff
  private GlSurfaceView glSurfaceView;
  private GlRenderer mRenderer = null;

  private LinearLayout mLayout;
  // private TextView     mText;
  private boolean rendererSet = false;
  private TglParser mParser;
  private Button mCurrentStation;

  // ---------------------------------------------------------------
  // LIFECYCLE

  @Override
  public void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
    // Log.v( "TopoGL", "on create");

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

    mCurrentStation = (Button) findViewById( R.id.current_station );
    mCurrentStation.setOnClickListener( this );
    mCurrentStation.setVisibility( View.GONE );
    
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
        // Log.v( "Cave3D-EXTRA", "TopoDroid filename " + name );
        if ( name != null ) {
          if ( doOpenFile( name ) ) {
            file_dialog = false;
          } else {
            Log.e( "TopoGL", "Cannot open input file " + name );
          }
        } else {
          name = extras.getString( "INPUT_SURVEY" );
          String base = extras.getString( "SURVEY_BASE" );
          // Log.v( "Cave3D-EXTRA", "open input survey " + name + " base " + base );
          if ( name != null ) {
            if ( doOpenSurvey( name, base ) ) {
              file_dialog = false;
            } else {
              Log.e( "TopoGL", "Cannot open input survey " + name );
            }
          } else {
            Log.e( "TopoGL", "No input file or survey");
          }
        }          
      }
      if ( file_dialog ) { 
        // Log.v("TopoGL", "open file dialog");
        (new DialogOpenFile( this, this )).show();
        // openFile();
      }
    } else {
      Log.e( "TopoGL-PERM", "finishing activity ... perms " + mCheckPerms );
      if ( perms_dialog != null ) perms_dialog.dismiss();
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
    mRenderer.setParser( mParser );
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
    R.string.menu_viewpoint,  // 5
    R.string.menu_alpha,
    R.string.menu_wall,       // 7
    R.string.menu_options,
    // R.string.menu_fractal, // FRACTAL
    R.string.menu_help
  };

  void setMenuAdapter( Resources res )
  {
    int size = getScaledSize( this );
    MyButton.setButtonBackground( this, mMenuImage, size, R.drawable.iz_menu );
    mMenuAdapter = new MyMenuAdapter( this, this, mMenu, R.layout.menu, new ArrayList< MyMenuItem >() );
    for ( int k=0; k<menus.length; ++k ) {
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
      new DialogExport( this, this, mParser ).show();
    } else if ( p++ == pos ) { // INFO
      if ( mFilename != null ) new DialogInfo(this, mParser, mRenderer).show();
    } else if ( p++ == pos ) { // ICO
      if ( mFilename != null ) new DialogIco(this, mParser).show();
    } else if ( p++ == pos ) { // ROSE
      if ( mFilename != null ) new DialogRose(this, mParser).show();
    } else if ( p++ == pos ) { // RESET
      if ( mFilename != null ) mRenderer.resetTopGeometry();
    } else if ( p++ == pos ) { // VIEWPOINT
      if ( mFilename != null ) {
        new DialogView( this, this, mRenderer ).show();
      }
    } else if ( p++ == pos ) { // SURFACE ALPHA
      new DialogSurface( this, this ).show();
    } else if ( p++ == pos ) { // DO_WALLS
      new DialogWalls( this, this, mParser ).show();
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
    R.drawable.iz_station_no,
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
  BitmapDrawable mBMstation;

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
    mButton1[0] = new MyButton( this, this, size, izons[0], 0 );
    mButton1[1] = new MyButton( this, this, size, izons[1], 0 );
    mButton1[2] = new MyButton( this, this, size, izons[2], 0 );
    mButton1[3] = new MyButton( this, this, size, izons[3], 0 );
    mButton1[4] = new MyButton( this, this, size, izons[4], 0 );
    mButton1[5] = new MyButton( this, this, size, izons[5], 0 );
    mButton1[6] = new MyButton( this, this, size, izons[6], 0 );
    mButton1[7] = new MyButton( this, this, size, izons[7], 0 );

    mBMlight = mButton1[BTN_MOVE].mBitmap;
    mBMturn = MyButton.getButtonBackground( this, size, R.drawable.iz_turn );
    mBMmove = MyButton.getButtonBackground( this, size, R.drawable.iz_move );
    // mBMconvex = mButton1[BTN_WALL].mBitmap;

    mBMwallNo      = mButton1[ BTN_WALL     ].mBitmap;
    mBMwall        = MyButton.getButtonBackground( this, size, R.drawable.iz_wall );

    mBMorthogonal  = mButton1[ BTN_PROJECT  ].mBitmap;
    mBMperspective = MyButton.getButtonBackground( this, size, R.drawable.iz_perspective);

    mBMsurfaceNo   = mButton1[ BTN_SURFACE  ].mBitmap;
    mBMsurface     = MyButton.getButtonBackground( this, size, R.drawable.iz_surface );

    mBMstationNo   = mButton1[ BTN_STATION ].mBitmap;
    mBMstationPoint= MyButton.getButtonBackground( this, size, R.drawable.iz_station_point );
    mBMstationName = MyButton.getButtonBackground( this, size, R.drawable.iz_station_name );
    mBMstation     = MyButton.getButtonBackground( this, size, R.drawable.iz_station );

    mBMsplaysNo    = mButton1[ BTN_SPLAYS  ].mBitmap;
    mBMsplaysLine  = MyButton.getButtonBackground( this, size, R.drawable.iz_splays_line );
    mBMsplaysPoint = MyButton.getButtonBackground( this, size, R.drawable.iz_splays_point );

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

    setButtonProjection();
    setButtonSurface();
    setButtonWall();
    setButtonMove();
    setButtonStation();
    setButtonSplays();
    // setButtonColor();
    // setButtonFrame();

    mButton1[ BTN_WALL ].setOnClickListener( this );
    mButton1[ BTN_SURFACE ].setOnClickListener( this );
  }

  private void setButtonStation()
  {
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
      case GlNames.STATION_ALL:
        mButton1[ BTN_STATION ].setBackgroundDrawable( mBMstation );
        break;
    }
  }

  private void setButtonFrame()
  {
    // ( mRenderer != null ) is guaranteed
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
    // ( mRenderer != null ) is guaranteed
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
    mButton1[ BTN_PROJECT ].setBackgroundDrawable( (mRenderer.projectionMode == GlRenderer.PROJ_PERSPECTIVE )? mBMperspective : mBMorthogonal );
  }

  private void setButtonMove()
  {
    if ( GlSurfaceView.mLightMode ) {
      if ( mRenderer != null && mRenderer.hasSurface() ) {
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
    mCurrentStation.setVisibility( View.VISIBLE );
  }
 
  void closeCurrentStation()
  {
    mCurrentStation.setVisibility( View.GONE );
    mRenderer.setModelPath( null );
    mParser.mStartStation = null;
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
    if ( b0 == mCurrentStation ) {
      closeCurrentStation();
    }

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
      if ( mFilename != null ) {
        mRenderer.toggleColorMode();
        setButtonColor();
      }
    } else if ( b0 == mButton1[k1++] ) { // FRAME
      if ( mFilename != null ) {
        mRenderer.toggleFrameMode();
        setButtonFrame();
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
    mAppBasePath = base;
    mFilename = survey;
    boolean ret = initRendering( survey, base );
    // Log.v( "TopoGL", "do open survey: " + (ret? "true" : "false" ) );
    return true;
  }

  boolean doOpenFile( final String filename )
  {
    mFilename = null;
    // setTitle( filename );
    int idx = filename.lastIndexOf( '/' );
    String path = ( idx >= 0 )? filename.substring( idx+1 ) : filename;
    Toast.makeText( this, String.format( getResources().getString( R.string.reading_file ), path ), Toast.LENGTH_SHORT ).show();
    (new AsyncTask<Void, Void, Boolean>() {
      @Override public Boolean doInBackground(Void ... v ) {
        return initRendering( filename );
      }
      @Override public void onPostExecute( Boolean b )
      {
        if ( b ) {
          mFilename = path;
          CWConvexHull.resetCounters();
          mRenderer.setParser( mParser );
        }
      }
    } ).execute();

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

  // ------------------------------ DEM
  void openDEM( String filename ) 
  {
    // Log.v("Cave3D-DEM", filename );
    ParserDEM dem = null;
    if ( filename.endsWith( ".grid" ) ) {
      dem = new DEMgridParser( filename, mDEMmaxsize );
    } else if ( filename.endsWith( ".asc" ) || filename.endsWith(".ascii") ) {
      dem = new DEMasciiParser( filename, mDEMmaxsize, false ); // false: flip horz
    } else { 
      return;
    }
    if ( dem.valid() ) {
      final float dd = mDEMbuffer;
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
            mRenderer.notifyDEM( my_dem );
            toast( R.string.dem_ok, true );
          } else {
            toast( R.string.dem_failed, true );
          }
        }
      }).execute( dem );

    }
  }

  // ---------------------------------------- PERMISSIONS
  TglPerms perms_dialog = null;

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
      perms_dialog = new TglPerms( this, mCheckPerms );
      perms_dialog.show();
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
  static boolean mStationDialog = false;
  static boolean mUseSplayVector = true; // ??? Hull Projection

  static final String CAVE3D_BASE_PATH        = "CAVE3D_BASE_PATH";
  static final String CAVE3D_TEXT_SIZE        = "CAVE3D_TEXT_SIZE";
  static final String CAVE3D_BUTTON_SIZE      = "CAVE3D_BUTTON_SIZE";
  static final String CAVE3D_SELECTION_RADIUS = "CAVE3D_SELECTION_RADIUS";
  static final String CAVE3D_STATION_TOAST    = "CAVE3D_STATION_TOAST";
  static final String CAVE3D_STATION_POINTS   = "CAVE3D_STATION_POINTS";
  static final String CAVE3D_GRID_ABOVE       = "CAVE3D_GRID_ABOVE";
  static final String CAVE3D_GRID_EXTENT      = "CAVE3D_GRID_EXTEND";
  static final String CAVE3D_NEG_CLINO        = "CAVE3D_NEG_CLINO";
  static final String CAVE3D_DEM_BUFFER       = "CAVE3D_DEM_BUFFER";
  static final String CAVE3D_DEM_MAXSIZE      = "CAVE3D_DEM_MAXSIZE";
  static final String CAVE3D_DEM_REDUCE       = "CAVE3D_DEM_REDUCE";
  // WALLS category
  static final String CAVE3D_ALL_SPLAY        = "CAVE3D_ALL_SPLAY";
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
    if ( k.equals( CAVE3D_BASE_PATH ) ) { 
      mAppBasePath = sp.getString( k, APP_BASE_PATH );
      // Log.v( "TopoGL", "SharedPref change: path " + mAppBasePath );
      if ( mAppBasePath == null ) mAppBasePath = APP_BASE_PATH;
    } else if ( k.equals( CAVE3D_TEXT_SIZE ) ) {
      try {
        int size = Integer.parseInt( sp.getString( k, "20" ) );
        if ( size > 6 ) GlNames.setTextSize( size );
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
        if ( radius < 10 && radius > 0.1f ) mSelectionRadius = radius;
      } catch ( NumberFormatException e ) { }
    } else if ( k.equals( CAVE3D_STATION_TOAST ) ) { 
      mStationDialog = sp.getBoolean( k, false );
    } else if ( k.equals( CAVE3D_STATION_POINTS ) ) { 
      GlModel.mStationPoints = sp.getBoolean( k, false );
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
    mAppBasePath = sp.getString( CAVE3D_BASE_PATH, APP_BASE_PATH );
    if ( mAppBasePath == null ) mAppBasePath = APP_BASE_PATH;
    try {
      int size = Integer.parseInt( sp.getString( CAVE3D_TEXT_SIZE, "20" ) );
      if ( size > 6 ) GlNames.setTextSize( size );
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
      if ( radius < 10 && radius > 0.1f ) mSelectionRadius = radius;
    } catch ( NumberFormatException e ) { }
    mStationDialog = sp.getBoolean( CAVE3D_STATION_TOAST, false );
    GlModel.mStationPoints = sp.getBoolean( CAVE3D_STATION_POINTS, false );
    GlModel.mGridAbove = sp.getBoolean( CAVE3D_GRID_ABOVE, false );
    try {
      int extent = Integer.parseInt( sp.getString( CAVE3D_GRID_EXTENT, "10" ) );
      if ( extent > 1 && extent < 100 ) GlModel.mGridExtent = extent;
    } catch ( NumberFormatException e ) { }
    GlRenderer.mMinClino  = sp.getBoolean( CAVE3D_NEG_CLINO, false ) ? 90 : 0;
    GlModel.mAllSplay  = sp.getBoolean( CAVE3D_ALL_SPLAY, true );
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
    if ( loong ) { Toast.makeText( this, msg, Toast.LENGTH_LONG).show(); } else { Toast.makeText( this, msg, Toast.LENGTH_SHORT).show(); }
  }

  void toast( int r, String str ) { toast( r, str, false ); }

  void toast( int r, int n1, int n2 )
  {
    String msg = String.format( getResources().getString( r ), n1, n2 );
    Toast.makeText( this, msg, Toast.LENGTH_SHORT).show();
  }

  // ------------------------------------------------------------------

  private boolean initRendering( String survey, String base ) 
  {
    // Log.v("TopoGL", "init rendering " + survey + " base " + base );
    try {
      mParser = new ParserTh( this, survey, base ); // survey data directly from TopoDroid database
      CWConvexHull.resetCounters();
      if ( mRenderer != null ) {
        mRenderer.clearModel();
        mRenderer.setParser( mParser );
      }
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
    try {
      mParser = null;
      mRenderer.clearModel();
      // resetAllPaths();
      if ( filename.endsWith( ".tdconfig" ) ) {
        mParser = new ParserTh( this, filename ); // tdconfig files are saved with therion syntax
      } else if ( filename.endsWith( ".th" ) || filename.endsWith( ".thconfig" ) ) {
        mParser = new ParserTh( this, filename );
      } else if ( filename.endsWith( ".lox" ) ) {
        mParser = new ParserLox( this, filename );
      } else if ( filename.endsWith( ".mak" ) || filename.endsWith( ".dat" ) ) {
        mParser = new ParserDat( this, filename );
      } else if ( filename.endsWith( ".tro" ) ) {
        mParser = new ParserTro( this, filename );
      } else {
        return false;
      }
      // CWConvexHull.resetCounters();
      // mRenderer.setParser( mParser );
      // Log.v( "TopoGL", "Station " + mParser.getStationNumber() + " shot " + mParser.getShotNumber() );
    } catch ( ParserException e ) {
      toast(R.string.error_parser_error, filename + " " + e.msg(), true );
      mParser = null;
      // Log.v( "TopoGL", "parser exception " + filename );
    }
    return (mParser != null);
  }

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
    }
    if ( mRenderer != null ) mRenderer.notifyWall( type, result );
  }

  void refresh()
  {
    if ( glSurfaceView != null ) glSurfaceView.requestRender(); 
    if ( mRenderer != null ) setTheTitle( mRenderer.getAngleString() );
  }
}
