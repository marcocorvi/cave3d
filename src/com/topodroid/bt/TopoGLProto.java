/* @file TopoGLProto.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid TopoDroid communication protocol
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import com.topodroid.Cave3D.TopoGL;

import android.util.Log;

// import java.lang.ref.WeakReference;

// import java.io.IOException;

import java.util.UUID;
import java.util.List;
import java.util.Locale;
// import java.lang.reflect.Field;

// import android.os.CountDownTimer;

// import android.content.Context;
import android.os.Bundle;
import android.os.Message;

import android.bluetooth.BluetoothDevice;


public class TopoGLProto
{
  public static final String BLOCK_UPDATE = "BLOCK_UPDATE";
  public static final String BLOCK_D= "BLOCK_D";
  public static final String BLOCK_B= "BLOCK_B";
  public static final String BLOCK_C= "BLOCK_C";
  public static final String BLOCK_T= "BLOCK_T";

  protected TopoGL mApp;
  protected int    mDeviceType;
  protected String mAddress; // reemote device address
  protected BluetoothDevice mBtDevice; // remote device

  // protected static final UUID MY_UUID = UUID.fromString( "00001101-0000-1000-8000-00805F9B34FB" );

  protected int  mError; // readToRead error code
  public int getErrorCode() { return mError; }

  protected double mDistance;
  protected double mBearing;
  protected double mClino;
  protected double mRoll;
  protected int    mType;
  protected double mAcc; // G intensity
  protected double mMag; // M intensity
  protected double mDip; // magnetic dip

  // int    getType() { return mDeviceType; }
  // byte[] getAddress() { return mAddress; }

  //-----------------------------------------------------

  public TopoGLProto( TopoGL app, int device_type, BluetoothDevice bt_device )
  {
    // Log.v("Cave3D", "TD proto: type " + device.mType + " addr " + device.mAddress );
    mApp        = app;
    mDeviceType = device_type;
    mBtDevice   = bt_device;
    mAddress    = (bt_device == null)? "null" : bt_device.getAddress();
  }

  // PACKETS HANDLING -----------------------------------------------------------

  // private AtomicInteger mNrPacketsRead; // FIXME_ATOMIC_INT
  protected volatile int mNrPacketsRead;

  // int getNrPacketsRead() { return ( mNrPacketsRead == null )? 0 : mNrPacketsRead.get(); } // FIXME_ATOMIC_INT 
  public int  getNrReadPackets() { return mNrPacketsRead; }
  public void setNrReadPackets( int nr ) { mNrPacketsRead = nr; }

  // void incNrReadPackets() { ++mNrPacketsRead; }
  // void resetNrReadPackets() { mNrPacketsRead = 0; }

  protected void handleData( )
  {
    // mNrPacketsRead.incrementAndGet(); // FIXME_ATOMIC_INT
    ++mNrPacketsRead;
    // double d = mProtocol.mDistance;
    // double b = mProtocol.mBearing;
    // double c = mProtocol.mClino;
    // double r = mProtocol.mRoll;
    // double dip  = mProtocol.mDip;
    // long status = ( d > TDSetting.mMaxShotLength )? TDStatus.OVERSHOOT : TDStatus.NORMAL;
    // TODO split the data insert in three places: one for each data packet
    // mLastShotId = TopoDroidApp.mData.insertDistoXShot( TDInstance.sid, index, d, b, c, r, ExtendType.EXTEND_IGNORE, status, TDInstance.deviceAddress() );
    // TopoDroidApp.mData.updateShotAMDR( mLastShotId, TDInstance.sid, clino, azimuth, dip, r, false );

    // FIXME TODO with Handler
    // Message msg = mApp.obtainMessage( BLOCK_UPDATE );
    Bundle bundle = new Bundle();
    bundle.putDouble( BLOCK_D, mDistance );
    bundle.putDouble( BLOCK_B, mBearing );
    bundle.putDouble( BLOCK_C, mClino );
    bundle.putInt( BLOCK_T, mType );
    // msg.setData(bundle);
    // mApp.sendMessage(msg);

    // if ( TDInstance.deviceType() == Device.DISTO_A3 && TDSetting.mWaitData > 10 ) {
    //   DeviceType.slowDown( 500 );
    // }
  }

  // to be overridden
  protected void handleDataBuffer( DataBuffer data_buffer ) { }

  // to be overridden
  // @param type    data buffer type (or 0)
  // @param bytes   data buffer payload
  DataBuffer getDataBuffer( int type, byte[] bytes ) { return null; }



  // PACKETS I/O ------------------------------------------------------------------------

  /* must be overridden
  // @param no_timeout
  // @param data_type  packet data type (to filter packet of different type)
  public int readPacket( boolean no_timeout, int data_type )
  {
    Log.v("Cave3D", "TD proto: read_packet returns NONE");
    return DataBuffer.DATA_NONE;
  }
  */

  /** write a command to the out channel
   * @param cmd command code
   * @return true if success
   *
   * must be overridden - default fails
   *
  public boolean sendCommand( byte cmd ) { return false; }
   */

  /** read the number of data to download
   * @param command command to send to the DistoX
   * @return number of data to download
   *
   * must be overridden - default returns 0
   *
  public int readToRead( byte[] command ) { return 0; }
   */

}
