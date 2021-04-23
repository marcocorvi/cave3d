/* @file TopoGLComm.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid bluetooth communication 
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import com.topodroid.Cave3D.TopoGL;
import com.topodroid.Cave3D.BluetoothComm;

import android.util.Log;

import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
// import java.util.List;
// import java.util.concurrent.atomic.AtomicInteger; // FIXME_ATOMIC_INT

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.content.Context;
// import android.content.Intent;
// import android.content.IntentFilter;
// import android.content.BroadcastReceiver;

public abstract class TopoGLComm implements BluetoothComm
{
  public static final int COMM_NONE   = 0;
  public static final int COMM_RFCOMM = 1;
  public static final int COMM_GATT   = 2;

  public static final String SERVICE_STRING = "00001101-0000-1000-8000-00805F9B34FB";
  public static final UUID   SERVICE_UUID = UUID.fromString( SERVICE_STRING );

  protected Context mContext;
  protected TopoGL mApp;
  protected String mAddress;
  public int    mCommType = 0;

  protected DataQueue mQueue;
  protected TopoGLProto mProto = null;
  protected boolean mBTConnected;
  protected boolean mReconnect = false;

  // -----------------------------------------------------------

  public TopoGLComm( Context ctx, TopoGL app, int comm_type, String address )
  {
    mContext  = ctx;
    mApp      = app;
    mCommType = comm_type;
    mAddress  = address;
    mQueue = new DataQueue();
    mBTConnected  = false;
  }

  public void resume()
  {
    // if ( mCommThread != null ) { mCommThread.resume(); }
  }

  public void suspend()
  {
    // if ( mCommThread != null ) { mCommThread.suspend(); }
  }

  /*
  public boolean sendCommand( int cmd )
  {
    boolean ret = false;
    if ( mProtocol != null ) {
      for (int k=0; k<3 && ! ret; ++k ) { // try three times
        ret = mProtocol.sendCommand( (byte)cmd ); // was ret |= ...
        // Log.v( "Cave3D", "sendCommand " + cmd + " " + k + "-ret " + ret );
        DeviceType.slowDown( 500 );
      }
    }
    return ret;
  }
  */

  // TIMER ------------------------------------------------------------------------------
  private Timer mTimer = null;

  protected boolean isScheduled() { return mTimer != null; }

  protected void resetTimer()
  {
    if ( mTimer != null ) {
      Log.v("Cave3D", "reset timer" );
      mTimer.cancel();
      mTimer = null;
    }
  }

  protected void scheduleDisconnect( final int delay )
  {
    if ( mTimer == null ) {
      Log.v("Cave3D", "schedule a disconnect Device" );
      mTimer = new Timer();
      mTimer.schedule(  new TimerTask() { @Override public void run() { disconnectDevice(); } }, delay );
    }
  }

  protected void scheduleConnect( final int delay )
  {
    if ( mTimer == null ) {
      Log.v("Cave3D", "schedule for a connect Device" );
      mTimer = new Timer();
      mTimer.schedule(  new TimerTask() { @Override public void run() { connectDevice( ); } }, delay );
    }
  }

  protected void scheduleReconnect( final int delay, final int period, final String address )
  {
    if ( mTimer == null ) {
      Log.v("Cave3D", "schedule for a re-connect" );
      mTimer = new Timer();
      mTimer.schedule(  new TimerTask() { @Override public void run() { connectDevice( ); } }, delay, period );
    }
  }


  // BluetoothComm ----------------------------------------------------------------------

  // @Implements BluetothComm
  public boolean connectDevice( ) 
  { 
    Log.v("Cave3D", "TopoGL comm connect device - return false");
    return false; 
  }

  // @Implements BluetothComm
  public boolean disconnectDevice()
  {
    Log.v("Cave3D", "TopoGL comm disconnect device - return true");
    return true;
  }

  // @Implements BluetothComm
  public boolean isConnected() { return mBTConnected; }

  // @Implements BluetothComm
  public boolean sendCommand( int cmd ) { return false; }

  public void setConnected( boolean connected ) { mBTConnected = connected; }


  // QUEUE CONSUMER ---------------------------------------------------------------
  private Thread mQueueConsumer = null;

  // set the protocol
  // @param proto   new protocol (or null to close the protocol)
  protected void setProto( TopoGLProto proto )
  {
    if ( mProto == null ) {
      mProto = proto;
      if ( mProto != null ) startQueueConsumer();
    } else {
      if ( proto == null ) {
        stopQueueConsumer();
        mProto = proto;
      } else {
        if ( mProto != proto ) {
          stopQueueConsumer();
          mProto = proto;
          if ( mProto != null ) startQueueConsumer();
        }
      }
    }
  }

  private boolean startQueueConsumer()
  {
    if ( mQueueConsumer != null ) return false;
    mQueueConsumer = new Thread(){
      public void run()
      {
        for ( ; ; ) {
          DataBuffer buffer = mQueue.get();
          if ( buffer.type >= DataBuffer.DATA_EXIT ) break; // DATA_EXIT
          if ( mProto != null ) {
            mProto.handleDataBuffer( buffer );
          } else {
            Log.v( "Cave3D", "queue consumer null proto" );
          }
        }
      } 
    };
    mQueueConsumer.start();
    return true;
  }

  private boolean stopQueueConsumer()
  {
    if ( mQueueConsumer == null ) return false;
    mQueue.put( new DataBuffer( DataBuffer.DATA_EXIT, DeviceType.DEVICE_ANY, null ) );
    try {
      mQueueConsumer.join();
    } catch ( InterruptedException e ) { }
    mQueueConsumer = null;
    return true;
  }

  // STATUS --------------------------------------------------------------------------

  // @Implements BluetoothComm
  public void notifyStatus( int status )
  {
    mApp.notifyStatus( status );
  }
}
