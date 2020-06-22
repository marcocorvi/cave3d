/* @file Tiff.h
 *
 * @brief GeoTiff subimage reader
 * ------------------------------------------------------------------
 * This file is a modification of the original powercrust file
 * marco corvi - may 2017
 *
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * ------------------------------------------------------------------
 */
// #include <iostream>
#ifndef TIFF_HH
#define TIFF_HH

#include "t4.h"
#include "tiffio.h"

#ifdef __cplusplus
extern "C" {
#endif

// public:
int getImageWidth();
int getImageHeight();

// private:
typedef struct Tiff 
{
  TIFF * tif;

  uint32 mWidth;
  uint32 mHeight;
  uint32 mDepth;
  
  uint16 mBps; // bits per sample
  uint16 mSpp; // sample per pixel
  uint16 mBpp; // bytes per pixel
  
  uint32 mSLSize;
  uint32 mRSLSize;
  
  uint16 mPlanar;
  uint16 mCompression;
  uint16 mPhotometric;
  uint16 mOrientation;
  
  uint32 mStripRows;
  
  uint32 mTileWidth;
  uint32 mTileHeight;
  
  uint16 mDirectoryKey;
  
  uint32 * mImage;

  double mXpos,  mYpos;
  double mXcell, mYcell;

  // used in read scanline with palette (colormap)
  unsigned char * mPaletteRed;
  unsigned char * mPaletteGreen;
  unsigned char * mPaletteBlue;

} TiffStruct;


unsigned char * getSubImage( const char *filename, double x1, double y1, double x2, double y2 );

    // void printTags()
    // {
    //   std::cout << "Size " << mWidth << "x" << mHeight << " Depth " << mDepth << "\n";
    //   std::cout << "Bits/sample " << mBps << " sample/pixel " << mSpp << " Byte/pixel " << mBpp << "\n";
    //   std::cout << "Compr. " << mCompression << " Photom. " << mPhotometric << "\n";
    //   std::cout << "PLanar " << mPlanar << " Orient. " << mOrientation << "\n";
    //   std::cout << "Rows/strip " << mStripRows << "\n";
    //   std::cout << "Tile " << mTileWidth << "x" << mTileHeight << "\n";
    //   // std::cout << "DirKey " << mDirectoryKey << "\n";
    // }

void imageRead( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
                unsigned char * img, uint32 xstart, uint32 ystart, uint32 width );
void imageRead2( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
                unsigned char * img, uint32 xstart, uint32 ystart, uint32 width );
void scanlineRead( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
                unsigned char * img, uint32 xstart, uint32 ystart, uint32 width );
void stripRead( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
                unsigned char * img, uint32 xstart, uint32 ystart, uint32 width );
void tileRead( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
                unsigned char * img, uint32 xstart, uint32 ystart, uint32 width );
void tileRead2( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
                unsigned char * img, uint32 xstart, uint32 ystart, uint32 width );

// uint32 getLast( uint32 off, uint32 size, uint32 max );
double * getTagsDouble( TIFF * tif, int tag, uint32 * count );

uint16 getTagUInt16( TIFF * tif, int tag );
uint32 getTagUInt32( TIFF * tif, int tag );
// double getTagDouble( TIFF * tif, int tag );

void closeTiff( TiffStruct * tiff );
TiffStruct * openTiff( const char * filename );
   
void copyImage( TiffStruct * tiff, unsigned char * ret, uint32 xstart, uint32 ystart, uint32 width,
                unsigned char * src, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend );
void copyScanline( TiffStruct * tiff, unsigned char * ret, uint32 start, unsigned char * buf, uint32 xoff, uint32 ww );
void copyScanPlane( TiffStruct * tiff, unsigned char * ret, uint32 start, unsigned char * buf, uint32 xoff, uint32 ww, uint16 plane );
void copyStrip( TiffStruct * tiff, unsigned char * ret, uint32 xstart, uint32 ystart, uint32 width,
                unsigned char * buf, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend, uint32 y0 );
void copyTile( TiffStruct * tiff, unsigned char * ret, uint32 x1, uint32 y1, uint32 width,
               unsigned char * tile, uint32 x2, uint32 y2, uint32 nxx, uint32 nyy );
void copyTilePlane( TiffStruct * tiff, unsigned char * ret, uint32 x1, uint32 y1, uint32 width,
                    unsigned char * tile, uint32 x2, uint32 y2, uint32 nxx, uint32 nyy, uint16 s );

int readPalette( TiffStruct * tiff );
void releasePalette( TiffStruct * tiff );
void resetPalette( TiffStruct * tiff );

#ifdef __cplusplus
}
#endif


#endif
