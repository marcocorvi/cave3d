/** @file Cave3DDrawPath.java
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

import android.util.Log;

public class Cave3DDrawPath // implements Renderer
{
  private static final String TAG = "Cave3D";

  Path path;
  Paint paint;
  int count;   // number of lines (shots) added to the path

// ------------------------------------------------------------
// cstr
// ------------------------------------------------------------
  public Cave3DDrawPath( Paint p )
  {
    path = new Path();
    paint = p;
    count = 0;
  }

  void draw( Canvas canvas )
  {
    canvas.drawPath( path, paint );
  }

}

