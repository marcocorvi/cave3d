/* @file SapComm.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid SAP5 communication REQUIRES API-18
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 * TopoDroid implementation of BLE callback follows the guidelines of 
 *   Chee Yi Ong,
 *   "The ultimate guide to Android bluetooth low energy"
 *   May 15, 2020
 *
 * WARNING TO BE FINISHED
 */
package com.topodroid.bt;

import com.topodroid.Cave3D.TopoGL;

import android.util.Log;

import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.content.Context;

import android.bluetooth.BluetoothDevice;
// import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattDescriptor;
// import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

// -----------------------------------------------------------------------------
public class SapComm extends TopoGLComm
                     implements BleComm // , BleChrtChanged
{
  // -----------------------------------------------
  // BluetoothAdapter   mAdapter;
  // private BluetoothGatt mGatt = null;
  // BluetoothGattCharacteristic mWriteChrt;

  private ConcurrentLinkedQueue< BleOperation > mOps;
  private BluetoothDevice mRemoteBtDevice;
  private BleCallback     mCallback;

  private BluetoothGattCharacteristic mReadChrt  = null;
  private BluetoothGattCharacteristic mWriteChrt = null;
  private boolean mReadInitialized  = false;
  private boolean mWriteInitialized = false;

  // BluetoothGattCharacteristic getReadChrt() { return mReadChrt; }
  // BluetoothGattCharacteristic getWriteChrt() { return mWriteChrt; }

  public SapComm( Context ctx, TopoGL app, BluetoothDevice bt_device ) 
  {
    super( ctx, app, TopoGLComm.COMM_GATT, bt_device.getAddress() );
    mRemoteBtDevice  = bt_device;
    setProto( new SapProto( mApp, DeviceType.DEVICE_SAP5, bt_device ) );
    Log.v("Cave3D", "SAP comm: cstr" );
  }

  // void setRemoteDevice( BluetoothDevice device ) 
  // { 
  //   Log.v("Cave3D", "SAP comm: set remote " + device.getAddress() );
  //   mRemoteBtDevice = device;
  // }

  // -------------------------------------------------------------
  /** 
   * connection and data handling must run on a separate thread
   */

  // Device has mAddress, mModel, mName, mNickname, mType
  // the only thing that coincide with the remote_device is the address
  //
  private void connectSapDevice( ) 
  {
    if ( mRemoteBtDevice == null ) {
      // TDToast.makeBad( R.string.ble_no_remote );
      Log.v("Cave3D", "SAP comm: error: null remote device");
    } else {
      // check that device.mAddress.equals( mRemoteBtDevice.getAddress() 
      Log.v("Cave3D", "SAP comm: connect remote addr " + mRemoteBtDevice.getAddress() );
      notifyStatus( ConnectionState.CONN_WAITING );
      mOps      = new ConcurrentLinkedQueue< BleOperation >();
      mCallback = new BleCallback( this, true ); // auto_connect true
      enqueueOp( new BleOpConnect( mContext, this, mRemoteBtDevice ) );
      doNextOp();
    }
  }



  // -------------------------------------------------------------

  private boolean mDisconnecting = false;

  // disconnect the GATT

  void doDisconnectGatt()
  {
    Log.v("Cave3D", "SAP comm: do disconnect GATT - disconnecting " + mDisconnecting );
    if ( mDisconnecting ) return;
    mDisconnecting = true;
    enqueueOp( new BleOpDisconnect( mContext, this ) );
    doNextOp();
    // closeChrt();
    // mCallback.disconnectGatt();
    notifyStatus( ConnectionState.CONN_WAITING );
    // mDisconnecting = false;
  }

  void doConnectGatt()
  {
    Log.v("Cave3D", "SAP comm: do connect GATT");
    notifyStatus( ConnectionState.CONN_WAITING );
    enqueueOp( new BleOpConnect( mContext, this, mRemoteBtDevice ) );
    doNextOp();
  }

  void connected( boolean is_connected )
  {
    Log.v("Cave3D", "SAP comm: connected ...");
    mBTConnected = is_connected;
    if (is_connected ) {
      notifyStatus( ConnectionState.CONN_CONNECTED );
    } else {
      // TODO
    }
  }

  // BleComm interface
  public void connected() { connected( true ); }

  void reconnectDevice()
  {
    Log.v("Cave3D", "SAP comm: reconnect ..." + mAddress );
    doDisconnectGatt();
    doConnectGatt();
    // mCallback.connectGatt( mContext, mRemoteBtDevice );
  }

  // unused
  // private boolean readSapPacket( )
  // { 
  //   enqueueOp( new BleOpChrtRead( mContext, this, SapConst.SAP5_SERVICE_UUID, SapConst.SAP5_CHRT_READ_UUID ) );
  //   doNextOp();
  //   return true;
  //   // return mCallback.readCharacteristic( );
  // }
    
  // ------------------------------------------------------------------------------------
  // CONTINUOUS DATA DOWNLOAD
  private int mConnectionMode = -1;

  // @Implements
  public boolean connectDevice( )
  {
    Log.v( "Cave3D", "SAP comm connect Device " + mAddress );
    mConnectionMode = 1;
    connectSapDevice( );
    return true;
  }

  @Override
  public boolean disconnectDevice() 
  {
    // Log.v("Cave3D", "SAP comm: disconnect device");
    if ( mDisconnecting ) return true;
    if ( ! mBTConnected ) return true;
    mDisconnecting = true;
    mConnectionMode = -1;
    mBTConnected = false;
    closeChrt();
    mCallback.disconnectCloseGatt();
    notifyStatus( ConnectionState.CONN_DISCONNECTED );
    mDisconnecting = false;
    return true;
  }

  private void closeChrt()
  {
    mWriteInitialized = false; 
    mReadInitialized  = false; 
  }

  // -------------------------------------------------------------------------------------

  // public void addService( BluetoothGattService srv );
  // public void addChrt( UUID srv_uuid, BluetoothGattCharacteristic chrt );
  // public void addDesc( UUID srv_uuid, UUID chrt_uuid, BluetoothGattDescriptor desc );

  private void writeChrt( )
  {
    byte[] bytes = ((SapProto)mProto).handleWrite( );
    if ( bytes != null ) {
      // mCallback.writeCharacteristic( mWriteChrt );
      mCallback.writeChrt( SapConst.SAP5_SERVICE_UUID, SapConst.SAP5_CHRT_WRITE_UUID, bytes );
    } // else // done with the buffer writing
  }

  // -------------------------------------------------------------------------------
  private BleOperation mPendingOp = null;

  private void clearPending() 
  { 
    mPendingOp = null; 
    if ( ! mOps.isEmpty() ) doNextOp();
  }

  // @return the length of the ops queue
  private int enqueueOp( BleOperation op ) 
  {
    mOps.add( op );
    return mOps.size();
  }

  private void doNextOp() 
  {
    if ( mPendingOp != null ) {
      // Log.v("Cave3D", "SAP comm: next op with pending not null, ops " + mOps.size() ); 
      return;
    }
    mPendingOp = mOps.poll();
    // Log.v("Cave3D", "SAP comm: polled, ops " + mOps.size() );
    if ( mPendingOp != null ) {
      mPendingOp.execute();
    } 
  }
  // -------------------------------------------------------------------------------
  // BleComm interface

  // public void changedMtu( int mtu ) { }

  // public void readedRemoteRssi( int rssi ) { }


  public void changedChrt( BluetoothGattCharacteristic chrt )
  {
    String uuid_str = chrt.getUuid().toString();
    Log.v("Cave3D", "SAP comm: changed chrt " + uuid_str );
    if ( uuid_str.equals( SapConst.SAP5_CHRT_READ_UUID_STR ) ) {
      DataBuffer data_buffer = mProto.getDataBuffer( DataBuffer.DATA_PACKET, chrt.getValue() );
      if ( data_buffer != null ) {
        mQueue.put( data_buffer );
      } else {
        error(-3);
      }
    } else if ( uuid_str.equals( SapConst.SAP5_CHRT_WRITE_UUID_STR ) ) {
      byte[] bytes = ((SapProto)mProto).handleWriteNotify( chrt );
      if ( bytes != null ) {
        mCallback.writeChrt( SapConst.SAP5_SERVICE_UUID, SapConst.SAP5_CHRT_WRITE_UUID, bytes );
      }
    }
  }

  // @param uuid_str short UUID string
  public void readedChrt( String uuid_str, byte[] bytes )
  {
    Log.v("Cave3D", "SAP comm: readed chrt " + uuid_str );
    if ( ! mReadInitialized ) { error(-1); return; }
    if ( ! uuid_str.equals( SapConst.SAP5_CHRT_READ_UUID_STR ) ) { error(-2); return; }
    DataBuffer data_buffer = mProto.getDataBuffer( DataBuffer.DATA_PACKET, bytes ); 
    if ( data_buffer != null ) {
      mQueue.put( data_buffer );
    } else {
      error(-3);
    }
  }

  // public void writtenChrt( String uuid_str, byte[] bytes )
  // {
  //   Log.v("Cave3D", "SAP comm: written chrt ...");
  //   if ( ! mWriteInitialized ) { error(-4); return; }
  //   writeChrt( ); // try to write again
  // }

  // public void readedDesc( String uuid_str, String uuid_chrt_str, byte[] bytes )
  // {
  //   Log.v("Cave3D", "SAP comm: readedDesc" );
  // }

  public void writtenDesc( String uuid_str, String uuid_chrt_str, byte[] bytes )
  {
    Log.v("Cave3D", "SAP comm: written desc - chrt " + uuid_chrt_str );
    connected( true );
  }

  // public void completedReliableWrite()
  // {
  //   Log.v("Cave3D", "SAP comm: realiable write" );
  // }

  public void disconnected()
  {
    Log.v("Cave3D", "SAP comm: disconnected ...");
    // if ( mDisconnecting ) return;
    mDisconnecting = false;
    mConnectionMode = -1;
    mBTConnected = false;
    notifyStatus( ConnectionState.CONN_DISCONNECTED );
  }

  public int servicesDiscovered( BluetoothGatt gatt )
  {
    Log.v("Cave3D", "SAP comm: service discovered" );
    BluetoothGattService srv = gatt.getService( SapConst.SAP5_SERVICE_UUID );

    mReadChrt  = srv.getCharacteristic( SapConst.SAP5_CHRT_READ_UUID );
    mWriteChrt = srv.getCharacteristic( SapConst.SAP5_CHRT_WRITE_UUID );

    // boolean write_has_write = BleUtils.isChrtRWrite( mWriteChrt.getProperties() );
    // boolean write_has_write_no_response = BleUtils.isChrtRWriteNoResp( mWriteChrt.getProperties() );
    // Log.v("Cave3D", "SAP callback W-chrt has write " + write_has_write );

    mWriteChrt.setWriteType( BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT );
    mWriteInitialized = gatt.setCharacteristicNotification( mWriteChrt, true );

    mReadInitialized = gatt.setCharacteristicNotification( mReadChrt, true );

    BluetoothGattDescriptor readDesc = mReadChrt.getDescriptor( BleUtils.CCCD_UUID );
    if ( readDesc == null ) {
      Log.v("Cave3D", "SAP callback FAIL no R-desc CCCD ");
      return -1;
    }

    // boolean read_has_write  = BleUtils.isChrtPWrite( mReadChrt.getProperties() );
     // Log.v("Cave3D", "SAP callback R-chrt has write " + read_has_write );

    byte[] notify = BleUtils.getChrtPNotify( mReadChrt );
    if ( notify == null ) {
      Log.v("Cave3D", "SAP callback FAIL no indicate/notify R-property ");
      return -2;
    } else {
      readDesc.setValue( notify );
      if ( ! gatt.writeDescriptor( readDesc ) ) {
        Log.v("Cave3D", "SAP callback ERROR writing readDesc");
        return -3;
      }
    }
    return 0;
  } 

  // this is run by BleOpChrtWrite
  public boolean writeChrt( UUID srv_uuid, UUID chrt_uuid, byte[] bytes )
  {
    Log.v("Cave3D", "SAP comm: ##### writeChrt TODO ..." );
    return mCallback.writeChrt( srv_uuid, chrt_uuid, bytes );
  }

  public boolean readChrt( UUID srv_uuid, UUID chrt_uuid )
  {
    // Log.v("Cave3D", "SAP comm: ##### readChrt" );
    return mCallback.readChrt( srv_uuid, chrt_uuid );
  }

  @Override
  public void error( int status )
  {
    Log.v("Cave3D", "SAP comm: error " + status );
  }

  @Override
  public void failure( int status )
  {
    Log.v("Cave3D", "SAP comm: failure " + status );
    switch ( status ) {
      case -1:
        // Log.v("Cave3D", "SAP comm: FAIL no R-desc CCCD ");
        break;
      case -2:
        // Log.v("Cave3D", "SAP comm: FAIL no indicate/notify R-property ");
        break;
      case -3:
        // Log.v("Cave3D", "SAP comm: ERROR writing readDesc");
        break;
      default:
    }
  }

  // not used
  public boolean enablePNotify( UUID srcUuid, UUID chrtUuid )
  {
    Log.v("Cave3D", "SAP comm: enable P notify");
    return true;
  }

  // public boolean enablePIndicate( UUID srcUuid, UUID chrtUuid )
  // {
  //   Log.v("Cave3D", "SAP comm: enable P indicate");
  //   return true;
  // }

  public void notifyStatus( int status )
  {
    mApp.notifyStatus( status );
  }

  public void connectGatt( Context ctx, BluetoothDevice device )
  {
    // Log.v("Cave3D", "SAP connect Gatt" );
    closeChrt();
    mCallback.connectGatt( ctx, device );
  }

  public void disconnectGatt()
  {
    // Log.v("Cave3D", "SAP comm: disconnect Gatt" );
    closeChrt();
    mCallback.disconnectGatt();
    notifyStatus( ConnectionState.CONN_WAITING );
    mDisconnecting = false;
  }

}
