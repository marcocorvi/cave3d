/* @file BleOpDisconnect.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief Bluetooth LE disconnect operation 
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import android.content.Context;

import android.util.Log;

public class BleOpDisconnect extends BleOperation 
{
  public BleOpDisconnect( Context ctx, BleComm pipe )
  {
    super( ctx, pipe );
  }

  // public String name() { return "Disconnect"; }

  @Override 
  public void execute()
  {
    Log.v("Cave3D", "BleOp exec disconnect");
    if ( mPipe == null ) { 
      return;
    }
    if ( mPipe != null ) mPipe.disconnectGatt();
  }
}
