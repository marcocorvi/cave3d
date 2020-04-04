/** @file DataHelper.java
 *
 * @author marco corvi
 * @date nov 2019
 *
 * @brief Interface to TopoDroid survey database
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 *
 */
package com.topodroid.Cave3D;

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.DataSetObservable;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteDiskIOException;

// import android.widget.Toast;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("SyntaxError")
class DataHelper extends DataSetObservable
{
  private static final String TAG = "Cave3D DB";

  private static final String SURVEY_TABLE = "surveys";
  private static final String SHOT_TABLE   = "shots";

  private final static String WHERE_NAME        = "name=?";
  private final static String WHERE_SID         = "surveyId=?";
  private final static String WHERE_SID_STATUS  = "surveyId=? AND status=?";

  private final static String WHERE_SID_ID      = "surveyId=? AND id=?";

  private SQLiteDatabase myDB = null;

  static private String[] mReducedShotFields =
    { "id", "millis", "fStation", "tStation", "distance", "bearing", "clino", "flag"};
  // static private String[] mFullShotFields =
  //   { "id", "fStation", "tStation", "distance", "bearing", "clino", "acceleration", "magnetic", "dip", // 0 ..  8
  //     "extend", "flag", "leg", "comment", "type", "millis", "color", "stretch", "address"              // 9 .. 17
  //   };

  public DataHelper( Context context, String db_name, int db_version )
  {
    openDatabase( context, db_name, db_version );
  }

  void closeDatabase()
  {
    if ( myDB == null ) return;
    myDB.close();
    myDB = null;
  }

  void openDatabase( Context context, String database_name, int db_version )
  {
    // String database_name = TDPath.getDatabase();
    DistoXOpenHelper openHelper = new DistoXOpenHelper( context, database_name, db_version );

    try {
        myDB = openHelper.getWritableDatabase();
        if ( myDB == null ) {
          // Log.v( TAG, "failed get writable database" );
          return;
        }

        // while ( myDB.isDbLockedByOtherThreads() ) {
        //   TDUtil.slowDown( 200 );
        // }

        // updateConfig = myDB.compileStatement( "UPDATE configs SET value=? WHERE key=?" );

     } catch ( SQLiteException e ) {
       myDB = null;
       logError( "Data Helper cstr failed to get DB", e );
     }
   }

  private void logError( String msg, Exception e )
  {
    Log.e( TAG, msg + ": " + e.getMessage() );
  }

  private void handleDiskIOError( SQLiteDiskIOException e )
  {
    Log.e( TAG, "disk error " + e.getMessage() );
  }

  // long getSurveyIdFromName( String name ) 
  // {
  //   long id = -1;
  //   if ( myDB == null ) { return -2; }
  //   Cursor cursor = myDB.query( SURVEY_TABLE, new String[] { "id" },
  //                               "name = ?", new String[] { name },
  //                               null, null, null );
  //   if (cursor != null ) {
  //     if (cursor.moveToFirst() ) {
  //       id = cursor.getLong(0);
  //     }
  //     if ( ! cursor.isClosed()) cursor.close();
  //   }
  //   return id;
  // }

  // ----------------------------------------------------------------
  // SELECT STATEMENTS - SHOT

  List<DBlock> getSurveyShots( long sid, long status )
  {
    List< DBlock > list = new ArrayList<>();
    if ( myDB == null ) return list;
    Cursor cursor = myDB.query(SHOT_TABLE, mReducedShotFields, // { "id", "millis", "fStation", "tStation", "distance", "bearing", "clino", "flag"};
                    WHERE_SID_STATUS, new String[]{ Long.toString(sid), Long.toString(status) },
                    null, null, "id" );
    if (cursor.moveToFirst()) {
      do {
        if ( cursor.getString(1).length() > 0 && cursor.getString(2).length() > 0 ) {
          DBlock block = new DBlock();
          reducedFillBlock( sid, block, cursor );
          list.add( block );
        }
      } while (cursor.moveToNext());
    }
    if ( /* cursor != null && */ !cursor.isClosed()) cursor.close();
    return list;
  }

  private void reducedFillBlock( long sid, DBlock blk, Cursor cursor )
  {
    blk.mId = cursor.getLong(0);
    blk.mSurveyId = sid;
    blk.mTime    = cursor.getLong( 1 );
    blk.mFrom = cursor.getString(2);
    blk.mTo   = cursor.getString(3);
    // blk.backshot = (leg == LegType.BACK) );  // from - to
    blk.mLength       = (float)( cursor.getDouble(4) );  // length [meters]
    blk.mBearing      = (float)( cursor.getDouble(5) );  // bearing [degrees]
    blk.mClino        = (float)( cursor.getDouble(6) );  // clino [degrees]
    blk.mFlag         = (int)( cursor.getLong(7) ); 
  }
  
  // ----------------------------------------------------------------------
  // SURVEY

  SurveyInfo getSurveyInfo( String name )
  {
    SurveyInfo info = null;
    if ( myDB == null ) return null;
    Cursor cursor = myDB.query( SURVEY_TABLE,
                               new String[] { "id", "name", "day", "team", "declination" }, // columns
                               WHERE_NAME, new String[] { name },
                               null, null, null );
    if (cursor.moveToFirst()) {
      info = new SurveyInfo();
      info.id      = cursor.getLong( 0 );
      info.name    = cursor.getString( 1 );
      info.date    = cursor.getString( 2 );
      info.team    = cursor.getString( 3 );
      info.declination = (float)(cursor.getDouble( 4 ));
    }
    if ( /* cursor != null && */ !cursor.isClosed()) cursor.close();
    return info;
  }

  List<SurveyFixed> getSurveyFixeds( long sid )
  {
    if ( myDB == null ) return null;
    List<SurveyFixed> ret = new ArrayList<SurveyFixed>();
    Cursor cursor = myDB.query( "fixeds", // FIXED_TABLE,
                               new String[] { "station", "longitude", "latitude", "altitude", "altimetric", "cs_name", "cs_longitude", "cs_latitude", "cs_altitude" }, // columns
                               WHERE_SID, new String[] { Long.toString(sid) },
                               null, null, null );
    if (cursor.moveToFirst()) {
      do {
        SurveyFixed fixed = new SurveyFixed( cursor.getString(0) );
        fixed.mLongitude  = cursor.getDouble(1); // longitude
        fixed.mLatitude   = cursor.getDouble(2);
        fixed.mAltitude   = cursor.getDouble(3);
        fixed.mAltimetric = cursor.getDouble(4);
        fixed.mCsName     = cursor.getString(5);
        fixed.mCsLongitude = cursor.getDouble(6);
        fixed.mCsLatitude  = cursor.getDouble(7);
        fixed.mCsAltitude  = cursor.getDouble(8);
        ret.add( fixed );
      } while (cursor.moveToNext());
    }
    if ( /* cursor != null && */ !cursor.isClosed()) cursor.close();
    return ret;
  }

  // ----------------------------------------------------------------------

  @SuppressWarnings("SyntaxError")
  private static class DistoXOpenHelper extends SQLiteOpenHelper
  {
     // private static final String create_table = "CREATE TABLE IF NOT EXISTS ";

     DistoXOpenHelper(Context context, String database_name, int db_version ) 
     {
        super(context, database_name, null, db_version ); 
        // Log.v( TAG, "NAME " + database_name );
     }

     @Override
     public void onCreate(SQLiteDatabase db) 
     {
     }

     @Override
     public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
     {  
     }
  }

}

