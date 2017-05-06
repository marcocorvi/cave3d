/** @file contsants.h
 *
 * @author marco corvi
 * @date may 2017
 *
 * @brief C constants extsrated from the file hull.h
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
#ifndef CONSTANTS_H
#define CONSTANTS_H

#define MAXDIM 8
#define BLOCKSIZE 4096
#define MAXBLOCKS 100
#define EXACT 1  /* sunghee */
#define NRAND 5  /* number of random points chosen to check orientation */
#define MAXNF 100 /* max number of conv hull triangles adjacent to a vertex */
#define MAXTA 100000
#define MAXTEMPA 100000
#define CNV 0 /* sunghee : status of simplex, if it's on convex hull */
#define VV 1 /* sunghee :    if it's regular simplex  */
#define SLV -1 /* sunghee : if orient3d=0, sliver simplex */
#define AV 2 /* if av contains the averaged pole vector */
#define PSLV -2 /* probably sliver */
#define POLE_OUTPUT 3 /* VV is pole and it's ouput */
#define SQ(a) ((a)*(a)) /* sunghee */

#define BAD_POLE -1

#define IN 2
#define OUT 1
#define INIT 0
#define NONE -1

#define FIRST 0
#define NO 1
#define DEG -1
#define NORM 2
#define VOR 3
#define VOR_NORM 4
#define SLVT 7
#define PSLVT 8
#define SURF 5
#define OPP 6

#define FIRST_EDGE 0
#define POW 1
#define NOT_POW 2
#define VISITED 3

/*RAVI */

#define VALIDEDGE 24
#define INVALIDEDGE 23
#define INEDGE 25
#define OUTEDGE 26
#define ADDAXIS 13
#define PRESENT 19
#define FIXED 20
#define REMOVED 21  /* for the thinning  stuff */

#endif
