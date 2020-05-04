/** @file Cave3DSite.java
 *
 *e @author marco corvi
 * @date may 2017 
 *
 * @brief Cave3D 3D wall site
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

class Cave3DSite extends Vector3D
{
  Angle angle; // head of list of sites
  Cave3DPolygon poly;

  class Angle // link between two sites
  {
    Cave3DSite v1;
    Cave3DSite v2;
    Angle next;
    Angle prev;

    Angle( Cave3DSite w1, Cave3DSite w2 )
    {
      v1 = w1;
      v2 = w2;
      next = prev = null; 
    }
  }

  Cave3DSite( float x, float y, float z )
  {
    super( x, y, z );
    angle = null;
    poly  = null;
  }

  void insertAngle( Cave3DSite w1, Cave3DSite w2 )
  {
    boolean done = false;
    for ( Angle a = angle; a != null; a = a.next ) {
      if ( a.v2 == w1 ) { // ... -p- v1 -a- w1 -n- ...
        done = true;
        a.v2 = w2;        // ... -p- v1 -a- w2  ||    w1 -n- ...
        for ( Angle b = angle; b != null; b = b.next ) {
          if ( b == a ) continue;
          if ( a.v2 == b.v1 ) { // ... -p- v1 -a- w2 -b- v2 -N- ...
            a.v2 = b.v2;        // ... -p- v1 -a- v2 - ...
            if ( b.next != null ) b.next.prev = a.prev;
            if ( b.prev != null ) b.prev.next = b.next; else angle = b.next;
            b.next = b.prev = null;
            break;
          }
        }
        break;
      }
      if ( a.v1 == w2 ) {
        done = true;
        a.v1 = w1;
        for ( Angle b = angle; b != null; b = b.next ) {
          if ( b == a ) continue;
          if ( a.v1 == b.v2 ) {
            a.v1 = b.v1;
            if ( b.next != null ) b.next.prev = a.prev;
            if ( b.prev != null ) b.prev.next = b.next; else angle = b.next;
            b.next = b.prev = null;
            break;
          }
        }
        break;
      }
    }
    if ( ! done ) {
      Angle a = new Angle( w1, w2 );
      a.next = angle;
      if ( angle != null ) angle.prev = a;
      angle = a;
    }
  }

  boolean isOpen( ) 
  {
    if ( angle == null ) return false;
    if ( angle.next != null ) {
      Log.e( "TopoGL-SITE", "site with more than one angle");
      Angle n = angle.next;
      while ( n.next != null ) n = n.next;
      return n.v2 != angle.v1;
    }
    return ( angle.v1 != angle.v2 );
  }

}
