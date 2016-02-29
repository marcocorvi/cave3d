/* @file LoxBitmap.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D loch bitmap
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

class LoxBitmap
{
  int sid; // surface
  int type;
  int size;
  double calib[];
  double det; // calib det
  byte[] data;
  int data_offset;

  int width;
  int height;
  byte[] red;
  byte[] green;
  byte[] blue;

  LoxBitmap( int _sid, int tp, int sz, double[] c, byte[] d, int d_off )
  {
    sid = _sid;
    type = tp;
    size = sz;
    data = d;
    data_offset = d_off;
    width  = 0;
    height = 0;
    red   = null;
    green = null;
    blue  = null;
    calib = new double[6];
    for ( int k=0; k<6; ++k ) calib[k]= c[k];

    det = calib[2] * calib[5] - calib[3] * calib[4];
    Data2RGB();
    // LOGI("Bitmap calib %.2f %.2f %.2f   %.2f %.2f %.2f", c[0], c[2], c[3], c[1], c[4], c[5] );
  }

  int Surface() { return sid; }
  int Type() { return type; }
  double Calib( int k ) { return calib[k]; }


  private double ENtoI( double e, double n )
  {
    e -= calib[0];
    n -= calib[1];
    return ( calib[5] * e - calib[3] * n )/det;
  }

  private double ENtoJ( double e, double n )
  {
    e -= calib[0];
    n -= calib[1];
    return ( calib[2] * n - calib[4] * e )/det;
  }

  int DataSize() { return size; }
  byte[] Data()  { return data; }
  int DataOffset() { return data_offset; }


// #include "Image_PNG.h"
// #include "Image_JPG.h"

  void GetRGB( float e, float n, float[] rgb )
  {
    double id = ENtoI( e, n );
    double jd = ENtoJ( e, n );
    int i = (int)id;
    int j = (int)jd;
    rgb[0] = rgb[1] = rgb[2] = 1.0f;
    if ( i < 0 || i >= width || j < 0 || j >= height ) return;
    rgb[0] = red[ j*width+i ] / 255.0f;
    rgb[1] = green[ j*width+i ] / 255.0f;
    rgb[2] = blue[ j*width+i ] / 255.0f;
  }

  private boolean isPNG( byte[] data, int off )
  {
    return data[0+off] == 0x89 
        && data[1+off] == 0x50
        && data[2+off] == 0x4e
        && data[3+off] == 0x47;
  }
  
  private boolean isJPG( byte[] data, int off ) 
  {
    return data[4+off] == 0x00 
        && data[5+off] == 0x10
        && data[6+off] == 0x4a
        && data[7+off] == 0x46;
  }

  void Data2RGB()
  {
    // Image img = NULL;
    // if ( isJPG( data ) ) {
    //   img = new Image_JPG();
    // } else if ( isPNG( data ) ) {
    //   img = new Image_PNG();
    // } else {
    //   LOGW("Unexpected image type %d", type );
    // }
    // if ( img != NULL && img.open( data, size ) ) {
    //   width  = img->width();
    //   height = img->height();
    //   // LOGI("Lox bitmap image %dx%d stride %d BPP %d", width, height, img->stride(), img->BPP() );
    //   byte[] image = img.image();
    //   red   = new byte[ width * height ];
    //   green = new byte[ width * height ];
    //   blue  = new byte[ width * height ];
    //   for ( int j=0; j<height; ++j ) {
    //     for ( int i=0; i<width; ++i ) {
    //       red[ j*width + i ]   = image[ 3*(j*width+i) + 0 ];
    //       green[ j*width + i ] = image[ 3*(j*width+i) + 1 ];
    //       blue[ j*width + i ]  = image[ 3*(j*width+i) + 2 ];
    //     }
    //   }
    // } else {
    //   // LOGW("LoxBitmap failed to read image data");
    // }
    // // LOGI("Lox bitmap %dx%d ", width, height );
  }      

}

