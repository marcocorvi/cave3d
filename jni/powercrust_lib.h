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
 */
#ifndef POWERCRUST_LIB_H
#define POWERCRUST_LIB_H

// library functions
void reset_sites();
void set_next_site( double x, double y, double z );
long getNrSites();
void release_wstructs();

int getNrPoles();
float getPoleX();
float getPoleY();
float getPoleZ();
int getNextPole();

int getNrFaces();
int getFaceSize();
int getFaceVertex( int k );
int getNextFace();

int driver( long seed, double est_radius, int defer, double deep, double theta, short vol, short bad );
#endif
