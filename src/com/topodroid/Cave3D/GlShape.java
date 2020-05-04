/** @file GlShape.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D (abstract) shape 
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

import android.content.Context;

class GlShape
{
  final static int BYTES_PER_FLOAT = 4;
  final static int BYTES_PER_SORTT = 2;

  protected FloatBuffer dataBuffer;
  protected ShortBuffer orderBuffer;

  protected Context mContext;


  // -----------------------------------------------------------------
  GlShape( Context ctx ) { mContext = ctx; }

  void initDataBuffer( float[] data )
  {
    dataBuffer = GL.getFloatBuffer( data.length );
    dataBuffer.put( data );
  }

}
