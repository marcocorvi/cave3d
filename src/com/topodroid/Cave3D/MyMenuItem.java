/** @file MyMenuItem.java
 *
 * @author marco corvi
 * @date 
 *
 * @brief TopoDroid drawing
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import java.util.ArrayList;

import android.content.Context;
// import android.app.Dialog;

import android.graphics.Color;
import android.graphics.Rect;

import android.widget.TextView;
import android.widget.Button;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
// import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
// import android.text.Layout;
import android.widget.LinearLayout;

// import android.util.Log;

class MyMenuItem extends TextView
{
  // private static final String TAG = "Cave3D MENU";

  // TextView     mTextView = null;
  // LinearLayout mView;
  OnClickListener mListener;
  float mX, mY;
  boolean  mTouch;

  // private Context mContext;

  public MyMenuItem( Context context, OnClickListener listener, String text )
  {  
    super( context );
    setTextColor( 0xff33ccff );
    setBackgroundColor( 0xff333333 );
    setTextSize( 16 );
    setText( text );
    setPadding( 10, 10, 5, 10 );
    mListener = listener;
    mTouch = false;

    // setOnClickListener( listener );
  }

  void resetBgColor() { setBackgroundColor( 0xff333333 ); }

  void setListener( OnClickListener listener ) { mListener = listener; }

  @Override
  public boolean onTouchEvent( MotionEvent ev )
  {
    int action = ev.getAction();
    if ( action == MotionEvent.ACTION_DOWN ) {
      mX = ev.getX();
      mY = ev.getY();
      mTouch = true;
      setBackgroundColor( 0xccff9900 );
      return true;
    } else if ( action == MotionEvent.ACTION_MOVE && mTouch ) {
      if ( mX - ev.getX() > 10 || Math.abs( mY - ev.getY() ) > 5 ) {
        mTouch = false;
        setBackgroundColor( 0xff333333 );
      }
      // Log.v( TAG, "XY " + ev.getX() + " " + ev.getY() );
    } else if ( action == MotionEvent.ACTION_UP ) {
      setBackgroundColor( 0xff333333 );
      if ( mTouch ) mListener.onClick( this );
    }
    return false;
  }

}

