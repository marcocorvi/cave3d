/** @file vv_arena.h
 *
 * @author marco corvi
 * @date may 2017
 *
 * @brief memory storage, allocating blocks of memory for small objects
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
#ifndef VV_ARENA_H
#define VV_ARENA_H

// #define ARENA_0 0 // unused
#define ARENA_BASIS    1 // basis_s *
#define ARENA_COORDS   2 // Coord *
#define ARENA_PLIST    3 // plist *
#define ARENA_EDGESIMP 4 // edgesimp *
#define ARENA_MAX      5
// #define ARENA_5 5 // polelabel *
// #define ARENA_6 6 
// #define ARENA_7 7 // double *

void * getArena( int type, int size );
// void * getArenaZero( int type, int size );
void freeArena( int type );
int getNumArena( int type );

#endif
