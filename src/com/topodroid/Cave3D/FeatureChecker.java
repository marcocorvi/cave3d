/* @file FeatureChecker.java
 *
 * @author marco corvi
 * @date june 2017
 *
 * @brief Cave3D feature checker
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import android.os.Build;
// import android.os.Build.VERSION_CODES;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
// import android.content.pm.FeatureInfo;

class FeatureChecker
{
  /** permissions string codes
   */ 
  static final private String perms[] = {
      android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
      // android.Manifest.permission.READ_EXTERNAL_STORAGE,
      // android.Manifest.permission.INTERNET,
  };

  static final private int NR_PERMS_D = 1;
  static final private int NR_PERMS   = 1;

  /** app specific code - for callback in MainWindow
   */
  static final int REQUEST_PERMISSIONS = 1;

  private static boolean MustRestart = false; // whether need to restart app
  static boolean GrantedPermission[] = { false, false, false, false, false, false };

  static void createPermissions( Context context, Activity activity )
  {
    // Log.v( "Cave3D-PERM", "create permissions" );
    MustRestart = false;
    // FIXME-23
    if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.M ) return;
    // FIXME-16 // nothing

    for ( int k=0; k<NR_PERMS; ++k ) { // check whether the app has the six permissions
      // FIXME-23
      GrantedPermission[k] = ( context.checkSelfPermission( perms[k] ) == PackageManager.PERMISSION_GRANTED );
      // FIXME-16 GrantedPermission[k] = true;
      // Log.v("Cave3D-PERM", "FC perm " + k + " granted " + GrantedPermission[k] );
      if ( ! GrantedPermission[k] ) MustRestart = true;
    }
    if ( MustRestart ) { // if a permission has not been granted request it
      // Log.v("Cave3D-PERM", "FC must restart now");
      /* FIXME-23 */
      activity.requestPermissions( perms, REQUEST_PERMISSIONS );
      android.os.Process.killProcess( android.os.Process.myPid() );
      System.exit( 1 );
      /* */
    }
  }

  /** check whether the running app has the needed permissions
   * @return 0 ok
   *         -1 missing some necessary permission
   *         >0 missing some complementary permssion (flag): not used
   */
  static int checkPermissions( Context context )
  {
    int k;
    for ( k=0; k<NR_PERMS_D; ++k ) {
      int res = context.checkCallingOrSelfPermission( perms[k] );
      if ( res != PackageManager.PERMISSION_GRANTED ) {
        // TDToast.make( mActivity, "Cave3D must have " + perms[k] );
        // Log.v( "Cave3D-PERM", "check permission: not granted" );
	return -1;
      }
    }
    int ret = 0;
    int flag = 1;
    for ( ; k<NR_PERMS; ++k ) {
      int res = context.checkCallingOrSelfPermission( perms[k] );
      if ( res != PackageManager.PERMISSION_GRANTED ) {
        // TDToast.make( mActivity, "Cave3D may need " + perms[k] );
	ret += flag;
      }
      flag *= 2;
    }
    // Log.v( "Cave3D-PERM", "check permission: return " + ret );
    return ret;
  }

  // static boolean checkMultitouch( Context context )
  // {
  //   // Log.v( Cave3D-PERM, "check multitouch" );
  //   return context.getPackageManager().hasSystemFeature( PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH );
  // }

  // static boolean checkInternet( Context context )
  // {
  //   // Log.v( Cave3D-PERM, "check internet" );
  //   return ( context.checkCallingOrSelfPermission( android.Manifest.permission.INTERNET ) == PackageManager.PERMISSION_GRANTED );
  // }
}
