/* @file CommThread.java
 *
 * @author marco corvi
 * @date feb 2021 (extracted from TopoDroidComm)
 *
 * @brief TopoDroid bluetooth RFcomm communication thread
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import android.util.Log;

import android.os.Handler;

public class CommThread extends Thread
{
  int mType;

  private DistoXComm     mComm;
  // private TopoDroidProtocol mProtocol;
  private int toRead; // number of packet to read
  // private long mLastShotId;   // last shot id

  private volatile boolean doWork = true;

  public void setDone()
  { 
    doWork = false;
  }

  /** 
   * @param type        communication type
   * @param comm        communication class
   * @param to_read     number of data to read (use -1 to read forever until timeout or an exception)
   * @param lister      optional data lister
   * @param data_type   packet datatype (either shot or calib)
   */
  public CommThread( int type, DistoXComm comm )
  {
    mType  = type;
    mComm  = comm;
    // mComm.setNrReadPackets( 0 );
    // mLastShotId = 0;
  }

  /** This thread blocks on read_Packet (socket read) and when a packet arrives 
   * it handles it
   */
  public void run()
  {
    doWork = true;

    if ( mType == TopoGLComm.COMM_RFCOMM ) {
      Log.v( "Cave3D", "Comm Thread for RFCOMM");
      while ( doWork ) {
        Log.v( "Cave3D", "Comm Thread reading ...");
        int res = mComm.readPacket( true );
        if ( res == DataBuffer.DATA_NONE ) {
          Log.v( "Cave3D", "Comm Thread read DATA_NONE");
          if ( toRead == -1 ) {
            setDone();
          } else {
            DeviceType.slowDown( 500 );
          }
        } else if ( res == DistoXConst.DISTOX_ERR_OFF ) {
          Log.v( "Cave3D", "Comm Thread read ERR_OFF");
          // if ( TDSetting.mCommType == 1 && TDSetting.mAutoReconnect ) { // FIXME ACL_DISCONNECT
          //   mApp.mDataDownloader.setConnected( false );
          //   mApp.notifyStatus();
          //   closeSocket( );
          //   mApp.notifyDisconnected();
          // }
          doWork = false;
        // } else { // handlePacket is automatically done by mComm if the packet if OK
        //   mComm.handlePacket( res );
        } else {
          Log.v( "Cave3D", "Comm Thread read OK");
        }
      }
    } else { // if ( mType == COMM_GATT ) 
      Log.v( "Cave3D", "Comm Thread for GATT");
      mComm.readPacket( true );
    }
    mComm.doneCommThread();

  }
}
