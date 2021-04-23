/* @file DataBuffer.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief data packet buffer 
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import java.util.Arrays;

public class DataBuffer
{
  // data buffer types
  final static int DATA_NONE    = 0;

  final static int DATA_PRIM    = 1;
  final static int DATA_META    = 2;
  final static int DATA_ERR     = 3;
  final static int DATA_TIME    = 4;

  final static int DATA_PACKET  = 5;
  final static int DATA_G       = 6;
  final static int DATA_M       = 7;
  final static int DATA_VECTOR  = 8;
  final static int DATA_REPLY   = 9;

  final static int DATA_EXIT    = 10; // first condition to exit the queue consumer

  final static String[] typeString = { "NONE", "PRIM", "META", "ERR", "TIME", "PACKET", "G", "M", "VECTOR", "REPLY", "EXIT" };

  public byte[] data;
  public int type;   // data type
  public int device; // device type
  public DataBuffer next;
  
  public DataBuffer( int t, int d, byte[] bytes )
  {
    data = Arrays.copyOfRange( bytes, 0, bytes.length );
    type = t;
    device = d;
    next = null;
  }
}

