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
  protected double mRollHigh;
  protected int    mType;
  protected double mAcc; // G intensity
  protected double mMag; // M intensity
  protected double mDip; // magnetic dip

  // int    getType() { return mDeviceType; }
  // byte[] getAddress() { return mAddress; }

  byte[] mAddrBuffer  = new byte[2];
  byte[] mReplyBuffer = new byte[4];

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

  protected void sendDataToApp( )
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
    Message msg = mApp.obtainMessage( TopoGL.MESSAGE_BLOCK );
    if ( msg != null ) {
      Bundle bundle = new Bundle();
      bundle.putDouble( TopoGL.BLOCK_D, mDistance );
      bundle.putDouble( TopoGL.BLOCK_B, mBearing );
      bundle.putDouble( TopoGL.BLOCK_C, mClino );
      bundle.putInt( TopoGL.BLOCK_T, mType );
      msg.setData(bundle);
      Log.v("Cave3D", "TopoGL proto - send message to app");
      mApp.sendMessage(msg);
    } else {
      Log.v("Cave3D", "TopoGL proto - could not obtain message");
    }
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

  protected void handleDistoXBuffer(  DataBuffer data_buffer )
  {
    if ( data_buffer.type != DataBuffer.DATA_PACKET ) {
      Log.v("Cave3D", "DistoX proto handle buffer - buffer not packet");
      return;
    }
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
            Log.e("Cave3D", "TD proto: does not handle packet BLE");
            break;
          case DeviceType.DEVICE_SAP5: 
            // Log.v("Cave3D", "TD proto: handle packet SAP");
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

        Log.v("Cave3D", "TopoGL proto handle buffer - data " + mDistance + " " + mBearing + " " + mClino );
        sendDataToApp();
        break; // return DataBuffer.DATA_PACKET;
      case 0x02:
        break; // return DataBuffer.DATA_G;
      case 0x03:
        break; // return DataBuffer.DATA_M;
      case 0x04:
        break; // return DataBuffer.DATA_VECTOR;
      case 0x38:  // Reply packet
        mAddrBuffer[0] = buffer[1];
        mAddrBuffer[1] = buffer[2];
        {
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
