/** @file Cave3DRenderer.java
 *
 *e @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D model renderer
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

// import java.io.PrintStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

// import android.graphics.Matrix;
// import android.graphics.Bitmap;
// import android.graphics.RectF;
// import android.graphics.PorterDuff;
// import android.graphics.PointF;

import android.os.Handler;

// import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import android.widget.Toast;

import android.util.Log;

public class Cave3DRenderer // implements Renderer
{
  private static final String TAG = "Cave3D";

  private static float RAD2DEG = (float)(180/Math.PI);
  static float PI             = (float)(Math.PI);
  static float TWOPI          = (float)(2*Math.PI);
  static float PIOVERTWO      = (float)(Math.PI/2);
  static float THREEPIOVERTWO = (float)(3*Math.PI/2);

  private Cave3DPowercrust powercrust = null;
  private Cave3D mCave3D;

  private float[] coords;    // station coordinates: E, N, Z
  private float[] projs;      // pre-projections: X, Y, |Z|, Z
  private float[] projs_grid_E; // grid pre-projections
  private float[] projs_grid_N; // grid pre-projections
  private float[] projs_surface_E;
  private float[] projs_surface_N;
  private float[] projs_border_E;
  private float[] projs_border_N;

  public static final int WALL_NONE       = 0;
  public static final int WALL_CW         = 1;
  public static final int WALL_POWERCRUST = 2;
  public static final int WALL_DELAUNAY   = 3;
  public static final int WALL_HULL       = 4;
  public static final int WALL_MAX        = 3;
  int wall_mode = WALL_NONE;

  private float ZOOM = 100;

  float xmin;
  float xmax;
  float ymin;
  float ymax;
  float zmin;
  float zmax;

  float shift_factor;
  private static final float rotation_factor = 0.002f;

  public static final int STROKE_WIDTH_SHOT = 2;
  public static final int STROKE_WIDTH_SPLAY = 1;
  public static final int STROKE_WIDTH_STATION = 1;

  public static final int SURVEY_PAINT_NR = 6;

  private static final int GRID_SIZE = 5;
  private static final int GRID_SIZE2 = 2*GRID_SIZE;

  private static Paint northPaintAbove;
  private static Paint northPaintBelow;
  private static Paint eastPaintAbove;
  private static Paint eastPaintBelow;
  private static Paint vertPaint;

  private static Paint shotPaint;
  private static Paint splayPaint;
  private static Paint splayPaintPoint;
  private static Paint stationPaint;
  private static Paint surveyPaint[];
  private static Paint surfacePaint;
  private static Paint borderPaint;
  private static Paint splitPaint;
  private static int surveyColor[] = 
    { 0xffff0000, 0xff00ff00, 0xff0000ff, 0xffff00ff, 0xffffff00, 0xff00ffff };

  static void setStationPaintTextSize( int size )
  {
    stationPaint.setTextSize( size );
  }

  private static final int COLOR_NONE = 0;
  private static final int COLOR_SURVEY = 1;
  private static final int COLOR_DEPTH = 2;
  private static final int COLOR_MAX = 3;
  private int colorMode;

  private static final int FRAME_NONE  = 0;
  private static final int FRAME_GRID  = 1;
  private static final int FRAME_FRAME = 2;
  private static final int FRAME_MAX   = 3;
  private int frameMode;

  // public static final int VIEW_ORTHO   = 0;
  // public static final int VIEW_FRUSTUM = 1;
  // public static final int VIEW_MAX     = 2;
  // private int view_mode;

  private int grid_step;               // set by prepareModel() nevr changed afterwards
  private List< Cave3DDrawPath > paths_frame;
  private List< Cave3DDrawPath > paths_legs;
  private List< Cave3DDrawPath > paths_splays;
  private List< Cave3DDrawPath > paths_wires;
  private List< Cave3DDrawPath > paths_stations;
  private List< Cave3DDrawPath > paths_walls;
  private List< Cave3DDrawPath > paths_borders;
  private List< Cave3DDrawPath > paths_surface;
  private List< Cave3DDrawPath > paths_planview;

  private ArrayList< Cave3DPolygon > planview = null;
  private ArrayList< Cave3DPolygon > profileview = null;

  private boolean do_paths_frame;
  private boolean do_paths_legs;
  private boolean do_paths_splays;
  private boolean do_paths_stations;
  private boolean do_paths_walls;
  private boolean do_paths_planview;
  // private boolean do_paths_borders;
  private boolean do_paths_surface;

  // private boolean do_repaint;

  // private Context context;

  // private float red, green, blue;
  // private int mode;
  // private float delta_axes;
  private int dim_axes;
  // private int dim_axes1;
  private int dim_axes2;

  // these are set to the canvas center and never changed
  private float xview0;  // center of the canvas
  private float yview0;
  
  private int do_splays;
  static final int DO_SPLAY_NO    = 0;
  static final int DO_SPLAY_SHOT  = 1;
  static final int DO_SPLAY_POINT = 2;
  static final int DO_SPLAY_WIRE  = 3; 
  static final int DO_SPLAY_MAX   = 3;

  private boolean do_stations;
  private boolean do_surface;
  private int do_planview = 0;

  private int nr_shots;      // number of shots
  private int nr_splays;     // number of splay shots
  private int nr_station;    // number of stations

  private ArrayList< Cave3DStation > stations;
  private ArrayList< Cave3DShot    > shots;
  private ArrayList< Cave3DShot    > splays;
  private WireFrame mWireFrame;
  private ArrayList< Cave3DTriangle > triangles_hull       = null;
  private ArrayList< Cave3DTriangle > triangles_delaunay   = null;
  private ArrayList< Cave3DTriangle > triangles_powercrust = null;
  private Cave3DSite[] vertices_powercrust;
  private ArrayList< CWConvexHull > walls   = null;
  private ArrayList< CWBorder >     borders = null;
  private Cave3DSurface mSurface;

  Cave3DShot getShot( int k ) { return shots.get(k); }

  boolean hasSurface() { return mSurface != null; }
  Cave3DSurface getSurface() { return mSurface; }

  boolean hasWall() { return triangles_powercrust != null || walls != null; }

  boolean hasPlanview() { return planview != null; }

  // TODO VECTOR
  private float x0, y0, z0;       // model center coords (remain fixed)
  private float xoff, yoff, zoff; // model offsets in the view (changed in resetGeometry and changeParams )
  private float xc, yc, zc;       // camera coords (computed in makeNZ )
  // private Cave3DVector v0;
  // private Cave3DVector voff;
  // private Cave3DVector vc;
  
  // TODO VECTOR
  // these are set only by makeNZ()
  private float nxx, nxy, nxz; // projection plane X-axis
  private float nyx, nyy, nyz; // projection plane Y-axis
  private float nzx, nzy, nzz; // projection plane Z-axis (from camera to plane)
  private float nxxZ, nxyZ, nxzZ; // projection plane X-axis
  private float nyxZ, nyyZ, nyzZ; // projection plane Y-axis
  // private float nzxZ, nzyZ, nzzZ; // projection plane Z-axis (from camera to plane)

  // private Cave3DVector nx;
  // private Cave3DVector ny;
  // private Cave3DVector nz;

  private float zoom0;      // reset focal plane
  private float zoom;       // focal plane distance from the camera
  private float radius0;    // initial distance camera-origin (set when computeModel)
  float radius;             // distance camera-origin (set by resetGeometry and never changed)
  // float theta;  // camera polar angle
  float clino;  // camera clino angle (= 90 - theta )
  float phi;    // camera longitude angle


  private Cave3DParser mParser = null;         // model parser

  boolean hasParser( ) 
  {
    return mParser != null && mParser.do_render;
  }
  

// -----------------------------------------------------------
// info getters
// -----------------------------------------------------------
  public int getNrShots()     { return nr_shots; }
  public int getNrSplays()    { return nr_splays; }
  public int getNrStations()  { return nr_station; }
  public int getGrid()        { return grid_step; }

  public int getNrSurveys()   
  { 
    return ( mParser == null )? 0 : mParser.getSurveySize();
  }

  public float getCaveLength()
  { 
    return ( mParser == null )? 0 : mParser.mCaveLength;
  }

  public float getCaveDepth() 
  { 
    return ( mParser == null )? 0 : mParser.zmax - mParser.zmin;
  }

  public float getConvexHullVolume() 
  {
    float vol = 0;
    if ( walls != null && borders != null ) {
      for ( CWConvexHull cw : walls ) {
        vol += cw.getVolume();
      }
      for ( CWBorder cb : borders ) {
        vol -= cb.getVolume();
      }
    }
    return vol / 6;
  }

  public float getPowercrustVolume()
  {
    if ( triangles_powercrust == null || vertices_powercrust == null ) return 0;
    Cave3DVector cm = new Cave3DVector();
    int nv = vertices_powercrust.length;
    for ( int k = 0; k < nv; ++k ) cm.add( vertices_powercrust[k] );
    cm.mul( 1.0f / nv );
    float vol = 0;
    for ( Cave3DTriangle t : triangles_powercrust ) vol += t.volume( cm );
    return vol / 6;
  }

  public void computePowercrustProfileView()
  {
    if ( triangles_powercrust == null || vertices_powercrust == null ) return;
    profileview = new ArrayList< Cave3DPolygon >();
    int nst = stations.size();
    int nsh = shots.size();
    int S[] = new int[ nsh ];
    Cave3DPoint F[] = new Cave3DPoint[ nsh ]; // P - from
    Cave3DPoint T[] = new Cave3DPoint[ nsh ]; // P - to
    Cave3DPoint P[] = new Cave3DPoint[ nsh ]; // point on bisecant
    Cave3DPoint B[] = new Cave3DPoint[ nst ]; // bisecant
    Cave3DPoint M[] = new Cave3DPoint[ nsh ]; // midpoints

    // find bisecant of shots at st
    for ( int k=0; k < nst; ++k ) {
      Cave3DStation st = stations.get(k);
      Cave3DShot sh1 = null;
      Cave3DShot sh2 = null;
      // find shots at st
      for ( Cave3DShot sh : shots ) {
        if ( sh.from_station == st || sh.to_station == st ) {
          if ( sh1 == null ) {
            sh1 = sh;
          } else {
            sh2 = sh;
            break;
          }
        }
      }
      if ( sh2 != null ) {
        Cave3DStation st1 = ( sh1.from_station == st )? sh1.to_station : sh1.from_station;
        Cave3DStation st2 = ( sh2.from_station == st )? sh2.to_station : sh2.from_station;
        float dx1 = st1.e - st.e;
        float dy1 = st1.n - st.n;
        float d1  = (float)Math.sqrt( dx1*dx1 + dy1*dy1 );
        dx1 /= d1;
        dy1 /= d1;
        float dx2 = st2.e - st.e;
        float dy2 = st2.n - st.n;
        float d2  = (float)Math.sqrt( dx2*dx2 + dy2*dy2 );
        dx2 /= d2;
        dy2 /= d2;
        float dx = dx1 + dx2;
        float dy = dy1 + dy2;
        // float d   = (float)Math.sqrt( dx*dx + dy*dy );
        // B[k] = new Cave3DPoint( dx/d, dy/d );
        B[k] = new Cave3DPoint( dx, dy );
      } else if ( sh1 != null ) {
        Cave3DStation st1 = ( sh1.from_station == st )? sh1.to_station : sh1.from_station;
        float dx1 = st1.e - st.e;
        float dy1 = st1.n - st.n;
        // float d1  = (float)Math.sqrt( dx1*dx1 + dy1*dy1 );
        // B[k] = new Cave3DPoint( dy1/d1, -dx1/d1 );
        B[k] = new Cave3DPoint( dy1, -dx1 ); // no need to normalize
      } else {
        Log.v("Cave3D", "ERROR missing station shots at " + st.name );
        B[k] = new Cave3DPoint( 0, 0 ); // ERROR
      }
    }

    // find midpoints
    for ( int k = 0; k < nsh; ++k ) {
      Cave3DShot sh = shots.get(k);
      Cave3DStation fr = sh.from_station;
      Cave3DStation to = sh.to_station;
      F[k] = new Cave3DPoint( fr.e, fr.n );
      T[k] = new Cave3DPoint( to.e, to.n );
      M[k] = new Cave3DPoint( (fr.e+to.e)/2, (fr.n+to.n)/2 );
      // intersection of bisecants
      Cave3DPoint b1 = null;
      Cave3DPoint b2 = null;
      for (int kk=0; kk<nst; ++kk ) {
        Cave3DStation st = stations.get(kk);
        if ( st == fr ) { b1 = B[kk]; if ( b2 != null ) break; }
        else if ( st == to ) { b2 = B[kk]; if ( b1 != null ) break; }
      }
      // lines: fr + b1 * t
      //        to + b2 * s
      // ie  b1.x t - b2.x s = to.x - fr.x
      //     b1.y t - b2.y s = to.y - fr.y
      float a11 = b1.x;  float a12 = -b2.x;  float c1 = to.e - fr.e;
      float a21 = b1.y;  float a22 = -b2.y;  float c2 = to.n - fr.n;
      float det = a11 * a22 - a12 * a21;
      float t = ( a22 * c1 - a12 * c2 ) / det;
      // float s = ( a11 * c2 - a21 * c1 ) / det;
      P[k] = new Cave3DPoint( fr.e + a11 * t, fr.n + a21 * t );
      if ( k == 0 ) {
        S[k] = 1;
      } else {
        // check ( P[k] - fr ) * (P[k1] - fr )
        float z = (P[k].x - fr.e)*(P[k-1].x - fr.e) + (P[k].y - fr.n)*(P[k-1].y - fr.n);
        S[k] = (z>0)? S[k-1] : -S[k-1];
      }
    }

    // clear sites angles
    int nvp = vertices_powercrust.length;
    for ( int k=0; k<nvp; ++k ) vertices_powercrust[k].angle = null;

    // project triangles
    int nup = 0;
    for ( Cave3DTriangle t : triangles_powercrust ) {
      int nn = t.size;
      if ( nn <= 2 ) continue;
      Cave3DPoint c = new Cave3DPoint( t.center.x, t.center.y );
      for ( int k=0; k<nsh; ++k ) {
        float dx = P[k].x - c.x;
        float dy = P[k].y - c.y;
        float zf = (P[k].x - F[k].x)*dy - (P[k].y-F[k].y)*dx;
        float zt = (P[k].x - T[k].x)*dy - (P[k].y-T[k].y)*dx;
        if ( zf * zt <= 0 ) {
          Cave3DPoint n = new Cave3DPoint( t.normal.x, t.normal.y );
          if ( (n.x*dx + n.y*dy)*S[k] > 0 ) {
            nup ++;
            Cave3DSite s1 = (Cave3DSite)t.vertex[nn-2];
            Cave3DSite s0 = (Cave3DSite)t.vertex[nn-1];
            for ( int kk=0; kk<nn; ++kk ) {
              Cave3DSite s2 = (Cave3DSite)t.vertex[kk];
              s0.insertAngle( s1, s2 );
              s1 = s0;
              s0 = s2;
            }
          }
          break;
        }
      }
    }
    profileview = new ArrayList< Cave3DPolygon >();
    ArrayList< Cave3DPolygon > tmp = new ArrayList< Cave3DPolygon >();
    makePolygons( tmp );
    // Log.v("Cave3D", "profile polygons " + tmp.size() );
    for ( Cave3DPolygon poly : tmp ) {
      Cave3DPolygon poly2 = new Cave3DPolygon();
      for ( Cave3DSite site : poly.points ) {
        float x = site.x;
        float y = site.y;
        // find the station the site lies close to
        Cave3DStation st = null;
        float dmin = 0;
        for ( Cave3DStation st1 : stations ) {
          float d = (x - st1.e)*(x - st1.e) + (y - st1.n)*(y - st1.n);
          if ( st == null || d < dmin ) { dmin = d; st = st1; }
        }
        for ( int k = 0; k < nsh; ++ k ) {
          Cave3DShot sh = shots.get(k);
          if ( sh.from_station != st && sh.to_station != st ) continue;
          float dx = P[k].x - x;
          float dy = P[k].y - y;
          float zf = (P[k].x - F[k].x)*dy - (P[k].y-F[k].y)*dx;
          float zt = (P[k].x - T[k].x)*dy - (P[k].y-T[k].y)*dx;
          if ( zf * zt <= 0 ) {
            // project from P[k] onto the line F[k]-T[k]:
            // intersection of F.x + (T.x-F.x) t = P.x + (site.x-P.x) s
            // ie,   (Tx-Fx) t + (Px-site.x) s = Px - Fx
            float a11 = T[k].x - F[k].x;  float a12 = dx;   float c1 = P[k].x - F[k].x;
            float a21 = T[k].y - F[k].y;  float a22 = dy;   float c2 = P[k].y - F[k].y;
            float det = a11 * a22 - a12 * a21;
            float t = ( a22 * c1 - a12 * c2 ) / det;
            Cave3DSite s1 = new Cave3DSite( F[k].x + a11*t, F[k].y + a21*t, site.z );
            poly2.addPoint( s1 );
            s1.poly = poly2;
            break;
          }
        }
      }
      profileview.add( poly2 );
    }
  }

  public void computePowercrustPlanView()
  {
    if ( triangles_powercrust == null || vertices_powercrust == null ) return;
    float eps = 0.01f;
    int nup = 0;
    for ( Cave3DTriangle t : triangles_powercrust ) {
      if ( t.normal.z < 0 ) {
        int nn = t.size;
        if ( nn > 2 ) { 
          nup ++;
          Cave3DSite s1 = (Cave3DSite)t.vertex[nn-2];
          Cave3DSite s0 = (Cave3DSite)t.vertex[nn-1];
          for ( int k=0; k<nn; ++k ) {
            Cave3DSite s2 = (Cave3DSite)t.vertex[k];
            s0.insertAngle( s1, s2 );
            s1 = s0;
            s0 = s2;
          }
        }
        t.direction = 1;
      } else {
        t.direction = -1;
      }
    }
    planview = new ArrayList< Cave3DPolygon >();
    makePolygons( planview );
    // Log.v("Cave3D", "plan polygons " + planview.size() );
  }

  private void makePolygons( ArrayList< Cave3DPolygon > polygons )
  {
    // Log.v("Cave3D", "up triangles " + nup );
    int nsite = 0;
    for ( int k = 0; k<vertices_powercrust.length; ++k ) {
      Cave3DSite s0 = vertices_powercrust[k];
      if ( s0.poly != null ) continue;
      if ( s0.isOpen() ) {
        // Log.v("Cave3D", "found at " + k + " initial polygon vertex " + s0.x + " " + s0.y );
        Cave3DPolygon polygon = new Cave3DPolygon();
        polygon.addPoint( s0 );
        s0.poly = polygon;  
        for ( Cave3DSite s1 = s0.angle.v1; s1 != s0; s1=s1.angle.v1 ) {
          // if ( s1.poly != null ) {
          //   Log.v("Cave3D", "site on two polygons " + s1.x + "  " + s1.y );
          // } else {
          //   // Log.v("Cave3D", "add site to polygon  " + s1.x + "  " + s1.y );
          // }
          polygon.addPoint( s1 );
          s1.poly = polygon;
        }
        // Log.v("Cave3D", "polygon size " + polygon.size() );
        polygons.add( polygon );
        nsite += polygon.size();
      }
    }
    // Log.v("Cave3D", "polygon sites " + nsite );
  }

    
  ArrayList< Cave3DSurvey > getSurveys()
  { 
    return ( mParser == null )? null : mParser.getSurveys();
  }

  Cave3DSurvey getSurvey( String name ) 
  { 
    return ( mParser == null )? null : mParser.getSurvey(name); 
  }

// -----------------------------------------------------------
// display setup modifiers
// -----------------------------------------------------------
  // public void setMode( int m ) { mode = m; }

  public void toggleDoSplays() 
  { 
    do_splays = (do_splays+1)%DO_SPLAY_MAX; 
    // do_repaint = true;
    do_paths_splays = true;
  }

  public void toggleDoStations() 
  { 
    do_stations = ! do_stations; 
    // do_repaint = true;
    do_paths_stations = true;
  }

  public void toggleDoPlanview() 
  {
    if ( planview != null ) {
      do_planview = ( do_planview + 1 ) % 4;
      do_paths_planview = true;
    }
  }

  public int toggleWallMode()
  {
    for ( ; ; ) {
      wall_mode = ( wall_mode + 1 ) % WALL_MAX;
      if ( wall_mode == WALL_NONE ) break;
      if ( wall_mode == WALL_CW && walls != null /* && Cave3D.mWallConvexHull */ ) break;
      if ( wall_mode == WALL_POWERCRUST && triangles_powercrust != null /* && Cave3D.mWallPowercrust */ ) break;
      // if ( wall_mode == WALL_DELAUNAY && Cave3D.mWallDelaunay ) break;
      // if ( wall_mode == WALL_HULL && Cave3D.mWallHull ) break;
    }
    do_paths_walls   = (wall_mode != WALL_NONE);
    // do_paths_borders = (wall_mode != WALL_NONE);
    if ( ! do_paths_walls ) paths_walls.clear();
    return wall_mode;
  }

  public void toggleDoSurface()
  {
    if ( mSurface != null ) {
      do_surface = ! do_surface;
      do_paths_surface = true; 
    }
  }

  public void changeZoom( float f )
  {
    zoom *= f;
    makeNZ_2();
    precomputeProjectionsXY();
    setDoPaths();
  }

  public void zoomIn()  
  { 
    zoom *= 1.2;
    makeNZ_2();
    precomputeProjectionsXY();
    setDoPaths();
  }
  public void zoomOut() 
  { 
    zoom /= 1.2;
    makeNZ_2();
    precomputeProjectionsXY();
    setDoPaths();
  }
  public void zoomOne() 
  { 
    zoom = zoom0;
    if ( mParser != null ) {
      makeNZ_2();
      precomputeProjectionsXY();
      setDoPaths();
    }
  }


  public void changeParams( float x, float y, int mode ) 
  {
    // if ( mParser == null ) return;
    switch ( mode ) {
      case Cave3D.MODE_TRANSLATE: // scene offset
        x *= shift_factor / zoom;
        y *= shift_factor / zoom;
        // TODO VECTOR
        xoff += nxx * x + nyx * y;
        yoff += nxy * x + nyy * y;
        zoff += nxz * x + nyz * y;
        // voff.add( nx, x );
        // voff.add( ny, y );

        // Log.v(TAG, "translate: offset " + xoff + " " + yoff + " " + zoff );
        break;
      case Cave3D.MODE_ROTATE: // camera orientation
        x *= rotation_factor;
        y *= rotation_factor;
        // Log.v(TAG, "rotate " + x + " " + y );
        clino += y;
        phi   += x;
        if ( clino > PIOVERTWO ) {
          clino = PIOVERTWO;
        }
        if ( clino < -PIOVERTWO ) {
          clino = -PIOVERTWO;
        }
        while ( phi >= TWOPI ) phi -= TWOPI;
        while ( phi < 0 ) phi += TWOPI;
        break;
      // case Cave3D.MODE_ZOOM:
      //   if ( Math.abs( x ) > Math.abs( y ) ) {
      //     zoom *= 1 + x/10;
      //     makeNZ_2();
      //   } else {
      //     zoom *= 1 + y/10;
      //     makeNZ_2();
      //   }
      //   do_paths = true;
      //   break;
    }
    if ( mCave3D != null ) mCave3D.showTitle( clino * RAD2DEG, phi * RAD2DEG );
    makeNZ();
  }

  public void toggleColorMode()
  {
    colorMode = (colorMode + 1) % COLOR_MAX;
    do_paths_legs = true;
  }

  public void toggleFrameMode()
  {
    frameMode = (frameMode + 1) % FRAME_MAX;
    do_paths_frame = true;
  }

  // public int toggleViewMode()
  // {
  //   view_mode = (view_mode + 1) % VIEW_MAX;
  //   do_paths = true;
  //   return view_mode;
  // }



// -----------------------------------------------------------
// initialization
// -----------------------------------------------------------
  void makePaints() 
  {
    northPaintAbove = new Paint();
    northPaintAbove.setDither(true);
    northPaintAbove.setColor( 0x660099ff ); // blue
    northPaintAbove.setStyle(Paint.Style.STROKE);
    northPaintAbove.setStrokeJoin(Paint.Join.ROUND);
    northPaintAbove.setStrokeCap(Paint.Cap.ROUND);
    northPaintAbove.setStrokeWidth( 2 * STROKE_WIDTH_SHOT );

    northPaintBelow = new Paint();
    northPaintBelow.setDither(true);
    northPaintBelow.setColor( 0x6666ccff ); // lightblue
    northPaintBelow.setStyle(Paint.Style.STROKE);
    northPaintBelow.setStrokeJoin(Paint.Join.ROUND);
    northPaintBelow.setStrokeCap(Paint.Cap.ROUND);
    northPaintBelow.setStrokeWidth( 2 * STROKE_WIDTH_SHOT );

    eastPaintAbove = new Paint();
    eastPaintAbove.setDither(true);
    eastPaintAbove.setColor( 0x6666ff00 ); // green
    eastPaintAbove.setStyle(Paint.Style.STROKE);
    eastPaintAbove.setStrokeJoin(Paint.Join.ROUND);
    eastPaintAbove.setStrokeCap(Paint.Cap.ROUND);
    eastPaintAbove.setStrokeWidth( 2 * STROKE_WIDTH_SHOT );

    eastPaintBelow = new Paint();
    eastPaintBelow.setDither(true);
    eastPaintBelow.setColor( 0x66ccff66 ); // light green
    eastPaintBelow.setStyle(Paint.Style.STROKE);
    eastPaintBelow.setStrokeJoin(Paint.Join.ROUND);
    eastPaintBelow.setStrokeCap(Paint.Cap.ROUND);
    eastPaintBelow.setStrokeWidth( 2 * STROKE_WIDTH_SHOT );


    vertPaint = new Paint();
    vertPaint.setDither(true);
    vertPaint.setColor( 0x99ff00ff ); // pink
    vertPaint.setStyle(Paint.Style.STROKE);
    vertPaint.setStrokeJoin(Paint.Join.ROUND);
    vertPaint.setStrokeCap(Paint.Cap.ROUND);
    vertPaint.setStrokeWidth( 2 * STROKE_WIDTH_SHOT );

    shotPaint = new Paint();
    shotPaint.setDither(true);
    shotPaint.setColor( 0xffffffff ); // white
    shotPaint.setStyle(Paint.Style.STROKE);
    shotPaint.setStrokeJoin(Paint.Join.ROUND);
    shotPaint.setStrokeCap(Paint.Cap.ROUND);
    shotPaint.setStrokeWidth( STROKE_WIDTH_SHOT );

    splayPaint = new Paint();
    splayPaint.setDither(true);
    splayPaint.setColor( 0xff666666 ); // grey
    splayPaint.setStyle(Paint.Style.STROKE);
    splayPaint.setStrokeJoin(Paint.Join.ROUND);
    splayPaint.setStrokeCap(Paint.Cap.ROUND);
    splayPaint.setStrokeWidth( STROKE_WIDTH_SPLAY );

    splayPaintPoint = new Paint();
    splayPaintPoint.setDither(true);
    splayPaintPoint.setColor( 0xff666666 ); // grey
    splayPaintPoint.setStyle(Paint.Style.FILL);
    splayPaintPoint.setStrokeJoin(Paint.Join.ROUND);
    splayPaintPoint.setStrokeCap(Paint.Cap.ROUND);
    splayPaintPoint.setStrokeWidth( STROKE_WIDTH_SPLAY );

    stationPaint = new Paint();
    stationPaint.setDither(true);
    stationPaint.setColor( 0xffff6666 ); // dark red
    stationPaint.setStyle(Paint.Style.STROKE);
    stationPaint.setStrokeJoin(Paint.Join.ROUND);
    stationPaint.setStrokeCap(Paint.Cap.ROUND);
    stationPaint.setStrokeWidth( STROKE_WIDTH_STATION );
    stationPaint.setTextSize( Cave3D.mTextSize );  // FIXME

    surfacePaint = new Paint();
    surfacePaint.setDither(true);
    surfacePaint.setColor( 0x11ffffff ); // white
    surfacePaint.setStyle(Paint.Style.STROKE);
    surfacePaint.setStrokeJoin(Paint.Join.ROUND);
    surfacePaint.setStrokeCap(Paint.Cap.ROUND);
    surfacePaint.setStrokeWidth( STROKE_WIDTH_SHOT );

    borderPaint = new Paint();
    borderPaint.setDither(true);
    borderPaint.setColor( 0xff0000ff ); // blue
    // borderPaint.setStyle(Paint.Style.FILL );
    borderPaint.setStyle(Paint.Style.STROKE);

    splitPaint = new Paint();
    splitPaint.setDither(true);
    splitPaint.setColor( 0xffff0000 ); // red
    // splitPaint.setStyle(Paint.Style.FILL );
    splitPaint.setStyle(Paint.Style.STROKE);

    surveyPaint = new Paint[ SURVEY_PAINT_NR ];
    for (int k=0; k<SURVEY_PAINT_NR; ++k ) {
      surveyPaint[k] = new Paint();
      surveyPaint[k].setDither(true);
      surveyPaint[k].setColor( surveyColor[k] ); 
      surveyPaint[k].setStyle(Paint.Style.STROKE);
      surveyPaint[k].setStrokeJoin(Paint.Join.ROUND);
      surveyPaint[k].setStrokeCap(Paint.Cap.ROUND);
      surveyPaint[k].setStrokeWidth( STROKE_WIDTH_SHOT );
    }

  }

  public void resetGeometry()
  {
    zoom0 = ZOOM / radius0; // FIXME use 200.0
    // Log.v( TAG, "radius0 " + radius0 + " zoom0 " + zoom0 );
    zoom  = zoom0;   // projection scale
    clino = PIOVERTWO;
    phi   = 0;

    // TODO VECTOR
    xoff = 0; 
    yoff = 0; 
    zoff = 0; 
    // voff = new Cave3DVector( 0, 0, 0 );

    radius = radius0;

    if ( mParser != null ) {
      if ( mCave3D != null ) mCave3D.showTitle( clino * RAD2DEG, phi * RAD2DEG );
      makeNZ();
    }
  }

  public void setAngles( float a, float c )
  {
    phi   = a;
    clino = c;
    setDoPaths();
  }

// ------------------------------------------------------------
// cstr
// ------------------------------------------------------------
  // public void setCave3D( Cave3D cave3d )
  // {
  //   mCave3D = cave3d;
  // }

  public Cave3DRenderer( float width, float height )
  {
    mCave3D = null;

    do_splays   = DO_SPLAY_NO;
    do_stations = false;
    do_surface  = false;
    do_planview = 0;
    wall_mode   = WALL_NONE;

    // red   = 1;
    // green = 1;
    // blue  = 1;
    colorMode  = COLOR_NONE;
    frameMode  = FRAME_GRID;

    shots      = null;
    splays     = null;
    stations   = null;
    triangles_hull       = null;
    triangles_delaunay   = null;
    triangles_powercrust = null;
    planview = null;
    profileview = null;

    walls     = null;
    borders   = null;
    mSurface  = null;

    xview0 = width  / 2; // these are set and never changed again
    yview0 = height / 2;

    ZOOM = width / 2;

    mParser = null;
    // do_repaint = false;

    clearDoPaths();
    makePaints();

    paths_frame    = Collections.synchronizedList( new ArrayList< Cave3DDrawPath >() );
    paths_legs     = Collections.synchronizedList( new ArrayList< Cave3DDrawPath >() );
    paths_splays   = Collections.synchronizedList( new ArrayList< Cave3DDrawPath >() );
    paths_wires    = Collections.synchronizedList( new ArrayList< Cave3DDrawPath >() );
    paths_stations = Collections.synchronizedList( new ArrayList< Cave3DDrawPath >() );
    paths_planview = Collections.synchronizedList( new ArrayList< Cave3DDrawPath >() );
    paths_walls    = Collections.synchronizedList( new ArrayList< Cave3DDrawPath >() );
    paths_borders  = Collections.synchronizedList( new ArrayList< Cave3DDrawPath >() );
    paths_surface  = Collections.synchronizedList( new ArrayList< Cave3DDrawPath >() );
  }

  public boolean initRendering( Cave3D cave3d, String filename )
  {
    mParser = null;
    try {
      if ( filename.endsWith( ".th" ) || filename.endsWith( ".thconfig" ) ) {
        mParser = new Cave3DThParser( cave3d, filename );
      } else if ( filename.endsWith( ".lox" ) ) {
        mParser = new Cave3DLoxParser( cave3d, filename );
      } else {
        return false;
      }
      mCave3D = cave3d;
      CWConvexHull.resetCounters();
      prepareModel();
      // setDoPaths(); // NOT NECESSARY
      // computeProjection();
    } catch ( Cave3DParserException e ) {
      // Log.v( TAG, "parser exception " + filename );
      Toast.makeText( mCave3D, "parser error " + filename, Toast.LENGTH_SHORT).show();
      mParser = null;
    }
    return hasParser(); 
  }

  float computeCavePathlength( Cave3DStation s1, Cave3DStation s2 )
  {
    float d;
    mParser.setStationsPathlength( 1000000 );
    s1.setPathlength( 0 );
    Stack<Cave3DStation> stack = new Stack<Cave3DStation>();
    stack.push( s1 );
    while ( ! stack.empty() ) {
      Cave3DStation s0 = stack.pop();
      ArrayList< Cave3DShot > legs = mParser.getLegsAt( s0 );
      for ( Cave3DShot leg : legs ) {
        Cave3DStation s3 = leg.getOtherStation( s0 );
        if ( s3 != null ) {
          d = s0.getPathlength() + leg.len;
          if ( s3.getPathlength() > d ) {
            s3.setPathlength( d );
            stack.push( s3 );
          }
        }
      }
    }
    d = s2.getPathlength();
    return ( d < 999999 )? d : -1;
  }
    

  // called only by initRendering
  private void prepareModel()
  {
    nr_shots   = mParser.getShotNumber();
    nr_splays  = mParser.getSplayNumber();
    nr_station = mParser.getStationNumber();

    shots      = mParser.getShots();
    splays     = mParser.getSplays();
    stations   = mParser.getStations();
    coords     = mParser.getStationVertices();
    projs      = new float[4*nr_station];
    projs_grid_E = new float[ 2 * 2 * GRID_SIZE2 ];
    projs_grid_N = new float[ 2 * 2 * GRID_SIZE2 ];

    mWireFrame = null;
    computeWireFrame( 8.0, 0.01, 4 ); //  max 2 m,   coincide 0.01 m
    
    mSurface = mParser.getSurface();
    if ( mSurface != null ) {
      mCave3D.setButtonSurface();
      projs_surface_E = new float[ mSurface.mNr1 * mSurface.mNr2 ];
      projs_surface_N = new float[ mSurface.mNr1 * mSurface.mNr2 ];
    } else {
      projs_surface_E = null;
      projs_surface_N = null;
    }

    // Log.v("Cave3D", "make walls");

    // synchronized( paths_walls ) {
    //   makeConvexHull();
    //   makePowercrust();
    //   // makeDelaunay();
    //   // makeHull();
    // }

    // Log.v(TAG, "prepareModel() shots " + shots.size() 
    //          + " splays " + splays.size() 
    //          + " stations " + stations.size() );

    xmin = mParser.emin; // - 1;    // X along East
    xmax = mParser.emax; // + 1;
    ymin = mParser.nmax; // + 1;  // Y along N
    ymax = mParser.nmin; // - 1;
    zmin = mParser.zmin; // + 1;  // Z pointing upward
    zmax = mParser.zmax; // - 1;

    float max = ( xmax - xmin );
    // if ( zmax - zmin > max ) max = zmax-zmin;
    if ( ymax - ymin > max ) max = ymax-ymin;
    radius0 = max;
    
    // initial zoom to plan extent: do not use vertical extent
    // float dmax = (float)Math.abs(zmax - zmin);
    // radius0 = (dmax > max )? dmax : max;

    max /= 1.4f;
    if ( max < 10 ) {
      grid_step =   2;
    } else if ( max < 25 ) {
      grid_step =   5;
    } else if ( max < 50 ) {
      grid_step =  10;
    } else if ( max < 100 ) {
      grid_step =  20;
    } else if ( max < 250 ) {
      grid_step =  50;
    } else if ( max < 500 ) {
      grid_step = 100;
    } else {
      grid_step = 200;
    }
    // delta_axes = grid_step / max;
    // shift_factor = grid_step / 20;
    shift_factor = 1;
    // Log.v(TAG, "shif factor " + shift_factor + " zoom " + zoom );

    // TODO VECTOR
    x0 = (xmin + xmax)/2;  // center of the model
    y0 = (ymin + ymax)/2;
    z0 = (zmin + zmax)/2;
    // v0 = new Cave3DVector( (xmin + xmax)/2, (ymin + ymax)/2, (zmin + zmax)/2 );

    // Log.v(TAG, "bounds X " + xmin + " " + xmax + " Y " + ymin + " " + ymax
    //            + " Z " + zmin + " " + zmax );
    // Log.v(TAG, "origin " + x0 + " " + y0 + " " + z0 );

    resetGeometry();
  }

  public void makeConvexHull( boolean clear )
  {
    walls   = null;
    borders = null;
    if ( ! clear ) {
      if ( WALL_CW < WALL_MAX /* && Cave3D.mWallConvexHull */ ) {
        walls   = new ArrayList< CWConvexHull >();
        borders = new ArrayList< CWBorder >();
        for ( Cave3DShot sh : shots ) {
          Cave3DStation sf = sh.from_station;
          Cave3DStation st = sh.to_station;
          if ( sf != null && st != null ) {
            ArrayList< Cave3DShot > legs1 = mParser.getLegsAt( sf, st );
            ArrayList< Cave3DShot > legs2 = mParser.getLegsAt( st, sf );
            ArrayList< Cave3DShot > splays1 = mParser.getSplayAt( sf, false );
            ArrayList< Cave3DShot > splays2 = mParser.getSplayAt( st, false );
            // Log.v("Cave3D", "splays at " + sf.name + " " + splays1.size() + " at " + st.name + " " + splays2.size() );
            // if ( splays1.size() > 0 && splays2.size() > 0 ) 
            {
              try {
                CWConvexHull cw = new CWConvexHull( );
                cw.create( legs1, legs2, splays1, splays2, sf, st, Cave3D.mAllSplay );
                // TODO make convex-concave hull
                walls.add( cw );
              } catch ( RuntimeException e ) { 
                Log.v("Cave3D", "CW create runtime exception [2] " + e.getMessage() );
              }
            }
          }
        }
        // Log.v("Cave3D", "convex hulls done. split triangles " + Cave3D.mSplitTriangles );

        // for ( CWConvexHull cv : walls ) cv.randomizePoints( 0.1f );
        if ( Cave3D.mSplitTriangles ) {
          // synchronized( paths_borders ) 
          {
            // Log.v("Cave3D", "convex hulls borders. nr walls " + walls.size() );
            for ( int k1 = 0; k1 < walls.size(); ++ k1 ) {
              CWConvexHull cv1 = walls.get( k1 );
              for ( int k2 = k1+1; k2 < walls.size(); ++ k2 ) {
                CWConvexHull cv2 = walls.get( k2 );
                if ( cv1.mFrom == cv2.mFrom || cv1.mFrom == cv2.mTo || cv1.mTo == cv2.mFrom || cv1.mTo == cv2.mTo ) {
                  CWBorder cwb = new CWBorder( cv1, cv2, 0.00001f );
                  if ( cwb.makeBorder( ) ) {
                    borders.add( cwb );
                    cwb.splitCWTriangles();
                  } 
                }
              }
            }
            // Log.v("Cave3D", "convex hulls borders done, nr borders " + borders.size() );
          }
        }
      }
      // Toast.makeText( mCave3D, "computing convex hull walls", Toast.LENGTH_SHORT).show();
    }
    mCave3D.setButtonWall();
  }

  /*    // FIXME skip WALL_HULL triangles
  public void makeHull()
  {
    triangles_hull = null;
    if ( WALL_HULL < WALL_MAX ) {
      ArrayList< Cave3DHull > hulls = new ArrayList< Cave3DHull >();
      for ( Cave3DShot sh : shots ) {
        Cave3DStation sf = sh.from_station;
        Cave3DStation st = sh.to_station;
        if ( sf != null && st != null ) {
          ArrayList< Cave3DShot > legs1 = mParser.getLegsAt( sf, st );
          ArrayList< Cave3DShot > legs2 = mParser.getLegsAt( st, sf );
          ArrayList< Cave3DShot > splays1 = mParser.getSplayAt( sf, false );
          ArrayList< Cave3DShot > splays2 = mParser.getSplayAt( st, false );
          // Log.v("Cave3D", "splays at " + sf.name + " " + splays1.size() + " at " + st.name + " " + splays2.size() );
          // if ( splays1.size() > 0 && splays2.size() > 0 ) 
          {
            if ( WALL_HULL < WALL_MAX && Cave3D.mWallHull ) {
              hulls.add( new Cave3DHull( sh, splays1, splays2, sf, st ) );
            }
          }
        }
      }
      triangles_hull = new ArrayList< Cave3DTriangle >();
      for ( Cave3DHull h : hulls ) {
        if ( h.triangles != null ) {
          for ( Cave3DTriangle t : h.triangles ) {
            triangles_hull.add( t );
            // Log.v("Cave3D", t.toString() );
          }
        }
      }
    }
  }
  */

  /*    // FIXME skip WALL_DELAUNAY triangles
  public void makeDelaunay()
  {
    triangles_delaunay = null;
    if ( WALL_DELAUNAY < WALL_MAX ) {
      triangles_delaunay = new ArrayList< Cave3DTriangle >();
      for ( Cave3DStation st : stations ) {
        ArrayList< Cave3DShot > station_splays = mParser.getSplayAt( st, false );
        int ns = station_splays.size();
        
        if ( ns >= 4 ) {
          Cave3DVector[] vec = new Cave3DVector[ns]; // vector of the splays at station "st"
          for ( int n=0; n<ns; ++n ) {
            Cave3DShot sh = station_splays.get( n );
            float h = sh.len * (float)Math.cos( sh.cln );
            vec[n] = new Cave3DVector( h * (float)Math.sin(sh.ber), h * (float)Math.cos(sh.ber), sh.len * (float)Math.sin(sh.cln) );
          }
          Cave3DDelaunay delaunay = new Cave3DDelaunay( vec );
          delaunay.insertTrianglesIn( triangles_delaunay, st );
        }
      }
    }
  }
  */

  public void makePowercrust( boolean clear )
  {
    triangles_powercrust = null;
    vertices_powercrust  = null;
    if ( ! clear ) {
      if ( WALL_POWERCRUST < WALL_MAX /* && Cave3D.mWallPowercrust */ ) {
        // Log.v("Cave3D PC", "PowerCrust" );
        triangles_powercrust = new ArrayList< Cave3DTriangle >();
        try {
          // Toast.makeText( mCave3D, "computing the powercrust", Toast.LENGTH_SHORT).show();
          powercrust = new Cave3DPowercrust( );
          powercrust.resetSites( 3 );
          int ntot = stations.size();
          // Log.v("Cave3D PC", "... add sites (stations " + ntot + ")" );
          double x, y, z;
          for ( int n0 = 0; n0 < ntot; ++n0 ) {
            Cave3DStation st = stations.get( n0 );
            x = st.e;
            y = st.n;
            z = st.z;
            powercrust.addSite( x, y, z );
            ArrayList< Cave3DShot > station_splays = mParser.getSplayAt( st, false );
            int ns = station_splays.size();
            // Log.v("Cave3D", "station " + n0 + ": splays " + ns ); 
            for ( int n=0; n<ns; ++n ) {
              Cave3DShot sh = station_splays.get( n );
              double h = sh.len * Math.cos( sh.cln );
              x = st.e + h * Math.sin(sh.ber);
              y = st.n + h * Math.cos(sh.ber);
              z = st.z + sh.len * Math.sin(sh.cln);
              powercrust.addSite( x, y, z );
            }
            // long nsites = powercrust.nrSites();
            // Log.v("Cave3D PC", "after station " + n0 + "/" + ns + " sites " + nsites );
          }
          // long nsites = powercrust.nrSites();
          // Log.v("Cave3D PC", "done stations. sites " + nsites + ". compute ...");
          // Log.v("Cave3D PC", "total sites " + powercrust.nrSites() + " ... compute" );
          int ok = powercrust.compute();
          if ( ok == 1 ) {
            // Log.v("Cave3D PC", "... insert triangles" );
            vertices_powercrust = powercrust.insertTrianglesIn( triangles_powercrust );
          }
          // Log.v("Cave3D", "... release powercrust NP " + powercrust.np + " NF " + powercrust.nf );
          powercrust.release();
          // Log.v("Cave3D PC", "powercrust done" );
          if ( ok == 1 ) {
            Toast.makeText( mCave3D, "powercrust successful " + powercrust.np + "/" + powercrust.nf, Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText( mCave3D, "powercrust failed", Toast.LENGTH_SHORT).show();
          }
        } catch ( Exception e ) {
          Log.v("Cave3D", "ERROR: " + e.getMessage() );
        }
        // Log.v("Cave3D", "Powercrust V " + vertices_powercrust.length + " F " + triangles_powercrust.size() );
      }
      computePowercrustPlanView();
      computePowercrustProfileView();
    }
    mCave3D.setButtonWall();
  }

  private void makeNZ( )
  {
    float ct = (float)Math.cos( clino );
    float st = (float)Math.sin( clino );
    float cp = (float)Math.cos( phi );
    float sp = (float)Math.sin( phi );

    // TODO VECTOR
    nxx =  cp; // NX pointing rightwards (along phi)
    nxy =  sp;
    nxz = 0;
    // nx = new Cave3DVector( cp, sp, 0 );

    // TODO VECTOR
    nyx =  st * sp; // NY pointing downwards (opposite to clino)
    nyy = -st * cp;
    nyz = -ct;
    // ny = new Cave3DVector( st*sp, -st*cp, -ct );

    // TODO VECTOR
    nzx =  ct * sp;
    nzy = -ct * cp;
    nzz =  st;
    // nz = new Cave3DVector( -ct*sp, ct*cp, -st );

    makeNZ_2();

    // Camera = Origin + radius * NZ
    // TODO VECTOR
    xc = x0 + xoff + radius * nzx; // st * cp;
    yc = y0 + yoff + radius * nzy; // st * sp;
    zc = z0 + zoff + radius * nzz; // ct;
    // vc = v0.plus( voff ).plus( nz.times( radius ) );

    // Log.v(TAG, "Origin " + x0 + " " + y0 + " " + z0 );
    // Log.v(TAG, "Offset " + xoff + " " + yoff + " " + zoff );
    // Log.v(TAG, "Camera " + radius + ": " + xc + " " + yc + " " + zc );

    // Log.v(TAG, "NX " + nxx + " " + nxy + " " + nxz );
    // Log.v(TAG, "NY " + nyx + " " + nyy + " " + nyz );
    // Log.v(TAG, "NZ " + nzx + " " + nzy + " " + nzz );

    precomputeProjections();
    setDoPaths();
  }

  private void makeNZ_2()
  {
    nxxZ = nxx * zoom; 
    nxyZ = nxy * zoom;
    nxzZ = nxz * zoom;

    nyxZ = nyx * zoom; 
    nyyZ = nyy * zoom;
    nyzZ = nyz * zoom;

    // nzxZ = nzx * zoom; 
    // nzyZ = nzy * zoom;
    // nzzZ = nzz * zoom;
  }


  // projs_surface_E/N size n1 * n2 
  private void precomputeProjectionsSurface()
  {
    if ( mSurface == null ) {
      // Log.v( TAG, "precomputeProjectionsSurface null mSurface");
      return;
    }
    int n1 = mSurface.mNr1;
    int n2 = mSurface.mNr2;
    float x0 = (float)(mSurface.mEast1  - xc);
    float y0 = (float)(mSurface.mNorth1 - yc);
    float dp10_E = (float)(nxxZ * mSurface.mDim1);
    float dp01_E = (float)(nxyZ * mSurface.mDim2);
    float dp10_N = (float)(nyxZ * mSurface.mDim1);
    float dp01_N = (float)(nyyZ * mSurface.mDim2);

    // float n = y0;
    float p01_E = (float)(nxyZ * y0);
    float p01_N = (float)(nyyZ * y0);

    int k = 0;
    for ( int y = 0; y < n2; ++y ) {
      // float e = x0;
      float p10_E = (float)(xview0 + nxxZ * x0 + p01_E);
      float p10_N = (float)(yview0 + nyxZ * x0 + p01_N);

      for ( int x = 0; x < n1; ++x ) {
        float z = (float)(mSurface.mZ[ y * n1 + x ] - zc);
        projs_surface_E[k  ] = (float)( p10_E + nxzZ * z ); // (float)( xview0 + nxxZ * e + nxyZ * n + nxzZ * z );
        projs_surface_N[k++] = (float)( p10_N + nyzZ * z ); // (float)( yview0 + nyxZ * x + nyyZ * y + nyzZ * z);
        // e += mSurface.mDim1;
        p10_E += dp10_E;
        p10_N += dp10_N;
      }
      // n += mSurface.mDim2;
      p01_E += dp01_E;
      p01_N += dp01_N;
    }
  }

  void precomputeProjectionsGrid()
  {
    int k = 0;
    float x, y;
    float z = ( Cave3D.mGridAbove )? (float)( zmax - zc ) : (float)( zmin - zc );
    float y1 = y0 - GRID_SIZE * grid_step;
    float y2 = y0 + GRID_SIZE * grid_step;
    for (int i = -GRID_SIZE; i < GRID_SIZE; ++i ) {
      float xx = x0 + i * grid_step;
      x = (float)( xx - xc ); // vector camera->station
      y = (float)( y1 - yc );
      projs_grid_E[k++] = projectedX( x, y, z );
      projs_grid_E[k++] = projectedY( x, y, z );

      x = (float)( xx  - xc ); // vector camera->station
      y = (float)( y2  - yc );
      projs_grid_E[k++] = projectedX( x, y, z );
      projs_grid_E[k++] = projectedY( x, y, z );
    }

    k = 0;
    float x1 = x0 - GRID_SIZE * grid_step;
    float x2 = x0 + GRID_SIZE * grid_step;
    for (int i = -GRID_SIZE; i < GRID_SIZE; ++i ) {
      float yy = y0 + i * grid_step;
      x = (float)( x1 - xc ); // vector camera->station
      y = (float)( yy - yc );
      projs_grid_N[k++] = projectedX( x, y, z );
      projs_grid_N[k++] = projectedY( x, y, z );

      x = (float)( x2  - xc ); // vector camera->station
      y = (float)( yy  - yc );
      projs_grid_N[k++] = projectedX( x, y, z );
      projs_grid_N[k++] = projectedY( x, y, z );
    }
  }

  private void precomputeProjectionsXY()
  {
    if ( Cave3D.mPreprojection ) {
      // Log.v(TAG, "precompute projections XY");
      int k3 = 0;
      int k4 = 0;
      for ( int k=0; k<nr_station; ++k ) {
        float x = coords[k3++] - xc;
        float y = coords[k3++] - yc;
        float z = coords[k3++] - zc;
        projs[k4++] = projectedX( x, y, z );
        projs[k4  ] = projectedY( x, y, z );
        k4+=3;
      }
      precomputeProjectionsGrid();
      precomputeProjectionsSurface();
    }
  }
   
  private void precomputeProjections()
  {
    if ( Cave3D.mPreprojection ) {
      int k3 = 0;
      int k4 = 0;
      for ( int k=0; k<nr_station; ++k ) {
        float x = coords[k3++] - xc;
        float y = coords[k3++] - yc;
        float z = coords[k3++] - zc;
        projs[k4++] = projectedX( x, y, z );
        projs[k4++] = projectedY( x, y, z );
        z = projectedZ( x, y, z );
        projs[k4++] = (float)(Math.abs(z));
        projs[k4++] = (float)( z );
      }
      precomputeProjectionsGrid();
      precomputeProjectionsSurface();
    }
  }
    

  void setDoPaths()
  {
    do_paths_frame    = true;
    do_paths_legs     = true;
    do_paths_splays   = true;
    do_paths_planview = true;
    do_paths_stations = true;
    do_paths_walls    = ( wall_mode != WALL_NONE );
    // do_paths_borders  = ( wall_mode != WALL_NONE );
    do_paths_surface = ( mSurface != null );
  }

  void clearDoPaths()
  {
    do_paths_frame    = false;
    do_paths_legs     = false;
    do_paths_splays   = false;
    do_paths_planview = false;
    do_paths_stations = false;
    do_paths_walls    = false;
    // do_paths_borders  = false;
    do_paths_surface  = false;
  }



// ------------------------------------------------------------
// private drawings
// -----------------------------------------------------------
  // (x,y,z) = Point - Camera = vector Camera->Point
  // (x,y,z) + Offset = Point + Offset - Camera = vector Camera->OffsetedPoint
  //                  = Point - (Camera - Offset)
  //                  = Point - (Origin + radius * NZ - Offset)

  // TODO VECTOR
  private float projectedX( float x, float y, float z ) 
  {
    // canvas X is rightwards (nx is rightward)
    return (float)( xview0 + nxxZ * x + nxyZ * y + nxzZ * z );
  }

  private float projectedY( float x, float y, float z ) 
  {
    // canvas Y is downwards (ny is downward)
    return (float)( yview0 + nyxZ * x + nyyZ * y + nyzZ * z);
  }

  private float projectedZ( float x, float y, float z )
  {
    return ( nzx * x + nzy * y + nzz * z );
  }
  
  private float projectedZabs( float x, float y, float z ) 
  {
    // canvas Z is downwards (ny is downward)
    return (float)Math.abs( nzx * x + nzy * y + nzz * z );
  }

  // private float projectedX( Cave3DVector v ) 
  // {
  //   return (float)( xview0 + v.dot(nx) * zoom );
  // }
  //
  // private float projectedY( Cave3DVector v )
  // {
  //   return (float)( yview0 + v.dot(ny) * zoom );
  // }
  // 
  // private float projectedZabs( Cave3DVector v )
  // {
  //   return (float)Math.abs( v.dot(nz) );
  // }
  
  
  void drawStation( Cave3DStation st, Canvas canvas, Paint paint )
  {
    float x1,y1;
    if ( Cave3D.mPreprojection ) {
      int k4 = 4 * st.vertex;
      x1 = projs[ k4   ];
      y1 = projs[ k4+1 ];
    } else {
      // TODO VECTOR
      float x,y,z;
      x = st.e - xc; // vector camera->station
      y = st.n - yc;
      z = st.z - zc;
      x1 = projectedX( x, y, z );
      y1 = projectedY( x, y, z );
      // Cave3DVector v = new Cave3DVector( st ).minus( vc);
      // x1 = projectedX( v );
      // y1 = projectedY( v );
    }

    if ( x1 >= 0 && x1 < Cave3D.mDisplayWidth && y1 >= 0 && y1 < Cave3D.mDisplayHeight ) {
      // draw string
      String text = st.short_name;
      Path path = new Path();
      path.moveTo( x1, y1 );
      path.lineTo( x1+20*text.length(), y1 );
      canvas.drawTextOnPath( text, path, 0, 0, stationPaint );
    }
  }
  
  void addStation( Cave3DStation st )
  {
    float x1,y1;
    if ( Cave3D.mPreprojection ) {
      int k4 = 4 * st.vertex; 
      x1 = projs[ k4   ];
      y1 = projs[ k4+1 ];
    } else {
      // TODO VECTOR
      float x,y,z;
      x = st.e - xc; // vector camera->station
      y = st.n - yc;
      z = st.z - zc;
      x1 = projectedX( x, y, z );
      y1 = projectedY( x, y, z );
      // Cave3DVector v = new Cave3DVector( st ).minus( vc);
      // x1 = projectedX( v );
      // y1 = projectedY( v );
    }

    if ( x1 >= 0 && x1 < Cave3D.mDisplayWidth && y1 >= 0 && y1 < Cave3D.mDisplayHeight ) {
      // draw string
      Cave3DDrawTextPath path = new Cave3DDrawTextPath( stationPaint, st.short_name );
      path.path.moveTo( x1, y1 );
      path.path.lineTo( x1+20* st.short_name.length(), y1 );
      synchronized( paths_stations ) {
        paths_stations.add( path );
      }
    }
  }

  Cave3DStation getStationAt( float x0, float y0 )
  {
    if ( ! do_stations ) return null;

    for ( Cave3DStation st : stations ) {
      float x1, y1;
      if ( Cave3D.mPreprojection ) {
        int k4 = 4 * st.vertex;
        x1 = projs[ k4   ];
        y1 = projs[ k4+1 ];
      } else {
        float x = st.e - xc; // vector camera->station
        float y = st.n - yc;
        float z = st.z - zc;
        x1 = projectedX( x, y, z );
        y1 = projectedY( x, y, z );
      }
      if ( Math.abs(x1-x0) < 40 && Math.abs(y1-y0) < 40 ) { // FIXME
        return st;
      }
    }
    return null;
  }

  void addTriangle( Cave3DTriangle tr )
  {
    addTriangle( tr.normal, tr.vertex, CWTriangle.TRIANGLE_NORMAL );
  }
 
  void addTriangle( CWTriangle tr )
  {
    Cave3DVector vertex[] = new Cave3DVector[3];
    vertex[0] = tr.v1;
    vertex[1] = tr.v2;
    vertex[2] = tr.v3;
    addTriangle( tr.un, vertex, tr.mType );
  }
  
  private void addTriangle( Cave3DVector normal, Cave3DVector[] vertex, int type )
  {
    float n = projectedZ( normal.x,  normal.y,  normal.z );
    if ( n >= 0 ) return;

    int size = vertex.length;
    float x,y,z;
    float[] x1 = new float[size];
    float[] y1 = new float[size];
    int out = 0;
    for (int k=0; k<size; ++k ) {
      Cave3DVector s1 = vertex[k];
      x = s1.x - xc; // vector camera->station
      y = s1.y - yc;
      z = s1.z - zc;
      x1[k] = projectedX( x, y, z );
      y1[k] = projectedY( x, y, z );
      if ( x1[k] < 0 || x1[k] > Cave3D.mDisplayWidth || y1[k] < 0 || y1[k] > Cave3D.mDisplayHeight ) ++out;
    }
    if ( out == size ) return;

    int col = 0x99 + (int)( 0x66 * n );
    // if ( col > 0xff ) col = 0xff;

    Paint wallPaint = null;
    if ( type == CWTriangle.TRIANGLE_SPLIT ) {
      // wallPaint = splitPaint;
      wallPaint = new Paint();
      wallPaint.setDither(true);
      wallPaint.setColor( 0xffffffff ); // FIXME yellow
      wallPaint.setStyle(Paint.Style.FILL_AND_STROKE );
      wallPaint.setAlpha( col );
    } else if ( type == CWTriangle.TRIANGLE_HIDDEN ) {
      return;
    } else {
      wallPaint = new Paint();
      wallPaint.setDither(true);
      wallPaint.setColor( 0xffffffff ); // white
      wallPaint.setStyle(Paint.Style.FILL_AND_STROKE );
      wallPaint.setAlpha( col );
    }

    Cave3DDrawPath p = new Cave3DDrawPath( wallPaint );
    p.path.moveTo( x1[0], y1[0] );
    for ( int k = 1; k<size; ++k ) {
      p.path.lineTo( x1[k], y1[k] );
    }
    p.path.close();
    paths_walls.add( p );
  }

  // called under synchronized paths_borders
  private void addBorderLine( CWIntersection ii )
  {
    float x,y,z;
    float[] x1 = new float[2];
    float[] y1 = new float[2];
    CWLinePoint[] v = new CWLinePoint[2];
    v[0] = ii.mV1;
    v[1] = ii.mV2;
    
    int out = 0;
    for (int k=0; k<2; ++k ) {
      x = v[k].x - xc; // vector camera->station
      y = v[k].y - yc;
      z = v[k].z - zc;
      x1[k] = projectedX( x, y, z );
      y1[k] = projectedY( x, y, z );
      if ( x1[k] < 0 || x1[k] > Cave3D.mDisplayWidth || y1[k] < 0 || y1[k] > Cave3D.mDisplayHeight ) ++out;
    }
    if ( out == 2 ) return;

    Cave3DDrawPath p = new Cave3DDrawPath( borderPaint );
    p.path.moveTo( x1[0], y1[0] );
    p.path.lineTo( x1[1], y1[1] );
    paths_borders.add( p );
  }    

  private void computeWireFrame( double max, double eps, int nn )
  {
    if ( DO_SPLAY_WIRE < DO_SPLAY_MAX ) {
      mWireFrame = new WireFrame( eps );
      for ( Cave3DStation st : stations ) {
        mWireFrame.addPoint( st );
      }
      for ( Cave3DShot sh : splays ) {
        mWireFrame.addSplayPoint( sh.to_station );
      }

      mWireFrame.makeFrame( max, nn );
      // Log.v("Cave3D", "number of wires " + mWireFrame.getSegments().size() );
    }
  }

  void addWireSegment( WireSegment ws, Path path )
  {
    Cave3DStation s1 = ws.wp1;
    Cave3DStation s2 = ws.wp2;

    float x1,y1, x2,y2;
    float d1, d2;

    // if ( Cave3D.mPreprojection ) {
    //   int k4 = 4 * s1.vertex;
    //   x1 = projs[ k4++ ];
    //   y1 = projs[ k4++ ];
    //   d1 = projs[ k4   ];

    //   if ( ( k4 = 4 * s2.vertex) < 0 ) {
    //     float x = s2.e - xc; // vector camera->station
    //     float y = s2.n - yc;
    //     float z = s2.z - zc;
    //     x2 = projectedX( x, y, z );
    //     y2 = projectedY( x, y, z );
    //     d2 = projectedZabs( x, y, z );
    //   } else {
    //     x2 = projs[ k4++ ];
    //     y2 = projs[ k4++ ];
    //     d2 = projs[ k4   ];
    //   }
    // } else {
      // TODO VECTOR
      float x,y,z;
      x = s1.e - xc; // vector camera->station
      y = s1.n - yc;
      z = s1.z - zc;
      x1 = projectedX( x, y, z );
      y1 = projectedY( x, y, z );
      d1 = projectedZabs( x, y, z );
      // Cave3DVector v = new Cave3DVector( s1 ).minus( vc );
      // x1 = projectedX( v );
      // y1 = projectedY( v );
      // d1 = projectedZabs( v );

      // TODO VECTOR
      x = s2.e - xc; // vector camera->station
      y = s2.n - yc;
      z = s2.z - zc;
      x2 = projectedX( x, y, z );
      y2 = projectedY( x, y, z );
      d2 = projectedZabs( x, y, z );
      // v = new Cave3DVector( s1 ).minus( vc );
      // x2 = projectedX( v );
      // y2 = projectedY( v );
      // d2 = projectedZabs( v );
    // }

    if ( ( x1 >= 0 && x1 < Cave3D.mDisplayWidth && y1 >= 0 && y1 < Cave3D.mDisplayHeight ) 
      || ( x2 >= 0 && x2 < Cave3D.mDisplayWidth && y2 >= 0 && y2 < Cave3D.mDisplayHeight ) ) {
      // add draw-line 1-2
      path.moveTo( x1, y1 );
      path.lineTo( x2, y2 );
    }
  }

  void addSplayShot( Cave3DShot sh, Path path, Paint paint, boolean endpoint )
  {
    Cave3DStation s1 = sh.from_station;
    Cave3DStation s2 = sh.to_station;

    if ( s1 == null || s2 == null ) {
      // Log.e(TAG, "error shot with null station(s)" );
      return;
    }

    // Log.v( TAG, "shot " + s1.name + " " + s1.vertex + " - " + s2.name + " " + s2.vertex );
    // Log.v( TAG, "add shot " + s1.e + " " + s1.n + " " + s1.z );

    float x1,y1, x2,y2;
    float d1, d2;

    if ( Cave3D.mPreprojection ) {
      int k4 = 4 * s1.vertex;
      x1 = projs[ k4++ ];
      y1 = projs[ k4++ ];
      d1 = projs[ k4   ];

      if ( ( k4 = 4 * s2.vertex) < 0 ) {
        float x = s2.e - xc; // vector camera->station
        float y = s2.n - yc;
        float z = s2.z - zc;
        x2 = projectedX( x, y, z );
        y2 = projectedY( x, y, z );
        d2 = projectedZabs( x, y, z );
      } else {
        x2 = projs[ k4++ ];
        y2 = projs[ k4++ ];
        d2 = projs[ k4   ];
      }
    } else {
      // TODO VECTOR
      float x,y,z;
      x = s1.e - xc; // vector camera->station
      y = s1.n - yc;
      z = s1.z - zc;
      x1 = projectedX( x, y, z );
      y1 = projectedY( x, y, z );
      d1 = projectedZabs( x, y, z );
      // Cave3DVector v = new Cave3DVector( s1 ).minus( vc );
      // x1 = projectedX( v );
      // y1 = projectedY( v );
      // d1 = projectedZabs( v );

      // TODO VECTOR
      x = s2.e - xc; // vector camera->station
      y = s2.n - yc;
      z = s2.z - zc;
      x2 = projectedX( x, y, z );
      y2 = projectedY( x, y, z );
      d2 = projectedZabs( x, y, z );
      // v = new Cave3DVector( s1 ).minus( vc );
      // x2 = projectedX( v );
      // y2 = projectedY( v );
      // d2 = projectedZabs( v );
    }

    if ( ( x1 >= 0 && x1 < Cave3D.mDisplayWidth && y1 >= 0 && y1 < Cave3D.mDisplayHeight ) 
      || ( x2 >= 0 && x2 < Cave3D.mDisplayWidth && y2 >= 0 && y2 < Cave3D.mDisplayHeight ) ) {
      // add draw-line 1-2
      if ( endpoint ) {
        // path.moveTo( x2, y2 );
        path.addCircle( x2, y2, 5, Path.Direction.CCW );
      } else {
        path.moveTo( x1, y1 );
        path.lineTo( x2, y2 );
        if ( paint != null ) {
          int col = 0xff - (int)( 0x88 * ( d1+d2 ) / radius0 );
          if ( col < 0x66 ) { 
            col = 0x66;
          } 
          paint.setAlpha( col );
        }
      }
    }
  }

  void addLegShot( Cave3DShot sh, Path path, Paint paint )
  {
    Cave3DStation s1 = sh.from_station;
    Cave3DStation s2 = sh.to_station;

    if ( s1 == null || s2 == null ) {
      // Log.e(TAG, "error shot with null station(s)" );
      return;
    }

    // Log.v( TAG, "shot " + s1.name + " " + s1.vertex + " - " + s2.name + " " + s2.vertex );
    // Log.v( TAG, "add shot " + s1.e + " " + s1.n + " " + s1.z );

    float x1,y1, x2,y2;
    float d1, d2;

    if ( Cave3D.mPreprojection ) {
      int k4 = 4 * s1.vertex;
      x1 = projs[ k4++ ];
      y1 = projs[ k4++ ];
      d1 = projs[ k4   ];

      k4 = 4 * s2.vertex;
      x2 = projs[ k4++ ];
      y2 = projs[ k4++ ];
      d2 = projs[ k4   ];
    } else {
      // TODO VECTOR
      float x,y,z;
      x = s1.e - xc; // vector camera->station
      y = s1.n - yc;
      z = s1.z - zc;
      x1 = projectedX( x, y, z );
      y1 = projectedY( x, y, z );
      d1 = projectedZabs( x, y, z );
      // Cave3DVector v = new Cave3DVector( s1 ).minus( vc );
      // x1 = projectedX( v );
      // y1 = projectedY( v );
      // d1 = projectedZabs( v );

      // TODO VECTOR
      x = s2.e - xc; // vector camera->station
      y = s2.n - yc;
      z = s2.z - zc;
      // d2 = (float)Math.sqrt( x*x + y*y + z*z );
      x2 = projectedX( x, y, z );
      y2 = projectedY( x, y, z );
      d2 = projectedZabs( x, y, z );
      // v = new Cave3DVector( s1 ).minus( vc );
      // x2 = projectedX( v );
      // y2 = projectedY( v );
      // d2 = projectedZabs( v );
    }

    if ( ( x1 >= 0 && x1 < Cave3D.mDisplayWidth && y1 >= 0 && y1 < Cave3D.mDisplayHeight ) 
      || ( x2 >= 0 && x2 < Cave3D.mDisplayWidth && y2 >= 0 && y2 < Cave3D.mDisplayHeight ) ) {
      // add draw-line 1-2
      path.moveTo( x1, y1 );
      path.lineTo( x2, y2 );
      if ( paint != null ) {
        int col = 0xff - (int)( 0x88 * ( d1+d2 ) / radius0 );
        if ( col < 0x66 ) { 
          col = 0x66;
        } 
        paint.setAlpha( col );
      }
    }
  }

  void addSegment( float e1, float n1, float z1, float e2, float n2, float z2, Path path )
  // void addSegment( Cave3DVector v1, Cave3DVector v2, Path path )
  {
    float x1,y1, x2,y2;

    // TODO VECTOR
    float x, y, z;
    x = (float)( e1 - xc ); // vector camera->station
    y = (float)( n1 - yc );
    z = (float)( z1 - zc );
    x1 = projectedX( x, y, z );
    y1 = projectedY( x, y, z );
    // Cave3DVector v = v1.minus( vc );
    // x1 = projectedX( v );
    // y1 = projectedY( v );

    // TODO VECTOR
    x = (float)( e2 - xc ); // vector camera->station
    y = (float)( n2 - yc );
    z = (float)( z2 - zc );
    x2 = projectedX( x, y, z );
    y2 = projectedY( x, y, z );
    // v = v2.minus( vc );
    // x2 = projectedX( v );
    // y2 = projectedY( v );

    // if ( ( x1 < 0 && x2 < 0 ) || ( x1 > Cave3D.mDisplayWidth && x2 > Cave3D.mDisplayWidth ) ) return;
    // if ( ( y1 < 0 && y2 < 0 ) || ( y1 > Cave3D.mDisplayHeight && y2 > Cave3D.mDisplayHeight ) ) return;
    path.moveTo( x1, y1 );
    path.lineTo( x2, y2 );
  }

  void addGridLine( float e1, float n1, float e2, float n2, Path path )
  {
    float x1,y1, x2,y2;

    // TODO VECTOR
    float x, y, z;
    x = (float)( e1 - xc ); // vector camera->station
    y = (float)( n1 - yc );
    z = (float)( zmin - zc );
    x1 = projectedX( x, y, z );
    y1 = projectedY( x, y, z );
    // Cave3DVector v = new Cave3DVector( e1, n1, zmin ).minus( vc );
    // x1 = projectedX( v );
    // y1 = projectedY( v );

    // TODO VECTOR
    x = (float)( e2  - xc ); // vector camera->station
    y = (float)( n2  - yc );
    // z = (float)( zmin - zc );
    x2 = projectedX( x, y, z );
    y2 = projectedY( x, y, z );
    // v = new Cave3DVector( e2, n2, zmin ).minus( vc );
    // x2 = projectedX( v );
    // y2 = projectedY( v );

    if ( ( x1 < 0 && x2 < 0 ) || ( x1 > Cave3D.mDisplayWidth && x2 > Cave3D.mDisplayWidth ) ) return;
    if ( ( y1 < 0 && y2 < 0 ) || ( y1 > Cave3D.mDisplayHeight && y2 > Cave3D.mDisplayHeight ) ) return;
    // add grid horiz-line
    path.moveTo( x1, y1 );
    path.lineTo( x2, y2 );
  }

  // NOT USED
  // private void addVert( float e, float n, float z1, float z2, Path path )
  // {
  //   float x1,y1, x2,y2;

  //   // TODO VECTOR
  //   float x, y, z;
  //   x = (float)( e  - xc ); // vector camera->station
  //   y = (float)( n  - yc );
  //   z = (float)( z1 - zc );
  //   x1 = projectedX( x, y, z );
  //   y1 = projectedY( x, y, z );
  //   // Cave3DVector v = new Cave3DVector( e, n, z1 ).minus( vc );
  //   // x1 = projectedX( v );
  //   // y1 = projectedY( v );

  //   // TODO VECTOR
  //   x = (float)( e  - xc ); // vector camera->station
  //   y = (float)( n  - yc );
  //   z = (float)( z2 - zc );
  //   x2 = projectedX( x, y, z );
  //   y2 = projectedY( x, y, z );
  //   // v = new Cave3DVector( e, n, z2 ).minus( vc );
  //   // x2 = projectedX( v );
  //   // y2 = projectedY( v );

  //   if ( ( x1 < 0 && x2 < 0 ) || ( x1 > Cave3D.mDisplayWidth && x2 > Cave3D.mDisplayWidth ) ) return;
  //   if ( ( y1 < 0 && y2 < 0 ) || ( y1 > Cave3D.mDisplayHeight && y2 > Cave3D.mDisplayHeight ) ) return;
  //   // add grid vert-line
  //   path.moveTo( x1, y1 );
  //   path.lineTo( x2, y2 );
  // }


// --------------------------------------------------------------
// drawing
// --------------------------------------------------------------
  private boolean computePaths( )
  {
    if ( mParser == null || mParser.do_render == false ) return false;

    Cave3DDrawPath path;

    float x1, y1, z1, x2, y2, z2;
    Cave3DVector v1, v2;

    float ww = Cave3D.mDisplayWidth;
    float hh = Cave3D.mDisplayHeight;

    Paint northPaint = northPaintAbove;
    Paint eastPaint  = eastPaintAbove;
    if ( clino < 0 ) {
      northPaint = northPaintBelow;
      eastPaint  = eastPaintBelow;
    }

    // grid
    if ( do_paths_frame ) {
      do_paths_frame = false;
      synchronized( paths_frame ) {
        // Log.v("Cave3D", "compute paths: frame ");
        paths_frame.clear();
        if ( frameMode == FRAME_FRAME ) {
          // view frame
          float r = radius / 2;

          // TODO VECTOR
          x1 = x0 + xoff;
          y1 = y0 + yoff;
          // z1 = z0 + zoff;
          z1 = Cave3D.mGridAbove ? z0 + zoff - 2*r: z0 + zoff;
          // v1 = v0.plus( voff );

          // TODO VECTOR
          x2 = x1 + r;
          y2 = y1;
          z2 = z1;
          // v2 = v1.plus( r, 0.0, 0.0 );

          path  = new Cave3DDrawPath( eastPaint );
          addSegment( x1, y1, z1, x2, y2, z2, path.path );
          // addSegment( v1, v2, path.path );
          paths_frame.add( path );

          // TODO VECTOR
          x2 = x1;
          y2 = y1 + r;
          z2 = z1;
          // v2 = v1.plus( 0.0, r, 0.0 );

          path  = new Cave3DDrawPath( northPaint );
          addSegment( x1, y1, z1, x2, y2, z2, path.path );
          // addSegment( v1, v2, path.path );
          paths_frame.add( path );

          // TODO VECTOR
          x2 = x1;
          y2 = y1;
          z2 = Cave3D.mGridAbove ? z1 + 2*r : z1 + 2*r ;
          // v2 = v1.plus( 0.0, 0.0, 2*r );

          path  = new Cave3DDrawPath( vertPaint );
          addSegment( x1, y1, z1, x2, y2, z2, path.path );
          // addSegment( v1, v2, path.path );
          paths_frame.add( path );
        } else if ( frameMode == FRAME_GRID ) {
          path  = new Cave3DDrawPath( northPaint );
          if ( Cave3D.mPreprojection ) {
            for ( int k=0; k<GRID_SIZE2; ++k ) {
              int k4 = k*4;
              float x1f = projs_grid_E[k4++];
              float y1f = projs_grid_E[k4++];
              float x2f = projs_grid_E[k4++];
              float y2f = projs_grid_E[k4  ];
              if ( ( x1f < 0 && x2f < 0 ) || ( x1f > ww && x2f > ww ) ) continue;
              if ( ( y1f < 0 && y2f < 0 ) || ( y1f > hh && y2f > hh ) ) continue;
              // add grid horiz-line
              path.path.moveTo( x1f, y1f );
              path.path.lineTo( x2f, y2f );
            }
          } else {
            // TODO VECTOR
            y1 = y0 - GRID_SIZE * grid_step;
            y2 = y0 + GRID_SIZE * grid_step;
            for (int i = -GRID_SIZE; i < GRID_SIZE; ++i ) {
              float x = x0 + i * grid_step;
              addGridLine( x, y1, x, y2, path.path );
            }
          }  
          paths_frame.add( path );

          path  = new Cave3DDrawPath( eastPaint );
          if ( Cave3D.mPreprojection ) {
            for ( int k=0; k<GRID_SIZE2; ++k ) {
              int k4 = k*4;
              float x1f = projs_grid_N[k4++];
              float y1f = projs_grid_N[k4++];
              float x2f = projs_grid_N[k4++];
              float y2f = projs_grid_N[k4  ];
              if ( ( x1f < 0 && x2f < 0 ) || ( x1f > ww && x2f > ww ) ) continue;
              if ( ( y1f < 0 && y2f < 0 ) || ( y1f > hh && y2f > hh ) ) continue;
              path.path.moveTo( x1f, y1f );
              path.path.lineTo( x2f, y2f );
            }
          } else {
            // TODO VECTOR
            x1 = x0 - GRID_SIZE * grid_step;
            x2 = x0 + GRID_SIZE * grid_step;
            for (int i = -GRID_SIZE; i < GRID_SIZE; ++i ) {
              float y = y0 + i * grid_step;
              addGridLine( x1, y, x2, y, path.path );
            }
          }  
          paths_frame.add( path );

          // vertical line is not very useful ...
          // path  = new Cave3DDrawPath( vertPaint );
          // addVert( x0, y0, zmax, zmin, path.path );
          // addVert( xmin, ymin, zmax, zmin, path.path );
          // paths_frame.add( path );
        }
      }
    }

    if ( do_paths_surface ) {
      do_paths_surface = false;
      synchronized( paths_surface ) {
        // Log.v("Cave3D", "compute paths: surface ");
        paths_surface.clear();
        if ( do_surface ) {
          int n1 = mSurface.mNr1;
          int n2 = mSurface.mNr2;
          int cnti = 0;
          int cntj = 0;
          for ( int j=0; j<n2; ++j ) {
            int ji0  = j*n1;
            Cave3DDrawPath pp = null; 
            int i = 0;
            for ( ; i<n1-1; ++i, ++ji0 ) {
              float x1s = projs_surface_E[ ji0 ]; // (i+0,j)
              float y1s = projs_surface_N[ ji0 ];
              if ( x1s < 0 || x1s > ww || y1s < 0 || y1s > hh ) continue;
              pp = new Cave3DDrawPath( surfacePaint );
              pp.path.moveTo( x1s, y1s );
              break;
            }
            if ( i < n1-1 ) {
              int cnt = 0;
              for ( ; i<n1; ++i, ++ji0 ) {
                float x1s = projs_surface_E[ ji0 ]; 
                float y1s = projs_surface_N[ ji0 ];
                if ( x1s < 0 || x1s > ww || y1s < 0 || y1s > hh ) break;
                pp.path.lineTo( x1s, y1s );
                ++ cnt;
              }
              if ( cnt > 0 ) {
                // Log.v( TAG, "add path J " + j );
                ++ cntj;
                paths_surface.add( pp );
              }
            }
          }
          for ( int i=0; i<n1; ++i ) {
            int ji0 = 0*n1 + i;
            Cave3DDrawPath pp = null; 
            int j = 0;
            for ( ; j<n2-1; ++j, ji0+=n1 ) {
              float x1s = projs_surface_E[ ji0 ]; // (i+0,j)
              float y1s = projs_surface_N[ ji0 ];
              if ( x1s < 0 || x1s > ww || y1s < 0 || y1s > hh ) continue;
              pp = new Cave3DDrawPath( surfacePaint );
              pp.path.moveTo( x1s, y1s );
              break;
            }
            if ( j < n2-1 ) {
              int cnt = 0;
              for ( ; j<n2; ++j, ji0+=n1 ) {
                float x1s = projs_surface_E[ ji0 ]; 
                float y1s = projs_surface_N[ ji0 ];
                if ( x1s < 0 || x1s > ww || y1s < 0 || y1s > hh ) break;
                pp.path.lineTo( x1s, y1s );
                ++ cnt;
              }
              if ( cnt > 0 ) {
                // Log.v( TAG, "add path I " + i );
                ++ cnti;
                paths_surface.add( pp );
              }
            }
          }
          // Log.v( TAG, "add path I " + cnti + " J " + cntj );
        }
      }
    }

    if ( do_paths_legs ) {
      do_paths_legs = false;
      synchronized( paths_legs ) {
        // Log.v("Cave3D", "compute paths: legs ");
        paths_legs.clear();
        switch ( colorMode ) {
          case COLOR_NONE:
            // path  = new Cave3DDrawPath( shotPaint );
            // for ( Cave3DShot sh : shots ) {
            //   addLegShot( sh, path.path, paint );
            // }
            // paths_legs.add( path );
            for ( Cave3DShot sh : shots ) {
              Paint paint = new Paint();
              paint.setDither(true);
              paint.setColor( 0xffffffff );
              paint.setStyle(Paint.Style.STROKE);
              paint.setStrokeJoin(Paint.Join.ROUND);
              paint.setStrokeCap(Paint.Cap.ROUND);
              paint.setStrokeWidth( STROKE_WIDTH_SHOT );
   
              path  = new Cave3DDrawPath( paint );
              addLegShot( sh, path.path, paint );
              paths_legs.add( path );
            }
            break;
          case COLOR_SURVEY:
            int nr = mParser.getSurveySize();
            // Log.v(TAG, "survey numbers " + nr );
            Cave3DDrawPath[] p = new Cave3DDrawPath[ SURVEY_PAINT_NR ];
            for (int n=0; n<SURVEY_PAINT_NR; ++n ) {
              p[n] = new Cave3DDrawPath( surveyPaint[ n ] );
            }
            for ( Cave3DShot sh : shots ) {
              // Log.v(TAG, "N " + n + " shot survey " + sh.surveyNr );
              int n = sh.surveyNr % SURVEY_PAINT_NR;
              addLegShot( sh, p[n].path, null );
              p[n].count ++;
            }
            for (int n=0; n<SURVEY_PAINT_NR; ++n ) {
              if ( p[n].count > 0 ) paths_legs.add( p[n] );
            }
            break;
          case COLOR_DEPTH:
            for ( Cave3DShot sh : shots ) {
              Paint paint = new Paint();
              paint.setDither(true);
              paint.setColor( (int)( 0xffff * sh.depth() ) + 0xffff0000 );
              paint.setStyle(Paint.Style.STROKE);
              paint.setStrokeJoin(Paint.Join.ROUND);
              paint.setStrokeCap(Paint.Cap.ROUND);
              paint.setStrokeWidth( STROKE_WIDTH_SHOT );
   
              path  = new Cave3DDrawPath( paint );
              addLegShot( sh, path.path, null );
              paths_legs.add( path );
            }
            break;
        }
      }
    }

    if ( do_paths_splays ) {
      do_paths_splays = false;
      synchronized( paths_splays ) {
        // Log.v("Cave3D", "compute paths: splays ");
        paths_splays.clear();
        paths_wires.clear();
        if ( do_splays == DO_SPLAY_SHOT ) {
          path  = new Cave3DDrawPath( splayPaint );
          for ( Cave3DShot sh : splays ) {
            addSplayShot( sh, path.path, null, false );
          }
          paths_splays.add( path );
        } else if ( do_splays == DO_SPLAY_POINT ) {
          path  = new Cave3DDrawPath( splayPaintPoint );
          for ( Cave3DShot sh : splays ) {
            addSplayShot( sh, path.path, null, true );
          }
          paths_splays.add( path );
        } else if ( do_splays == DO_SPLAY_WIRE ) {
          if ( mWireFrame != null ) {
            path  = new Cave3DDrawPath( splayPaint );
            for ( WireSegment ws : mWireFrame.getSegments() ) {
              addWireSegment( ws, path.path );
            }
            paths_wires.add( path );
          }
        }
      }
    }
   
    if ( do_paths_stations ) {
      do_paths_stations = false;
      synchronized( paths_stations ) {
        // Log.v("Cave3D", "compute paths: stations ");
        paths_stations.clear();
        if ( do_stations ) {
          for ( Cave3DStation st : stations ) {
            addStation( st );
          }
        }
      }
    }

    if ( do_paths_walls ) {
      do_paths_walls = false;
      synchronized( paths_walls ) {
        // Log.v("Cave3D", "compute paths: walls ");
        paths_walls.clear();

        if ( wall_mode == WALL_CW && walls != null /* && Cave3D.mWallConvexHull */ ) {
          for ( CWConvexHull cw : walls ) {
            synchronized( cw ) {
              for ( CWTriangle tr : cw.mFace ) {
                addTriangle( tr );
              }
            }
          }

/*        // FIXME WALL_HULL dropped in favour of WALL_CW
        } else if ( wall_mode == WALL_HULL && triangles_hull != null ) {
          for ( Cave3DTriangle tr : triangles_hull ) {
            addTriangle( tr );
          }
*/
/*        // FIXME skip WALL_DELAUNAY triangles
        } else if ( wall_mode == WALL_DELAUNAY && triangles_delaunay != null ) {
          for ( Cave3DTriangle tr : triangles_delaunay ) {
            addTriangle( tr );
          }
*/
        } else if ( wall_mode == WALL_POWERCRUST && triangles_powercrust != null /* && Cave3D.mWallPowercrust */ ) {
          for ( Cave3DTriangle tr : triangles_powercrust ) {
            addTriangle( tr );
          }
        } // else WALL_NONE 

        if ( Cave3D.mSplitTriangles ) {
          // synchronized( paths_borders )
          {
            // Log.v("Cave3D", "compute paths: borders ");
            paths_borders.clear();
            if ( wall_mode == WALL_CW /* && Cave3D.mWallConvexHull */ ) {
              for ( CWBorder cb : borders ) {
                synchronized( cb ) {
                  for ( CWIntersection ii : cb.mInts ) {
                    addBorderLine( ii );
                  }
                }
              }
            }
          }
        }
      }
    }

    if ( do_paths_planview ) {
      do_paths_planview = false;
      paths_planview.clear();
      if ( planview != null && ( do_planview == 1 || do_planview == 2 ) ) {
        for ( Cave3DPolygon poly : planview ) {
          int nn = poly.size();
          if ( nn > 2 ) {
            path  = new Cave3DDrawPath( vertPaint );
            Cave3DSite p1 = poly.get( nn - 1 );
            float x = p1.x - xc; // vector camera->station
            float y = p1.y - yc;
            float z = zmin - zc;
            x1 = projectedX( x, y, z );
            y1 = projectedY( x, y, z );
            // float d1 = projectedZabs( x, y, z );
            path.path.moveTo( x1, y1 );
            for ( int k=0; k<nn; ++k ) {
              p1 = poly.get( k );
              x = p1.x - xc; // vector camera->station
              y = p1.y - yc;
              z = zmin - zc;
              x1 = projectedX( x, y, z );
              y1 = projectedY( x, y, z );
              // d1 = projectedZabs( x, y, z );
              path.path.lineTo( x1, y1 );
            }
            path.path.close();
            synchronized( paths_planview ) {
              paths_planview.add( path );
            }
          }
        }
      }
      if ( profileview != null && ( do_planview == 3 || do_planview == 2 ) ) {
        for ( Cave3DPolygon poly : profileview ) {
          int nn = poly.size();
          if ( nn > 2 ) {
            path  = new Cave3DDrawPath( vertPaint );
            Cave3DSite p1 = poly.get( nn - 1 );
            float x = p1.x - xc; // vector camera->station
            float y = p1.y - yc;
            float z = p1.z - zc;
            x1 = projectedX( x, y, z );
            y1 = projectedY( x, y, z );
            // float d1 = projectedZabs( x, y, z );
            path.path.moveTo( x1, y1 );
            for ( int k=0; k<nn; ++k ) {
              p1 = poly.get( k );
              x = p1.x - xc; // vector camera->station
              y = p1.y - yc;
              z = p1.z - zc;
              x1 = projectedX( x, y, z );
              y1 = projectedY( x, y, z );
              // d1 = projectedZabs( x, y, z );
              path.path.lineTo( x1, y1 );
            }
            path.path.close();
            synchronized( paths_planview ) {
              paths_planview.add( path );
            }
          }
        }
      }
    }

    // do_repaint = true;
    return true;
  } 

  public boolean computeProjection( Canvas canvas, Handler doneHandler )
  {
    if ( ! computePaths() ) return false;
    
    synchronized( paths_frame ) {
      for ( Cave3DDrawPath p : paths_frame ) {
        p.draw( canvas );
      }
    }
    synchronized( paths_planview ) {
      for ( Cave3DDrawPath p : paths_planview ) {
        p.draw( canvas );
      }
    }
    synchronized( paths_surface ) {
      for ( Cave3DDrawPath p : paths_surface ) {
        p.draw( canvas );
      }
    }
    synchronized( paths_walls ) {
      for ( Cave3DDrawPath p : paths_walls ) {
        p.draw( canvas );
      }
      // FIXME BORDER
      // synchronized( paths_borders ) {
      //   for ( Cave3DDrawPath p : paths_borders ) {
      //     p.draw( canvas );
      //   }
      // }
    }
    synchronized( paths_splays ) {
      for ( Cave3DDrawPath p : paths_splays ) {
        p.draw( canvas );
      }
    }
    synchronized( paths_wires ) {
      for ( Cave3DDrawPath p : paths_wires ) {
        p.draw( canvas );
      }
    }
    synchronized( paths_legs ) {
      for ( Cave3DDrawPath p : paths_legs ) {
        p.draw( canvas );
      }
    }
    synchronized( paths_stations ) {
      for ( Cave3DDrawPath p : paths_stations ) {
        p.draw( canvas );
      }
    }
    // do_repaint = false;
    //doneHandler.sendEmptyMessage(1);
    return true;
  } 

  void serializeWalls( String filename )
  {
    // if ( Cave3D.mWallConvexHull ) {
      FileWriter fw = null;
      try {
        if ( walls != null ) {
          fw = new FileWriter( filename );
          PrintWriter out = new PrintWriter( fw );
          out.format("E %d %d\n", walls.size(), borders.size() );
          for ( CWConvexHull wall : walls ) {
            wall.serialize( out );
          }
          for ( CWBorder border : borders ) {
            border.serialize( out );
          }
        } else if ( triangles_powercrust != null ) {
          // TODO
        }
      } catch ( FileNotFoundException e ) { 
        Toast.makeText( mCave3D, "File not found", Toast.LENGTH_SHORT).show();
      } catch ( IOException e ) {
        Toast.makeText( mCave3D, "IO Exception", Toast.LENGTH_SHORT).show();
      } finally {
        if ( fw != null ) {
          try {
            fw.flush();
            fw.close();
          } catch (IOException e ) { }
        }
      }
    // } else  {
    //   Toast.makeText( mCave3D, "ConvexHull walls are disabled", Toast.LENGTH_SHORT).show();
    // }
  }

  void exportModel( int type, String pathname, boolean b_splays, boolean b_walls, boolean b_surface, boolean overwrite )
  { 
    if ( (new File(pathname)).exists() && ! overwrite ) {
      Toast.makeText( mCave3D, "Not overwriting existing file" + pathname, Toast.LENGTH_SHORT).show();
      return;
    }
      
    if ( type == ModelType.SERIAL ) { // serialization
      if ( ! pathname.endsWith(".txt") ) {
        Toast.makeText( mCave3D, "Not a txt file" + pathname, Toast.LENGTH_SHORT).show();
      } else {
        serializeWalls( pathname );
      }
    } else {                          // model export 
      boolean ret = false;
      if ( type == ModelType.KML_ASCII ) { // KML export ASCII
        if ( ! pathname.endsWith(".kml") ) {
          Toast.makeText( mCave3D, "Not a kml file" + pathname, Toast.LENGTH_SHORT).show();
        } else {
          KMLExporter kml = new KMLExporter();
          if ( walls != null ) {
            for( CWConvexHull cw : walls ) {
              synchronized( cw ) {
                for ( CWTriangle f : cw.mFace ) kml.add( f );
              }
            }
          } else if ( triangles_powercrust != null ) {
            kml.mTriangles = triangles_powercrust;
          }
          ret = kml.exportASCII( pathname, mParser, b_splays, b_walls, b_surface );
        }
      } else if ( type == ModelType.CGAL_ASCII ) { // CGAL export: only stations and splay-points
        if ( ! pathname.endsWith(".cgal") ) {
          Toast.makeText( mCave3D, "Not a CGAL text file" + pathname, Toast.LENGTH_SHORT).show();
        } else {
          CGALExporter cgal = new CGALExporter();
          ret = cgal.exportASCII( pathname, mParser, b_splays, b_walls, b_surface );
        }
      } else {                                     // STL export ASCII or binary
        if ( ! pathname.endsWith(".stl") ) {
          Toast.makeText( mCave3D, "Not a STL file" + pathname, Toast.LENGTH_SHORT).show();
        } else {
          STLExporter stl = new STLExporter();
          if ( walls != null ) {
            for ( CWConvexHull cw : walls ) {
              synchronized( cw ) {
                for ( CWTriangle f : cw.mFace ) stl.add( f );
              }
            }
          } else if ( triangles_powercrust != null ) {
            stl.mTriangles = triangles_powercrust;
            stl.mVertex    = vertices_powercrust;
          }

          if ( type == ModelType.STL_BINARY ) {
            ret = stl.exportBinary( pathname, b_splays, b_walls, b_surface );
          } else { // type == ModelType.STL_ASCII
            ret = stl.exportASCII( pathname, b_splays, b_walls, b_surface );
          }
        }
      }
      if ( ret ) {
        Toast.makeText( mCave3D, "OK. Exported " + pathname, Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText( mCave3D, "Failed Export " + pathname, Toast.LENGTH_SHORT).show();
      }
    }
  }
}


