/* @file DistoXConst.java
 *
 * @author marco corvi
 * @date apr 2021 (from TopoDroid)
 *
 * @brief DistoX2 commands and error codes
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import android.util.Log;

import java.io.StringWriter;
import java.io.PrintWriter;

import java.util.Locale;


public class DistoXConst
{
  // commands
  // public static final int CALIB_OFF        = 0x30;
  // public static final int CALIB_ON         = 0x31;
  // public static final int SILENT_ON        = 0x32;
  // public static final int SILENT_OFF       = 0x33;
  public static final int DISTOX_OFF       = 0x34;
  public static final int DISTOX_35        = 0x35;
  public static final int LASER_ON         = 0x36;
  public static final int LASER_OFF        = 0x37;
  public static final int MEASURE          = 0x38;

  // error codes
  public static final int DISTOX_ERR_OK           =  0; // OK: no error
  public static final int DISTOX_ERR_HEADTAIL     = -1;
  public static final int DISTOX_ERR_HEADTAIL_IO  = -2;
  public static final int DISTOX_ERR_HEADTAIL_EOF = -3;
  public static final int DISTOX_ERR_CONNECTED    = -4;
  public static final int DISTOX_ERR_OFF          = -5; // distox has turned off
  public static final int DISTOX_ERR_PROTOCOL     = -6; // protocol is null

  public static final byte BIT_BACKSIGHT2 = 0x20;
  public static final byte BIT_BACKSIGHT  = 0x40; // backsight bit of vector packet


  // ------------------------------------------------------------

  public static float getDistance( byte[] bytes ) { return toDistance( bytes[0], bytes[1], bytes[2] ); }
  public static float getAzimuth( byte[] bytes ) { return toAzimuth( bytes[3], bytes[4] ); }
  public static float getClino( byte[] bytes ) { return toAzimuth( bytes[5], bytes[6] ); }
  public static float getRoll( byte[] bytes ) { return toRoll( bytes[7] ); }
  public static float getAcc( byte[] bytes ) { return toInt( bytes[2], bytes[1] ); }
  public static float getMag( byte[] bytes ) { return toInt( bytes[4], bytes[3] ); }
  public static float getDip( byte[] bytes ) { return toDip( bytes[5], bytes[6] ); }
  public static int   getType( byte[] bytes ) { return 0; } // REGULAR_SHOT

  // ------------------------------------------------------------
  // @file MmeoryOctet.java
  // private int index; // memory index
  // // A3:   index = address/8
  // // X310: index = 56*(address/1024) + (address%1024)/18


  public static void printHexString( PrintWriter pw, byte[] data )
  {
    boolean hot  = (int)( data[0] & 0x80 ) == 0x80; // test hot bit
    pw.format( "%c %02x %02x %02x %02x %02x %02x %02x %02x",
               hot? '?' : '>',
               data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7] );
  }

  public static String toString( byte[] data )
  {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter( sw );

    if ( data[0] == 0xff ) {
      pw.format("invalid data");
    } else {
  
      boolean hot  = (int)( data[0] & 0x80 ) == 0x80; // test hot bit
      int type = (int)( data[0] & 0x0f ); // bits 0-5 but here check only 0-3 because bit 5 = backsight
      switch ( type ) {
        case 0x01:
          float dd = toDistance( data[0], data[1], data[2] );
          float bb = toAzimuth( data[3], data[4] );
          float cc = toClino( data[5], data[6] );
          if ( (data[0] & BIT_BACKSIGHT2) == BIT_BACKSIGHT2 ) {
            pw.format(Locale.US, "%c %.2f %.1f %.1f", hot? 'B' : 'b', dd, bb, cc );
          } else {
            pw.format(Locale.US, "%c %.2f %.1f %.1f", hot? 'D' : 'd', dd, bb, cc );
          }
          break;
        case 0x02:
        case 0x03:
          // long X = toInt( data[2], data[1] );
          // long Y = toInt( data[4], data[3] );
          // long Z = toInt( data[6], data[5] );
          // if ( X > TDUtil.ZERO ) X = X - TDUtil.NEG;
          // if ( Y > TDUtil.ZERO ) Y = Y - TDUtil.NEG;
          // if ( Z > TDUtil.ZERO ) Z = Z - TDUtil.NEG;
          if ( type == 0x02 ) {
            pw.format("%c %02x %02x %02x %02x %02x %02x", hot? 'G' : 'g', data[1], data[2], data[3], data[4], data[5], data[6] );
          } else {
            // pw.format("%4d %c %x %x %x", index, hot? 'M' : 'm', X, Y, Z );
            pw.format("%c %02x %02x %02x %02x %02x %02x", hot? 'M' : 'm', data[1], data[2], data[3], data[4], data[5], data[6] );
          }
          break;
        case 0x04:
          boolean backsight = ( (data[0] & BIT_BACKSIGHT) == BIT_BACKSIGHT);
          int acc = toInt( data[2], data[1] );
          int mag = toInt( data[4], data[3] );
          float dip = toDip( data[5], data[6] );
          pw.format(Locale.US, "%c %d %d %.2f %02x", backsight? 'V' : 'v', acc, mag, dip, data[0] ); // is data[7] important ?
          break;
        default:
          printHexString( pw, data );
          break;
      }
    }
    return sw.getBuffer().toString();
  }


  // --------------------------------------------------------
  private static float toDistance( byte b0, byte b1, byte b2 )
  {
    int dhh = (int)( b0 & 0x40 );
    float d =  dhh * 1024.0f + toInt( b2, b1 );
    if ( d < 99999 ) {
      return d / 1000.0f;
    }
    return 100 + (d-100000) / 100.0f;
  }

  private static float toAzimuth( byte b1, byte b2 ) // b1 low, b2 high
  {
    int b = toInt( b2, b1 );
    return b * 180.0f / 32768.0f; // 180/0x8000;
  }

  private static float toClino( byte b1, byte b2 ) // b1 low, b2 high
  {
    int c = toInt( b2, b1 );
    if ( c >= 32768 ) return (65536 - c) * (-90.0f) / 16384.0f; 
    return c * 90.0f  / 16384.0f; // 90/0x4000;
  }

  private static float toRoll( byte b2 ) // high byte only
  {
    int r7 = (int)(b2 & 0xff); if ( r7 < 0 ) r7 += 256;
    return r7 * 180.0f / 128.0f;
  }

  private static float toDip( byte b1, byte b2 )
  {
    float dip = toInt( b1, b2 );
    if ( dip >= 32768 ) return (65536 - dip) * (-90.0f) / 16384.0f;
    return dip * 90.0f  / 16384.0f; // 90/0x4000;
  }

  static int toInt( byte b ) 
  {
    int ret = (int)(b & 0xff);
    if ( ret < 0 ) ret += 256;
    return ret;
  }

  static int toInt( byte bh, byte bl )
  {
    int h = (int)(bh & 0xff);   // high
    if ( h < 0 ) h += 256;
    int l = (int)(bl & 0xff);   // low
    if ( l < 0 ) l += 256;
    return (h * 256 + l);
  }

}
