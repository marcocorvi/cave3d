/** @file GlSketch.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief TopoDroid symbols
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.nio.FloatBuffer;
// import java.nio.ShortBuffer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.content.Context;

import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.graphics.Canvas;
import android.graphics.Bitmap;

import java.util.ArrayList;
// import java.util.HashMap;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

class GlSketch extends GlShape
{
  private final static int UNIT  = 72;
  private final static int HALF  = 36;
  private final static int WIDTH  = 20 * UNIT;
  private final static int HEIGHT = 15 * UNIT;

  private static int row( int i ) { return i/20; }
  private static int col( int i ) { return i%20; }

  private static int xmin( int i ) { return UNIT * col(i); }
  private static int ymin( int i ) { return UNIT * row(i); }

  final static int COORDS_PER_VERTEX = 3;
  final static int OFFSET_VERTEX     = 0;
  final static int STRIDE_VERTEX     = 12; // Float.BYTES * COORDS_PER_VERTEX;

  final static int COORDS_PER_COLOR  = 3;
  final static int OFFSET_COLOR      = 3;
  final static int STRIDE_LINE_VERTEX = 24; // Float.BYTES * (COORDS_PER_VERTEX + COORDS_PER_COLOR)

  final static int COORDS_PER_ACOLOR = 4;
  final static int OFFSET_ACOLOR     = 3;
  final static int STRIDE_AREA_VERTEX = 28; // Float.BYTES * (COORDS_PER_VERTEX + COORDS_PER_ACOLOR)

  final static int COORDS_PER_DELTA  = 2;
  final static int COORDS_PER_TEXEL  = 2;
  final static int OFFSET_DELTA      = 0;
  final static int OFFSET_TEXEL      = 2;  // COORDS_PER_DELTA;
  final static int STRIDE_TEXEL      = 16; // Float.BYTES * (COORDS_PER_DELTA + COORDS_PER_TEXEL);

  private static ArrayList< String > mSymbols; // symbol names
  private static int mTexId = -1;
  private static Bitmap mBitmap = null;

  String mName;
  int    mType;
  private ArrayList< SketchPoint > mPoints;
  private ArrayList< SketchLine  > mLines;
  private ArrayList< SketchLine  > mAreas;
  private float[] mData;
  private int pointCount;
  private int lineCount; // number of line segments
  private int areaCount; // number of area segments

  // private FloatBuffer nameBuffer = null; // textures
  private FloatBuffer pointBuffer;
  private FloatBuffer lineBuffer;
  private FloatBuffer areaBuffer;

  final static float SYMBOL_SIZE = 10.0f;

  private static float mSymbolSizeP = 1.2f;
  private static float mSymbolSizeO = 0.6f;

  boolean mShow = true;
  boolean mDelete = false; // whether to drop this sketch
  private boolean mEmpty = true;

  void reset()
  {
    mShow = true;
    mSymbolSizeP = 1.2f;
    mSymbolSizeO = 0.6f;
  }

  void toggleShowSymbols() { mShow = ! mShow; }

  static void setSymbolSize( int size ) 
  { 
    if ( size <= 1 ) return;
    mSymbolSizeP = size / 10.0f; 
    mSymbolSizeO = size / 20.0f;  // half size
  }

  GlSketch( Context ctx, String name, int type, ArrayList< SketchPoint > pts, ArrayList< SketchLine > lns, ArrayList< SketchLine > areas  )
  {
    super( ctx );
    mName   = name;
    mType   = type; // 1 PLAN, 2 PROFILE
    mPoints = pts;
    mLines  = lns;
    mAreas  = areas;
  }
 
  static int getPointIndex( String th_name ) 
  {
    for ( int k=0; k<mSymbols.size(); ++k ) if ( th_name.equals( mSymbols.get(k) ) ) return k;
    return -1;
  }

  // --------------------------------------------------------
  // LOG

  void logMinMax()
  {
    if ( mPoints.size() == 0 ) return;
    float xmin, xmax, ymin, ymax, zmin, zmax;
    Vector3D v0 = mPoints.get(0);
    xmin = xmax = v0.x;
    ymin = ymax = v0.y;
    zmin = zmax = v0.z;
    for ( SketchPoint point : mPoints ) {
      Vector3D v = point;
      Log.i("TopoGL", "Point " + v.x + " " + v.y + " " + v.z );
      if ( v.x < xmin ) { xmin = v.x; } else if ( v.x > xmax ) { xmax = v.x; }
      if ( v.y < ymin ) { ymin = v.y; } else if ( v.y > ymax ) { ymax = v.y; }
      if ( v.z < zmin ) { zmin = v.z; } else if ( v.z > zmax ) { zmax = v.z; }
    }
    Log.i("TopoGL-SKETCH", "points " + mPoints.size() + " X " + xmin + " " + xmax + " Y " + ymin + " " + ymax + " Z " + zmin + " " + zmax );

    for ( SketchLine line : mLines ) { 
      Log.i("TopoGL-SKETCH", "line size " + line.size() + " color " + line.red + " " + line.green + " " + line.blue );
      for ( Vector3D v : line.pts ) {
        if ( v.x < xmin ) { xmin = v.x; } else if ( v.x > xmax ) { xmax = v.x; }
        if ( v.y < ymin ) { ymin = v.y; } else if ( v.y > ymax ) { ymax = v.y; }
        if ( v.z < zmin ) { zmin = v.z; } else if ( v.z > zmax ) { zmax = v.z; }
      }
    }
    Log.i("TopoGL-SKETCH", "lines " + mLines.size() + " X " + xmin + " " + xmax + " Y " + ymin + " " + ymax + " Z " + zmin + " " + zmax );
  }
  //

  // ----------------------------------------------------
  // PROGRAM

  void initData( float xmed, float ymed, float zmed )
  {
    pointCount = mPoints.size();

    lineCount = 0;
    for ( SketchLine line : mLines ) if ( line.size() > 1 ) lineCount += line.size() - 1;

    areaCount = 0;
    for ( SketchLine area : mAreas ) if ( area.size() > 2 ) areaCount += area.size() - 2; // triangles per area

    // Log.v("TopoGL-SKETCH", "point coint " + pointCount + " line count " + lineCount + " area count " + areaCount );
    if ( (pointCount + lineCount + areaCount) == 0 ) {
      mEmpty = true;
      return;
    }
    mEmpty = false;
    initBuffer( xmed, ymed, zmed );
  }
      
  // ------------------------------------------------------
  // DRAW

  // never unbind texture
  static void unbindTexture() 
  {
    // if ( mSymbols.size() == 0 ) return;
    // if ( mTexId < 0 ) return;
    // GL.unbindTextTexture( mTexId );
    // mTexId = -1;
  }

  static void rebindTexture() { mTexId = -1; }

  void draw( float[] mvpMatrix ) // , float[] inverseScaleMatrix )
  {
    if ( mEmpty ) return;
    if ( ! mShow ) return;
    // ------- BIND TEXT BITMAP
    if ( mBitmap == null ) {
      Log.e("TopoGL-SKETCH", "null bitmap");
      return;
    }
    if ( mTexId < 0 ) {
      GLES20.glActiveTexture( GLES20.GL_TEXTURE0 );
      mTexId = GL.bindTextTexture( mBitmap );
      // Log.v("TopoGL", "bound symbols texture Id " + mTexId + " points " + pointCount );
    }
    // mBitmap.recycle(); // do not clean up, but keep the bitmap
    // mBitmap = null;
    if ( pointCount > 0 ) {
      GL.useProgram( mProgram );
      bindData( mvpMatrix ); 
      GL.drawTriangle( 0, pointCount*2 );
      // unbindData();
      // GL.useProgram( mProgramPos );
      // bindDataPos( mvpMatrix );
      // GL.drawPoint( 0, pointCount );
    }

    if ( lineCount > 0 ) {
      GL.setLineWidth( 2.0f );
      GL.useProgram( mProgramLine );
      bindDataLine( mvpMatrix ); 
      GL.drawLine( 0, lineCount );
    }

    if ( areaCount > 0 ) {
      GL.useProgram( mProgramArea );
      bindDataArea( mvpMatrix ); 
      GL.drawTriangle( 0, areaCount );
    }
  }

  private void bindData( float[] mvpMatrix )
  {
    GL.setAttributePointer( mAPosition, dataBuffer, OFFSET_VERTEX, COORDS_PER_VERTEX,  STRIDE_VERTEX );
    GL.setAttributePointer( mADelta,    pointBuffer, OFFSET_DELTA,  COORDS_PER_DELTA,  STRIDE_TEXEL );
    GL.setAttributePointer( mATexCoord, pointBuffer, OFFSET_TEXEL,  COORDS_PER_TEXEL,  STRIDE_TEXEL );
    if ( GlRenderer.projectionMode == GlRenderer.PROJ_PERSPECTIVE ) {
      GL.setUniform( mUTextSize, mSymbolSizeP );
    } else {
      GL.setUniform( mUTextSize, mSymbolSizeO );
    }

    GL.setUniformTexture( mUTexUnit, 0 );
    if ( mTexId >= 0 ) {
      GLES20.glActiveTexture( GLES20.GL_TEXTURE0 );
      GLES20.glBindTexture( GLES20.GL_TEXTURE_2D, mTexId ); // texture-id from load-texture
    }
    GL.setUniformMatrix( mUMVPMatrix, mvpMatrix );
  }

  // private void bindDataPos( float[] mvpMatrix )
  // {
  //   // float[] color = TglColor.ColorStation;
  //   GL.setAttributePointer( mpAPosition, dataBuffer, OFFSET_VERTEX, COORDS_PER_VERTEX, STRIDE_VERTEX*6 ); // one vertex every 6
  //   // GL.setUniform( mpUPointSize, mPointSize );
  //   GL.setUniform( mpUPointSize, 10f );
  //   // GL.setUniform( mpUColor, color[0], color[1], color[2], color[3] );
  //   GL.setUniform( mpUColor, 1.0f, 1.0f, 0.0f, 1.0f ); // yellow
  //   GL.setUniformMatrix( mpUMVPMatrix, mvpMatrix );
  // }

  private void bindDataLine( float[] mvpMatrix )
  {
    GL.setAttributePointer( mlAPosition, lineBuffer,  OFFSET_VERTEX, COORDS_PER_VERTEX, STRIDE_LINE_VERTEX );
    GL.setAttributePointer( mlAColor,    lineBuffer,  OFFSET_COLOR,  COORDS_PER_COLOR,  STRIDE_LINE_VERTEX );
    GL.setUniform( mlUPointSize, 10f );
    GL.setUniform( mlUAlpha, 1.0f );
    GL.setUniformMatrix( mlUMVPMatrix, mvpMatrix );
  }

  private void bindDataArea( float[] mvpMatrix )
  {
    GL.setAttributePointer( maAPosition, areaBuffer,  OFFSET_VERTEX, COORDS_PER_VERTEX, STRIDE_AREA_VERTEX );
    GL.setAttributePointer( maAColor,    areaBuffer,  OFFSET_ACOLOR, COORDS_PER_ACOLOR, STRIDE_AREA_VERTEX );
    // GL.setUniform( maUPointSize, 10f );
    GL.setUniformMatrix( maUMVPMatrix, mvpMatrix );
  }

  // void unbindData() 
  // {
  //   GL.releaseAttribute( mAPosition );
  //   GL.releaseAttribute( mADelta );
  //   GL.releaseAttribute( mATexCoord );
  //   GLES20.glActiveTexture( 0 );
  // }
  // ------------------------------------------------------------------------------
  class Point2D
  {
    float x, y;
    int idx;    // index in the area boundary
    Point2D( Vector3D v, int i ) { x = v.x; y = v.y; idx = i; }
  }

  private boolean isRightHanded( Point2D v1, Point2D v2, Point2D v3 )
  {
    return ( v2.x - v1.x ) * ( v3.y - v1.y ) - ( v3.x - v1.x ) * ( v2.y - v1.y ) < 0;
  }

  private boolean isRightInside( Point2D v0, Point2D v1, Point2D v2, Point2D v3 )
  {
    return isRightHanded( v1, v2, v0 ) && isRightHanded( v2, v3, v0 ) && isRightHanded( v3, v1, v0 );
  }

  private boolean isLeftHanded( Point2D v1, Point2D v2, Point2D v3 )
  {
    return ( v2.x - v1.x ) * ( v3.y - v1.y ) - ( v3.x - v1.x ) * ( v2.y - v1.y ) > 0;
  }

  private boolean isLeftInside( Point2D v0, Point2D v1, Point2D v2, Point2D v3 )
  {
    return isLeftHanded( v1, v2, v0 ) && isLeftHanded( v2, v3, v0 ) && isLeftHanded( v3, v1, v0 );
  }

  private int[] triangulate( SketchLine area )
  {
    int sz = area.size();
    int tz = sz - 2;
    int[] ret = new int[3 * tz];
    ArrayList< Point2D > pts = new ArrayList<>();
    for ( int k=0; k<sz; ++k ) pts.add( new Point2D( area.pts.get(k), k ) );

    int nt = 0;
    Point2D p0, p1, p2;
    if ( isRightHanded( pts.get(0), pts.get(sz/3), pts.get((2*sz)/3) ) ) {
      // Log.v("TopoGL-SKETCH", "RIGHT" );
      while ( pts.size() > 3 ) {
        sz = pts.size();
        boolean insert = false;
        for ( int k = 2; k<sz; ++k ) {
          p2 = pts.get(k-2);
          p1 = pts.get(k-1);
          p0 = pts.get(k);
          if ( isRightHanded( p2, p1, p0 ) ) {
            boolean ok = true;
            for ( int j = 1; j < sz-2; ++ j ) {
              if ( isRightInside( pts.get( (k+j)%sz ), p2, p1, p0 ) ) {
                ok = false;
                break;
              }
            }
            if ( ok ) {
              ret[ nt++ ] = p2.idx;
              ret[ nt++ ] = p1.idx;
              ret[ nt++ ] = p0.idx;
              pts.remove( k-1 );
              insert = true;
              break;
            }
          }
        }
        if ( ! insert ) break;
      }
      p2 = pts.get(0);
      p1 = pts.get(1);
      p0 = pts.get(2);
      ret[ nt++ ] = p2.idx;
      ret[ nt++ ] = p1.idx;
      ret[ nt++ ] = p0.idx;
    } else {
      // Log.v("TopoGL-SKETCH", "LEFT" );
      while ( pts.size() > 3 ) {
        sz = pts.size();
        boolean insert = false;
        for ( int k = 2; k<sz; ++k ) {
          p2 = pts.get(k-2);
          p1 = pts.get(k-1);
          p0 = pts.get(k);
          if ( isLeftHanded( p2, p1, p0 ) ) {
            boolean ok = true;
            for ( int j = 1; j < sz-2; ++ j ) {
              if ( isLeftInside( pts.get( (k+j)%sz ), p2, p1, p0 ) ) {
                ok = false;
                break;
              }
            }
            if ( ok ) {
              ret[ nt++ ] = p2.idx;
              ret[ nt++ ] = p1.idx;
              ret[ nt++ ] = p0.idx;
              pts.remove( k-1 );
              insert = true;
              break;
            }
          }
        }
        if ( ! insert ) break;
      }
      p2 = pts.get(0);
      p1 = pts.get(1);
      p0 = pts.get(2);
      ret[ nt++ ] = p2.idx;
      ret[ nt++ ] = p1.idx;
      ret[ nt++ ] = p0.idx;
    }
    // Log.v("TopoGL-SKETCH", "tri points " + nt + " expected tri " + tz );
    // assert( nt == 3 * tz );
    // for ( int k = 0; k<tz; ++k ) Log.v("TopoGL-SKETCH", "TRI " + ret[3*k] + " " + ret[3*k+1] + " " + ret[3*k+2] );
    return ret;
  }

  // ------------------------------------------------------------------------------
  // point Vector3D NOT in GL orientation (Y upward)

  private void initBuffer( float xmed, float ymed, float zmed )
  {
    float x2 = 144.0f / GlModel.mWidth;
    float y2 = 144.0f / GlModel.mHeight;
    float ds = 72.0f/WIDTH;
    float dt = 72.0f/HEIGHT;

    int NN = 6; // 2 trinagles (3 vertex per triangle)
    // ---------- BASE POINT
    float[] data6 = new float[ pointCount * 3 * NN ]; // 3 float, XYZ, per vertex
    float[] pos = new float[ pointCount * 4 * NN ]; // Dx, Dy, Dz=0, S, T
    int off = 0;
    int off6 = 0;
    for ( SketchPoint pt : mPoints ) {
      float xoff = col( pt.idx )*UNIT;
      float yoff = row( pt.idx )*UNIT;
      float s1 = xoff/WIDTH;
      float t2 = yoff/HEIGHT;
      float s2 = s1 + ds;
      float t1 = t2 + dt;

      for (int j=0; j<NN; ++j ) {
        data6[ off6++ ] =   pt.x - xmed;
        data6[ off6++ ] =   pt.z - ymed;
        data6[ off6++ ] = - pt.y - zmed;
      }
      pos[off++] =-x2; pos[off++] =-y2; pos[off++] = s1; pos[off++] = t1; 
      pos[off++] =-x2; pos[off++] = y2; pos[off++] = s1; pos[off++] = t2; 
      pos[off++] = x2; pos[off++] = y2; pos[off++] = s2; pos[off++] = t2; 
      
      pos[off++] =-x2; pos[off++] =-y2; pos[off++] = s1; pos[off++] = t1; 
      pos[off++] = x2; pos[off++] = y2; pos[off++] = s2; pos[off++] = t2; 
      pos[off++] = x2; pos[off++] =-y2; pos[off++] = s2; pos[off++] = t1; 
    }
    dataBuffer = GL.getFloatBuffer( data6.length );
    dataBuffer.put( data6 );

    pointBuffer = GL.getFloatBuffer( pointCount * 4 * NN );
    pointBuffer.put( pos, 0, pointCount * 4 * NN );

    
    float[] data2 = new float[ lineCount * 6 * 2 ]; // X,Y,Z R,G,B
    int off2 = 0;
    for ( SketchLine line : mLines ) {
      int sz = line.pts.size();
      if ( sz > 1 ) {
        Vector3D v1 = line.pts.get( 0 );
        for ( int k=1; k<sz; ++k ) {
          Vector3D v2 = line.pts.get(k);
          data2[ off2++ ] =   v1.x - xmed;
          data2[ off2++ ] =   v1.z - ymed;
          data2[ off2++ ] = - v1.y - zmed;
          data2[ off2++ ] = line.red;
          data2[ off2++ ] = line.green;
          data2[ off2++ ] = line.blue;
          data2[ off2++ ] =   v2.x - xmed;
          data2[ off2++ ] =   v2.z - ymed;
          data2[ off2++ ] = - v2.y - zmed;
          data2[ off2++ ] = line.red;
          data2[ off2++ ] = line.green;
          data2[ off2++ ] = line.blue;
          v1 = v2;
        }
      }
    }
    lineBuffer = GL.getFloatBuffer( lineCount * 6 * 2 );
    lineBuffer.put( data2, 0, lineCount * 6 * 2 );
    
    float[] data3 = new float[ areaCount * 7 * 3 ]; // X,Y,Z R,G,B,A
    int off3 = 0;
    for ( SketchLine area : mAreas ) {
      int tz = area.pts.size() - 2; // number of triangles
      // Log.v("TopoGL-SKETCH", "area triangles " + tz + " color " + area.red + " " + area.green + " " + area.blue + " " + area.alpha );
      if ( tz > 0 ) {
        int[] idx = triangulate( area ); // 3 * tz
        for ( int k=0; k < idx.length; ++k ) {
          Vector3D v0 = area.pts.get( idx[k] );
          // if ( (k%3) == 1 ) Log.v("TopoGL-SKETCH", "VERTEX " + v0.x + " " + v0.y + " " + v0.z );
          data3[ off3++ ] =   v0.x - xmed;
          data3[ off3++ ] =   v0.z - ymed;
          data3[ off3++ ] = - v0.y - zmed;
          data3[ off3++ ] = area.red;
          data3[ off3++ ] = area.green;
          data3[ off3++ ] = area.blue;
          data3[ off3++ ] = area.alpha;
        }
      }
    }
    // Log.v("TopoGL-SKETCH", "areaCount " + areaCount + " " + off3 );
    areaCount = off3/21;
    areaBuffer = GL.getFloatBuffer( off3 );
    areaBuffer.put( data3, 0, off3 );
  }

  // --------------------------------------------------------------------
  // UTILITIES

  int size() { return pointCount; }

  // --------------------------------------------------------------------
  // OpenGL

  private static int mProgram;
  // private static int mProgramPos;
  private static int mProgramLine;
  private static int mProgramArea;

  private static int mUMVPMatrix;
  private static int mAPosition;
  private static int mADelta;
  private static int mUTextSize;
  private static int mATexCoord;
  private static int mUTexUnit;
  
  // private static int mpUMVPMatrix;
  // private static int mpAPosition;
  // private static int mpUPointSize;
  // private static int mpUColor;

  private static int mlUMVPMatrix;
  private static int mlAPosition;
  private static int mlUPointSize;
  private static int mlUAlpha;
  private static int mlAColor;

  private static int maUMVPMatrix;
  private static int maAPosition;
  // private static int maUPointSize;
  private static int maAColor;

  static void initGL( Context ctx ) 
  {
    mProgram = GL.makeProgram( ctx, R.raw.name_vertex, R.raw.name_fragment );
    setLocations( mProgram );
    // mProgramPos = GL.makeProgram( ctx, R.raw.name_pos_vertex, R.raw.name_pos_fragment );
    // setLocationsPos( mProgramPos );
    mProgramLine = GL.makeProgram( ctx, R.raw.line_acolor_vertex, R.raw.line_acolor_fragment );
    setLocationsLine( mProgramLine );
    mProgramArea = GL.makeProgram( ctx, R.raw.area_acolor_vertex, R.raw.area_acolor_fragment );
    setLocationsArea( mProgramArea );
  }

  private static void setLocations( int program ) 
  {
    mAPosition  = GL.getAttribute( program, GL.aPosition );
    mADelta     = GL.getAttribute( program, GL.aDelta );
    mATexCoord  = GL.getAttribute( program, GL.aTexCoord );
    mUTextSize  = GL.getUniform(   program, GL.uTextSize );
    mUTexUnit   = GL.getUniform(   program, GL.uTexUnit );
    mUMVPMatrix = GL.getUniform(   program, GL.uMVPMatrix );
  }

  // private static void setLocationsPos( int program ) 
  // {
  //   mpAPosition  = GL.getAttribute( program, GL.aPosition );
  //   mpUPointSize = GL.getUniform(   program, GL.uPointSize );
  //   mpUColor     = GL.getUniform(   program, GL.uColor );
  //   mpUMVPMatrix = GL.getUniform(   program, GL.uMVPMatrix );
  // }

  private static void setLocationsLine( int program ) 
  {
    mlAPosition  = GL.getAttribute( program, GL.aPosition );
    mlAColor     = GL.getAttribute( program, GL.aColor );
    mlUPointSize = GL.getUniform(   program, GL.uPointSize );
    mlUAlpha     = GL.getUniform(   program, GL.uAlpha );
    mlUMVPMatrix = GL.getUniform(   program, GL.uMVPMatrix );
    // Log.v("TopoGL-SKETCH", "line locs " + mlAPosition + " " + mlAColor + " " + mlUMVPMatrix + " " + mProgramLine );
  }

  private static void setLocationsArea( int program ) 
  {
    maAPosition  = GL.getAttribute( program, GL.aPosition );
    maAColor     = GL.getAttribute( program, GL.aColor );
    // maUPointSize = GL.getUniform(   program, GL.uPointSize );
    maUMVPMatrix = GL.getUniform(   program, GL.uMVPMatrix );
    // Log.v("TopoGL-SKETCH", "area locs " + maAPosition + " " + maAColor + " " + maUMVPMatrix + " " + mProgramArea );
  }

  // ====================================================================
  // POINT SYMBOLS BITMAP
  // static void reloadSymbols( String dirpath )
  // {
  //   mBitmap.recycle();
  //   mBitmap = null;
  //   unbindTexture();
  //   loadSymbols( dirpath );
  // }

  static void loadSymbols( String dirpath )
  {
    if ( mBitmap != null ) return;
    mSymbols = new ArrayList< String >();
    mBitmap  = Bitmap.createBitmap( WIDTH, HEIGHT, Bitmap.Config.ARGB_8888 );
    Canvas canvas = new Canvas(mBitmap); // get a canvas to paint over the bitmap
    canvas.drawColor( 0x00000000 );
    File dir = new File( dirpath );
    if ( ! dir.exists() ) return;
    File[] files = dir.listFiles();
    if ( files == null ) return;
    for ( File file : files ) addSymbol( file, canvas );
  }

  // XYZ-med in OpenGL
  private static void addSymbol( File file, Canvas canvas )
  {
    String th_name = null;
    int color = 0xffffffff;
    String path_str = null;
    if ( ! file.exists() ) return;
    try { 
      FileReader fr = new FileReader( file );
      BufferedReader br = new BufferedReader( fr );
      String line;
      while ( ( line = br.readLine() ) != null ) {
        line = line.trim().replaceAll("\\s+", " ");
        String[] vals = line.split(" ");
        int s = vals.length;
        for (int k=0; k<s; ++k ) {
          if ( vals[k].startsWith( "#" ) ) break;
          if ( vals[k].equals("th_name") ) {
            ++k; while ( k < s && vals[k].length() == 0 ) ++k;
            if ( k < s ) th_name = vals[k];
          } else if ( vals[k].equals("color") ) {
            ++k; while ( k < s && vals[k].length() == 0 ) ++k;
            if ( k < s ) {
              try {
                color = Integer.decode( vals[k] ) | 0xff000000;
              } catch ( NumberFormatException e ) { }
            }
          } else if ( vals[k].equals("path") ) {
            line = br.readLine();
            if ( line != null ) {
              path_str = line.trim().replaceAll("\\s+", " ");
              while ( ( line = br.readLine() ) != null ) {
                line = line.trim().replaceAll("\\s+", " ");
                if ( line.startsWith( "endpath" ) ) break;
                path_str = path_str + " " + line;
              }
            }
          }
        }
      }
    } catch ( IOException e ) {
    }
    if ( th_name != null && path_str != null ) {
      int index = mSymbols.size();
      if ( drawSymbol( path_str, color, canvas, index ) ) {
        // Log.v("TopoGL", "add symbol " + th_name + " at " + index );
        mSymbols.add( th_name );
      }
    }
  }

  private static boolean drawSymbol( String path_str, int color, Canvas canvas, int index )
  {
    Paint paint = new Paint();
    paint.setDither(true);
    paint.setColor( color );
    paint.setStyle( Paint.Style.STROKE );
    paint.setStrokeJoin(Paint.Join.ROUND);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStrokeWidth( 2 );
    int xc = HALF + xmin( index );
    int yc = HALF + ymin( index );

    Path path = makePointPath( path_str );
    if ( path == null ) return false;
    path.offset( xc, yc );
    canvas.drawPath( path, paint );
    return true;
  }
    
  private static Path makePointPath( String path_str )
  {
    if ( path_str == null ) return null;

    float unit = 3.0f;
    Path path = new Path();
    String[] vals = path_str.split(" ");
    int s = vals.length;
    for ( int k = 0; k<s; ++k ) {
      float x0=0, y0=0, x1=0, y1=0, x2=0, y2=0;
      if ( "moveTo".equals( vals[k] ) ) {
        try {
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { x0 = Float.parseFloat( vals[k] ); }
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) {
            y0 = Float.parseFloat( vals[k] );
            path.moveTo( x0*unit, y0*unit );
          }
        } catch ( NumberFormatException e ) {
          Log.e("TopoGL-SYMBOL", path + " parse moveTo error" );
        }
      } else if ( "lineTo".equals( vals[k] ) ) {      
        try {
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { x0 = Float.parseFloat( vals[k] ); }
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { 
            y0 = Float.parseFloat( vals[k] ); 
            path.lineTo( x0*unit, y0*unit );
          }
        } catch ( NumberFormatException e ) {
          Log.e("TopoGL-SYMBOL", path + " parse lineTo error" );
        }
      } else if ( "cubicTo".equals( vals[k] ) ) {
        // cp1x cp1y cp2x cp2y p2x p2y
        try {
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;  // CP1
          if ( k < s ) { x0 = Float.parseFloat( vals[k] ); }
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { y0 = Float.parseFloat( vals[k] ); }

          ++k; while ( k < s && vals[k].length() == 0 ) ++k;  // CP2
          if ( k < s ) { x1 = Float.parseFloat( vals[k] ); }
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { y1 = Float.parseFloat( vals[k] ); }

          ++k; while ( k < s && vals[k].length() == 0 ) ++k;  // P2
          if ( k < s ) { x2 = Float.parseFloat( vals[k] ); }
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { 
            y2 = Float.parseFloat( vals[k] ); 
            path.cubicTo( x0*unit, y0*unit, x1*unit, y1*unit, x2*unit, y2*unit );
          }
        } catch ( NumberFormatException e ) {
          Log.e("TopoGL_SYMBOL", path + " parse cubicTo error" );
        }
      } else if ( "addCircle".equals( vals[k] ) ) {
        try {
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { x0 = Float.parseFloat( vals[k] ); }  // center X coord
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { y0 = Float.parseFloat( vals[k] ); }  // center Y coord
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) {
            x1 = Float.parseFloat( vals[k] );                 // radius
            path.addCircle( x0*unit, y0*unit, x1*unit, Path.Direction.CCW );
          }
        } catch ( NumberFormatException e ) {
          Log.e("TopoGL-SYMBOL", path + " parse circle error" );
        }
      } else if ( "arcTo".equals( vals[k] ) ) {
        // (x0,y0) top-left corner of rect
        // (x1,y1) bottom-right corner of rect
        // x2 start-angle [degrees]
        // y2 sweep angle (clockwise) [degrees]
        // 
        //    (x0,y0) +-----=-----+
        //            |     |     |
        //            |=====+=====| 0 angle (?)
        //            |     |     | | sweep direction
        //            +-----=-----+ V
        //
        try {
          ++k; while ( k < s && vals[k].length() == 0 ) ++k; // RECTANGLE first endpoint
          if ( k < s ) { x0 = Float.parseFloat( vals[k] ); }
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { y0 = Float.parseFloat( vals[k] ); }

          ++k; while ( k < s && vals[k].length() == 0 ) ++k; // RECTANGLE second endpoint
          if ( k < s ) { x1 = Float.parseFloat( vals[k] ); }
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { y1 = Float.parseFloat( vals[k] ); }

          ++k; while ( k < s && vals[k].length() == 0 ) ++k;  // FROM, TT angles [deg]
          if ( k < s ) { x2 = Float.parseFloat( vals[k] ); }
          ++k; while ( k < s && vals[k].length() == 0 ) ++k;
          if ( k < s ) { 
            y2 = Float.parseFloat( vals[k] ); 
            path.arcTo( new RectF(x0*unit, y0*unit, x1*unit, y1*unit), x2, y2 );
          }
        } catch ( NumberFormatException e ) {
          Log.e("TopoGL-SYMBOL", path + " parse arcTo error" );
        }
      }
    }
    return path;
  }
}


