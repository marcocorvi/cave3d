/** @file Cave3DDrawTextPath.java
 *
 *e @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D canvas drawing path
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

// import android.graphics.Matrix;
// import android.graphics.Bitmap;
// import android.graphics.RectF;
// import android.graphics.PorterDuff;
// import android.graphics.PointF;

import android.util.Log;

public class Cave3DDrawTextPath extends Cave3DDrawPath
{
  // private static final String TAG = "Cave3D TEXT";

  private String text;

// ------------------------------------------------------------
// cstr
// ------------------------------------------------------------
  public Cave3DDrawTextPath( Paint p, String t )
  {
    super( p );
    text = t;
  }

  @Override
  void draw( Canvas canvas )
  {
    canvas.drawTextOnPath( text, path, 0f, 0f, paint );
  }

}

