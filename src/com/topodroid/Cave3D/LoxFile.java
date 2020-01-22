/* @file LoxFile.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D loch file parser
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;

// #include "LoxSurvey.h"
// #include "LoxStation.h"
// #include "LoxShot.h"
// #include "LoxScrap.h"
// #include "LoxSurface.h"
// #include "LoxBitmap.h"

import android.util.Log;

class LoxFile
{
  final static String TAG = "Cave3D LOX";

  private class Chunk_t
  {
    int type;
    int rec_size;
    int rec_cnt;
    int data_size;
    byte[] records;
    byte[] data;

    int size() { return rec_cnt; }

    Chunk_t( int t )
    {
      type      = t;
      rec_size  = 0;
      rec_cnt   = 0;
      data_size = 0;
      records   = null;
      data      = null;
    }
  }

  private Chunk_t mSurveyChunk;
  private Chunk_t mStationChunk;
  private Chunk_t mShotChunk;
  private Chunk_t mScrapChunk;
  private Chunk_t mSurfaceChunk;
  private Chunk_t mBitmapChunk;

  private ArrayList< LoxSurvey >  mSurveys;
  private ArrayList< LoxStation > mStations;
  private ArrayList< LoxShot >    mShots;
  private ArrayList< LoxScrap >   mScraps;
  private LoxSurface              mSurface;
  private LoxBitmap               mBitmap;

  private int linenr = 0; // chunk number

  LoxFile( String filename ) throws Cave3DParserException
  {
    mSurface = null;
    mBitmap  = null;

    mSurveys  = new ArrayList< LoxSurvey >();
    mStations = new ArrayList< LoxStation >();
    mShots    = new ArrayList< LoxShot >();
    mScraps   = new ArrayList< LoxScrap >();
    
    File file = new File( filename );
    linenr = 0;
    if ( file.exists() ) {
      readChunks( filename );
    } else {
      throw new Cave3DParserException( filename, linenr );
    }
  }

  int NrSurveys()  { return mSurveyChunk.size(); }
  int NrStations() { return mStationChunk.size(); }
  int NrShots()    { return mShotChunk.size(); }
  int NrScraps()   { return mScrapChunk.size(); }
  int NrSurfaces() { return mSurfaceChunk.size(); }
  int NrBitmaps()  { return mBitmapChunk.size(); }

  ArrayList< LoxSurvey >  GetSurveys()  { return mSurveys; }
  ArrayList< LoxStation > GetStations() { return mStations; }
  ArrayList< LoxShot >    GetShots()    { return mShots; }
  ArrayList< LoxScrap >   GetScraps()   { return mScraps; }
  LoxSurface              GetSurface()  { return mSurface; }
  LoxBitmap               GetBitmap()   { return mBitmap; }


  static private final int SIZEDBL = 8; // ( sizeof( double ) )
  static private final int SIZE32  = 4; // ( sizeof( uint32_t ) )
  static private final int SIZE_SURVEY  = ( ( 6 * SIZE32 + 0 * SIZEDBL ) );
  static private final int SIZE_STATION = ( ( 7 * SIZE32 + 3 * SIZEDBL ) ); // 52 bytes
  static private final int SIZE_SHOT    = ( ( 5 * SIZE32 + 9 * SIZEDBL ) );
  static private final int SIZE_SCRAP   = ( ( 8 * SIZE32 + 0 * SIZEDBL ) );
  static private final int SIZE_SURFACE = ( ( 5 * SIZE32 + 6 * SIZEDBL ) );

// one can safely assume that all Android are little endian (after ARM-3)
// #ifdef BIG_ENDIAN
//   int toIntLEndian( byte val[] )  // int32 to int
//   {
//     return val[3] | ( ((int)val[2]) << 8 ) | ( ((int)(val[1])) << 16 ) | ( ((int)(val[0])) << 24 );
//   }
//   
//   float toFloatFloatLEndian( byte val[] )
//   {
//     return (float)( val[3] | ( ((int)val[2]) << 8 ) | ( ((int)(val[1])) << 16 ) | ( ((int)(val[0])) << 24 ) );
//   }
// 
//   double toDoubleLEndian( byte val[] )
//   {
//     return (double)(
//       (long)(val[7]) | ( ((long)val[6]) << 8 ) | ( ((long)(val[5])) << 16 ) | ( ((long)(val[4])) << 24 ) |
//       ( ((long)(val[3])<<32) ) | ( ((long)val[2]) << 40 ) | ( ((long)(val[1])) << 48 ) | ( ((long)(val[0])) << 56 ) );
//   }
// #else
  int toIntLEndian( byte val[] ) 
  {
    // Log.v(TAG, "toInt " + val[0] + " " + val[1] + " " + val[2] + " " + val[3] );
    int r0 = ( val[0] < 0 )? 256+val[0] : val[0];
    int r1 = ( val[1] < 0 )? 256+val[1] : val[1];
    int r2 = ( val[2] < 0 )? 256+val[2] : val[2];
    int r3 = ( val[3] < 0 )? 256+val[3] : val[3];
    return r0 | ( r1 << 8 ) | ( r2 << 16 ) | ( r3 << 24 );
  }

  float toFloatLEndian( byte val[] ) 
  {
    return (float)( val[0] | ( ((int)val[1]) << 8 ) | ( ((int)(val[2])) << 16 ) | ( ((int)(val[3])) << 24 ) );
  }

  double toDoubleLEndian( byte val[] )
  {
    int r0 = ( val[0] < 0 )? 256+val[0] : val[0];
    int r1 = ( val[1] < 0 )? 256+val[1] : val[1];
    int r2 = ( val[2] < 0 )? 256+val[2] : val[2];
    int r3 = ( val[3] < 0 )? 256+val[3] : val[3];
    int r4 = ( val[4] < 0 )? 256+val[4] : val[4];
    int r5 = ( val[5] < 0 )? 256+val[5] : val[5];
    int r6 = ( val[6] < 0 )? 256+val[6] : val[6];
    int r7 = ( val[7] < 0 )? 256+val[7] : val[7];
    // reinterpret as double
    return Double.longBitsToDouble(
      (long)(r0) | ( ((long)r1) << 8 ) | ( ((long)r2) << 16 ) | ( ((long)r3) << 24 ) |
      ( ((long)r4) << 32 ) | ( ((long)r5) << 40 ) | ( ((long)r6) << 48 ) | ( ((long)r7) << 56 ) 
    );
  }
// #endif

  int toIntLEndian( byte[] val, int off ) 
  {
    byte tmp[] = new byte[4];
    for (int k=0; k<4; ++k ) tmp[k] = val[off+k];
    return toIntLEndian( tmp );
  }

  double toDoubleLEndian( byte[] val, int off ) 
  {
    byte tmp[] = new byte[8];
    for (int k=0; k<8; ++k ) tmp[k] = val[off+k];
    return toDoubleLEndian( tmp );
  }

  // void convertToIntLEndian( byte[] val, int off )
  // {
  //   byte tmp = val[off+0]; val[off+0] = val[off+3]; val[off+3] = tmp;
  //        tmp = val[off+1]; val[off+1] = val[off+2]; val[off+2] = tmp;
  // }

  // void convertToDoubleLEndian( byte[] val, int off )
  // {
  //   byte tmp = val[off+0]; val[off+0] = val[off+7]; val[off+7] = tmp;
  //        tmp = val[off+1]; val[off+1] = val[off+6]; val[off+6] = tmp;
  //        tmp = val[off+2]; val[off+2] = val[off+5]; val[off+5] = tmp;
  //        tmp = val[off+3]; val[off+3] = val[off+4]; val[off+4] = tmp;
  // }

  private void readChunks( String filename ) throws Cave3DParserException
  {
    // Log.v( TAG, "read chunks " + filename );
    int type;
    byte int32[] = new byte[ SIZE32 ];
    FileInputStream fis = null;
    try {
      fis = new FileInputStream( filename );
      while ( true ) {
	++linenr;
        fis.read( int32, 0, SIZE32 );
        type = toIntLEndian( int32 );
        if ( type < 1 || type > 6 ) {
          Log.e( TAG, "Unexpected chunk type " + type );
          return;
        }
        Chunk_t c = new Chunk_t( type );
        fis.read( int32, 0, SIZE32 ); c.rec_size  = toIntLEndian( int32 );
        fis.read( int32, 0, SIZE32 ); c.rec_cnt   = toIntLEndian( int32 );
        fis.read( int32, 0, SIZE32 ); c.data_size = toIntLEndian( int32 );
        // Log.v( TAG, "Type " + c.type + " RecSize " + c.rec_size + " RecCnt " + c.rec_cnt + " DataSize " + c.data_size );
        if ( c.rec_size > 0 ) {
          c.records = new byte[ c.rec_size ];
          fis.read( c.records, 0, c.rec_size );
          // Log.v( TAG, c.records[0] + " " + c.records[1] + " " + c.records[2] + " " + c.records[3] + " " + 
          //            c.records[4] + " " + c.records[5] + " " + c.records[6] + " " + c.records[7] );
        }
        if ( c.data_size > 0 ) {
          c.data = new byte[ c.data_size ];
          fis.read( c.data, 0, c.data_size );
        }
        // Log.v(TAG, "Read: bytes " + (4 * SIZE32 + c.rec_size + c.data_size) );
        switch ( type ) {
          case 1: // SURVEY
            HandleSurvey( c );
            break;
          case 2: // STATIONS
            HandleStations( c );
            break;
          case 3: // SHOTS
            HandleShots( c );
            break;
          case 4: // SCRAPS
            HandleScraps( c );
            break;
          case 5: // SURFACE
            HandleSurface( c );
            break;
          case 6: // SURFACE_BITMAP
            HandleBitmap( c );
            break;
          default:
        }
      }
    } catch( FileNotFoundException e ) {
      Log.e( TAG, "File not found " + e.getMessage() );
      throw new Cave3DParserException( filename, linenr );
    } catch( IOException e ) {
      Log.e( TAG, "I/O error " + e.getMessage() );
      throw new Cave3DParserException( filename, linenr );
    } finally {
      try {
        if ( fis != null ) fis.close();
      } catch( IOException e ) { }
    }
  }


  private void HandleSurvey( Chunk_t chunk )
  {
    mSurveyChunk = chunk;
    int n0 = chunk.rec_cnt;
    // Log.v( TAG, "Handle Survey: Nr. " + n0 );
    byte[] recs = chunk.records; // as int32
    byte[] data = chunk.data;    // as char
    String name  = null;
    String title = null;
    for ( int i=0; i<n0; ++i ) {
      int id = toIntLEndian( recs, 4*(6*i + 0) );
      int np = toIntLEndian( recs, 4*(6*i + 1) );
      int ns = toIntLEndian( recs, 4*(6*i + 2) );
      int pnt= toIntLEndian( recs, 4*(6*i + 3) );
      int tp = toIntLEndian( recs, 4*(6*i + 4) );
      int ts = toIntLEndian( recs, 4*(6*i + 5) );
      name  = getString( data, np, ns );
      title = getString( data, tp, ts );
      // Log.v(TAG, i + "/" + n0 + ": Survey " + id + " (parent "+ pnt + ") Name " + name + " Title " + title );
      mSurveys.add( new LoxSurvey( id, pnt, name, title ) );
    }
    // Log.v(TAG, "Handle Survey done");
  }

  private String getString( byte[] data, int np, int ns )
  {
    if ( ns == 0 ) return "";
    String ret = new String( data, np, ns );
    return ret.substring(0, ret.length()-1);
  }
  
  
  private void HandleStations( Chunk_t chunk )
  {
    mStationChunk = chunk;
    int n0 = chunk.rec_cnt;
    byte[] recs = chunk.records; // as int32
    byte[] data = chunk.data;    // as char
    String name    = null;
    String comment = null;
    for ( int i=0; i<n0; ++i ) {
      int off = ( i * SIZE_STATION );
      int id = toIntLEndian( recs, off ); off += SIZE32;
      int sid= toIntLEndian( recs, off ); off += SIZE32;
      int np = toIntLEndian( recs, off ); off += SIZE32;
      int ns = toIntLEndian( recs, off ); off += SIZE32;
      int tp = toIntLEndian( recs, off ); off += SIZE32;
      int ts = toIntLEndian( recs, off ); off += SIZE32;
      int fl = toIntLEndian( recs, off ); off += SIZE32;
      double c0 = toDoubleLEndian( recs, off ); off += SIZEDBL;
      double c1 = toDoubleLEndian( recs, off ); off += SIZEDBL;
      double c2 = toDoubleLEndian( recs, off ); off += SIZEDBL;
      name    = getString( data, np, ns );
      comment = getString( data, tp, ts );
      mStations.add( new LoxStation( id, sid, name, comment, fl, c0, c1, c2 ) );
      // if ( i < 3 ) {
      //   Log.v(TAG, "station " + id + " / " + sid + " <" + name + "> " + np + "-" + ns + " flag " + fl + " " + c0 + " " + c1 + " " + c2 );
      // }
    }
  }
  
  
  private void HandleShots( Chunk_t chunk )
  {
    mShotChunk = chunk;
    int n0 = chunk.rec_cnt;
    byte[] recs = chunk.records; // as int32
    // byte[] data = chunk.data;    // as char
    for ( int i=0; i<n0; ++i ) {
      int off = i * SIZE_SHOT;
      int fr = toIntLEndian( recs, off ); off += SIZE32;
      int to = toIntLEndian( recs, off ); off += SIZE32;
      double f0 = toDoubleLEndian( recs, off ); off += SIZEDBL;
      double f1 = toDoubleLEndian( recs, off ); off += SIZEDBL;
      double f2 = toDoubleLEndian( recs, off ); off += SIZEDBL;
      double f3 = toDoubleLEndian( recs, off ); off += SIZEDBL;
      double t0 = toDoubleLEndian( recs, off ); off += SIZEDBL;
      double t1 = toDoubleLEndian( recs, off ); off += SIZEDBL;
      double t2 = toDoubleLEndian( recs, off ); off += SIZEDBL;
      double t3 = toDoubleLEndian( recs, off ); off += SIZEDBL;
  
      // flag: SURFACE DUPLICATE NOT_VISIBLE NOT_LRUD SPLAY
      int fl = toIntLEndian( recs, off ); off += SIZE32;
      // type: NONE OVAL SQUARE DIAMOND TUNNEL
      int ty = toIntLEndian( recs, off ); off += SIZE32;
  
      int sid= toIntLEndian( recs, off ); off += SIZE32;
      double tr = toDoubleLEndian( recs, off ); off += SIZEDBL; // vthreshold
  
      // LOGI("Shot %d %d (%d) Flag %d Type %d thr %.2f", fr, to, sid, fl, ty, tr );
      // LOGI("  From-LRUD %.2f %.2f %.2f %.2f", f0, f1, f2, f3 );
      // LOGI("  To-LRUD %.2f %.2f %.2f %.2f", t0, t1, t2, t3 );
  
      mShots.add( new LoxShot( fr, to, sid, fl, ty, tr, f0, f1, f2, f3, t0, t1, t2, t3 ) );
      // if ( i < 3 ) {
      //   Log.v(TAG, "Shot " + fr + " " + to + " (" + sid + ") flag " + fl + " type " + ty );
      // }
    }
  }
  
  
  private void HandleScraps( Chunk_t chunk )
  {
    mScrapChunk = chunk;
    int n0 = chunk.rec_cnt;
    byte[] recs = chunk.records; // as int32
    byte[] data = chunk.data;    // as char
    for ( int i=0; i<n0; ++i ) {
      int off = i * SIZE_SCRAP;
      int id = toIntLEndian( recs, off ); off += SIZE32;
      int sid= toIntLEndian( recs, off ); off += SIZE32;
      int np = toIntLEndian( recs, off ); off += SIZE32;
      int pp = toIntLEndian( recs, off ); off += SIZE32;
      int ps = toIntLEndian( recs, off ); off += SIZE32;
      int na = toIntLEndian( recs, off ); off += SIZE32;
      int ap = toIntLEndian( recs, off ); off += SIZE32;
      int as = toIntLEndian( recs, off ); off += SIZE32;
      // LOGI("Scrap %d (Survey %d) N.pts %d %d %d N.ang %d %d %d Size %d",
      //   id, sid, np, pp, ps, na, ap, as, mScrapChunk.data_size );
      // assert( pp + np * 3 * sizeof(double) == ap );
      // assert( np * 3 * sizeof(double) == ps );
      // assert( na * 3 * SIZE32 == as );

      // double * ptr = (double *)( data + pp );
      double ptr[] = new double[ 3*np ];
      for ( int j=0; j<3*np; ++j) {
        ptr[j] = toDoubleLEndian( data, pp + j*SIZEDBL );
      }
      // uint32_t * itr = (uint32_t *)( data + ap );
      int itr[] = new int[ 3*na ];
      for ( int k=0; k<3*na; ++k ) {
        itr[k] = toIntLEndian( data, ap + k*SIZE32 );
      }
  
      mScraps.add( new LoxScrap( id, sid, np, na, ptr, itr ) );
    }
  }
  
  
  private void HandleSurface( Chunk_t chunk )
  {
    mSurfaceChunk = chunk;
    int n0 = chunk.rec_cnt;
    byte[] recs = chunk.records; // as int32
    byte[] data = chunk.data;    // as char
    int off = 0;
    int id = toIntLEndian( recs, off ); off += SIZE32;
    int ww = toIntLEndian( recs, off ); off += SIZE32;
    int hh = toIntLEndian( recs, off ); off += SIZE32;
    int dp = toIntLEndian( recs, off ); off += SIZE32;
    int ds = toIntLEndian( recs, off ); off += SIZE32;
    double c[] = new double[6];
    c[0]  = toDoubleLEndian( recs, off ); off += SIZEDBL;
    c[1]  = toDoubleLEndian( recs, off ); off += SIZEDBL;
    c[2]  = toDoubleLEndian( recs, off ); off += SIZEDBL;
    c[3]  = toDoubleLEndian( recs, off ); off += SIZEDBL;
    c[4]  = toDoubleLEndian( recs, off ); off += SIZEDBL;
    c[5]  = toDoubleLEndian( recs, off ); off += SIZEDBL;
    // Log.v( TAG, "Surface %d %dx%d Calib %.2f %.2f %.2f %.2f %.2f %.2f", id, ww, hh, c[0], c[1], c[2], c[3], c[4], c[5] );

    int npts = ww * hh;
    // assert( ds == npts * sizeof(double) );
    // double * ptr = (double *)( data + dp );
    double ptr[] = new double[ npts ];
    for ( int i=0; i< npts; ++i ) {
      ptr[i] = toDoubleLEndian( data, dp + i*SIZEDBL );
    }
    mSurface = new LoxSurface( id, ww, hh, c, ptr );
  }
  
  private void HandleBitmap( Chunk_t chunk )
  {
    mBitmapChunk = chunk;
    int n0 = chunk.rec_cnt;
    byte[] recs = chunk.records; // as int32
    byte[] data = chunk.data;    // as char
    int off = 0;
    int id = toIntLEndian( recs, off ); off += SIZE32; // surface id
    int tp = toIntLEndian( recs, off ); off += SIZE32; // type: JPEG PNG
    int dp = toIntLEndian( recs, off ); off += SIZE32;
    int ds = toIntLEndian( recs, off ); off += SIZE32;
    double c[] = new double[6];
    c[0] = toDoubleLEndian( recs, off ); off += SIZEDBL;
    c[1] = toDoubleLEndian( recs, off ); off += SIZEDBL;
    c[2] = toDoubleLEndian( recs, off ); off += SIZEDBL;
    c[3] = toDoubleLEndian( recs, off ); off += SIZEDBL;
    c[4] = toDoubleLEndian( recs, off ); off += SIZEDBL;
    c[5] = toDoubleLEndian( recs, off ); off += SIZEDBL;
    // LOGI("Bitmap %d Type %d Calib %.2f %.2f %.2f %.2f %.2f %.2f File off %d size %d",
    //   id, tp, c[0], c[1], c[2], c[3], c[4], c[5], dp, ds );
    // image file binary data
    // unsigned char * img = data + dp;
    mBitmap = new LoxBitmap( id, tp, ds, c, data, dp );
  }

}

