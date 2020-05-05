/** @file GlLines.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D shots (either legs or splays)
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

// set of lines
class GlLines extends GlShape
{
  final static int COORDS_PER_VERTEX = 3;
  final static int COORDS_PER_COLOR  = 4;

  final static int STRIDE = 7; // COORDS_PER_VERTEX + COORDS_PER_COLOR;
  final static int BYTE_STRIDE = 28; // STRIDE * Float.BYTES;

  final static int OFFSET_VERTEX = 0;
  final static int OFFSET_COLOR  = 3; // COORDS_PER_VERTEX;

  private class Line3D
  {
    Vector3D v1;
    Vector3D v2;
    int      color; // color index 
    boolean  survey; // survey or axis

    // w1, w2 in survey frame
    Line3D( Vector3D w1, Vector3D w2, int c, boolean s ) 
    { 
       v1 = new Vector3D( w1.x, w1.z, -w1.y );
       v2 = new Vector3D( w2.x, w2.z, -w2.y );
       color=c;
       survey=s;
    }

    // w1, w2 in survey frame
    // XYZ med in OpenGL
    Line3D( Vector3D w1, Vector3D w2, int c, boolean s, float xmed, float ymed, float zmed ) 
    { 
       v1 = new Vector3D( w1.x-xmed, w1.z-ymed, -w1.y-zmed );
       v2 = new Vector3D( w2.x-xmed, w2.z-ymed, -w2.y-zmed );
       color=c;
       survey=s;
     }

    // XYZ med in OpenGL
    void reduce( float x0, float y0, float z0 )
    {
      v1.x -= x0;   v1.y -= y0;   v1.z -= z0;
      v2.x -= x0;   v2.y -= y0;   v2.z -= z0;
    }
  }
  //-------------------------------------------------------

  private float mPointSize = 5.0f;
          int   mColorMode = COLOR_NONE;
  private float mAlpha = 1.0f;

  private FloatBuffer depthBuffer = null;

  ArrayList< Line3D > lines;
  private int lineCount;
  private TglColor mColor;

  private float xmin, xmax; // OpenGL frame
  private float ymin, ymax;
  private float zmin, zmax;

  float getXmin()   { return xmin; }
  float getYmin()   { return ymin; }
  float getZmin()   { return zmin; }
  float getXmax()   { return xmax; }
  float getYmax()   { return ymax; }
  float getZmax()   { return zmax; }

  float getYdelta() { return ymax - ymin; }
  float getXmed()   { return (xmin + xmax)/2; }
  float getYmed()   { return (ymin + ymax)/2; }
  float getZmed()   { return (zmin + zmax)/2; }

  String getBBoxString() // LOG
  {
    StringBuilder sb = new StringBuilder();
    sb.append( " X " + xmin + " " + xmax );
    sb.append( " Y " + ymin + " " + ymax );
    sb.append( " Z " + zmin + " " + zmax );
    return sb.toString();
  }

  // this is not the rel diameter, but the diagonal of the enclosing axis-parallel parallelepiped 
  float diameter()
  {
    float x = xmax - xmin;
    float y = ymax - ymin;
    float z = zmax - zmin;
    return (float)Math.sqrt( x*x + y*y + z*z );
  }

  // vertex data ( X Y Z R G B A )
  GlLines( Context ctx, int color_mode )
  {
    super( ctx );
    mColorMode = color_mode; 
    mColor = new TglColor( TglColor.ColorSplay );
    lines = new ArrayList< Line3D >();
    mAlpha = 1.0f;
  }

  GlLines( Context ctx, TglColor color )
  {
    super( ctx );
    mColorMode = COLOR_SURVEY;
    mColor = color; // mColor will remain constant
    lines = new ArrayList< Line3D >();
    mAlpha = 1.0f;
  }

  GlLines( Context ctx, float[] color )
  {
    super( ctx );
    mColorMode = COLOR_SURVEY;
    mColor = new TglColor( color ); // mColor will remain constant
    lines = new ArrayList< Line3D >();
    mAlpha = 1.0f;
  }
  // GlLines( Context ctx, float red, float green, float blue, float alpha )
  // {
  //   super( ctx );
  //   mColorMode = COLOR_SURVEY;
  //   mColor = new TglColor( red, green, blue, alpha );
  //   lines = new ArrayList< Line3D >();
  //   mAlpha = 1.0f;
  // }

  // survey = survey or fixed (axis) color
  // color  = color index: [0-12) for survey, [0-5) for fixed
  void addLine( Vector3D w1, Vector3D w2, int color, boolean survey ) 
  { 
    lines.add( new Line3D( w1, w2, color, survey ) ); 
    if ( lines.size() == 1 ) {
      xmin = xmax = w1.x;
      ymin = ymax = w1.z;
      zmin = zmax = -w1.y;
    } else {
      updateBounds( w1.x, w1.z, -w1.y );
    }
    updateBounds( w2.x, w2.z, -w2.y );
  }

  // survey = index of survey in Cave3DSurvey list
  // w1, w2 in survey frame
  // XYZ med n OpenGlL
  void addLine( Vector3D w1, Vector3D w2, int color, boolean survey, float xmed, float ymed, float zmed ) 
  { 
    lines.add( new Line3D( w1, w2, color, survey, xmed, ymed, zmed ) ); 
  }

  void prepareDepthBuffer( List<Cave3DShot> legs, DEMsurface surface )
  {
    depthBuffer = null;
    if ( surface == null ) return;
    int count = 2 * legs.size();
    float[] col = new float[ count ];
    int k = 0;
    float zmax = 0;
    for ( Cave3DShot leg : legs ) {
      Cave3DStation f = leg.from_station;
      Cave3DStation t = leg.to_station;
      col[k] = surface.computeZ( f.x, f.y ) - f.z;
      if ( col[k] < 0 )         { col[k] = 0; }
      else if ( col[k] > zmax ) { zmax = col[k]; }
      ++k;
      col[k] = surface.computeZ( t.x, t.y ) - t.z;
      if ( col[k] < 0 )         { col[k] = 0; }
      else if ( col[k] > zmax ) { zmax = col[k]; }
      ++k;
    }
    zmax = 1.0f/zmax;
    for ( k=0; k<count; ++k ) col[k] *= zmax;
    depthBuffer = GL.getFloatBuffer( count );
    depthBuffer.put( col );
  }

  // compute BBox in OpenGL frame
  void computeBBox()
  {
    if ( lines.size() == 0 ) return;
    Vector3D v1 = lines.get(0).v1;
    xmin = xmax = v1.x;
    ymin = ymax = v1.y;
    zmin = zmax = v1.z;
    for ( Line3D line : lines ) {
      v1 = line.v1;
      updateBounds( v1.x, v1.y, v1.z );
      v1 = line.v2;
      updateBounds( v1.x, v1.y, v1.z );
    }
  }

  /* LOG
  void logMinMax() 
  {
    if ( lines.size() == 0 ) {
      return;
    }
    float xmin, xmax, ymin, ymax, zmin, zmax;
    Vector3D v = lines.get( 0 ).v1;
    xmin = xmax = v.x;
    ymin = ymax = v.y;
    zmin = zmax = v.z;
    for ( Line3D line : lines ) {
      v = line.v1;
      if ( v.x < xmin ) { xmin = v.x; } else if ( v.x > xmax ) { xmax = v.x; }
      if ( v.y < ymin ) { ymin = v.y; } else if ( v.y > ymax ) { ymax = v.y; }
      if ( v.z < zmin ) { zmin = v.z; } else if ( v.z > zmax ) { zmax = v.z; }
      v = line.v2;
      if ( v.x < xmin ) { xmin = v.x; } else if ( v.x > xmax ) { xmax = v.x; }
      if ( v.y < ymin ) { ymin = v.y; } else if ( v.y > ymax ) { ymax = v.y; }
      if ( v.z < zmin ) { zmin = v.z; } else if ( v.z > zmax ) { zmax = v.z; }
    }
    Log.i("TopoGL-LINE", "lines " + lines.size() + " X " + xmin + " " + xmax + " Y " + ymin + " " + ymax + " Z " + zmin + " " + zmax );
  }
  */

  // must be called only on legs - for the others use addLine with reduced XYZ med
  // X,Y,Z openGL
  void reduceData( float xmed, float ymed, float zmed )
  {
    for ( Line3D line : lines ) line.reduce( xmed, ymed, zmed );
    computeBBox();
  }

  private float[] prepareData( )
  { 
    if ( lines.size() == 0 ) return null;
    lineCount   = lines.size();
    // Log.v("TopoGL", "lines " + lineCount + " X " + xmin + " " + xmax + " Y " + ymin + " " + ymax + " Z " + zmin + " " + zmax );
    int vertexCount = lineCount * 2;
    float[] data = new float[ vertexCount * STRIDE ];
    float[] color = new float[ COORDS_PER_COLOR ];
    TglColor.getSurveyColor( lines.get(0).color, color );
    // Log.v("TopoGL-LINES", "prepare lines " + lineCount + " color " + color[0] + " " + color[1] + " " + color[2] + " " + color[3] );
    // Log.v("TopoGL-LINES", "prepare lines " + lineCount + " zmin " + zmin );
    int k = 0;
    for ( Line3D line : lines ) {
      Vector3D w1 = line.v1;
      Vector3D w2 = line.v2;
      if ( line.survey ) {
        TglColor.getSurveyColor( line.color, color );
      } else {
        TglColor.getAxisColor( line.color, color );
      }
      data[k++] = w1.x;
      data[k++] = w1.y;
      data[k++] = w1.z;
      data[k++] = color[0];
      data[k++] = color[1];
      data[k++] = color[2];
      data[k++] = 1.0f; // alpha;
      data[k++] = w2.x;
      data[k++] = w2.y;
      data[k++] = w2.z;
      data[k++] = color[0];
      data[k++] = color[1];
      data[k++] = color[2];
      data[k++] = 1.0f; // alpha;
    }
    return data;
  }

  void initData( ) { initData( prepareData(), lines.size() ); }

  void initData( float[] data, int count )
  { 
    lineCount = count;
    if ( lineCount == 0 ) return;
    initDataBuffer( data );

    // this order collects a sequence of triangles
    // ByteBuffer ob = ByteBuffer.allocateDirect( lineCount * 2 * 2 ); // 2 bytes / short
    // ob.order(ByteOrder.nativeOrder());
    // orderBuffer = ob.asShortBuffer();
    // short[] order = new short[ lineCount * 2 ];
    // int k = 0;
    // for (int i = 0; i<lineCount; ++i ) {
    //   order[ k++ ] = (short)(2*i+0);
    //   order[ k++ ] = (short)(2*i+1);
    // }
    // orderBuffer.put( order );
    // orderBuffer.position( 0 );
  }

  // ---------------------------------------------------
  // DRAW

  void draw( float[] mvpMatrix, int draw_mode, boolean points ) // for legs-only
  {
    if ( draw_mode == GlModel.DRAW_NONE || lineCount == 0 ) return;
    if ( mColorMode == COLOR_NONE   ) {
      GL.useProgram( mProgramUColor );
      bindDataUColor( mvpMatrix, TglColor.ColorLeg );
    } else if ( mColorMode == COLOR_SURVEY ) {
      GL.useProgram( mProgramAColor );
      bindDataAColor( mvpMatrix );
    } else if ( mColorMode == COLOR_DEPTH  ) {
      GL.useProgram( mProgramZColor );
      bindDataZColor( mvpMatrix, TglColor.ColorLeg );
    } else if ( mColorMode == COLOR_SURFACE  ) {
      if ( depthBuffer != null ) {
        GL.useProgram( mProgramSColor );
        bindDataSColor( mvpMatrix );
      } else {
        GL.useProgram( mProgramUColor );
        bindDataUColor( mvpMatrix, TglColor.ColorLeg );
      }
    } else { 
      return; 
    }

    GL.drawLine( 0, lineCount ); 
    if ( mPointSize > 0 && points ) {
      GL.useProgram( mProgramStation ); // Sttaion is the same as UColor 
      bindDataStation( mvpMatrix, TglColor.ColorStation );
      GL.drawPoint( 0, lineCount * 2 );
    }
    // unbindData();
  }

  void draw( float[] mvpMatrix, int draw_mode )
  {
    if ( draw_mode == GlModel.DRAW_NONE || lineCount == 0 ) return;
    if ( mColorMode == COLOR_NONE   ) {
      GL.useProgram( mProgramUColor );
      bindDataUColor( mvpMatrix, mColor.color );
    } else if ( mColorMode == COLOR_SURVEY ) {
      GL.useProgram( mProgramAColor );
      bindDataAColor( mvpMatrix );
    } else if ( mColorMode == COLOR_DEPTH  ) {
      GL.useProgram( mProgramZColor );
      bindDataZColor( mvpMatrix, mColor.color );
    } else if ( mColorMode == COLOR_SURFACE  ) {
      if ( depthBuffer != null ) {
        GL.useProgram( mProgramSColor );
        bindDataSColor( mvpMatrix );
      } else {
        GL.useProgram( mProgramUColor );
        bindDataUColor( mvpMatrix, mColor.color );
      }
    } else { 
      return; 
    }

    if ( draw_mode == GlModel.DRAW_LINE ) {
      GL.drawLine( 0, lineCount ); 
    } else if ( draw_mode == GlModel.DRAW_POINT ) {
      if ( mPointSize > 0 ) GL.drawPoint( 0, lineCount * 2 );
    } else if ( draw_mode == GlModel.DRAW_ALL ) {
      GL.drawLine( 0, lineCount );
      if ( mPointSize > 0 ) GL.drawPoint( 0, lineCount * 2 );
    }
    // unbindData();
  }

  private void bindDataStation( float[] mvpMatrix, float[] color ) // Station is the same as UColor, just with a different color
  {
    GL.setUniformMatrix( mstUMVPMatrix, mvpMatrix );
    GL.setUniform( mstUPointSize, mPointSize );
    GL.setAttributePointer( mstAPosition, dataBuffer, OFFSET_VERTEX, COORDS_PER_VERTEX, BYTE_STRIDE );
    GL.setUniform( mstUColor, color[0], color[1], color[2], color[3] );
  }

  private void bindDataUColor( float[] mvpMatrix, float[] color )
  {
    GL.setUniformMatrix( muUMVPMatrix, mvpMatrix );
    GL.setUniform( muUPointSize, mPointSize );
    GL.setAttributePointer( muAPosition, dataBuffer, OFFSET_VERTEX, COORDS_PER_VERTEX, BYTE_STRIDE );
    GL.setUniform( muUColor, color[0], color[1], color[2], color[3] );
  }

  private void bindDataAColor( float[] mvpMatrix )
  {
    GL.setUniformMatrix( maUMVPMatrix, mvpMatrix );
    GL.setUniform( maUPointSize, mPointSize );
    GL.setUniform( maUAlpha, mAlpha );
    GL.setAttributePointer( maAPosition, dataBuffer, OFFSET_VERTEX, COORDS_PER_VERTEX, BYTE_STRIDE );
    GL.setAttributePointer( maAColor,    dataBuffer, OFFSET_COLOR,  COORDS_PER_COLOR,  BYTE_STRIDE );
  }

  private void bindDataZColor( float[] mvpMatrix, float[] color )
  {
    GL.setUniformMatrix( mzUMVPMatrix, mvpMatrix );
    GL.setUniform( mzUPointSize, mPointSize );
    GL.setAttributePointer( mzAPosition, dataBuffer, OFFSET_VERTEX, COORDS_PER_VERTEX, BYTE_STRIDE );
    GL.setUniform( mzUZMin,   GlModel.mZMin );
    GL.setUniform( mzUZDelta, GlModel.mZDelta );
    GL.setUniform( mzUColor, color[0], color[1], color[2], color[3] );
  }

  private void bindDataSColor( float[] mvpMatrix )
  {
    GL.setUniformMatrix( msUMVPMatrix, mvpMatrix );
    GL.setUniform( msUPointSize, mPointSize );
    GL.setAttributePointer( msAPosition, dataBuffer, OFFSET_VERTEX, COORDS_PER_VERTEX, BYTE_STRIDE );
    GL.setAttributePointer( msAColor, depthBuffer, 0, 1, 4 );
  }

  // ------------------------------------------------------------
  // UTILITIES

  int size() { return lines.size(); }

  void setPointSize( float size ) { mPointSize = 5.0f * size; }

  void setAlpha( float a ) { mAlpha = ( a < 0.2f )? 0.2f : ( a > 1.0f )? 1.0f : a; }

  private void updateBounds( float x, float y, float z )
  {
    if ( xmin > x ) { xmin = x; } else if ( xmax < x ) { xmax = x; }
    if ( ymin > y ) { ymin = y; } else if ( ymax < y ) { ymax = y; }
    if ( zmin > z ) { zmin = z; } else if ( zmax < z ) { zmax = z; }
  }

  // -----------------------------------------------------------------
  // COLOR MODE
  static final int COLOR_NONE    = 0;
  static final int COLOR_SURVEY  = 1;
  static final int COLOR_DEPTH   = 2;
  static final int COLOR_SURFACE = 3;
  static final int COLOR_MAX     = 4;

  void toggleColorMode() { mColorMode = ( mColorMode + 1 ) % COLOR_MAX; }
  void setColorMode(int mode) { mColorMode = ( mode % COLOR_MAX ); }

  // -----------------------------------------------------------------
  // OpenGL

  private static int mProgramUColor;
  private static int mProgramAColor;
  private static int mProgramZColor;
  private static int mProgramSColor;
  private static int mProgramStation;

  private static int maAPosition;
  private static int muAPosition;
  private static int mzAPosition;
  private static int msAPosition;
  private static int mstAPosition;

  private static int maAColor;
  private static int muUColor;
  private static int mzUColor;
  private static int mstUColor;
  private static int msAColor;
  private static int maUAlpha;
  
  private static int maUMVPMatrix;
  private static int muUMVPMatrix;
  private static int mzUMVPMatrix;
  private static int msUMVPMatrix;
  private static int mstUMVPMatrix;

  private static int maUPointSize;
  private static int muUPointSize;
  private static int mzUPointSize;
  private static int msUPointSize;
  private static int mstUPointSize;

  private static int mzUZMin;
  private static int mzUZDelta;

  static void initGL( Context ctx )
  {
    mProgramUColor = GL.makeProgram( ctx, R.raw.line_ucolor_vertex, R.raw.line_ucolor_fragment );
    setLocationsUColor( mProgramUColor );

    mProgramAColor = GL.makeProgram( ctx, R.raw.line_acolor_vertex, R.raw.line_acolor_fragment );
    setLocationsAColor( mProgramAColor );

    mProgramZColor = GL.makeProgram( ctx, R.raw.line_zcolor_vertex, R.raw.line_zcolor_fragment );
    setLocationsZColor( mProgramZColor );

    mProgramSColor = GL.makeProgram( ctx, R.raw.line_scolor_vertex, R.raw.line_scolor_fragment );
    setLocationsSColor( mProgramSColor );

    mProgramStation = GL.makeProgram( ctx, R.raw.line_station_vertex, R.raw.line_station_fragment );
    setLocationsStation( mProgramStation );
    // Log.v("TopoGL", "Line progs " + mProgramAColor + " " + mProgramUColor + " " + mProgramZColor );
    // Log.v("TopoGL", "Line progs " + mProgramAColor + " " + mProgramUColor + " " + mProgramZColor );
  }

  private static void setLocationsSColor( int program )
  {
    msUMVPMatrix = GL.getUniform(   program, GL.uMVPMatrix );
    msUPointSize = GL.getUniform(   program, GL.uPointSize );
    msAPosition  = GL.getAttribute( program, GL.aPosition );  // variable names must coincide with those in fragments
    msAColor     = GL.getAttribute( program, GL.aColor );
    // Log.v("TopoGL", "Line-A " + maUPointSize + " " + maAPosition + " " + maAColor + " " + maUAlpha );
  }
  private static void setLocationsAColor( int program )
  {
    maUMVPMatrix = GL.getUniform(   program, GL.uMVPMatrix );
    maUPointSize = GL.getUniform(   program, GL.uPointSize );
    maAPosition  = GL.getAttribute( program, GL.aPosition );  // variable names must coincide with those in fragments
    maAColor     = GL.getAttribute( program, GL.aColor );
    maUAlpha     = GL.getUniform(   program, GL.uAlpha );
    // Log.v("TopoGL", "Line-A " + maUPointSize + " " + maAPosition + " " + maAColor + " " + maUAlpha );
  }
  private static void setLocationsUColor( int program )
  {
    muUMVPMatrix = GL.getUniform(   program, GL.uMVPMatrix );
    muUPointSize = GL.getUniform(   program, GL.uPointSize );
    muAPosition  = GL.getAttribute( program, GL.aPosition );  // variable names must coincide with those in fragments
    muUColor     = GL.getUniform(   program, GL.uColor );
    // Log.v("TopoGL", "Line-U " + muUPointSize + " " + muAPosition + " " + muUColor );
  }
  // Station is the sane as UColor
  private static void setLocationsStation( int program )
  {
    mstUMVPMatrix = GL.getUniform(   program, GL.uMVPMatrix );
    mstUPointSize = GL.getUniform(   program, GL.uPointSize );
    mstAPosition  = GL.getAttribute( program, GL.aPosition );  // variable names must coincide with those in fragments
    mstUColor     = GL.getUniform(   program, GL.uColor );
    // Log.v("TopoGL", "Line-U " + muUPointSize + " " + muAPosition + " " + muUColor );
  }
  private static void setLocationsZColor( int program )
  {
    mzUMVPMatrix = GL.getUniform(   program, GL.uMVPMatrix );
    mzUPointSize = GL.getUniform(   program, GL.uPointSize );
    mzAPosition  = GL.getAttribute( program, GL.aPosition );  // variable names must coincide with those in fragments
    mzUZMin      = GL.getUniform(   program, GL.uZMin );
    mzUZDelta    = GL.getUniform(   program, GL.uZDelta );
    mzUColor     = GL.getUniform(   program, GL.uColor );
    // Log.v("TopoGL", "Line-Z " + mzUPointSize + " " + mzAPosition + " " + mzUColor + " " + mzUZMin + " " + mzUZDelta );
  }
}


