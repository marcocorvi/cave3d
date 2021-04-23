/* @file BleOpChrtWrite.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief Bluetooth LE characteristic write operation
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import android.content.Context;

import android.bluetooth.BluetoothDevice;

import android.util.Log;

import java.util.UUID;
import java.util.Arrays;

public class BleOpChrtWrite extends BleOperation 
{
  byte[] bytes;
  UUID   mSrvUuid;
  UUID   mChrtUuid;

  public BleOpChrtWrite( Context ctx, BleComm pipe, UUID srv_uuid, UUID chrt_uuid, byte[] b )
  {
    super( ctx, pipe );
    mSrvUuid  = srv_uuid;
    mChrtUuid = chrt_uuid;
    bytes = Arrays.copyOf( b, b.length );
  }

  // public String name() { return "ChrtWrite"; }

  @Override 
  public void execute()
  {
    if ( mPipe == null ) { 
      return;
    }
    boolean ret = 
      mPipe.writeChrt( mSrvUuid, mChrtUuid, bytes );
    Log.v("Cave3D", "BleOp exec chrt write: ret " + ret );
  }
}
