/** @file Cave3DSegmentList.java
 *
 * @author marco corvi
 * @date may 2017
 *
 * @brief 3D segment list
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

class Cave3DSegmentList
{
  Cave3DSegment head;
  int size;

  Cave3DSegmentList( )
  {
    head = null;
    size = 0;
  }

  Cave3DSegmentList( Cave3DSegment s )
  {
    head = s;
    size = 1;
  }
/*
  void mergeIn( Cave3DSegmentList ll ) 
  {
    Cave3DSegment ss = ll.head;
    while ( ss.next != null ) ss = ss.next;
    ss.next = head;
    head    = ll.head;
    size += ll.size;
    ll.head = null;
    ll.size = 0;
  }

  void add( Cave3DSegment sgm )
  {
    sgm.next = head;
    head = sgm;
    ++ size;
  }
*/
 
  // insert a segment keeping the list ordered by increasing s
  void insert( Cave3DSegment sgm ) 
  {
    if ( head == null ) {
      sgm.next = null;
      head = sgm;
    } else if ( head.s() > sgm.s() ) {
      sgm.next = head;
      head = sgm;
    } else {
      Cave3DSegment s2 = head;
      while ( s2.next != null && s2.next.s() < sgm.s() ) s2 = s2.next;
      sgm.next = s2.next;
      s2.next  = sgm;
    }
    ++ size;
  }

  float centerZ()
  {
    float ret = 0;
    for ( Cave3DSegment s = head; s != null; s = s.next ) {
      ret += s.v1.z;
      ret += s.v2.z;
    }
    return ret/( 2 * size );
  }

  float minZ()
  {
    float ret = 0;
    for ( Cave3DSegment s = head; s != null; s = s.next ) {
      if ( s.v1.z < ret ) ret = s.v1.z;
      if ( s.v2.z < ret ) ret = s.v2.z;
    }
    return ret;
  }

  float maxZ()
  {
    float ret = 0;
    for ( Cave3DSegment s = head; s != null; s = s.next ) {
      if ( s.v1.z > ret ) ret = s.v1.z;
      if ( s.v2.z > ret ) ret = s.v2.z;
    }
    return ret;
  }

}
