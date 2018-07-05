/** @file Cave3DIcoDialog.java
 *
 * @author marco corvi
 * @date jan 2012
 *
 * @brief Cave3D Ico dialog
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

// import java.util.List;
import java.util.Locale;

import java.io.StringWriter;
import java.io.PrintWriter;


import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import android.app.Dialog;
import android.os.Bundle;

// import android.widget.Toast;

import android.content.Context;

import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View.OnTouchListener;
import android.view.View;
import android.view.MotionEvent;

// import android.util.Log;
import android.util.DisplayMetrics;

public class Cave3DIcoDialog extends Dialog
                             implements OnTouchListener
{
  private static int SIDE  = 180;
  private static int CX = SIDE/2;
  private static int CY = SIDE/2;
  private static int RADIUS = SIDE/2 - 10;

  int mNr;
  private double mTheta, mPhi;

  private ImageView mImage;
  private TextView mText;
  private Context mContext;
  private IcoDiagram diagram;
  private double mMax;

  double n1x, n1y, n1z;
  double n2x, n2y, n2z;
  double n3x, n3y, n3z;

  public Cave3DIcoDialog( Context context, Cave3DRenderer renderer )
  {
    super( context );
    mContext = context;

    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    // float density  = dm.density;
    // mDisplayWidth  = dm.widthPixels;
    // mDisplayHeight = dm.heightPixels;
    SIDE = dm.widthPixels;
    CX = SIDE/2;
    CY = SIDE/2;
    RADIUS = (SIDE - 20)/2;

    mTheta  = 0.0;
    mPhi    = 0.0;
    prepareIcoDiagram( renderer );
  }

  private void computeNVectors( )
  {
    double ct = Math.cos(mTheta);
    double st = Math.sin(mTheta);
    double cp = Math.cos(mPhi);
    double sp = Math.sin(mPhi);

    n1x =  cp;    n1y = -sp;    n1z = 0;
    n2x = -ct*sp; n2y = -ct*cp; n2z = st;
    n3x = st*sp;  n3y = st*cp;  n3z = ct;

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter( sw );
    pw.format(Locale.US, "Clino %.0f Azimuth %.0f",
      90 - mTheta*180/Math.PI, 
      mPhi*180/Math.PI );
    mText.setText( sw.getBuffer().toString() );
  }

  private void render()
  { 
    Bitmap bitmap = Bitmap.createBitmap( SIDE, SIDE, Bitmap.Config.ARGB_8888 );
    mImage.setImageBitmap( bitmap );
    computeNVectors();
    evalIcoDiagram( new Canvas( bitmap ) );
  }

  @Override
  public void onCreate( Bundle bundle )
  {
    super.onCreate( bundle );
    requestWindowFeature( Window.FEATURE_NO_TITLE );
    setContentView( R.layout.diagram );

    mText  = (TextView) findViewById( R.id.viewpoint );
    mImage = (ImageView) findViewById( R.id.image );
    mImage.setOnTouchListener( this );
    render();
  }


  private void prepareIcoDiagram( Cave3DRenderer renderer )
  { 
    mNr = 8;
    diagram = new IcoDiagram( mNr );
    double eps = diagram.mEps;
    int ns = renderer.getNrShots();
    diagram.reset();
    for (int k=0; k<ns; ++k ) {
      Cave3DShot shot = renderer.getShot( k );
      diagram.add( shot.len, shot.ber, shot.cln, eps ); // angles in radians
    }
    mMax = diagram.maxValue();
    // Log.v( "Cave3D", "eps " + eps + " max " + mMax );
  }

  private void evalIcoDiagram( Canvas canvas )
  { 
    for ( int k=0; k<diagram.mPointNr; ++k ) {
      IcoPoint p = diagram.getDirection( k );
      
      double x = (n1x * p.x + n1y * p.y + n1z * p.z)/IcoPoint.R;
      double y = (n2x * p.x + n2y * p.y + n2z * p.z)/IcoPoint.R;

      double v = diagram.mValue[k] / mMax;
      float dx = (float)(  v * RADIUS * x);
      float dy = (float)(- v * RADIUS * y);
      int col = (int)(0xff * v);
      // Log.v( "Cave3D", " path to " + dx + " " + dy );
      Path path = new Path();
      path.moveTo( CX, CY );
      path.lineTo( CX + dx + dy/20, CY + dy - dx/20 );
      path.lineTo( CX + dx - dy/20, CY + dy + dx/20 );
      path.close();
      Paint paint = new Paint();
      paint.setARGB( 0x99, col, col, col );
      paint.setStyle( Paint.Style.FILL );
      paint.setStrokeWidth( 2 );
      canvas.drawPath( path, paint );
    } 
  }

  double mSaveX, mSaveY;

  @Override
  public boolean onTouch( View v, MotionEvent e )
  {
    if ( e.getAction() == MotionEvent.ACTION_DOWN ) {
      mSaveX = e.getX();
      mSaveY = e.getY();
      return true;
    } else if ( e.getAction() == MotionEvent.ACTION_MOVE ) {
      return true;
    } else if ( e.getAction() == MotionEvent.ACTION_UP ) {
      double x = (e.getX() - mSaveX)/SIDE;
      double y = (e.getY() - mSaveY)/SIDE;
      changeThetaPhi( x, y );
      render();
      return true;
    }
    return false;
  }

  private void changeThetaPhi( double x, double y )
  {
    mTheta += y;
    if ( mTheta < 0 ) mTheta = 0;
    if ( mTheta > Math.PI ) mTheta = Math.PI;
    mPhi += x;
    if ( mPhi < 0 ) mPhi += 2*Math.PI;
    if ( mPhi > 2*Math.PI ) mPhi -= 2*Math.PI;
  }
   
}
