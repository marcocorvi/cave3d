/* @file BleOpNotify.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief Bluetooth LE notify operation 
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import android.content.Context;

import android.bluetooth.BluetoothGattCharacteristic;

import android.util.Log;

import java.util.UUID;

public class BleOpNotify extends BleOperation 
{
  boolean mEnable;
  UUID mSrvUuid;
  UUID mChrtUuid;
  // BluetoothGattCharacteristic mChrt = null;

  // BleOpNotify( Context ctx, BleComm pipe, UUID srvUuid, BluetoothGattCharacteristic chrt, boolean enable )
  // {
  //   super( ctx, pipe );
  //   mSrvUuid  = srvUuid;
  //   mChrtUuid = chrt.getUuid();
  //   mChrt = chrt;
  //   mEnable   = enable;
  // }

  public BleOpNotify( Context ctx, BleComm pipe, UUID srvUuid, UUID chrtUuid, boolean enable )
  {
    super( ctx, pipe );
    mSrvUuid  = srvUuid;
    mChrtUuid = chrtUuid;
    // mChrt     = null;
    mEnable   = enable;
  }

  // public String name() { return "Notify"; }

  @Override 
  public void execute()
  {
    Log.v("Cave3D", "BleOp exec notify " + mEnable + " " + mChrtUuid.toString() );
    if ( mPipe == null ) { 
      return;
    }
    if ( mEnable ) {
      mPipe.enablePNotify( mSrvUuid, mChrtUuid );
    } else {
      // mPipe.disablePNotify( mSrvUuid, mChrtUuid );
    }
  }
}
