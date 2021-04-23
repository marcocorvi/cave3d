/* @file SapProto.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief SAP5 protocol REQUIRES API-18
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 *
 * WARNING TO BE FINISHED
 */
package com.topodroid.bt;

import com.topodroid.Cave3D.TopoGL;

import android.util.Log;

// import android.os.Handler;
import android.content.Context;

import android.bluetooth.BluetoothDevice;
// import android.bluetooth.BluetoothAdapter;
// import android.bluetooth.BluetoothGatt;
// import android.bluetooth.BluetoothGattService;
// import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;
import java.util.Arrays;

// -----------------------------------------------------------------------------
class SapProto extends TopoGLProto
{
  
  SapProto( TopoGL app, int device_type, BluetoothDevice bt_device )
  {
    super( app, device_type, bt_device );
    mWriteBuffer = new ArrayList< byte[] >(); // WRITE_BUFFER
    Log.v("DistoX", "SAP proto: cstr");
  }

  // WRITE_BUFFER -------------------------------------------------------------

  private ArrayList< byte[] > mWriteBuffer = null;

  private void addToWriteBuffer( byte[] bytes ) 
  {
    int pos =  0;
    int len = ( 20 > bytes.length )? bytes.length : 20;
    while ( pos < bytes.length ) {
      mWriteBuffer.add( Arrays.copyOfRange( bytes, pos, len ) );
      pos += len;
      len = ( len + 20 <= bytes.length )? len+20 : bytes.length;
    }
  }

  // DATA BUFFER -------------------------------------------------------------


  @Override
  DataBuffer getDataBuffer( int type, byte[] bytes )
  {
    type = DataBuffer.DATA_NONE;
    byte type_byte = (byte)(bytes[0] & 0x3f);
    switch ( type_byte ) {
      case 0x01:  type = DataBuffer.DATA_PACKET; break;
    }
    if ( type == DataBuffer.DATA_NONE ) return null;
    Log.v("Cave3D", "SAP proto get buffer - device type " + mDeviceType );
    return new DataBuffer( type, mDeviceType, Arrays.copyOf( bytes, 8 ) );
  }


  @Override
  protected void handleDataBuffer( DataBuffer data_buffer )
  {
    if ( ! DeviceType.isSap( data_buffer.device ) ) {
      Log.v( "Cave3D", "SAP proto handle buffer - not a BIC data buffer");
      return;
    }
    Log.v( "Cave3D", "SAP proto handle buffer " + DataBuffer.typeString[ data_buffer.type ] );
    handleDistoXBuffer( data_buffer );
  }

  // not used
  // @return buffer packet type
  // int handlePacket( byte[] buffer ) 
  // {
  //   Log.v("Cave3D", "DistoXComm handle packet " + String.format("%02X", buffer[0]) );
  //   DataBuffer data_buffer = getDataBuffer( DataBuffer.DATA_PACKET, buffer );
  //   if ( data_buffer != null ) {
  //     Log.v("Cave3D", "queueing buffer ");
  //     mQueue.put( data_buffer );
  //   }
  //   return data_buffer.type;
  // }

  // --------------------------------------------------------------------------
  // methods for SapComm

  // @param crtr   GATT write characteristic
  // @return number of bytes set into the write characteristic
  byte[] handleWrite( )
  {
    Log.v("DistoX", "SAP proto: handle write ");
    byte[] bytes = null;
    if ( mWriteBuffer != null ) { // WRITE_BUFFER
      synchronized ( mWriteBuffer ) { 
        if ( ! mWriteBuffer.isEmpty() ) {
          bytes = mWriteBuffer.remove(0);
        }
      }
    }
    return bytes;
  }

  // not used
  // @return bytes packet type
  // int handleRead( byte[] bytes )
  // {
  //   Log.v("DistoX", "SAP proto: read bytes " + bytes.length );
  //   byte[] buffer = new byte[8];
  //   System.arraycopy( bytes, 0, buffer, 0, 8 );
  //   // ACKNOWLEDGMENT
  //   // byte[] ack = new byte[1];
  //   // ack[0] = (byte)( ( buffer[0] & 0x80 ) | 0x55 );
  //   // addToWriteBuffer( ack );
  //   // TODO tell mComm to writeChrt
  //   return handlePacket( buffer );
  // }

  // not used
  // @param chrt   Sap Gatt characteristic
  // int handleReadNotify( BluetoothGattCharacteristic chrt )
  // {
  //   Log.v("Cave3D", "SAP proto handle-read-notify");
  //   return handleRead( chrt.getValue() );
  // }

  byte[] handleWriteNotify( BluetoothGattCharacteristic chrt )
  {
    Log.v("Cave3D", "SAP proto handle-write-notify");
    return handleWrite( ); // WRITE_BUFFER
  }

}
