/* @file BricProto.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief BRIC4 protocol
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

public class BricProto extends TopoGLProto
{

  private ConcurrentLinkedQueue< BleOperation > mOps;
  // private BricComm mComm;
  private byte[] mLastTime;   // content of LastTime payload
  private byte[] mLastPrim;   // used to check if the coming Prim is new
  private boolean mPrimToDo = false;

  BleCallback mCallback;

  // data struct
  // private int   mIndex; // unused
  private long  mThisTime; // data timestamp [msec]
  long mTime = 0;          // timestamp of data that must be processed
  // float mDip;

  // unused
  // short mYear;
  // char  mMonth, mDay, mHour, mMinute, mSecond, mCentisecond;


  public BricProto( TopoGL app, int device_type, BluetoothDevice bt_device )
  {
    super( app, device_type, bt_device );
    // mComm   = comm;
    // mIndex  = -1;
    mLastTime = null;
    mLastPrim = new byte[20];
  }

  @Override
  DataBuffer getDataBuffer( int type, byte[] bytes )
  {
    return new DataBuffer( type, DeviceType.DEVICE_BRIC4, bytes );
  }

  @Override
  protected void handleDataBuffer( DataBuffer data_buffer )
  {
    if ( ! DeviceType.isBric( data_buffer.device ) ) return;
    switch ( data_buffer.type ) {
      case DataBuffer.DATA_PRIM: handleMeasPrim( data_buffer.data ); break;
      case DataBuffer.DATA_META: handleMeasMeta( data_buffer.data ); break;
      case DataBuffer.DATA_ERR:  handleMeasErr( data_buffer.data );  break;
    }
  }

  // DATA -------------------------------------------------------

  /* check if the bytes coincide with the last Prim
   * @return true if the bytes are equal to the last Prim
   * @note the last Prim is always filled with the new bytes on exit
   */ 
  private boolean checkPrim( byte[] bytes )
  {
    mThisTime = BricConst.getTimestamp( bytes ); // first 8 bytes
    if ( mTime != mThisTime ) {
      for ( int h=0; h<20; ++h ) mLastPrim[h] = bytes[h];
      return true;
    }
    for ( int k=8; k<20; ++k ) { // data bytes
      if ( bytes[k] != mLastPrim[k] ) {
        for ( int h=k; h<20; ++h ) mLastPrim[h] = bytes[h];
        return true;
      }
    }
    return false;
  }

  private void handleMeasPrim( byte[] bytes ) 
  {
    if ( checkPrim( bytes ) ) { // if Prim is new
      if ( mPrimToDo ) {        // and there is a previous Prim unprocessed
        processData();
      }
      // Log.v("Cave3D", "BRIC proto: meas_prim " );
      mTime     = mThisTime;
      mDistance = BricConst.getDistance( bytes );
      mBearing  = BricConst.getAzimuth( bytes );
      mClino    = BricConst.getClino( bytes );
      mPrimToDo = true;
    } else {
      Log.e("Cave3D", "BRIC proto: add Prim - repeated primary" );
    }
  }

  private void handleMeasMeta( byte[] bytes ) 
  {
    // Log.v("Cave3D", "BRIC proto: add Meta " );
    // mIndex = BricConst.getIndex( bytes );
    mRoll  = BricConst.getRoll( bytes );
    mDip   = BricConst.getDip( bytes );
    mType  = BricConst.getType( bytes );
    // mSamples = BricConst.getSamples( bytes );
  }

  private void handleMeasErr( byte[] bytes ) 
  {
    // Log.v("Cave3D", "BRIC proto: add Err " );
    processData();
  }
  
  private void processData()
  {
    if ( mPrimToDo ) {
      // Log.v("Cave3D", "BRIC proto send data to the app thru comm");
      handleData();
      mPrimToDo = false;
    } else {
      Log.e("Cave3D", "BRIC proto: process - PrimToDo false: ... skip");
    }
  }

  /*
  private void handleMeasPrimAndProcess( byte[] bytes )
  {
    mTime = mThisTime;
    if ( checkPrim( bytes ) ) { // if Prim is new
      // Log.v("Cave3D", "BRIC proto: add Prim " );
      mTime     = mThisTime;
      mDistance = BricConst.getDistance( bytes );
      mBearing  = BricConst.getAzimuth( bytes );
      mClino    = BricConst.getClino( bytes );
      handleData();
    } else {
      Log.e("Cave3D", "BRIC proto: add+process - Prim repeated: ... skip");
    }
  }
  */

  private void setLastTime( byte[] bytes )
  {
    // Log.v("Cave3D", "BRIC proto: set last time " + BleUtils.bytesToString( bytes ) );
    mLastTime = Arrays.copyOfRange( bytes, 0, bytes.length );
  }

  void clearLastTime() { mLastTime = null; }

  byte[] getLastTime() { return mLastTime; }

}

