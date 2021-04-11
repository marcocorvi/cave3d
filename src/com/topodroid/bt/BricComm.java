/* @file BricComm.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief BRIC4 communication 
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import com.topodroid.Cave3D.TopoGL;
import com.topodroid.Cave3D.BluetoothComm;
import com.topodroid.Cave3D.BluetoothCommand;

import android.os.Looper;
import android.os.Handler;
import android.content.Context;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattCallback;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.Timer;
import java.util.TimerTask;

public class BricComm extends TopoGLComm
                      implements BleComm, BluetoothComm
{
  private ConcurrentLinkedQueue< BleOperation > mOps;
  // private int mPendingCommands; // FIXME COMPOSITE_COMMANDS

  private BleCallback mCallback;
  private boolean mReconnect = false;
  private BluetoothDevice mRemoteBtDevice = null;

  private long onData = 0;

  public BricComm( Context ctx, TopoGL app, BluetoothDevice bt_device ) 
  {
    super( ctx, app, TopoGLComm.COMM_GATT, bt_device.getAddress() );
    mRemoteBtDevice = bt_device;
    setProto( new BricProto( mApp, DeviceType.DEVICE_BRIC4, bt_device ) );
  }

  // public boolean setMemory( byte[] bytes )
  // {
  //   if ( bytes == null ) { // CLEAR
  //     Log.v("Cave3D", "BRIC clear memory");
  //     return sendCommand( BluetoothCommand.CMD_CLEAR );
  //   } else { // LAST TIME
  //     Log.v("Cave3D", "BRIC reset memory ... ");
  //     enqueueOp( new BleOpChrtWrite( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.LAST_TIME_UUID, bytes ) );
  //     clearPending();
  //     return true;
  //   }
  //   // return false;
  // }

  // not used
  public boolean enablePNotify( UUID srvUuid, UUID chrtUuid ) { return (mCallback != null) && mCallback.enablePNotify( srvUuid, chrtUuid ); }
  // public boolean enablePIndicate( UUID srvUuid, UUID chrtUuid ) { return (mCallback != null ) && mCallback.enablePIndicate( srvUuid, chrtUuid ); }
  
  // ---------------------------------------------------------------------------
  // callback action completions - these methods must clear the pending action by calling
  // clearPending() which starts a new action if there is one waiting

  // from onServicesDiscovered
  public int servicesDiscovered( BluetoothGatt gatt )
  {
    // Log.v( "Cave3D", "BRIC comm service discovered");
    /*
    // (new Handler( Looper.getMainLooper() )).post( new Runnable() {
    //   public void run() {
        List< BluetoothGattService > services = gatt.getServices();
        for ( BluetoothGattService service : services ) {
          // addService() does not do anything
          // addService( service );
          UUID srv_uuid = service.getUuid();
          // Log.v("Cave3D", "BRIC comm Srv  " + srv_uuid.toString() );
          List< BluetoothGattCharacteristic> chrts = service.getCharacteristics();
          for ( BluetoothGattCharacteristic chrt : chrts ) {
            addChrt( srv_uuid, chrt );

            // addDesc() does not do anything
            // UUID chrt_uuid = chrt.getUuid();
            // // Log.v("Cave3D", "BRIC comm Chrt " + chrt_uuid.toString() + BleUtils.chrtPermString(chrt) + BleUtils.chrtPropString(chrt) );
            // List< BluetoothGattDescriptor> descs = chrt.getDescriptors();
            // for ( BluetoothGattDescriptor desc : descs ) {
            //   addDesc( srv_uuid, chrt_uuid, desc );
            //   // Log.v("Cave3D", "BRIC comm Desc " + desc.getUuid().toString() + BleUtils.descPermString( desc ) );
            // }
          }
        }
    //   }
    // } );
    */

    // THIS IS THE BEST 
    enqueueOp( new BleOpNotify( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.MEAS_META_UUID, true ) );
    enqueueOp( new BleOpNotify( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.MEAS_ERR_UUID, true ) );
    enqueueOp( new BleOpNotify( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.MEAS_PRIM_UUID, true ) );
    doNextOp();
    // clearPending();

    mBTConnected = true;
    // Log.v("Cave3D", "BRIC comm discovered services status CONNECTED" );
    notifyStatus( ConnectionState.CONN_CONNECTED ); 

    return 0;
  }

  // from onDescriptorWrite
  public void writtenDesc( String uuid_str, String uuid_chrt_str, byte[] bytes ) { clearPending(); }


  // this is run by BleOpChrtWrite
  public boolean writeChrt( UUID srvUuid, UUID chrtUuid, byte[] bytes )
  { 
    Log.v( "Cave3D", "BRIC comm: write chsr " + chrtUuid.toString() );
    return mCallback.writeChrt( srvUuid, chrtUuid, bytes ); 
  }

  // from onCharacteristicChanged - this is called when the BRIC4 signals
  // MEAS_META, MEAS_ERR, and LAST_TIME are change-notified 
  public void changedChrt( BluetoothGattCharacteristic chrt )
  {
    int ret;
    String chrt_uuid = chrt.getUuid().toString();
    // delay closing one second after a characteristic change
    if ( chrt_uuid.equals( BricConst.MEAS_PRIM ) ) {
      onData = System.currentTimeMillis() + 1000;
      // Log.v("Cave3D", "BRIC comm changed char PRIM" );
      mQueue.put( new DataBuffer( DataBuffer.DATA_PRIM, DeviceType.DEVICE_BRIC4, chrt.getValue() ) );
    } else if ( chrt_uuid.equals( BricConst.MEAS_META ) ) { 
      // Log.v("Cave3D", "BRIC comm changed char META" ); 
      mQueue.put( new DataBuffer( DataBuffer.DATA_META, DeviceType.DEVICE_BRIC4, chrt.getValue() ) );
    } else if ( chrt_uuid.equals( BricConst.MEAS_ERR  ) ) {
      // Log.v("Cave3D", "BRIC comm changed char ERR" ); 
      mQueue.put( new DataBuffer( DataBuffer.DATA_ERR, DeviceType.DEVICE_BRIC4, chrt.getValue() ) );
    }
    // this is not necessary
    // clearPending();
    doNextOp();
  }

  // general error condition
  // the action may depend on the error status TODO
  public void error( int status /*, String extra */ )
  {
    switch ( status ) {
      case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH: 
        // TDLog.Error("BRIC COMM: invalid attr length " + extra );
        break;
      case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
        // TDLog.Error("BRIC COMM: write not permitted " + extra );
        break;
      case BluetoothGatt.GATT_READ_NOT_PERMITTED:
        // TDLog.Error("BRIC COMM: read not permitted " + extra );
        break;
      case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
        // TDLog.Error("BRIC COMM: insufficient encrypt " + extra );
        break;
      case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
        // TDLog.Error("BRIC COMM: insufficient auth " + extra );
        break;
      case BleCallback.CONNECTION_TIMEOUT:
      case BleCallback.CONNECTION_133: // unfortunately this happens
        // Log.v("Cave3D", "BRIC comm: connection timeout or 133");
        notifyStatus( ConnectionState.CONN_WAITING );
        reconnectDevice();
        break;
      default:
        // TDLog.Error("BRIC comm ***** ERROR " + status + ": reconnecting ...");
        reconnectDevice();
    }
    clearPending();
  }

  public void failure( int status /* , String extra */ )
  {
    // notifyStatus( ConnectionState.CONN_DISCONNECTED ); // this will be called by disconnected
    clearPending();
    // Log.v("Cave3D", "BRIC comm Failure: disconnecting ...");
    closeDevice();
  }
    
  // ----------------- CONNECT -------------------------------

  // @Implements
  public boolean connectDevice( )
  {
    // Log.v( "Cave3D", "BRIC comm ***** connect Device");
    return connectBricDevice( );
  }

  private boolean connectBricDevice() // FIXME BLEX_DATA_TYPE
  {
    if ( mRemoteBtDevice == null ) {
      // TDToast.makeBad( R.string.ble_no_remote );
      // TDLog.Error("BRIC comm ERROR null remote device");
      // Log.v( "Cave3D", "BRIC comm ***** connect Device: null = [3b] status DISCONNECTED" );
      notifyStatus( ConnectionState.CONN_DISCONNECTED );
      return false;
    } 
    notifyStatus( ConnectionState.CONN_WAITING );
    mReconnect   = true;
    mOps         = new ConcurrentLinkedQueue< BleOperation >();
    mCallback    = new BleCallback( this, false ); // auto_connect false

    // mPendingCommands = 0; // FIXME COMPOSITE_COMMANDS

    int ret = enqueueOp( new BleOpConnect( mContext, this, mRemoteBtDevice ) ); // exec connectGatt()
    // Log.v("Cave3D", "BRIC comm connects ... " + ret);
    clearPending();
    // doNextOp();
    return true;
  }

  public void connectGatt( Context ctx, BluetoothDevice bt_device ) // called from BleOpConnect
  {
    // Log.v( "Cave3D", "BRIC comm ***** connect GATT");
    mContext = ctx;
    mCallback.connectGatt( mContext, bt_device );
    // setupNotifications(); // FIXME_BRIC
  }

  // try to recover from an error ... 
  private void reconnectDevice()
  {
    mOps.clear();
    // mPendingCommands = 0; // FIXME COMPOSITE_COMMANDS
    clearPending();
    mCallback.closeGatt();
    if ( mReconnect ) {
      // Log.v("Cave3D", "BRIC comm ***** reconnect Device = [4a] status WAITING" );
      notifyStatus( ConnectionState.CONN_WAITING );
      int ret = enqueueOp( new BleOpConnect( mContext, this, mRemoteBtDevice ) ); // exec connectGatt()
      doNextOp();
      mBTConnected = true;
    } else {
      // Log.v("Cave3D", "BRIC comm ***** reconnect Device = [4b] status DISCONNECTED" );
      notifyStatus( ConnectionState.CONN_DISCONNECTED );
    }
  }


  // ----------------- DISCONNECT -------------------------------

  // @Implements
  public boolean disconnectDevice()
  {
    resetTimer();
    // Log.v( "Cave3D", "BRIC comm ***** disconnect device = connected:" + mBTConnected );
    return closeDevice();
/*
    mReconnect = false;
    if ( mBTConnected ) {
      mBTConnected = false;
      notifyStatus( ConnectionState.CONN_DISCONNECTED );
      mCallback.closeGatt();
    }
*/
  }

  // from onConnectionStateChange STATE_DISCONNECTED
  public void disconnected()
  {
    // Log.v( "Cave3D", "BRIC comm ***** disconnected" );
    clearPending();
    mOps.clear(); 
    // mPendingCommands = 0; // FIXME COMPOSITE_COMMANDS
    mBTConnected = false;
    notifyStatus( ConnectionState.CONN_DISCONNECTED );
  }

  public void connected() { clearPending(); }

  public void disconnectGatt()  // called from BleOpDisconnect
  {
    // Log.v( "Cave3D", "BRIC comm ***** disconnect GATT" );
    notifyStatus( ConnectionState.CONN_DISCONNECTED );
    mCallback.closeGatt();
  }

  // this is called only on a GATT failure, or the user disconnects 
  private boolean closeDevice()
  {
    mReconnect = false;
    if ( System.currentTimeMillis() < onData ) {
      if ( mBTConnected ) notifyStatus( ConnectionState.CONN_WAITING );
      scheduleDisconnect( 1000 );
      return false;
    }
    if ( mBTConnected ) {
      mBTConnected = false;
      notifyStatus( ConnectionState.CONN_DISCONNECTED ); // not necessary
      // Log.v( "Cave3D", "BRIC comm ***** close device");
      int ret = enqueueOp( new BleOpDisconnect( mContext, this ) ); // exec disconnectGatt
      doNextOp();
      // Log.v("Cave3D", "BRIC comm: disconnect ... ops " + ret );
    }
    return true;
  }

  // ----------------- SEND COMMAND -------------------------------
  public boolean sendCommand( int cmd )
  {
    if ( ! isConnected() ) return false;
    byte[] command = null;
    switch ( cmd ) {
      case BluetoothCommand.CMD_SCAN:  command = Arrays.copyOfRange( BricConst.COMMAND_SCAN,  0,  4 ); break;
      case BluetoothCommand.CMD_SHOT:  command = Arrays.copyOfRange( BricConst.COMMAND_SHOT,  0,  4 ); break;
      case BluetoothCommand.CMD_LASER: command = Arrays.copyOfRange( BricConst.COMMAND_LASER, 0,  5 ); break;
      case BluetoothCommand.CMD_CLEAR: command = Arrays.copyOfRange( BricConst.COMMAND_CLEAR, 0, 12 ); break;
      case BluetoothCommand.CMD_OFF:   command = Arrays.copyOfRange( BricConst.COMMAND_OFF,   0,  9 ); break;
/*
      case BluetoothCommand.CMD_SPLAY: 
        Log.v("Cave3D", "BRIC comm send cmd SPLAY");
        mPendingCommands += 1;
        break;
      case BluetoothCommand.CMD_LEG: 
        Log.v("Cave3D", "BRIC comm send cmd LEG");
        mPendingCommands += 3;
        break;
*/
    }
    if ( command != null ) {
      // Log.v("Cave3D", "BRIC comm send cmd " + cmd );
      enqueueOp( new BleOpChrtWrite( mContext, this, BricConst.CTRL_SRV_UUID, BricConst.CTRL_CHRT_UUID, command ) );
      doNextOp();
    // } else { // FIXME COMPOSITE_COMMANDS
    //   if ( mPendingOp == null ) doPendingCommand();
    }
    return true;
  }
/*
  private void enqueueShot( final BleComm comm )
  {
    (new Thread() {
      public void run() {
        Log.v("Cave3D", "BRIC comm: enqueue LASER cmd");
        byte[] cmd1 = Arrays.copyOfRange( BricConst.COMMAND_LASER, 0, 5 );
        enqueueOp( new BleOpChrtWrite( mContext, comm, BricConst.CTRL_SRV_UUID, BricConst.CTRL_CHRT_UUID, cmd1 ) );
        doNextOp();
        DeviceType.slowDown( 600 );
        Log.v("Cave3D", "BRIC comm: enqueue SHOT cmd");
        byte[] cmd2 = Arrays.copyOfRange( BricConst.COMMAND_SHOT, 0, 4 );
        enqueueOp( new BleOpChrtWrite( mContext, comm, BricConst.CTRL_SRV_UUID, BricConst.CTRL_CHRT_UUID, cmd2 ) );
        doNextOp();
        DeviceType.slowDown( 800 );
      }
    } ).start();
  }

  private boolean sendLastTime( )
  {
    byte[] last_time = mProto.getLastTime();
    // Log.v("Cave3D", "BRIC comm send last time: " + BleUtils.bytesToString( last_time ) );
    if ( last_time == null ) return false;
    enqueueOp( new BleOpChrtWrite( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.LAST_TIME_UUID, last_time ) );
    doNextOp();
    return true;
  } 
*/

  // --------------------------------------------------------------------------
  private BleOperation mPendingOp = null;

  private void clearPending() 
  { 
    mPendingOp = null; 
    // if ( ! mOps.isEmpty() || mPendingCommands > 0 ) doNextOp();
    if ( ! mOps.isEmpty() ) doNextOp();
  }

  // @return the length of the ops queue
  private int enqueueOp( BleOperation op ) 
  {
    mOps.add( op );
    // printOps(); // DEBUG
    return mOps.size();
  }

  // access by BricChrtChanged
  private void doNextOp() 
  {
    if ( mPendingOp != null ) {
      // Log.v("Cave3D", "BRIC comm: next op with pending not null, ops " + mOps.size() ); 
      return;
    }
    mPendingOp = mOps.poll();
    // Log.v("Cave3D", "BRIC comm: polled, ops " + mOps.size() );
    if ( mPendingOp != null ) {
      mPendingOp.execute();
    } 
    // else if ( mPendingCommands > 0 ) {
    //   enqueueShot( this );
    //   -- mPendingCommands;
    // }
  }

/* FIXME COMPOSITE_COMMANDS
  private void doPendingCommand()
  {
    if ( mPendingCommands > 0 ) {
      enqueueShot( this );
      -- mPendingCommands;
    }
  }
*/

  /* DEBUG
  private void printOps()
  {
    StringBuilder sb = new StringBuilder();
    sb.append( "BRIC comm Ops: ");
    for ( BleOperation op : mOps ) sb.append( op.name() ).append(" ");
    Log.v("Cave3D", sb.toString() );
  }
  */
    

}
