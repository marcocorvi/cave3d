/* @file DataQueue.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief BRIC4 packet-buffer queue
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.bt;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class DataQueue
{
  final Lock mLock = new ReentrantLock();
  final Condition notEmpty = mLock.newCondition();

  DataBuffer mHead = null;
  DataBuffer mTail = null;
  public int size = 0;

  public void put( DataBuffer buffer )
  {
    mLock.lock();
    try {
      if ( mTail == null ) {
        mHead = buffer;
        mTail = mHead;
        notEmpty.signal();
      } else {
        mTail.next = buffer;
      }
      ++ size;
    } finally {
      mLock.unlock();
    }
  }

  public DataBuffer get()
  {
    mLock.lock();
    try {
      while ( mHead == null ) {
        try {
          notEmpty.await();
        } catch ( InterruptedException e ) { }
      }
      DataBuffer ret = mHead;
      mHead = mHead.next;
      if ( mHead == null ) mTail = null;
      // ret.next = null;
      -- size;
      return ret;
    } finally {
      mLock.unlock();
    }
  }

} 
