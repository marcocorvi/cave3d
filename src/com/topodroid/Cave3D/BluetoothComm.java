/* @file BluetoothComm.java
 *
 * @author marco corvi
 * @date apr 2021
 *
 * @brief BT comm interface to the app
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

public interface BluetoothComm
{
  public boolean connectDevice();

  public boolean disconnectDevice();

  public boolean isConnected();

  public boolean sendCommand( int cmd );

  public void notifyStatus( int state );

}
