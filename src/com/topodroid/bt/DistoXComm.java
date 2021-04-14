/* @file DistoXComm.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief DistoX2 communication 
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

import java.io.IOException;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.nio.channels.ClosedByInterruptException;
// import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;


import java.util.Timer;
import java.util.TimerTask;

public class DistoXComm extends TopoGLComm
                        implements BluetoothComm
{
  // private int mPendingCommands; // FIXME COMPOSITE_COMMANDS


  private Timer mDisconnectTimer = null;
  private Timer mConnectTimer = null;
  private long onData = 0;

  private BtConnection mBtConnection;

  // protected byte[] mHeadTailA3;  // head/tail for Protocol A3
  protected byte[] mAddr8000;     // could be used by DistoXA3Protocol.read8000 
  private byte[]   mAcknowledge;
  private byte     mSeqBit;         // sequence bit: 0x00 or 0x80
  protected byte[] mBuffer;

  // protected Socket  mSocket = null;
  protected DataInputStream  mIn;
  protected DataOutputStream mOut;

  // final protected byte[] mBuffer = new byte[8]; // packet buffer
  // int mMaxTimeout = 8;
  
  // address is bt_device.getAddress()
  public DistoXComm( Context ctx, TopoGL app, BluetoothDevice bt_device, String address ) 
  {
    super( ctx, app, TopoGLComm.COMM_RFCOMM, address );
    mBtConnection = new BtConnection( app, this );
    setProto( new DistoXProto( mApp, DeviceType.DEVICE_DISTOX, bt_device ) );

    // allocate device-specific buffers
    mAddr8000 = new byte[3];
    mAddr8000[0] = 0x38;
    mAddr8000[1] = 0x00; // address 0x8000 - already assigned but repeat for completeness
    mAddr8000[2] = (byte)0x80;
    mAcknowledge = new byte[1];
    mBuffer = new byte[8];
    // mAcknowledge[0] = ( b & 0x80 ) | 0x55;
    // mHeadTailA3 = new byte[3];   // to read head/tail for Protocol A3
    // mHeadTailA3[0] = 0x38;
    // mHeadTailA3[1] = 0x20;       // address 0xC020
    // mHeadTailA3[2] = (byte)0xC0;
  }

  // ---------------------------------------------------------------
  // general error condition
  // the action may depend on the error status TODO
  public void error( int status )
  {
    switch ( status ) {
      default:
        // TDLog.Error("DistoX comm ***** ERROR " + status + ": reconnecting ...");
        reconnectDevice();
    }
  }

  public void failure( int status )
  {
    // Log.v("Cave3D", "DistoX comm Failure: disconnecting ...");
    disconnectDevice();
  }
    
  // ----------------- CONNECT -------------------------------
  private CommThread mCommThread = null;

  public boolean isCommThreadNull( ) { return ( mCommThread == null ); }

  void doneCommThread() { mCommThread = null; }

  // close the socket and terminates the thread
  void closeCommThread() 
  { 
    mBtConnection.closeSocket(); // if socket is already closed this returns immediately

    if ( mCommThread != null ) {
      mCommThread.setDone();
      try {
        mCommThread.join();
      } catch ( InterruptedException e ) { }
      finally { mCommThread = null; }
    }
  }

  // create BT connection socket and download Thread
  // @param address   remote device address
  boolean startCommThread( String address )
  {
    closeCommThread(); // safety;
    DeviceType.slowDown( 500 );
    if ( mBtConnection.connectSocket( address ) ) {
      if ( mCommThread == null ) {
        mCommThread = new CommThread( TopoGLComm.COMM_RFCOMM, this );
        mCommThread.start();
        // Log.v( "Cave3D", "startRFcommThread started");
      } else {
        Log.e( "Cave3D", "start Comm Thread already running");
      }
      return true;
    } else {
      mCommThread = null;
      Log.e( "Cave3D", "startRFcommThread: null socket");
      return false;
    }
  }

  // long mLastShotId;   // last shot id

  // @Implements
  public boolean connectDevice( )
  {
    Log.v("Cave3D", "DistoXComm connect device - address " + mAddress );
    resetTimer();
    if ( mBTConnected ) return true; // already connected
    return startCommThread( mAddress ); // create BT connection socket and download Thread
  }

  // @Implements
  public boolean disconnectDevice()
  {
    Log.v("Cave3D", "DistoXComm disconnect device - connected " + mBTConnected );
    if ( mBTConnected ) {
      closeCommThread(); // this closes socket
      mBTConnected = false;
    }
    return ! mBTConnected;
  }

  private void reconnectDevice( )
  {
    disconnectDevice();
    if ( mAddress != null ) {
      // DeviceType.slowDown( 500 );
      scheduleReconnect( 500, 1000, mAddress );
    }
  }


  // IO / STREAMS -----------------------------------------------------

  void setIOstreams( DataInputStream in, DataOutputStream out )
  {
    // mSocket = socket;
    mSeqBit = (byte)0xff;
    mIn  = in;
    mOut = out;
  }

  // @Override
  public void closeIOstreams()
  {
    if ( mIn != null ) {
      try { mIn.close(); } catch ( IOException e ) { }
      mIn = null;
    }
    if ( mOut != null ) {
      try { mOut.close(); } catch ( IOException e ) { }
      mOut = null;
    }
  }


  // PACKETS I/O ------------------------------------------------------------------------

  // TODO use DistoXProto getDataBuffer()
  int handlePacket( byte[] buffer ) 
  {
    int data_type = DataBuffer.DATA_NONE;
    switch ( buffer[0] & 0x3f ) {
      case 0x01: data_type = DataBuffer.DATA_PACKET; break;
      case 0x02: data_type = DataBuffer.DATA_G; break;
      case 0x03: data_type = DataBuffer.DATA_M; break;
      case 0x04: data_type = DataBuffer.DATA_VECTOR; break;
      case 0x38: data_type = DataBuffer.DATA_REPLY; break;
    }
    if ( data_type != DataBuffer.DATA_NONE ) {
      mQueue.put( new DataBuffer( data_type, DeviceType.DEVICE_DISTOX, Arrays.copyOf( buffer, buffer.length ) ) );
    }
    return data_type;
  }

  /** try to read 8 bytes - return the number of read bytes
   * @param timeout    joining timeout
   * @param maxtimeout max number of join attempts
   * @param data_type  expected data type (shot or calib)
   * @return number of data to read
   *
  private int getAvailable( long timeout, int maxtimeout ) throws IOException
  {
    mMaxTimeout = maxtimeout;
    final int[] dataRead = { 0 };
    final int[] toRead   = { 8 }; // 8 bytes to read
    final int[] count    = { 0 };
    final IOException[] maybeException = { null };
    final Thread reader = new Thread() {
      public void run() {
        // TDLog.Log( TDLog.LOG_PROTO, "reader thread run " + dataRead[0] + "/" + toRead[0] );
        try {
          // synchronized( dataRead ) 
          {
            count[0] = mIn.read( mBuffer, dataRead[0], toRead[0] );

            toRead[0]   -= count[0];
            dataRead[0] += count[0];
          }
        } catch ( ClosedByInterruptException e ) {
          TDLog.Error( "reader closed by interrupt");
        } catch ( IOException e ) {
          maybeException[0] = e;
        }
        // TDLog.Log( TDLog.LOG_PROTO, "reader thread done " + dataRead[0] + "/" + toRead[0] );
      }
    };
    reader.start();

    for ( int k=0; k<mMaxTimeout; ++k) {
      // Log.v("DistoX", "interrupt loop " + k + " " + dataRead[0] + "/" + toRead[0] );
      try {
        reader.join( timeout );
      } catch ( InterruptedException e ) { TDLog.Log(TDLog.LOG_DEBUG, "reader join-1 interrupted"); }
      if ( ! reader.isAlive() ) break;
      {
        Thread interruptor = new Thread() { public void run() {
          // Log.v("DistoX", "interruptor run " + dataRead[0] );
          for ( ; ; ) {
            // synchronized ( dataRead ) 
            {
              if ( dataRead[0] > 0 && toRead[0] > 0 ) { // FIXME
                try { wait( 100 ); } catch ( InterruptedException e ) { }
              } else {
                if ( reader.isAlive() ) reader.interrupt(); 
                break;
              }
            }
          }
          // Log.v("DistoX", "interruptor done " + dataRead[0] );
        } };
        interruptor.start(); // TODO catch ( OutOfmemoryError e ) { }

        try {
          interruptor.join( 200 );
        } catch ( InterruptedException e ) { TDLog.Log(TDLog.LOG_DEBUG, "interruptor join interrupted"); }
      }
      try {
        reader.join( 200 );
      } catch ( InterruptedException e ) { TDLog.Log(TDLog.LOG_DEBUG, "reader join-2 interrupted"); }
      if ( ! reader.isAlive() ) break; 
    }
    if ( maybeException[0] != null ) throw maybeException[0];
    return dataRead[0];
  }
  */

  /**
   * @param no_timeout  whether not to timeout
   * @return packet type (if successful)
   */
  // @Override
  public int readPacket( boolean no_timeout )
  {
    // int min_available = ( mDeviceType == Device.DISTO_X000)? 8 : 1; // FIXME 8 should work in every case // FIXME VirtualDistoX
    int min_available = 1; // FIXME 8 should work in every case

    // Log.v( "Cave3D", "DistoX proto: read packet no-timeout " + (no_timeout?"true":"false") );
    try {
      final int maxtimeout = 8;
      int timeout = 0;
      int available = 0;

      if ( no_timeout ) {
        available = 8;
      } else { // do timeout
        // if ( TDSetting.mZ6Workaround ) {
        //   available = getAvailable( 200, 2*maxtimeout );
        // } else {
          // while ( ( available = mIn.available() ) == 0 && timeout < maxtimeout ) 
          while ( ( available = mIn.available() ) < min_available && timeout < maxtimeout ) {
            ++ timeout;
            // Log.v( "Cave3D", "Proto read packet sleep " + timeout + "/" + maxtimeout );
            DeviceType.slowDown( 100 ); // , "Proto read packet InterruptedException" );
          }
        // }
      }
      // Log.v( "Cave3D", "DistoX proto: read packet available " + available );
      // if ( available > 0 ) 
      if ( available >= min_available ) {
        if ( no_timeout /* || ! TDSetting.mZ6Workaround */ ) {
          mIn.readFully( mBuffer, 0, 8 );
        }

        // DistoX packets have a sequence bit that flips between 0 and 1
        byte seq  = (byte)(mBuffer[0] & 0x80); 
        // Log.v( "Cave3D", "VD read packet seq bit " + String.format("%02x %02x %02x", mBuffer[0], seq, mSeqBit ) );
        boolean ok = ( seq != mSeqBit );
        mSeqBit = seq;
        // if ( (mBuffer[0] & 0x0f) != 0 ) // ack every packet
        { 
          mAcknowledge[0] = (byte)(( mBuffer[0] & 0x80 ) | 0x55);
          mOut.write( mAcknowledge, 0, 1 );
          // Log.v( "Cave3D", "read packet byte " + String.format(" %02x", mBuffer[0] ) + " ... writing ack" );
        }
        if ( ok ) return handlePacket( mBuffer );
      } // else timedout with no packet
    } catch ( EOFException e ) {
      Log.e( "Cave3D", "Proto read packet EOFException" + e.toString() );
    } catch (ClosedByInterruptException e ) {
      Log.e( "Cave3D", "Proto read packet ClosedByInterruptException" + e.toString() );
    } catch (IOException e ) {
      // this is OK: the DistoX has been turned off
      Log.v( "Cave3D", "Proto read packet IOException " + e.toString() + " OK distox turned off" );
      // mError = DistoXConst.DISTOX_ERR_OFF;
      return DistoXConst.DISTOX_ERR_OFF;
    }
    return DataBuffer.DATA_NONE;
  }

  /** write a command to the out channel
   * @param cmd command code
   * @return true if success
   */
  // @Override
  public boolean sendCommand( byte cmd )
  {
    // TDLog.Log( TDLog.LOG_PROTO, String.format("send command %02x", cmd ) );
    // Log.v( "DistoX", String.format("send command %02x", cmd ) );
    byte[] buffer = new byte[8];  // request buffer

    try {
      buffer[0] = (byte)(cmd);
      mOut.write( buffer, 0, 1 );
      mOut.flush();
      // if ( TDSetting.mPacketLog ) logPacket1( 1L, buffer );
    } catch (IOException e ) {
      // TDLog.Error( "send command failed" );
      return false;
    }
    return true;
  }

}

