/* @file BtConnection.java
 *
 * @author marco corvi
 * @date apr 2021 (from TopoDroid)
 *
 * @brief Socket bluetooth communication 
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

// import com.topodroid.DistoX.DataDownloader;

import com.topodroid.Cave3D.TopoGL;


import android.util.Log;

// import java.nio.ByteBuffer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
// import java.io.EOFException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
// import java.util.List;
// import java.util.ArrayList;
// import java.util.concurrent.atomic.AtomicInteger;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

// import android.os.Bundle;
import android.os.Handler;
// import android.os.Message;

// import android.os.Parcelable;
import android.os.ParcelUuid;
// import android.os.AsyncTask;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;

// import android.database.DataSetObserver;

public class BtConnection 
{
  static final int TD_SOCK_DEFAULT    = 1;
  static final int TD_SOCK_INSEC      = 2;
  static final int TD_SOCK_INSEC_PORT = 3;
  static final int TD_SOCK_PORT       = 4;
  int mSockType = TD_SOCK_INSEC;

  TopoGL mApp;
  String mAddress = null;
  DistoXComm mComm;
  protected BluetoothDevice mBTDevice;
  protected BluetoothSocket mBTSocket;

  // --------------------------------------------------

  public BtConnection( TopoGL app, DistoXComm comm )
  {
    mApp      = app;
    mComm     = comm;
    mAddress  = null; // BtConnection is create disconnected - the device and address are set when it connects
    mBTDevice = null;
    mBTSocket = null;
    // Log.v( "Cave3D", "BT Comm cstr");
  }

  // public void resume()
  // {
  //   // if ( mCommThread != null ) { mCommThread.resume(); }
  // }

  // public void suspend()
  // {
  //   // if ( mCommThread != null ) { mCommThread.suspend(); }
  // }

  // Bluetooth receiver -----------------------------------------------------

  private BroadcastReceiver mBTReceiver = null;

  private void resetBTReceiver()
  {
    if ( mBTReceiver == null ) return;
    Log.v( "Cave3D", "reset BT receiver");
    try {
      mApp.unregisterReceiver( mBTReceiver );
    } catch ( IllegalArgumentException e ) {
      Log.e( "Cave3D", "unregister BT receiver error " + e.getMessage() );
    }
    mBTReceiver = null;
  }

  // called only by connectSocket
  private void setupBTReceiver( )
  {
    resetBTReceiver();
    Log.v( "Cave3D", "setup BT receiver");
    mBTReceiver = new BroadcastReceiver() 
    {
      @Override
      public void onReceive( Context ctx, Intent data )
      {
        String action = data.getAction();
        BluetoothDevice bt_device = data.getParcelableExtra( DeviceUtil.EXTRA_DEVICE );
        String address = ( bt_device != null )? bt_device.getAddress() : "undefined";
 
        // if ( DeviceUtil.ACTION_DISCOVERY_STARTED.equals( action ) ) {
        // } else if ( DeviceUtil.ACTION_DISCOVERY_FINISHED.equals( action ) ) {
        // } else if ( DeviceUtil.ACTION_FOUND.equals( action ) ) {
       
        if ( address.equals(mAddress) ) {
          Log.v("Cave3D", "on receive");
          if ( DeviceUtil.ACTION_ACL_CONNECTED.equals( action ) ) {
            Log.v( "Cave3D", "[C] ACL_CONNECTED " + address + " addr " + mAddress );
            notifyStatus( ConnectionState.CONN_CONNECTED );
            // mDataDownloader.updateConnected( true );
          } else if ( DeviceUtil.ACTION_ACL_DISCONNECT_REQUESTED.equals( action ) ) {
            Log.v( "Cave3D", "[C] ACL_DISCONNECT_REQUESTED " + address + " addr " + mAddress );
            notifyStatus( ConnectionState.CONN_DISCONNECTED );
            // FIXME TODO mDataDownloader.updateConnected( false );
            closeSocket( );
          } else if ( DeviceUtil.ACTION_ACL_DISCONNECTED.equals( action ) ) {
            Log.v( "Cave3D", "[C] ACL_DISCONNECTED " + address + " addr " + mAddress );
            notifyStatus( ConnectionState.CONN_WAITING );
            mComm.reconnectDevice();
            // FIXME TODO mDataDownloader.updateConnected( false );
            // closeSocket( );
            // FIXME TODO mApp.notifyDisconnected( ); // run reconnect-task
          }
        } else if ( DeviceUtil.ACTION_BOND_STATE_CHANGED.equals( action ) ) { // NOT USED
          Log.v("Cave3D", "***** Disto comm: on Receive() BOND STATE CHANGED" );
          final int state     = data.getIntExtra(DeviceUtil.EXTRA_BOND_STATE, DeviceUtil.ERROR);
          final int prevState = data.getIntExtra(DeviceUtil.EXTRA_PREVIOUS_BOND_STATE, DeviceUtil.ERROR);
          if (state == DeviceUtil.BOND_BONDED && prevState == DeviceUtil.BOND_BONDING) {
            // Log.v( "Cave3D", "BOND STATE CHANGED paired (BONDING --> BONDED) " + address );
          } else if (state == DeviceUtil.BOND_NONE && prevState == DeviceUtil.BOND_BONDED){
            // Log.v( "Cave3D", "BOND STATE CHANGED unpaired (BONDED --> NONE) " + address );
          } else if (state == DeviceUtil.BOND_BONDING && prevState == DeviceUtil.BOND_BONDED) {
            // Log.v( "Cave3D", "BOND STATE CHANGED unpaired (BONDED --> BONDING) " + address );
            if ( mBTSocket != null ) {
              Log.e( "Cave3D", "[*] socket is not null: close and retry connect ");
              notifyStatus( ConnectionState.CONN_WAITING );
              // FIXME TODO mDataDownloader.setConnected( ConnectionState.CONN_WAITING );
              closeSocket( );
              // FIXME TODO mApp.notifyDisconnected( ); // run reconnect-task
              connectSocket( mAddress ); // returns immediately if mAddress == null
            }
          } else {
            Log.v( "Cave3D", "BOND STATE CHANGED " + prevState + " --> " + state + " " + address );
          }

          // DeviceUtil.bind2Device( data );
        // } else if ( DeviceUtil.ACTION_PAIRING_REQUEST.equals(action) ) {
        //   Log.v("Cave3D", "PAIRING REQUEST");
        //   // BluetoothDevice device = getDevice();
        //   // //To avoid the popup notification:
        //   // device.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(device, true);
        //   // device.getClass().getMethod("cancelPairingUserInput", boolean.class).invoke(device, true);
        //   // byte[] pin = ByteBuffer.allocate(4).putInt(0000).array();
        //   // //Entering pin programmatically:  
        //   // Method ms = device.getClass().getMethod("setPin", byte[].class);
        //   // //Method ms = device.getClass().getMethod("setPasskey", int.class);
        //   // ms.invoke(device, pin);
        //     
        //   //Bonding the device:
        //   // Method mm = device.getClass().getMethod("createBond", (Class[]) null);
        //   // mm.invoke(device, (Object[]) null);
        }
      }
    };


    // mApp.registerReceiver( mBTReceiver, new IntentFilter( DeviceUtil.ACTION_FOUND ) );
    // mApp.registerReceiver( mBTReceiver, new IntentFilter( DeviceUtil.ACTION_DISCOVERY_STARTED ) );
    // mApp.registerReceiver( mBTReceiver, new IntentFilter( DeviceUtil.ACTION_DISCOVERY_FINISHED ) );
    mApp.registerReceiver( mBTReceiver, new IntentFilter( DeviceUtil.ACTION_ACL_CONNECTED ) );
    mApp.registerReceiver( mBTReceiver, new IntentFilter( DeviceUtil.ACTION_ACL_DISCONNECT_REQUESTED ) );
    mApp.registerReceiver( mBTReceiver, new IntentFilter( DeviceUtil.ACTION_ACL_DISCONNECTED ) );
    // mApp.registerReceiver( mBTReceiver, uuidFilter  = new IntentFilter( myUUIDaction ) );
    mApp.registerReceiver( mBTReceiver, new IntentFilter( DeviceUtil.ACTION_BOND_STATE_CHANGED ) );
    // mApp.registerReceiver( mBTReceiver, new IntentFilter( DeviceUtil.ACTION_PAIRING_REQUEST ) );
  }

  // SOCKET -------------------------------------------------------- 
  // API
  //     connectSocket( address )
  //     closeSocket()

  /** close the socket (and the RFcomm thread) but don't delete it
   * alwasy called with wait_thread
   */
  synchronized private void closeSocket( )
  {
    if ( mBTSocket == null ) {
      Log.v( "Cave3D", "close socket() already closed");
      return;
    }
    
    Log.v( "Cave3D", "close socket() address " + mAddress );
    for ( int k=0; k<3 && mBTSocket != null; ++k ) { 
      Log.e( "Cave3D", "try close socket nr " + k );
      DeviceType.slowDown(100);
      // mComm.closeCommThread();
      try {
        mBTSocket.close();
        mBTSocket = null;
      } catch ( IOException e ) {
        Log.v( "Cave3D", "close socket IOexception " + e.getMessage() );
      } finally {
        mComm.closeCommThread();
      }
    }
    Log.v( "Cave3D", "close socket() done");
    mComm.setConnected( false );
  }

  /** close the socket and delete it
   * the connection becomes unusable
   * As a matter of fact this is alwyas called with wait_thread = true
   */
  public void destroySocket( ) // boolean wait_thread )
  {
    if ( mBTSocket == null ) return;
    Log.v( "Cave3D", "destroy socket() address " + mAddress );
    closeSocket();
    // mBTSocket = null;
    resetBTReceiver();
  }

  /** create a socket (not connected)
   *  and a connection protocol on it
   */
  synchronized private void createSocket( String address )
  {
    if ( address == null ) return;
    final int port = 1;
    // if ( address.equals( mAddress ) ) { // already connected
    //   Log.v( "Cave3D", "create Socket() addr " + address + " mAddress " + mAddress + " skipping " );
    //   return;
    // }
    Log.v( "Cave3D", "create Socket() addr " + address + " creating ");

    if ( mBTSocket != null ) {
      Log.v( "Cave3D", "create Socket() BTSocket not null ... closing");
      try {
        mBTSocket.close();
      } catch ( IOException e ) { 
        Log.v( "Cave3D", "close Socket IO " + e.getMessage() );
      }
      mBTSocket = null;
    }

    try {
      mBTDevice = DeviceUtil.getRemoteDevice( address );
    } catch ( IllegalArgumentException e ) {
      Log.v( "Cave3D", "create Socket failed to get remode device - address " + address );
      mBTDevice = null;
      mAddress  = null;
      mComm.setConnected( false );
      return;
    }
    
    // FIXME PAIRING
    // Log.v( "Cave3D", "[1] device state " + mBTDevice.getBondState() );
    if ( ! DeviceUtil.isPaired( mBTDevice ) ) {
      int ret = DeviceUtil.pairDevice( mBTDevice );
      Log.v( "Cave3D", "[1b] pairing device " + ret );
    // }

      // Log.v( "Cave3D", "[2] device state " + mBTDevice.getBondState() );
    // // if ( mBTDevice.getBondState() == DeviceUtil.BOND_NONE ) 
    // if ( ! DeviceUtil.isPaired( mBTDevice ) ) 
    // {
    //   Log.v( "Cave3D", "bind device " );
      DeviceUtil.bindDevice( mBTDevice, "0000" );
    }

    // wait "delay" seconds
    if ( ! DeviceUtil.isPaired( mBTDevice ) ) {
      for ( int n=0; n < 5; ++n ) {
        Log.v( "Cave3D", "[4] pairing device: trial " + n );
        DeviceType.yieldDown( 100 );
        if ( DeviceUtil.isPaired( mBTDevice ) ) {
          // Log.v( "Cave3D", "[4a] device paired at time " + n );
          break;
        }
      }
    }

    if ( ! DeviceUtil.isPaired( mBTDevice ) ) {
      Log.v( "Cave3D", "create Socket failed to get paired device - address " + address );
      mBTDevice = null;
      mAddress  = null;
      mComm.setConnected( false );
      return;
    }

    try {
      // Log.v( "Cave3D", "create socket");
      Class[] classes1 = new Class[]{ int.class };
      Class[] classes2 = new Class[]{ UUID.class };
      if ( mSockType == TD_SOCK_DEFAULT ) {
        // Log.v( "Cave3D", "[5a] createRfcommSocketToServiceRecord " );
        mBTSocket = mBTDevice.createRfcommSocketToServiceRecord( TopoGLComm.SERVICE_UUID );
      } else if ( mSockType == TD_SOCK_INSEC ) {
        // Log.v( "Cave3D", "[5b] createInsecureRfcommSocketToServiceRecord " );
        Method m3 = mBTDevice.getClass().getMethod( "createInsecureRfcommSocketToServiceRecord", classes2 );
        mBTSocket = (BluetoothSocket) m3.invoke( mBTDevice, TopoGLComm.SERVICE_UUID );
      } else if ( mSockType == TD_SOCK_INSEC_PORT ) {
        // Log.v( "Cave3D", "[5c] invoke createInsecureRfcommSocket " );
        Method m1 = mBTDevice.getClass().getMethod( "createInsecureRfcommSocket", classes1 );
        mBTSocket = (BluetoothSocket) m1.invoke( mBTDevice, port );
        // mBTSocket = mBTDevice.createInsecureRfcommSocket( port );
        // mBTSocket = (BluetoothSocket) m1.invoke( mBTDevice, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") );
      } else if ( mSockType == TD_SOCK_PORT ) {
        // Log.v( "Cave3D", "[5d] invoke createRfcommSocket " );
        Method m2 = mBTDevice.getClass().getMethod( "createRfcommSocket", classes1 );
        mBTSocket = (BluetoothSocket) m2.invoke( mBTDevice, port );
      }
    } catch ( InvocationTargetException e ) {
      Log.e( "Cave3D", "create Socket invoke target " + e.getMessage() );
      if ( mBTSocket != null ) { mBTSocket = null; }
    } catch ( UnsupportedEncodingException e ) {
      Log.e( "Cave3D", "create Socket encoding " + e.getMessage() );
      if ( mBTSocket != null ) { mBTSocket = null; }
    } catch ( NoSuchMethodException e ) {
      Log.e( "Cave3D", "create Socket no method " + e.getMessage() );
      if ( mBTSocket != null ) { mBTSocket = null; }
    } catch ( IllegalAccessException e ) {
      Log.e( "Cave3D", "create Socket access " + e.getMessage() );
      if ( mBTSocket != null ) { mBTSocket = null; }
    } catch ( IOException e ) {
      Log.e( "Cave3D", "create Socket IO " + e.getMessage() );
      if ( mBTSocket != null ) { mBTSocket = null; }
    }

    if ( mBTSocket != null ) {
      Log.v( "Cave3D", "[6a] create Socket OK: create I/O streams");
      // mBTSocket.setSoTimeout( 200 ); // BlueToothSocket does not have timeout 
      try {
        DataInputStream in   = new DataInputStream( mBTSocket.getInputStream() );
        DataOutputStream out = new DataOutputStream( mBTSocket.getOutputStream() );
        mComm.setIOstreams( in, out );
        mAddress = address;
      } catch ( IOException e ) {
        Log.e( "Cave3D", "[6d] create Socket stream error " + e.getMessage() );
        mAddress = null;
        try {
          mBTSocket.close();
        } catch ( IOException ee ) { 
          Log.e( "Cave3D", "[6e] close Socket IO " + ee.getMessage() );
        }
        mBTSocket = null;
      }
    } else {
      Log.e( "Cave3D", "[7] create Socket failure");
      mAddress = null;
    }
    mComm.setConnected( false ); // socket is created but not connected
  }

  int connectTrials = 3;
  int mSocketState = -1;

  // 0 disconnected
  // 1 connecting
  // 2 connected
  void setSocketState( int state ) 
  { 
    Log.v( "Cave3D", "connect socket(): set socket state " + state );
    mSocketState = state;
  }

  boolean connectSocket( String address )
  {
    if ( address == null ) return false;
    Log.v( "Cave3D", "connect socket(): " + address );
    if ( mBTSocket == null ) {
      createSocket( address );
      // mSocketState = ( mBTSocket == null)? -1 : 0;
    }

    Log.v( "Cave3D", "connect socket(): created " + ((mBTSocket == null)? "null" : "non-null") );
    if ( mBTSocket != null ) { // mSocketState == 0
      DeviceUtil.cancelDiscovery();
      setupBTReceiver( );
      DeviceType.yieldDown( 1000 ); // wait 100 msec

      setSocketState( 1 ); // CONNECTING

      int trial  = 0;
      while ( mSocketState == 1 && ! mComm.isConnected() /* && trial != connectTrials */ ) {
        ++ trial;
        if ( mBTSocket != null ) {
          Log.v( "Cave3D", "connect socket() trial " + trial );
          try {
            Log.v( "Cave3D", "device state " + mBTDevice.getBondState() );
            mBTSocket.connect();
            mComm.setConnected( true );
          } catch ( IOException e ) {
            Log.e( "Cave3D", "connect socket() (trial " + trial + ") IO error " + e.getMessage() + ": close socket");
            // closeSocket();
            // DeviceType.slowDown( 1000 );
            // mBTSocket = null;
          }
        }
        if ( mBTSocket == null && mSocketState == 1 /* && trial != connectTrials */ ) { // retry: create the socket again
          Log.v( "Cave3D", "connect socket() create and retry, after 1 sec.");
          DeviceType.slowDown( 1000 );
          // createSocket( address );
        }
        Log.v( "Cave3D", "connect socket() trial " + trial + " connected " + mComm.isConnected() );
      }
      if ( mSocketState == 0 ) closeSocket();
    } else {
      Log.e( "Cave3D", "connect socket() null socket");
    }
    setSocketState( ( mBTSocket == null)? -1 : (! mComm.isConnected())? 0 : 2 );
    Log.v( "Cave3D", "connect socket() result " + mComm.isConnected() + " socket state " + mSocketState );
    return mComm.isConnected();
  }

  // -------------------------------------------------------------------------------------

  private void notifyStatus( int status )
  {
    Log.v("Cave3D", "BT connection notify status " + status );
    mComm.notifyStatus( status );
  }

}
