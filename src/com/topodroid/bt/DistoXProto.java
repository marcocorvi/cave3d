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


  // private int   mIndex;
  // private byte[] mLastTime;       // content of LastTime payload
  // private byte[] mLastPrim;   // used to check if the coming Prim is new
  private byte[] mAddress;
  byte mRollHigh; // high byte of roll

  public DistoXProto( TopoGL app, int device_type, BluetoothDevice bt_device )
  {
    super( app, device_type, bt_device );
    // mComm   = comm;
    // mIndex  = -1;
    // mLastTime = null;
    mAddress = new byte[ 2 ];
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
    return new DataBuffer( type, mDeviceType, Arrays.copyOf( bytes, 8 ) );
  }

  @Override
  protected void handleDataBuffer( DataBuffer data_buffer )
  {
    if ( ! DeviceType.isDistoX( data_buffer.device ) ) return;
    if ( data_buffer.type != DataBuffer.DATA_PACKET ) return;
    byte[] buffer = data_buffer.data;
  
    byte type = (byte)(buffer[0] & 0x3f);
    // int high, low;
    switch ( type ) {
      case 0x01: // Data
        // mBackshot = false;
        int dhh = (int)( buffer[0] & 0x40 );
        double d =  dhh * 1024.0 + DistoXConst.toInt( buffer[2], buffer[1] );
        double b = DistoXConst.toInt( buffer[4], buffer[3] );
        double c = DistoXConst.toInt( buffer[6], buffer[5] );
        // X31--ready
        mRollHigh = buffer[7];

        int r7 = (int)(buffer[7] & 0xff); if ( r7 < 0 ) r7 += 256;
        // double r = (buffer[7] & 0xff);
        double r = r7;

        // if ( mDeviceType == Device.DISTO_A3 || mDeviceType == Device.DISTO_X000) // FIXME VirtualDistoX
        switch ( mDeviceType ) {
          case DeviceType.DEVICE_DISTOX1:
            mDistance = d / 1000.0;
            break;
          case DeviceType.DEVICE_DISTOX2:
            if ( d < 99999 ) {
              mDistance = d / 1000.0;
            } else {
              mDistance = 100 + (d-100000) / 100.0;
            }
            break;
          case DeviceType.DEVICE_BRIC4: 
            // TDLog.Error("TD proto: does not handle packet BLE");
            Log.e("DistoX", "TD proto: does not handle packet BLE");
            break;
          case DeviceType.DEVICE_SAP5: 
            // Log.v("DistoX", "TD proto: handle packet SAP");
            mDistance = d / 1000.0;
            break;
          default:
            mDistance = d / 1000.0;
            break;
        }

        mBearing  = b * 180.0 / 32768.0; // 180/0x8000;
        mClino    = c * 90.0  / 16384.0; // 90/0x4000;
        if ( c >= 32768 ) { mClino = (65536 - c) * (-90.0) / 16384.0; }
        mRoll = r * 180.0 / 128.0;

        handleData();
        break; // return DataBuffer.DATA_PACKET;
      case 0x02:
        break; // return DataBuffer.DATA_G;
      case 0x03:
        break; // return DataBuffer.DATA_M;
      case 0x04:
        break; // return DataBuffer.DATA_VECTOR;
      case 0x38:  // Reply packet
        mAddress[0] = buffer[1];
        mAddress[1] = buffer[2];
        {
          byte[] mReplyBuffer = new byte[4];
          mReplyBuffer[0] = buffer[3];
          mReplyBuffer[1] = buffer[4];
          mReplyBuffer[2] = buffer[5];
          mReplyBuffer[3] = buffer[6];
          // TDLog.Log( TDLog.LOG_PROTO, "handle Packet mReplyBuffer" );
          // TODO
        }
        break; // return DataBuffer.DATA_REPLY;
      default:
        Log.e( "Cave3D",
          "packet error. type " + type + " " + 
          String.format("%02x %02x %02x %02x %02x %02x %02x %02x", buffer[0], buffer[1], buffer[2],
          buffer[3], buffer[4], buffer[5], buffer[6], buffer[7] ) );
      //   return DataBuffer.DATA_NONE;
    }
    // return DataBuffer.DATA_NONE;
  }
}

