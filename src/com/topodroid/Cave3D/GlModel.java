/** @file GlModel.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D model
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import android.content.Context;

import android.graphics.Bitmap;

import android.opengl.Matrix;

import java.util.ArrayList;

public class GlModel
{
  Context mContext;
  static float   mWidth  = 0;
  static float   mHeight = 0; // unused
  TglParser mParser = null;

  private float mXmed, mYmed, mZmed; // XYZ openGL
  float grid_size = 1;
  static float mZMin, mZDelta; // draw params
  private float mZ0Min;        // minimum model altitude (survey frame)

  private float mY0med    = 0; // used in drawing
  private float mDiameter = 0;

  GlSurface glSurface = null; // surface
  GlNames   glNames   = null; // stations
  GlLines   glLegs    = null;
  GlLines   glSplays  = null;
  GlLines   glGrid    = null;
  GlLines   glFrame   = null;
  GlLines   glSurfaceLegs = null;
  GlTriangles glWalls = null;
  GlLines   glPlan    = null; // plan projection
  GlLines   glProfile = null;
  GlPath    glPath    = null;

  // --------------------------------------------------------------
  // MAINTENANCE

  boolean hasParser() { return mParser != null; }

  boolean hasSurface() { return  surfaceMode && glSurface != null; }

  void rebindTextures()
  {
    // if ( glNames != null ) glNames.resetTexture(); // nothing to do
  }

  void unbindTextures()
  {
    Log.w("TopoGL", "unbind textures");
    if ( glNames != null ) glNames.unbindTexture();
  }

  float getDiameter() { return mDiameter; } 

  float getGridCell() { return grid_size; }

  // float getDx0() { return ((glLegs == null)? 0 : glLegs.xmed ); }
  // float getDy0() { return ((glLegs == null)? 0 : glLegs.ymin ); }
  // float getDz0() { return ((glLegs == null)? 0 : glLegs.zmin ); }

  synchronized void clearAll() 
  {
    glSurfaceLegs = null;
    glSurface = null;
    glGrid    = null;
    glFrame   = null;
    glNames   = null;
    glWalls   = null;
    glPlan    = null;
    glProfile = null;
    glSplays  = null;
    glLegs    = null;
    glPath    = null;
    mParser   = null;
  }

  static void setWidthAndHeight( float w, float h ) 
  {
    mWidth  = w;
    mHeight = h;
  }

  void initGL()
  {
    GlLines.initGL( mContext ); // init GL programs
    GlNames.initGL( mContext );
    GlSurface.initGL( mContext );
    GlTriangles.initGL( mContext );
    GlPath.initGL( mContext );
  }

  // ----------------------------------------------------------------------------------
  // DISPLAY MODE

  static final int FRAME_NONE = 0;
  static final int FRAME_GRID = 1;
  static final int FRAME_AXES = 2;
  static final int FRAME_MAX  = 3;

  static final int DRAW_NONE  = 0;
  static final int DRAW_LINE  = 1;
  static final int DRAW_POINT = 2;
  static final int DRAW_ALL   = 3; 
  static final int DRAW_MAX   = 3; // splayMode < DRAW_MAX
  // static final int DRAW_WIRE  = 3; 

  static final int PROJ_NONE    = 0;
  static final int PROJ_PLAN    = 1;
  static final int PROJ_PROFILE = 2;

  // -------------------------- DISPLAY MODE
  static boolean surfaceMode = false;     // show_surface;
  static boolean surfaceLegsMode = false; // show_surface_legs;
  static boolean surfaceTexture  = true;  // show_surface texture;
  static boolean wallMode = false;        // shaw_walls
  static int planviewMode = 0;            // howto_show_planview = 0;
  static int splayMode = DRAW_NONE;       // howto_show_splays;
  static int frameMode = FRAME_GRID;      // howto_show_grid/frame
  static int projMode  = PROJ_NONE;       // howto_show_proj

  static boolean mStationPoints = false;
  static boolean mAllSplay  = true;
  static boolean mGridAbove = false;
  static int     mGridExtent = 10;
  static boolean mSplitTriangles = true;
  static boolean mSplitRandomize = true;
  static boolean mSplitStretch   = false;
  static float mSplitRandomizeDelta = 0.1f; // meters
  static float mSplitStretchDelta   = 0.1f;
  static float mPowercrustDelta     = 0.1f; // meters

  static void toggleSplays()   { splayMode = (splayMode+1) % DRAW_MAX; }
  static void togglePlanview() { planviewMode = ( planviewMode + 1 ) % 4; }
  static void toggleWallMode() { wallMode = ! wallMode; }
  static void toggleSurface() { surfaceMode = ! surfaceMode; }
  static void toggleSurfaceLegs() { surfaceLegsMode = ! surfaceLegsMode; }
  static void toggleFrameMode()   { frameMode = (frameMode + 1) % FRAME_MAX; }

  synchronized void toggleColorMode() { 
    glLegs.toggleColorMode( );
    glSplays.setColorMode( glLegs.mColorMode ); 
  }

  int getColorMode() 
  {
    return ( glLegs != null )? glLegs.mColorMode : GlLines.COLOR_NONE;
  }

  public void zoomOne() { /* TODO */ }

  private boolean modelCreated = false;

  // ---------------------------------------------------------------------------
  GlModel ( Context ctx )
  { 
    mContext = ctx;
  }

  void draw( float[] mvp_matrix, /* float[] inverse_scale_matrix, */ float[] mv_matrix, Vector3D light ) 
  { 
    if ( ! modelCreated ) return;

    GL.enableDepth( true );
    if ( surfaceMode ) {
      GlSurface gl_surface = null;
      synchronized( this ) { gl_surface = glSurface; }
      if ( gl_surface != null ) gl_surface.draw( mvp_matrix, mv_matrix, light );
    }

    if ( frameMode == FRAME_GRID ) { 
      GlLines grid = null;
      synchronized( this ) { grid = glGrid; } 
      if ( grid != null ) {
        GL.setLineWidth( 1.0f );
        if ( mGridAbove ) {
          float[] revMatrix = new float[16];
          float[] matrix = new float[16];
          Matrix.setIdentityM( revMatrix, 0 );
          revMatrix[13] = mY0med; // glLegs.getYmed();
          revMatrix[ 5] = -1;
          Matrix.multiplyMM( matrix, 0, mvp_matrix, 0, revMatrix, 0 );
          grid.draw( matrix, DRAW_LINE ); 
        } else {
          grid.draw( mvp_matrix, DRAW_LINE );
        }
      }
    } else if ( frameMode == FRAME_AXES  ) { 
      GlLines frame = null;
      synchronized( this ) { frame = glFrame; } 
      if ( frame != null ) {
        GL.setLineWidth( 2.0f );
        // if ( mGridAbove ) {
        //   float[] revMatrix = new float[16];
        //   float[] matrix = new float[16];
        //   Matrix.setIdentityM( revMatrix, 0 );
        //   revMatrix[14] = (glLegs.getZmax() + glLegs.getZmin())/2;
        //   revMatrix[10] = -1;
        //   Matrix.multiplyMM( matrix, 0, mvp_matrix, 0, revMatrix, 0 );
        //   frame.draw( matrix, DRAW_LINE ); 
        // } else {
          frame.draw( mvp_matrix, DRAW_LINE  );
        // }
      }
    }
    if ( surfaceLegsMode ) {
      GlLines surface_legs = null;
      synchronized( this ) { surface_legs = glSurfaceLegs; }
      if ( surface_legs != null ) {
        GL.setLineWidth( 1.0f );
        surface_legs.draw( mvp_matrix, DRAW_ALL );
      }
    }

    if ( wallMode ) {
      GlTriangles walls = null;
      synchronized( this ) { walls = glWalls; }
      if ( walls != null ) { 
        // GL.enableCull( false );
        walls.draw( mvp_matrix, mv_matrix, light );
        // GL.enableCull( true );
      }
    }
    GL.enableDepth( false );

    if ( projMode == PROJ_PLAN ) {
      GlLines plan = null;
      synchronized( this ) { plan = glPlan; }
      if ( plan != null ) { 
        GL.setLineWidth( 1.0f );
        plan.draw( mvp_matrix, DRAW_LINE );
      }
    } else if ( projMode == PROJ_PROFILE ) {
      GlLines profile = null;
      synchronized( this ) { profile = glProfile; }
      if ( profile != null ) { 
        GL.setLineWidth( 1.0f );
        profile.draw( mvp_matrix, DRAW_LINE );
      }
    }
    GlLines splays = null;
    synchronized( this ) { splays = glSplays; }
    if ( splays != null ) {
      GL.setLineWidth( 1.0f );
      splays.draw( mvp_matrix, splayMode );
    }
    GlLines legs = null;
    synchronized( this ) { legs = glLegs; }
    if ( legs   != null ) {
      GL.setLineWidth( 2.0f );
      legs.draw( mvp_matrix, DRAW_LINE, mStationPoints );
    }

    GlNames names = null;
    synchronized( this ) { names = glNames; }
    if ( names  != null ) {
      names.draw( mvp_matrix );
    }

    GlPath gl_path = null;
    synchronized( this ) { gl_path = glPath; }
    if ( gl_path != null ) {
      GL.setLineWidth( 4.0f );
      gl_path.draw( mvp_matrix );
    }
    // if ( glPath != null ) {
    //   GL.setLineWidth( 3.0f );
    //   glPath.draw( mvp_matrix );
    // }

  }

  // String checkNames( float x, float y, float[] mvpMatrix, float dmin ) { return ( glNames != null )? glNames.check( x, y, mvpMatrix, dmin ) : null; }
  String checkNames( float[] zn, float[] zf, float dmin, boolean highlight ) { return ( glNames != null )? glNames.check( zn, zf, dmin, highlight ) : null; }

  // ----------------------------------------------------------------------
  synchronized void clearWalls( ) 
  { 
    glWalls   = null; 
    glPlan    = null;
    glProfile = null;
  }

  boolean setPath( ArrayList< Cave3DStation > path )
  {
    boolean ret = false;
    // GlPath gl_path_old = glPath;
    if ( path != null && path.size() > 1 ) {
      // Log.v("TopoGL-PATH", "make with size " + path.size() );
      GlPath gl_path = new GlPath( mContext, TglColor.ColorStation );
      for ( Cave3DStation station : path ) {
        gl_path.addVertex( station, mXmed, mYmed, mZmed );
      }
      gl_path.initData();
      // gl_path.logMinMax();
      synchronized( this ) { glPath = gl_path; }
      ret = true;
    } else {
      // Log.v("TopoGL-PATH", "clear path");
      synchronized( this ) { glPath = null; }
    }
    // if ( gl_path_old != null ) gl_path_old.releaseBuffer(); // FIXME doesnot do anything
    // if ( glPath != null ) glPath.logMinMax();
    return ret;
  }

  // legs must have already been reduced ( bbox must be symmetric )
  void prepareGridAndFrame( GlLines legs, float grid_size, float delta )
  {
    // Log.v("TopoGL", "BBox " + legs.getBBoxString() );

    float xmin = legs.getXmin() - delta;
    float xmax = legs.getXmax() + delta;
    float zmin = legs.getZmin() - delta;
    float zmax = legs.getZmax() + delta;
    
    makeGrid(  xmin, xmax, zmin, zmax, legs.getYmin(), legs.getYmax(), grid_size );
    makeFrame( xmin, xmax, zmin, zmax, legs.getYmin(), legs.getYmax() );
  }

  void prepareWalls( ConvexHullComputer computer, boolean make )
  {
    if ( ! make ) {
      clearWalls();
      return;
    }
    if ( computer == null ) return;
    GlTriangles walls = new GlTriangles( mContext );
    for ( CWConvexHull cw : computer.getWalls() ) {
      for ( CWTriangle tr : cw.mFace ) {
        Vector3D v1 = new Vector3D( tr.v1.x - mXmed, tr.v1.z - mYmed, -tr.v1.y - mZmed );
        Vector3D v2 = new Vector3D( tr.v2.x - mXmed, tr.v2.z - mYmed, -tr.v2.y - mZmed );
        Vector3D v3 = new Vector3D( tr.v3.x - mXmed, tr.v3.z - mYmed, -tr.v3.y - mZmed );
        walls.addTriangle( v1, v2, v3 );
      }
    }
    walls.initData();
    synchronized( this ) { glWalls = walls; }
    // Log.v("TopoGL", "convex hull triangles " + walls.triangleCount );
  }

  void prepareWalls( PowercrustComputer computer, boolean make )
  {
    if ( ! make ) {
      clearWalls();
      return;
    }
    if ( computer == null ) return;
    GlTriangles walls = new GlTriangles( mContext );
    for ( Triangle3D tr : computer.getTriangles() ) {
      // Vector3D v1 = new Vector3D( tr.vertex[0].x - mXmed, tr.vertex[0].z - mYmed, -tr.vertex[0].y - mZmed );
      // Vector3D v2 = new Vector3D( tr.vertex[1].x - mXmed, tr.vertex[1].z - mYmed, -tr.vertex[1].y - mZmed );
      // Vector3D v3 = new Vector3D( tr.vertex[2].x - mXmed, tr.vertex[2].z - mYmed, -tr.vertex[2].y - mZmed );
      // walls.addTriangle( v1, v2, v3 );
      walls.addTriangle( tr, mXmed, mYmed, mZmed );
    }
    walls.initData();
    synchronized( this ) { glWalls = walls; }
    // Log.v("TopoGL", "powercrust triangles " + walls.triangleCount );
    preparePlanAndProfile( computer );
  }

  private void preparePlanAndProfile( PowercrustComputer computer )
  {
    if ( computer.hasPlanview() ) {
      GlLines plan = new GlLines( mContext, TglColor.ColorPlan );
      for ( Cave3DPolygon poly : computer.getPlanview() ) {
        int nn = poly.size();
        if ( nn > 2 ) {
          Vector3D p1 = new Vector3D( poly.get( nn - 1 ) );
          p1.z = mZ0Min;
          for ( int k = 0; k < nn; ++k ) {
            Vector3D p2 = new Vector3D( poly.get( k ) );
            p2.z = mZ0Min;
            plan.addLine( p1, p2, 4, false, mXmed, mYmed, mZmed );
            p1 = p2;
          }
        }
      }
      plan.initData();
      synchronized( this ) { glPlan = plan; }
    }
    if ( computer.hasProfilearcs() ) {
      GlLines profile = new GlLines( mContext, TglColor.ColorPlan );
      for ( Cave3DSegment sgm : computer.getProfilearcs() ) {
        Vector3D p1 = sgm.v1;
        Vector3D p2 = sgm.v2;
        profile.addLine( p1, p2, 4, false, mXmed, mYmed, mZmed );
      }
      profile.initData();
      synchronized( this ) { glProfile = profile; }
    }
  }

  void prepareDEM( ParserDEM dem ) 
  {
    if ( dem == null ) return;
    // Log.v("TopoGL", "prepare DEM");
    GlSurface surface = new GlSurface( mContext );
    surface.initData( dem, mXmed, mYmed, mZmed );
    synchronized( this ) { glSurface = surface; }
    glLegs.prepareDepthBuffer( mParser.getShots(), dem );
  }
  
  void prepareSurfaceLegs( TglParser parser, DEMsurface surface )
  {
    if ( parser == null || surface == null ) return;
    // Log.v("TopoGL", "prepare surface legs");
    GlLines surface_legs = new GlLines( mContext, TglColor.ColorSurfaceLeg );
    for ( Cave3DShot leg : parser.getShots() ) {
      Vector3D v1 = new Vector3D( leg.from_station );
      Vector3D v2 = new Vector3D( leg.to_station );
      v1.z = surface.computeZ( v1.x, v1.y );
      v2.z = surface.computeZ( v2.x, v2.y );
      surface_legs.addLine( v1, v2, 3, false, mXmed, mYmed, mZmed ); // 3: color_index, false: fixed colors
    }
    surface_legs.computeBBox();
    surface_legs.initData();
    synchronized( this ) { glSurfaceLegs = surface_legs; }
  }

  // ----------------------------------------------------------------------
  private void makeGrid( float x1, float x2, float z1, float z2, float y1, float y2, float step )
  {
    int nx = 1 + (int)((x2-x1)/step);
    int nz = 1 + (int)((z2-z1)/step);
    // Log.v("TopoGL", "Grid NX " + nx + " NY " + nz + " cell " + step + " X0 " + x1 + " Y0 " + y1 + " Z0 " + z1 );
    int count = nx + nz + 1;
    float[] data = new float[ count*2 * 7 ];
    int k = 0;
    float x0 = x1;
    for ( int i=0; i<nx; ++i ) {
      data[ k++ ] = x0;
      data[ k++ ] = y1;
      data[ k++ ] = z1;
      data[ k++ ] = 0; data[ k++ ] = 0.3f; data[ k++ ] = 1.0f; data[ k++ ] = 1.0f;
      data[ k++ ] = x0;
      data[ k++ ] = y1;
      data[ k++ ] = z2;
      data[ k++ ] = 0.8f; data[ k++ ] = 0.2f; data[ k++ ] = 1.0f; data[ k++ ] = 1.0f;
      x0 += step;
    }

    float z0 = z1;
    for ( int i=0; i<nz; ++i ) {
      data[ k++ ] = x1;
      data[ k++ ] = y1;
      data[ k++ ] = z0;
      data[ k++ ] = 0.8f; data[ k++ ] = 0.7f; data[ k++ ] = 0.2f; data[ k++ ] = 1.0f;
      data[ k++ ] = x2;
      data[ k++ ] = y1;
      data[ k++ ] = z0;
      data[ k++ ] = 0f; data[ k++ ] = 0.8f; data[ k++ ] = 0f; data[ k++ ] = 1.0f;
      z0 += step;
    }

    data[ k++ ] = x1;
    data[ k++ ] = y1;
    data[ k++ ] = z2;
    data[ k++ ] = 1; data[ k++ ] = 0; data[ k++ ] = 0; data[ k++ ] = 1.0f;
    data[ k++ ] = x1;
    data[ k++ ] = y2;
    data[ k++ ] = z2;
    data[ k++ ] = 1; data[ k++ ] = 0; data[ k++ ] = 0; data[ k++ ] = 1.0f;
      
    glGrid = new GlLines( mContext, GlLines.COLOR_SURVEY );
    glGrid.setAlpha( 0.5f );
    glGrid.initData( data, count ); // , R.raw.line_acolor_vertex, R.raw.line_fragment );
  }
      
  private void makeFrame( float x1, float x2, float z1, float z2, float y1, float y2 )
  { 
    float[] data = new float[ 3*2 * 7 ];
    int k = 0;
    // line y1-y2
    data[ k++ ] = x1;
    data[ k++ ] = y1;
    data[ k++ ] = z2;
    data[ k++ ] = 1; data[ k++ ] = 0; data[ k++ ] = 0; data[ k++ ] = 1.0f;
    data[ k++ ] = x1;
    data[ k++ ] = y2;
    data[ k++ ] = z2;
    data[ k++ ] = 1; data[ k++ ] = 0; data[ k++ ] = 0; data[ k++ ] = 1.0f;

    // line x1-x2
    data[ k++ ] = x1;
    data[ k++ ] = y1;
    data[ k++ ] = z2;
    data[ k++ ] = 0; data[ k++ ] = 0.7f; data[ k++ ] = 0; data[ k++ ] = 1.0f;
    data[ k++ ] = x2;
    data[ k++ ] = y1;
    data[ k++ ] = z2;
    data[ k++ ] = 0; data[ k++ ] = 0.7f; data[ k++ ] = 0; data[ k++ ] = 1.0f;

    // line z1-z2
    data[ k++ ] = x1;
    data[ k++ ] = y1;
    data[ k++ ] = z1;
    data[ k++ ] = 0; data[ k++ ] = 0; data[ k++ ] = 1; data[ k++ ] = 1.0f;
    data[ k++ ] = x1;
    data[ k++ ] = y1;
    data[ k++ ] = z2;
    data[ k++ ] = 0; data[ k++ ] = 0; data[ k++ ] = 1; data[ k++ ] = 1.0f;
      
    glFrame = new GlLines( mContext, GlLines.COLOR_SURVEY);
    glFrame.setAlpha( 0.9f );
    glFrame.initData( data, 3 ); // , R.raw.line_acolor_vertex, R.raw.line_fragment );
  }

  void prepareModel( TglParser parser )
  {
    modelCreated = false;
    if ( parser.getShotNumber() == 0 ) {
      Log.e("TopoGL", "Error. Cannot create model witout shots");
      return;
    }
    mParser = parser;
    mZ0Min  = parser.getCaveZMin();
    GlLines legs   = new GlLines( mContext, GlLines.COLOR_NONE );
    GlLines splays = new GlLines( mContext, GlLines.COLOR_NONE );
    GlNames names  = new GlNames( mContext );

    // Log.v("TopoGL", "create model. shots " + parser.getShotNumber() + "/" + parser.getSplayNumber() + " stations " + parser.getStationNumber() );
    for ( Cave3DShot leg : parser.getShots() ) {
      legs.addLine( leg.from_station, leg.to_station, leg.mSurveyNr, true ); // leg.mSurveyNr = color-index
    }
    mXmed = (legs.getXmin() + legs.getXmax())/2;
    mYmed = (legs.getYmin() + legs.getYmax())/2;
    mZmed = (legs.getZmin() + legs.getZmax())/2;
    legs.reduceData( mXmed, mYmed, mZmed );
    // Log.v("TopoGL-MODEL", "center " + mXmed + " " + mYmed + " " + mZmed );
    // legs.logMinMax();
    
    for ( Cave3DShot splay : parser.getSplays() ) {
      if ( splay.from_station != null ) {
        splays.addLine( splay.from_station, splay.toPoint3D(), splay.mSurveyNr, true, mXmed, mYmed, mZmed );
      } else if ( splay.to_station != null ) {
        splays.addLine( splay.to_station, splay.toPoint3D(), splay.mSurveyNr, true, mXmed, mYmed, mZmed );
      }
    }
    splays.computeBBox();
    mZMin = legs.getYmin();
    float mZMax = legs.getYmax();
    // Log.v("TopoGL", "med " + mXmed + " " + mYmed + " " + mZmed + " Z " + mZMin + " " + mZMax );
    if ( mZMin > splays.getYmin() ) mZMin = splays.getYmin();
    if ( mZMax < splays.getYmax() ) mZMax = splays.getYmax();
    mZDelta = mZMax - mZMin;
    // Log.v("TopoGL", " after reduce Z " + mZMin + " " + mZMax + " DZ " + mZDelta );
    // splays.logMinMax();

    for ( Cave3DStation st : parser.getStations() ) {
      String name = st.short_name;
      if ( name != null && name.length() > 0 && ( ! name.equals("-") ) && ( ! name.equals(".") ) ) {
        // Log.v("TopoGL-NAME", "add " + st.short_name + " " + st.name );
        names.addName( st, st.short_name, st.name, mXmed, mYmed, mZmed );
      }
    }
    // names.logMinMax();

    DEMsurface surface = parser.getSurface();
    if ( surface != null ) {
      // Log.v("TopoGL", "parser has surface");
      GlSurface gl_surface = new GlSurface( mContext );
      gl_surface.initData( surface, mXmed, mYmed, mZmed, parser.surfaceFlipped() );
      if ( parser.mBitmap != null ) {
        Bitmap texture = parser.mBitmap.getBitmap( surface.mEast1, surface.mNorth1, surface.mEast2, surface.mNorth2 );
        if ( texture != null ) {
          gl_surface.setBitmap( texture );
        }
      }
      synchronized( this ) { glSurface = gl_surface; }
      prepareSurfaceLegs( parser, surface );
    }

    legs.initData( );
    splays.initData( );
    names.initData( );
    float grid_size = parser.getGridSize();
    prepareGridAndFrame( legs, grid_size, mGridExtent*grid_size );

    legs.prepareDepthBuffer( parser.getShots(), surface );

    mDiameter = legs.diameter();
    mY0med    = legs.getYmed();

    synchronized( this ) {
      glLegs = legs;
      glSplays = splays;
      glNames  = names;
    }
  }

  void createModel( )
  {
    if ( mParser == null ) return;
    modelCreated = true;
  }
}
