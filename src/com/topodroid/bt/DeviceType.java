/* @file DeviceType.java
 *
 * @author marco corvi
 * @date apr 2021
 *
 * @brief Bluetooth low-energy utility functions and constants
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import android.bluetooth.BluetoothDevice;
// import android.bluetooth.BluetoothProfile;
// import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattCharacteristic;
// import android.bluetooth.BluetoothGattCallback;
// import android.bluetooth.BluetoothGattService;

import java.util.UUID;
import java.util.Arrays;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DeviceType
{
  // device types
  static final int DEVICE_UNKNOWN = 0;
  static final int DEVICE_DISTOX1 = 1;
  static final int DEVICE_DISTOX2 = 2;
  static final int DEVICE_DISTOX  = 3;
  static final int DEVICE_BRIC4   = 4;
  static final int DEVICE_SAP5    = 8;
  static final int DEVICE_ANY     = 15;

  public static boolean isDistoX( int device ) { return (device == DEVICE_DISTOX2) || (device == DEVICE_DISTOX1); }
  public static boolean isSap( int device ) { return (device == DEVICE_SAP5); }
  public static boolean isBric( int device ) { return (device == DEVICE_BRIC4); }

  public static boolean isDistoX1( BluetoothDevice device ) { return (device != null) && device.getName().equals("DistoX"); }
  public static boolean isDistoX2( BluetoothDevice device ) { return (device != null) && device.getName().startsWith("DistoX-"); }
  public static boolean isBric4( BluetoothDevice device ) { return (device != null) && device.getName().startsWith("BRIC4_"); }
  public static boolean isSap5( BluetoothDevice device ) { return (device != null) && device.getName().startsWith("SAP5"); }

  public static int getDeviceType( BluetoothDevice device )
  {
    if ( isSap5( device ) )    return DEVICE_SAP5;
    if ( isBric4( device ) )   return DEVICE_BRIC4;
    if ( isDistoX2( device ) ) return DEVICE_DISTOX2;
    if ( isDistoX1( device ) ) return DEVICE_DISTOX1;
    return DEVICE_UNKNOWN;
  }


  // UTILS ----------------------------------------------------------

  public static String getDeviceString( BluetoothDevice device )
  {
    String name = device.getName();
    if ( name == null ) name = "--";
    return name + " " + device.getAddress();
  }

  // SLOW ------------------------------------------------------------

  public static boolean slowDown( int msec ) 
  {
    try {
      Thread.sleep( msec );
    } catch ( InterruptedException e ) { return false; }
    return true;
  }

  public static boolean yieldDown( int msec ) 
  {
    try {
      Thread.yield();
      Thread.sleep( msec );
    } catch ( InterruptedException e ) { return false; }
    return true;
  }

}

