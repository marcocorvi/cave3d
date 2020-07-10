/** @file GPS.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief 3D Topo-GL activity
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus;
import android.location.GpsSatellite;
// import android.location.GpsStatus.Listener;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.content.Context;
import android.content.pm.PackageManager;

import java.util.Iterator;

class GPS implements LocationListener
          , GpsStatus.Listener
{
  private LocationManager locManager = null;
  private GpsStatus mStatus;

  boolean mIsLocating;
  boolean mHasLocation;
  private double mLat  = 0;  // decimal degrees
  private double mLng  = 0;  // decimal degrees
  private double mLat0 = 0;  // decimal degrees
  private double mLng0 = 0;  // decimal degrees

  private double mErr2 = -1; // location error [m]
  private int mNrSatellites = 0;
  private double mDelta;

  interface GPSListener
  {
    public void notifyLocation( double lng, double lat );
  }

  private GPSListener mListener = null;

  void setListener( GPSListener listener ) { mListener = listener; }

  void setDelta( double delta ) { if ( delta > 0 ) mDelta = delta; }


  public static boolean checkLocation( Context context )
  {
    // TDLog.Log( LOG_PERM, "check location" );
    // Log.v("DistoX-PERM", "Check location ");
    PackageManager pm = context.getPackageManager();
    return ( context.checkCallingOrSelfPermission( android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED )
        && pm.hasSystemFeature(PackageManager.FEATURE_LOCATION)
        && pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
  }

  GPS( Context ctx )
  {
    mDelta = 1.0e-6;
    mIsLocating = false;
    // mNrSatellites = 0;
    if ( checkLocation( ctx ) ) { // CHECK_PERMISSIONS
      locManager = (LocationManager) ctx.getSystemService( Context.LOCATION_SERVICE );
      if ( locManager != null ) {
        mStatus = locManager.getGpsStatus( null );
      }
    }
    mHasLocation = false;
  }

  private final double mW0 = 0.8;
  private final double mW1 = 1 - mW0;
  private final double mW2 = mW1 / mW0;
  private final double DEG2RAD = Math.PI/180.0;
  private final double EARTH_A = 6378137.0; // approx earth radius [meter]


  // location is stored in decimal degrees 
  private void assignLocation( Location loc ) 
  {
    if ( mErr2 < 0 ) {	  
      mLat  = loc.getLatitude();  // decimal degree
      mLng  = loc.getLongitude();
      // mHEll = loc.getAltitude();  // meter
      mErr2 = 10000;              // start with a large value
    } else {
      double lat0 = loc.getLatitude();
      double lng0 = loc.getLongitude();
      // double hel0 = loc.getAltitude();
      double lat  = mW1 * lat0 + mW0 * mLat;
      double lng  = mW1 * lng0 + mW0 * mLng;
      // double hell = mW1 * hel0 + mW0 * mHEll;
      double dlat = (lat0-mLat) * EARTH_A * DEG2RAD;
      double dlng = (lng0-mLng) * EARTH_A * DEG2RAD * Math.cos( mLat * DEG2RAD );
      // double dhel = hel0 - mHEll;
      double err2 = ( dlat*dlat + dlng*dlng /* + dhel*dhel */ );
      mErr2 = mW0 * mErr2 + mW2 * err2;
      mLat  = lat;
      mLng  = lng;
      // mHEll = hell;
      if ( Math.sqrt( mErr2 ) < 1 ) {
        if ( ! mHasLocation ) {
          mLng0 = mLng;
          mLat0 = mLat;
          mHasLocation = true;
          if ( mListener != null ) mListener.notifyLocation( mLng, mLat );
        } else {
          if ( Math.abs( mLat - mLat0 ) + Math.abs( mLng - mLng0 ) > mDelta ) {
            mLng0 = mLng;
            mLat0 = mLat;
            if ( mListener != null ) mListener.notifyLocation( mLng, mLat );
            mHasLocation = true;
          }
        }
        mErr2 = -1;
      }
    }
  }

  // boolean getLocation( Vector3D v )
  // {
  //   if ( ! mHasLocation ) return false;
  //   v.x = mLng;
  //   v.y = mLat;
  //   v.z = 0; // FIXME
  //   mHasLocation = false;
  //   return true;
  // }

  @SuppressLint("MissingPermission")
  void setGPSoff()
  {
    if ( locManager != null ) {
      locManager.removeUpdates( this );
      locManager.removeGpsStatusListener( this );
    }
    mIsLocating = false;
    mHasLocation = false;
  }

  @SuppressLint("MissingPermission")
  boolean setGPSon()
  {
    mHasLocation = false;
    mErr2 = -1; // restart location averaging
    if ( locManager == null ) return false;
    locManager.addGpsStatusListener( this );
    locManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 1000, 0, this );
    mIsLocating = true;
    mNrSatellites = 0;
    return true;
  }

  @Override
  public void onLocationChanged( Location loc )
  {
    if ( loc != null && mNrSatellites > 3 ) assignLocation( loc );
  }

  public void onProviderDisabled( String provider )
  {
  }

  public void onProviderEnabled( String provider )
  {
  }

  public void onStatusChanged( String provider, int status, Bundle extras )
  {
    // Log.v("TopoGL-GPS", "onStatusChanged status " + status );
  }

  @SuppressLint("MissingPermission")
  private int getNrSatellites()
  {
    locManager.getGpsStatus( mStatus );
    Iterator< GpsSatellite > sats = mStatus.getSatellites().iterator();
    int  nr = 0;
    while( sats.hasNext() ) {
      GpsSatellite sat = sats.next();
      if ( sat.usedInFix() ) ++nr;
    }
    return nr;
  }

  public void onGpsStatusChanged( int event ) 
  {
    if ( event == GpsStatus.GPS_EVENT_SATELLITE_STATUS ) {
      if ( locManager == null ) return;
      mNrSatellites = getNrSatellites();
      // Log.v("TopoGL-GPS", "GPS Status Changed nr satellites used in fix " + mNrSatellites );
      if ( mNrSatellites > 3 ) {
        try {
          Location loc = locManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
          if ( loc != null ) assignLocation( loc );
        } catch ( IllegalArgumentException e ) {
          Log.e("TopoGL", "onGpsStatusChanged IllegalArgumentException " );
        } catch ( SecurityException e ) {
          Log.e("TopoGL", "onGpsStatusChanged SecurityException " );
        }
      }
    }
  }

}
