/** @file powercrust_lib.java
 *
 * @author marco corvi
 * @date May 2017
 *
 * @brief powercrust jni interface
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 * Usage:
 *   resetSites( 3 )
 *   foreach site addSite( site.x, site.y, site.z )
 *   compute();
 *   // retrieve surface structs
 *   np = getNrPoles();
 *   for ( n=0; n<np; ++n ) {
 *     pole[n] = ( getPoleX(), getPoleY(), getPoleZ() );
 *     if ( getNextPole() == 0 ) break;
 *   }
 *   nf = getNrFaces();
 *   for ( n=0; n<nf; ++n ) {
 *     sz = getFaceSize();
 *     face[n].setSize( sz );
 *     for ( s=0; s<sz; ++s ) {
 *       face[n].vertex[s] = getFaceVertex( s );
 *     }
 *     if ( getNextFace() == 0 ) break;
 *   }
 *   release_wstructs();
 */
#ifndef POWERCRUST_LIB_H
#define POWERCRUST_LIB_H

// library functions
/** 
 * @param d   site space dimension (must be 3)
 */
void reset_sites( int d );

/** set the next site coords
 * @param x   x coord
 * @param y   y coord
 * @param z   z coord
 */
void set_next_site( double x, double y, double z );

/** retrieve the number of sites
 * @return number of sites
 */
long getNrSites();

/** release the powercrust structures
 * must be called at the end
 */
void release_wstructs();

/** get the number of poles
 * @return the number of poles
 */
int getNrPoles();

/** set the pole iterator to the next pole
 * @return 0 if there are no more poles
 */
int getNextPole();

/** get the pole X coord */
float getPoleX();
float getPoleY();
float getPoleZ();

int getNrFaces();
int getFaceSize();
int getFaceVertex( int k );
int getNextFace();

int driver( long seed, double est_radius, int defer, double deep, double theta, short vol, short bad );
#endif
