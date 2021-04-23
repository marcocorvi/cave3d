/* @file ConnectionState.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid connection state
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 *
 */
package com.topodroid.bt;

public class ConnectionState
{
  public static final int CONN_DISCONNECTED = 0;
  public static final int CONN_CONNECTED    = 1;
  public static final int CONN_WAITING      = 2;

  public static final String[] statusString = { "Disconnected", "Connected", "Waiting" };
}
