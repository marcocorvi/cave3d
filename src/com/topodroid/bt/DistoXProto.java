/* @file DistoXProto.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief DistoX2 protocol
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import com.topodroid.Cave3D.TopoGL;

import android.os.Looper;
import android.os.Handler;
import android.content.Context;

import android.bluetooth.BluetoothDevice;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DistoXProto extends TopoGLProto
{
  public DistoXProto( TopoGL app, int device_type, BluetoothDevice bt_device )
  {
    super( app, device_type, bt_device );
  }

  // DATA -------------------------------------------------------

  @Override
  DataBuffer getDataBuffer( int type, byte[] bytes )
  {
    type = DataBuffer.DATA_NONE;
    byte type_byte = (byte)(bytes[0] & 0x3f);
    switch ( type_byte ) {
      case 0x01:  type = DataBuffer.DATA_PACKET; break;
      case 0x02:  type = DataBuffer.DATA_G; break;
      case 0x03:  type = DataBuffer.DATA_M; break;
      case 0x04:  type = DataBuffer.DATA_VECTOR; break;
      case 0x38:  type = DataBuffer.DATA_REPLY; break;
    }
    if ( type == DataBuffer.DATA_NONE ) return null;
    Log.v("Cave3D", "DistoX proto get buffer - device type " + mDeviceType );
    return new DataBuffer( type, mDeviceType, Arrays.copyOf( bytes, 8 ) );
  }

  @Override
  protected void handleDataBuffer( DataBuffer data_buffer )
  {
    if ( ! DeviceType.isDistoX( data_buffer.device ) ) {
      Log.v("Cave3D", "DistoX proto handle buffer - device is not distox " + data_buffer.device );
      return;
    }
    handleDistoXBuffer( data_buffer );
  }

}

