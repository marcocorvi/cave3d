/** @file Cave3Dview.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D surface view
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import android.view.MotionEvent;

import android.content.Context;

import android.graphics.*;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.widget.ZoomControls;
import android.widget.ZoomButton;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;

import android.util.DisplayMetrics;
import android.util.AttributeSet;
// import android.util.FloatMath;
import android.util.Log;

public class Cave3DView extends SurfaceView
                        implements SurfaceHolder.Callback
{
  static final int RADIUS1 = 40;
  static final int RADIUS2 = 40;
  private static final String TAG = "Cave3D";

  static final int MODE_MOVE = 0;
  static final int MODE_ZOOM = 1;
  private int mTouchMode = MODE_MOVE;
  private int mMode = Cave3D.MODE_ROTATE;
  private float oldDist;  // zoom pointer-sapcing
  private boolean mIsNotMultitouch;
  private Cave3DStation  mStartStation;

  void setMode( int mode ) { mMode = mode; }

  float spacing( WrapMotionEvent ev )
  {
    int np = ev.getPointerCount();
    if ( np < 2 ) return 0.0f;
    float x = ev.getX(1) - ev.getX(0);
    float y = ev.getY(1) - ev.getY(0);
    return (float)(Math.sqrt(x*x + y*y));
  }

  private Boolean _run;
  protected DrawThread thread;
  private Bitmap mBitmap;
  public boolean isDrawing = true;
  private SurfaceHolder mHolder; // canvas holder
  private Context mContext;
  private AttributeSet mAttrs;
  private int mWidth;            // canvas width
  private int mHeight;           // canvas height

  private Cave3DRenderer mRenderer;

  ZoomButtonsController mZoomBtnsCtrl;
  // View mZoomView;
  // ZoomControls mZoomCtrl;
  int mZoomY;

  public int width()  { return mWidth; }
  public int height() { return mHeight; }

  float x_save, y_save;
  float x_start, y_start;

  void startStationDistance( Cave3DStation station )
  {
    // Log.v("Cave3D", "set station for distance " + station.name );
    mStartStation = station;
  }

  void centerStation( Cave3DStation station )
  {
    float x = x_start - mWidth/2;
    float y = y_start - mHeight/2;
    mRenderer.centerAtStation( station );
    mRenderer.changeParams( x, y, Cave3D.MODE_TRANSLATE );
  }

  public Cave3DView(Context context, AttributeSet attrs )
  {
    super(context, attrs);
    mWidth = 0;
    mHeight = 0;
    mStartStation = null;

    thread = null;
    mContext = context;
    mAttrs   = attrs;

    mHolder = getHolder();
    mHolder.addCallback(this);

    DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
    // float density  = dm.density;
    float width  = dm.widthPixels;
    float height = dm.heightPixels;
    mRenderer = new Cave3DRenderer( width, height );
    x_save = 0.0f;
    y_save = 0.0f;
    x_start = 0.0f;
    y_start = 0.0f;

    mZoomBtnsCtrl = null;
    mZoomY = 0;
    mIsNotMultitouch = true;
  }

  public Cave3DRenderer getRenderer() { return mRenderer; }


  /* called by Cave3D to set the controls and the Y-distance */
  void setZoomControl( ZoomButtonsController zoom_ctrl, int y, boolean notmultitouch ) 
  {
     mZoomBtnsCtrl = zoom_ctrl;
     mZoomY = y;
     mIsNotMultitouch = notmultitouch;
  }

  private void changeZoom( float f ) 
  {
    mRenderer.changeZoom( f );
  }
  
// -------------------------------------------------------------------------
// Touch (drag) events
// -------------------------------------------------------------------------

  public boolean onTouchEvent(final MotionEvent rawEvent) 
  {
    if ( ! mRenderer.hasParser() ) return false;

    WrapMotionEvent event = WrapMotionEvent.wrap(rawEvent);
    // dumpEvent( event );

    float x_canvas = event.getX();
    float y_canvas = event.getY();
    if ( mIsNotMultitouch && y_canvas > mZoomY && mZoomBtnsCtrl != null ) {
      mZoomBtnsCtrl.setVisible( true );
      // mZoomCtrl.show( );
      return true;
    }

    int action = event.getAction() & MotionEvent.ACTION_MASK;
    // int actionCode = event.getAction() & MotionEvent.ACTION_MASK;

    if (action == MotionEvent.ACTION_POINTER_DOWN) {
      mTouchMode = MODE_ZOOM;
      oldDist = spacing( event );
    } else if ( action == MotionEvent.ACTION_POINTER_UP) {
      /* nothing */
    } else if ( action == MotionEvent.ACTION_DOWN ) {
      x_start = event.getX();
      y_start = event.getY();
    } else if ( event.getAction() == MotionEvent.ACTION_UP ) {
      if ( mTouchMode == MODE_ZOOM ) {
        mTouchMode = MODE_MOVE;
      } else {
        if ( Math.abs(x_start - event.getX()) < RADIUS1 && Math.abs(y_start - event.getY()) < RADIUS1 ) {
          Cave3DStation st = mRenderer.getStationAt( x_start, y_start );
          if ( st != null ) {
            // Log.v( TAG, "got station " + st.name + " start station " + mStartStation );
            if ( mStartStation != null ) {
              if ( mStartStation != st ) {
                // TODO find shortest cave-path between stations
                float cave_pathlength = mRenderer.computeCavePathlength( mStartStation, st );
                (new Cave3DStationDistanceDialog( mContext, st, mStartStation, cave_pathlength )).show();
                mStartStation = null;
              }
            } else {
              (new Cave3DStationDialog( mContext, this, st, mRenderer.getSurface() )).show();
            }
          }
        } else {
          final float dx = x_save - event.getX();
          final float dy = y_save - event.getY();
          if ( Math.abs(dx) < RADIUS2 && Math.abs(dy) < RADIUS2 ) {
            mRenderer.changeParams( dx, dy, mMode );
            // renderer.setColor( event.getX()/getWidth(), event.getY()/getHeight(), 1.0f );
          }
        }
      }
    } else if ( action == MotionEvent.ACTION_MOVE ) {
      if ( mTouchMode == MODE_ZOOM ) {
        float newDist = spacing( event );
        if ( newDist > 16.0f && oldDist > 16.0f ) {
          float factor = newDist/oldDist;
          if ( factor > 0.05f && factor < 4.0f ) {
            changeZoom( factor );
            oldDist = newDist;
          }
        }
        final float dx = x_save - event.getX();
        final float dy = y_save - event.getY();
        if ( Math.abs(dx) < RADIUS2 && Math.abs(dy) < RADIUS2 ) {
          mRenderer.changeParams( dx, dy, Cave3D.MODE_TRANSLATE );
          // renderer.setColor( event.getX()/getWidth(), event.getY()/getHeight(), 1.0f );
        }
      } else {
        final float dx = x_save - event.getX();
        final float dy = y_save - event.getY();
        mRenderer.changeParams( dx, dy, mMode );
        // renderer.setColor( event.getX()/getWidth(), event.getY()/getHeight(), 1.0f );
      }
    }
    x_save = event.getX();
    y_save = event.getY();
    return true;
  }

// -------------------------------------------------------------------------
// Handler stuff and drawing
// -------------------------------------------------------------------------

    void refresh()
    {
      Canvas canvas = null;
      try {
        canvas = mHolder.lockCanvas();
        if ( canvas != null ) {
          if ( mBitmap == null ) {
            mBitmap = Bitmap.createBitmap (1, 1, Bitmap.Config.ARGB_8888);
          }
          final Canvas c = new Canvas (mBitmap);
          mWidth  = c.getWidth();
          mHeight = c.getHeight();

          c.drawColor(0, PorterDuff.Mode.CLEAR);
          canvas.drawColor(0, PorterDuff.Mode.CLEAR);

          if ( mRenderer.computeProjection( c, previewDoneHandler ) ) {
            canvas.drawBitmap (mBitmap, 0,  0,null);
          }
        }
      } finally {
        if ( canvas != null ) {
          mHolder.unlockCanvasAndPost( canvas );
        }
      }
    }

    private Handler previewDoneHandler = new Handler()
    {
      @Override
      public void handleMessage(Message msg) {
        isDrawing = false;
      }
    };

    class DrawThread extends  Thread
    {
      private SurfaceHolder mSurfaceHolder;

      public DrawThread(SurfaceHolder surfaceHolder)
      {
          mSurfaceHolder = surfaceHolder;
      }

      public void setRunning(boolean run)
      {
        _run = run;
      }

      @Override
      public void run() 
      {
        while ( _run ) {
          if ( isDrawing == true ) {
            refresh();
            // Canvas canvas = null;
            // try{
            //   canvas = mSurfaceHolder.lockCanvas(null);
            //   if(mBitmap == null){
            //     mBitmap = Bitmap.createBitmap (1, 1, Bitmap.Config.ARGB_8888);
            //   }
            //   final Canvas c = new Canvas (mBitmap);
            //   mWidth  = c.getWidth();
            //   mHeight = c.getHeight();

            //   c.drawColor(0, PorterDuff.Mode.CLEAR);
            //   canvas.drawColor(0, PorterDuff.Mode.CLEAR);

            //   commandManager.executeAll(c,previewDoneHandler);
            //   previewPath.draw(c);
            //     
            //   canvas.drawBitmap (mBitmap, 0,  0,null);
            // } finally {
            //   mSurfaceHolder.unlockCanvasAndPost(canvas);
            // }
          }
        }
      }
    }

// -------------------------------------------------------------------------
// SurfaceHolder stuff
// -------------------------------------------------------------------------

    public void surfaceChanged(SurfaceHolder mHolder, int format, int width,  int height) 
    {
      // TODO Auto-generated method stub
      mBitmap =  Bitmap.createBitmap (width, height, Bitmap.Config.ARGB_8888);;
    }


    public void surfaceCreated(SurfaceHolder mHolder) 
    {
      // TODO Auto-generated method stub
      if (thread == null ) {
        thread = new DrawThread(mHolder);
      }
      thread.setRunning(true);
      thread.start();
    }

    public void surfaceDestroyed(SurfaceHolder mHolder) 
    {
      // TODO Auto-generated method stub
      boolean retry = true;
      thread.setRunning(false);
      while (retry) {
        try {
          thread.join();
          retry = false;
        } catch (InterruptedException e) {
          // we will try it again and again...
        }
      }
      thread = null;
    }

}
