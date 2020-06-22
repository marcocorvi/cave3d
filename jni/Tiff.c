/* @file Tiff.c
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

#include <stdlib.h>
#include <assert.h>
#include <string.h>

// #include <iostream>
// #include <string>

#include "Tiff.h"

#define GEOKEY_DIRECTORYKEY 34735

#define GEOKEY_CELLSIZE  33550 // pixel scale (3 doubles)
#define GEOKEY_POSITION  33922 // tie-point XZY_image XYZ_world
#define GEOKEY_PROJINFO  34735 // projection info
#define GEOKEY_CRSCOEFFS 34736
#define GEOKEY_CRSNAME   34737
#define GEOKEY_GDAL      42112

#define BPP 3 // RGB

uint32 mImageWidth  = 0;
uint32 mImageHeight = 0;

int getImageWidth()  { return (int)mImageWidth; }
int getImageHeight() { return (int)mImageHeight; }

TiffStruct *
openTiff(const char * filename)
{
  TIFF * tif = TIFFOpen( filename, "r" );
  if ( tif == 0 ) {
    // std::cout << "failed open tiff file \n";
    return NULL;
  }
  // std::cout << "opened tiff file " << filename << "\n";
  // TIFFPrintDirectory( tif, stdout, 0 );
  TiffStruct * tiff = (TiffStruct *) malloc( sizeof(TiffStruct) );

  tiff->tif = tif;
  tiff->mDepth  = getTagUInt32( tif, TIFFTAG_IMAGEDEPTH );
  tiff->mHeight = getTagUInt32( tif, TIFFTAG_IMAGELENGTH );
  tiff->mWidth  = getTagUInt32( tif, TIFFTAG_IMAGEWIDTH );
  tiff->mBps    = getTagUInt16( tif, TIFFTAG_BITSPERSAMPLE );
  tiff->mSpp    = getTagUInt16( tif, TIFFTAG_SAMPLESPERPIXEL );
  tiff->mBpp = ( tiff->mBps * tiff->mSpp )/8;

  tiff->mCompression = getTagUInt16( tif, TIFFTAG_COMPRESSION );
  tiff->mPhotometric = getTagUInt16( tif, TIFFTAG_PHOTOMETRIC );
  tiff->mOrientation = getTagUInt16( tif, TIFFTAG_ORIENTATION );

  tiff->mStripRows   = getTagUInt32( tif, TIFFTAG_ROWSPERSTRIP );
  tiff->mPlanar      = getTagUInt16( tif, TIFFTAG_PLANARCONFIG );
  tiff->mTileWidth   = getTagUInt32( tif, TIFFTAG_TILEWIDTH );
  tiff->mTileHeight  = getTagUInt32( tif, TIFFTAG_TILELENGTH );

  tiff->mSLSize  = TIFFScanlineSize( tif );
  tiff->mRSLSize = TIFFRasterScanlineSize( tif );

  // uint16 * dirkey;
  // int ret = TIFFGetFieldDefaulted(tif, GEOKEY_DIRECTORYKEY, &dirkey, NULL ); 
  // if ( dirkey == NULL ) {
  //   std::cout << "no directory key \n";
  // } else {
  //   std::cout << "dir key " << ret << ": " << dirkey[0] << " " << dirkey[1] << " " << dirkey[2] << " " << dirkey[3] << "\n";
  // }

  uint32 count;
  double * cellsize = getTagsDouble( tif, GEOKEY_CELLSIZE, &count ); 
  tiff->mXcell= cellsize[0];
  tiff->mYcell= cellsize[1];
  _TIFFfree( cellsize );

  double * position = getTagsDouble( tif, GEOKEY_POSITION, &count );
  double ximg = position[0];
  double yimg = position[1];
  tiff->mXpos = position[3] - ximg * tiff->mXcell; // top-left corner
  tiff->mYpos = position[4] + yimg * tiff->mYcell;
  _TIFFfree( position );

  // printTags();
  return tiff;
}

// get a subimage of the TIFF image
// (x1, y1) (x2, y2) bounds of the subimage in world coords
// (Xpos, Ypos) upper-left corner world-coords
// (Xcell, Ycell) pixel size in the world
// (Xpos_mWidth*Xcell, Ypos+mHeight*Ycell) lower-right corner in world coords
//
// (Xoff, Yoff) upper-left corner offset in source image
// (Xstart, Ystrat) upper-left offset in target image
//
//
//         Xstart (Xoff=0)
//   +-----+--------+
//   |              |
//   +     +--------+----- Ystart (Yoff=0)
//   |     |........|
//   +-----+--------+ Yend
//         |        Xend
//
//         Xstart (Xoff=0)
//         +------------------
//         |
//   +-----+-------+ Yoff (Ystart=0)
//   |     |.......|
//   |     |.......|
//   +-----+-------+ Yend
//         |       Xend
//
// (x1,y1) lower-left corner
// (x2,y2) upper-right corner
unsigned char *
getSubImage( const char * filename, double x1, double y1, double x2, double y2 )
{
  TiffStruct * tiff = openTiff( filename );
  mImageWidth  = 0;
  mImageHeight = 0;
  if ( tiff == NULL ) return NULL;

  // mXpos left
  // mYpos top
  uint32 mXend = (uint32)( tiff->mXpos + tiff->mXcell * tiff->mWidth );  // right
  uint32 mYend = (uint32)( tiff->mYpos - tiff->mYcell * tiff->mHeight ); // bottom

  // std::cout << "X " << mXpos << " " << mXend << "\n";
  // std::cout << "Y " << mYend << " " << mYpos << "\n";

  uint32 xoff = (uint32)( (x1 > tiff->mXpos)? (x1 - tiff->mXpos)/tiff->mXcell : 0 ); // offset in source image
  uint32 yoff = (uint32)( (y2 < tiff->mYpos)? (tiff->mYpos - y2)/tiff->mYcell : 0 );

  uint32 xstart = (uint32)( (x1 > tiff->mXpos)? 0 : (tiff->mXpos - x1)/tiff->mXcell ); // start in target image
  uint32 ystart = (uint32)( (y2 < tiff->mYpos)? 0 : (y2 - tiff->mYpos)/tiff->mYcell );
  
  // end in source
  uint32 xend = (uint32)( (x2 > mXend)? tiff->mWidth  : (x2 > tiff->mXpos)? (x2 - tiff->mXpos)/tiff->mXcell : 0 ); 
  uint32 yend = (uint32)( (y1 < mYend)? tiff->mHeight : (mYend < y1)? (tiff->mYpos - y1)/tiff->mYcell : 0 );

  // std::cout << "X " << xoff << " " << xend << " start " << xstart << "\n";
  // std::cout << "Y " << yoff << " " << yend << " start " << ystart << "\n";

  if ( xend == 0 || yend == 0 ) return NULL;

  mImageWidth  = xend - xoff;
  mImageHeight = yend - yoff;
  uint32 size = BPP * mImageWidth * mImageHeight;

  if ( size == 0 ) return NULL;

  unsigned char * ret = (unsigned char *)malloc( size ); // BPP bytes per pixel
  memset( ret, 0xff, size ); // init white

  resetPalette( tiff );
  if ( tiff->mCompression == 1 && tiff->mPhotometric == 2 ) {
    if ( tiff->mStripRows == 1 ) {
      scanlineRead( tiff, xoff, yoff, xend, yend, ret, xstart, ystart, mImageWidth );
    } else {
      stripRead( tiff, xoff, yoff, xend, yend, ret, xstart, ystart, mImageWidth );
    }
  } else if ( tiff->mPhotometric == 3 && readPalette( tiff ) == 1 ) {
    if ( tiff->mStripRows == 1 ) {
      scanlineRead( tiff, xoff, yoff, xend, yend, ret, xstart, ystart, mImageWidth );
    } else {
      stripRead( tiff, xoff, yoff, xend, yend, ret, xstart, ystart, mImageWidth );
    }
    releasePalette( tiff );
  } else {
    imageRead( tiff, xoff, yoff, xend, yend, ret, xstart, ystart, mImageWidth );
  }

  // uint32 linewidth = (mWidth * mBps + 7 ) / 8; // 8 bits per byte
  // assert( linewidth <= mSLSize );
  // uint32 size = mHeight * linewidth;
  // size = size;

  // printf( "Image linewidth %d (%d %d) size %d \n", linewidth, mWidth, mBps, size );
  closeTiff( tiff );

  return ret;
}

int
readPalette( TiffStruct * tiff )
{
  uint16 * red;
  uint16 * green;
  uint16 * blue;
  // tiff->mPaletteRed   = NULL; // not necessary
  // tiff->mPaletteGreen = NULL;
  // tiff->mPaletteBlue  = NULL;
  if ( TIFFGetField( tiff->tif, TIFFTAG_COLORMAP, &red, &green, &blue ) ) {
    uint16 nColors = 1U << tiff->mBps;
    tiff->mPaletteRed   = (unsigned char *)malloc( nColors * sizeof(unsigned char) );
    tiff->mPaletteGreen = (unsigned char *)malloc( nColors * sizeof(unsigned char) );
    tiff->mPaletteBlue  = (unsigned char *)malloc( nColors * sizeof(unsigned char) );
    for ( uint16 k = 0; k < nColors; ++k ) {
      tiff->mPaletteRed[k]   = (unsigned char)( red[k]   >> 8 );
      tiff->mPaletteGreen[k] = (unsigned char)( green[k] >> 8 );
      tiff->mPaletteBlue[k]  = (unsigned char)( blue[k]  >> 8 );
    }
    return 1;
  }
  return 0;
}

void
releasePalette( TiffStruct * tiff )
{
  if ( tiff->mPaletteRed   != NULL ) free( tiff->mPaletteRed );
  if ( tiff->mPaletteGreen != NULL ) free( tiff->mPaletteGreen );
  if ( tiff->mPaletteBlue  != NULL ) free( tiff->mPaletteBlue );
}

void
resetPalette( TiffStruct * tiff )
{
  tiff->mPaletteRed   = NULL;
  tiff->mPaletteGreen = NULL;
  tiff->mPaletteBlue  = NULL;
}



// -----------------------------------------------------------------
// to use TIFFReadRGBAImage
// this is the worst way to read a big TIFF file
//
// the raster image is organized raster[y*width + x] with origin at lower-left corner
// tested with RGB input and RGB output
      
void
copyImage( TiffStruct * tiff, unsigned char * ret, uint32 xstart, uint32 ystart, uint32 width,
                 unsigned char * src, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend )
{
  for (uint32 j=yoff; j<yend; ++j) {
    uint32 j1 = ((ystart + (j - yoff))*width + xstart ) * BPP;
    uint32 j2 = (tiff->mHeight - 1 - j) * tiff->mWidth * 4; // RGBA
    // uint32 j2 = j * mWidth * 4; // RGBA
    for (uint32 i=xoff; i<xend; ++i) {
      for ( uint16 s = 0; s < BPP; ++s ) {
        ret[ j1 + s] = src[ j2 + s ];
      }
      j1 += BPP;
      j2 += 4;
    }
  }
}

void
imageRead( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
                 unsigned char * ret, uint32 xstart, uint32 ystart, uint32 width )
{
  uint32 npixels = tiff->mWidth * tiff->mHeight;
  uint32 * raster = (uint32*) _TIFFmalloc(npixels * sizeof (uint32));
  if (raster != NULL) {
    TIFFReadRGBAImage( tiff->tif, tiff->mWidth, tiff->mHeight, raster, 0 );
    copyImage( tiff, ret, xstart, ystart, width, (unsigned char *)raster, xoff, yoff, xend, yend );
  }
  _TIFFfree(raster);
}

void 
imageRead2( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
                  unsigned char * ret, uint32 xstart, uint32 ystart, uint32 width )
{
  TIFFRGBAImage img;
  char emsg[1024];
  if (TIFFRGBAImageBegin(&img, tiff->tif, 0, emsg)) {
    size_t npixels = img.width * img.height;
    uint32 * raster = (uint32*) _TIFFmalloc(npixels * sizeof (uint32));
    if (raster != NULL) {
      if (TIFFRGBAImageGet(&img, raster, img.width, img.height)) {
        copyImage( tiff, ret, xstart, ystart, width, (unsigned char *)raster, xoff, yoff, xend, yend );
      }
      _TIFFfree(raster);
    }
    TIFFRGBAImageEnd(&img);
  } else {
    // TIFFError(argv[1], emsg);
  }
}

// -----------------------------------------------------------------
// if the file is organized by lines this is OK
// this has been testes with input RGB, output RGB

void
copyScanline( TiffStruct * tiff, unsigned char * ret, uint32 start, unsigned char * buf, uint32 xoff, uint32 ww )
{
  if ( tiff->mPaletteRed != NULL ) {
    for ( uint32 i = 0; i < ww; ++i ) {
      uint32 x1 = BPP*(start+i);
      uint32 x2 = buf[ xoff+i ];
      ret[x1++] = tiff->mPaletteRed[ x2 ];
      ret[x1++] = tiff->mPaletteGreen[ x2 ];
      ret[x1  ] = tiff->mPaletteBlue[ x2 ];
    }
  } else {
    for ( uint32 i = 0; i < ww; ++i ) {
      uint32 x1 = BPP*(start+i) + (BPP - tiff->mBpp); // red
      uint32 x2 = tiff->mBpp*(xoff+i);
      for ( uint16 b = 0; b<tiff->mBpp; ++b ) ret[x1++] = buf[x2++];
    }
  }
}

void
copyScanPlane( TiffStruct * tiff, unsigned char * ret, uint32 start, unsigned char * buf, uint32 xoff, uint32 ww, uint16 plane )
{
  if ( tiff->mPaletteRed != NULL ) {
    unsigned char * palette = ( plane == 0 )? tiff->mPaletteRed : (plane == 1)? tiff->mPaletteGreen : tiff->mPaletteBlue;
    for ( uint32 i = 0; i < ww; ++i ) {
      ret[ BPP*(start+i) + plane ] = palette[ buf[ xoff+i ] ];
    }

  } else {
    for ( uint32 i = 0; i < ww; ++i ) {
      uint32 x1 = BPP*(start+i) + (BPP - tiff->mBpp) + plane; // red
      uint32 x2 = xoff + i;
      ret[x1] = buf[x2];
    }
  }
}

void 
scanlineRead( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
              unsigned char * ret, uint32 xstart, uint32 ystart, uint32 width )
{
  uint32 linesize = TIFFScanlineSize( tiff->tif );
  // std::cout << "scanline size " << linesize << "\n"; // scanline_size = width * Bpp

  tdata_t buf = _TIFFmalloc(TIFFScanlineSize( tiff->tif ));

  uint32 ww = xend - xoff;

  if ( tiff->mPlanar == PLANARCONFIG_CONTIG ) {
    // std::cout << "read scanline with contiguous planes Spp " << mSpp << "\n";
    uint32 start = ystart*width + xstart;
    for ( uint32 j = yoff; j < yend; ++j) {
      TIFFReadScanline( tiff->tif, buf, j, 0);
      copyScanline( tiff, ret, start, (unsigned char *)buf, xoff, ww );
      start += width;
    }
  } else if ( tiff->mPlanar == PLANARCONFIG_SEPARATE ) {
    // std::cout << "read scanline with separate planes Spp " << mSpp << "\n";
    for ( uint16 s = 0; s < tiff->mSpp; ++ s ) {
      uint32 start = ystart*width + xstart;
      for ( uint32 j = yoff; j < yend; ++j) {
        TIFFReadScanline( tiff->tif, buf, j, s);
        copyScanPlane( tiff, ret, start, (unsigned char *)buf, xoff, ww, s );
        start += width;
      }
    }
  }
  _TIFFfree( buf );
}

// -----------------------------------------------------------------
// if the file is organized in strips
// this has been testes with input RGB, output RGB

// y0 Y-line in source buffer where the strip begins
void
copyStrip( TiffStruct * tiff, unsigned char * ret, uint32 xstart, uint32 ystart, uint32 width,
                 unsigned char * buf, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend, uint32 y0 )
{
  uint32 y2 = y0 + tiff->mStripRows;
  if (y2 > yend) y2 = yend;
  uint32 y1 = ( y0 > yoff )? y0 : yoff;
  for ( uint32 y = y1; y < y2; ++y ) {
    // copy buf[y-y0] into ret[y-yoff]
    uint32 ub = (y-y0) * tiff->mWidth * tiff->mBpp + xoff * tiff->mBpp;
    uint32 ur = (y-yoff) * width * BPP;
    for (uint32 x = xoff; x < xend; ++x ) {
      ur += (BPP - tiff->mBpp);
      for ( uint16 s=0; s<tiff->mBpp; ++s ) {
        ret[ur++] = buf[ub++];
      }
    }
  }
}

void
stripRead( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
                 unsigned char * ret, uint32 xstart, uint32 ystart, uint32 width )
{
  uint32 * bc;
  TIFFGetField( tiff->tif, TIFFTAG_STRIPBYTECOUNTS, &bc );

  // uint32 yend = getLast( yoff, h, mHeight );
  // uint32 xend = getLast( xoff, w, mWidth );
  // uint32 ww = xend - xoff;

  // tdata_t buf = _TIFFmalloc(TIFFStripSize(tif));
  tstrip_t strip = yoff / tiff->mStripRows;
  tstrip_t end   = ( yend + tiff->mStripRows - 1 ) / tiff->mStripRows;
  uint32 stripsize = bc[0];
  // std::cout << "strip 0-size " << stripsize << "\n";
  // std::cout << "strips " << strip << " " << end << " max " << TIFFNumberOfStrips(tif) << "\n";
  if ( end > TIFFNumberOfStrips( tiff->tif ) ) end = TIFFNumberOfStrips( tiff->tif );
  
  tdata_t buf = _TIFFmalloc( stripsize ); // tdatat is void *
  for ( ; strip < end; strip++) {
    if ( bc[strip] > stripsize ) {
      buf = _TIFFrealloc( buf, bc[strip] );
      stripsize = bc[strip];
      // std::cout << "strip size " << stripsize << "\n";
    }
    TIFFReadEncodedStrip( tiff->tif, strip, buf, bc[strip] );
    copyStrip( tiff, ret, xstart, ystart, width, (unsigned char *)buf, xoff, yoff, xend, yend, strip* tiff->mStripRows );
  }
  _TIFFfree(buf);
}

// -----------------------------------------------------------------
// if the file is organized in tiles

void 
copyTile( TiffStruct * tiff, unsigned char * ret, uint32 x1, uint32 y1, uint32 width,
                unsigned char * tile, uint32 x2, uint32 y2, uint32 nxx, uint32 nyy )
{
  for ( uint32 y = 0; y < nyy; ++y ) {
    uint32 roff = ((y1+y)*width + x1 ) * BPP + (BPP- tiff->mBpp); // 1 for alpha
    uint32 toff = ((y2+y)* tiff->mTileWidth + x2 ) *  tiff->mBpp;
    for ( uint32 x = 0; x < nxx; ++x ) {
      for ( uint16 s = 0; s < tiff->mBpp; ++s ) ret[ roff + s ] = tile[ toff + s ];
      roff += BPP;
      toff += tiff->mBpp;
    }
  }
}

// s: plane
void 
copyTilePlane( TiffStruct * tiff, unsigned char * ret, uint32 x1, uint32 y1, uint32 width,
                     unsigned char * tile, uint32 x2, uint32 y2, uint32 nxx, uint32 nyy, uint16 s )
{
  for ( uint32 y = 0; y < nyy; ++y ) {
    uint32 roff = ((y1+y)*width + x1 ) * BPP + (BPP - tiff->mBpp); // 1 for alpha
    uint32 toff = ((y2+y) * tiff->mTileWidth + x2 );
    for ( uint32 x = 0; x < nxx; ++x ) {
      ret[ roff + s ] = tile[ toff ];
      roff += BPP;
      toff ++;
    }
  }
}

void
tileRead( TiffStruct * tiff, uint32 xoff, uint32 yoff, uint32 xend, uint32 yend,
                unsigned char * ret, uint32 xstart, uint32 ystart, uint32 width )
{
  tdata_t buf = _TIFFmalloc(TIFFTileSize( tiff->tif ) );
  uint32 yrow = yoff - (yoff % tiff->mTileHeight); // Y-row of first tile
  uint32 y1 = 0;
  for ( ; yrow < yend; yrow += tiff->mTileHeight ) {
    uint32 nyy = ( yrow < yoff)? tiff->mTileHeight - yoff  // number of lines to copy
               : ( yrow + tiff->mTileHeight > yend)? yend - yrow 
               : tiff->mTileHeight;
    uint32 y0  = ( yrow < yoff )? tiff->mTileHeight - nyy : 0; // first line to copy
    uint32 xcol = xoff - (xoff % tiff->mTileWidth); // X-col of first tile
    uint32 x1 = xstart;
    for ( ; xcol < xend; xcol += tiff->mTileWidth ) {
      uint32 nxx = ( xcol < xoff )? tiff->mTileWidth - xoff
                 : ( xcol + tiff->mTileWidth > xend)? xend - xcol
                 : tiff->mTileWidth;
      uint32 x0  = ( xcol < xoff)? tiff->mTileWidth - nxx : 0; // first col to copy
      if ( tiff->mPlanar == PLANARCONFIG_CONTIG ) {
        TIFFReadTile( tiff->tif, buf, xcol, yrow, 0, 0);  // z=0 (depth==1) sample=0 (mPlanar=CONTIG)
        copyTile( tiff, ret, x1, y1, width, (unsigned char *)buf, x0, y0, nxx, nyy );
      } else {
        for ( uint16 s = 0; s < tiff->mBpp; ++s ) {
          TIFFReadTile( tiff->tif, buf, xcol, yrow, 0, s);  // z=0 (depth==1) sample=s 
          copyTilePlane( tiff, ret, x1, y1, width, (unsigned char *)buf, x0, y0, nxx, nyy, s );
        }
      }
      x1 += nxx;
    }
    y1 += nyy;
  }  
  _TIFFfree(buf);
}

/*
void
tileRead2( TIFF * tif, uint32 xoff, uint32 yoff, uint32 w, uint32 h )
{
  // uint32 mTileWidth, mTileHeight;
  // uint32 x, yrow;

  // uint32 yend = getLast( yoff, h, mHeight );
  // uint32 xend = getLast( xoff, w, mWidth );
  // uint32 ww = xend - xoff;

  TIFFGetField(tif, TIFFTAG_TILEWIDTH, &mTileWidth);
  TIFFGetField(tif, TIFFTAG_TILELENGTH, &mTileHeight);
  tdata_t buf = _TIFFmalloc(TIFFTileSize(tif));
  for ( ttile_t tile = 0; tile < TIFFNumberOfTiles( tif ); ++tile ) {
    TIFFReadEncodedTile(tif, tile, buf, (tsize_t) -1 );
    // TODO
  }
 _TIFFfree(buf);
}
*/

// -----------------------------------------------------------------

void
closeTiff( TiffStruct * tiff )
{
  if ( tiff->tif != NULL ) {
    // FIXME  tif should be closed but this produces a segfault
    // TIFFClose( tiff->tif );
    tiff->tif = NULL;
  }
}

// uint32
// getLast( uint32 yoff, uint32 h, uint32 max )
// {
//   return ( yoff + h > max )? max : yoff+h;
// }

double * getTagsDouble( TIFF * tif, int tag, uint32 * count )
{
  void * data;
  if ( TIFFGetField( tif, tag, count, &data ) != 1 ) {
    return NULL;
  }
  return (double*)data;
}

uint16 getTagUInt16( TIFF * tif, int tag )
{
  uint16 data;
  if ( TIFFGetField( tif, tag, &data ) != 1 ) return 0;
  return data;
}

uint32 getTagUInt32( TIFF * tif, int tag )
{
  uint32 data;
  if ( TIFFGetField( tif, tag, &data ) != 1 ) return 0;
  return data;
}


// double getTagDouble( TIFF * tif, int tag )
// {
//   double data;
//   if ( TIFFGetField( tif, tag, &data ) != 1 ) return 0;
//   return data;
// }

// ===================================================================
#ifdef TEST_TIFF

#include <iostream>
#include "../../XLIB/gui++.h"

int main( int argc, char ** argv )
{
  Tiff tiff( argv[1] );
  double x1 = 1553200;
  double y1 = 4871900;
  double x2 = 1554200;
  double y2 = 4872900;
  
  unsigned char * img = tiff.getImage( x1, y1, x2, y2 );
  uint32 ww = tiff.mImageWidth;
  uint32 hh = tiff.mImageHeight;
  std::cout << "SubImage size " << ww << "x" << hh << "\n";
 
  GUI gui( 1, ww, hh, 1 );
  gui.waitKeyPress();
  gui.displayImage( img, ww, hh, 0, 0, BPP );
  gui.waitKeyPress();

  return 0;
}

#endif
