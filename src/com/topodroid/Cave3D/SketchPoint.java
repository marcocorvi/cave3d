/** @file SketchPoint.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief TopoDroid sketch point
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

class SketchPoint extends Vector3D
{
  String thname;
  int    idx;  // symbol index

  SketchPoint( double x, double y, double z, String th )
  {
    super( x, y, z );
    thname = th;
    idx = GlSketch.getPointIndex( thname );
  }
}
