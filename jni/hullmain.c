/* @file power.c
 *
 * @brief
 *
 * ------------------------------------------------------------------
 * This file is amodification of the original powercrust file
 * marco corvi - may 2017
 *
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 *
 * ------------------------------------------------------------------
 * Power Crust software, by Nina Amenta, Sunghee Choi and Ravi Krishna Kolluri.
 * Copyright (c) 2000 by the University of Texas
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee under the GNU Public License is hereby granted,
 * provided that this entire notice  is included in all copies of any software
 * which is or includes a copy or modification of this software and in all copies
 * of the supporting documentation for such software.
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTY.  IN PARTICULAR, NEITHER THE AUTHORS NOR AT&T MAKE ANY
 * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY
 * OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 *
 * ------------------------------------------------------------------
 * This file is a significant modification of Ken Clarkson's file hullmain.c.
 * We include his copyright notice in accordance with its terms.
 *                                                   - Nina, Sunghee and Ravi
 *
 * Ken Clarkson wrote this.  Copyright (c) 1995 by AT&T..
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee is hereby granted, provided that this entire notice
 * is included in all copies of any software which is or includes a copy
 * or modification of this software and in all copies of the supporting
 * documentation for such software.
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTY.  IN PARTICULAR, NEITHER THE AUTHORS NOR AT&T MAKE ANY
 * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY
 * OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 */
#include <float.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <locale.h>
#include <string.h>
#include <getopt.h>
#include <ctype.h>

#define POINTSITES 1

#include "defines.h" /* must be first */
#include "debug.h"
#include "constants.h"
#include "hull.h"
#include "heap.h"
#include "vv_arena.h"

FILE * fplog   = NULL;
FILE * OUTFILE = NULL;
#ifdef MYDEBUG
  FILE * DFILE = NULL;
#endif

#ifdef OUT_PC
  FILE * PC;  // powercrust points
  FILE * PNF; // powercrust faces
  extern int num_vtxs;  // references from power.c
  extern int num_faces;
#endif

// FILE *POLE;
// FILE *INPOLE, *INPBALL, *INVBALL, *TFILE;
#ifdef DO_AXIS
  FILE * AXIS;
  FILE * AXISFACE;
  int num_axedgs = 0;
  int num_axfaces = 0;
#endif

#ifdef SIGSEGV_HANDLER
  #include <signal.h>
  #include <setjmp.h>

  jmp_buf env;

  void sigsegv_handler( int signo )
  {
    if ( signo == SIGSEGV ) {
      longjmp( env, 1 );
    }
  }
#endif

// local functions

Coord mins[MAXDIM] = {DBL_MAX,DBL_MAX,DBL_MAX,DBL_MAX,DBL_MAX,DBL_MAX,DBL_MAX,DBL_MAX};
Coord maxs[MAXDIM] = {-DBL_MAX,-DBL_MAX,-DBL_MAX,-DBL_MAX,-DBL_MAX,-DBL_MAX,-DBL_MAX,-DBL_MAX};

double mult_up = 100000.0;
double toScaled( double x ) { return x * mult_up; }
double toOrig( double x ) { return x / mult_up; }
#ifdef MAIN
  void set_mult_up( double m ) { mult_up = m; }
#endif

double bound[8][3];
double ominmax[6];  /* 8 vertices for bounding box */
point site_blocks[MAXBLOCKS];
int num_blocks = 0;

/* Data structures for poles */
double * pole1_distance = NULL;
double * pole2_distance = NULL;
struct simplex ** pole1 = NULL;
struct simplex ** pole2 = NULL;    /* arrays of poles - per sample*/
struct polelabel * adjlist = NULL; /* array of additional info - per pole */
struct plist ** opplist = NULL;    /* opposite pid and angle between poles - per pole */
double * lfs_lb = NULL;            /*  array of lower bounds for lfs of each sample */
double est_r = 0.6;                /* estimated value of r - user input, used by crust.c */

long  num_sites = 0; // private to the file
short vd = 1;
short power_diagram = 0; /* 1 if power diagram */
int   dim;

// shared among execs
// simplex * root;

// ============================================================


/* tests pole to see if it's farther than estimated local feature size.
   v is a sample, p is a pole. */
// private
int close_pole(double* v, double* p, double lfs_lb) 
{
  return (sqdist(v,p) < lfs_lb * lfs_lb);
}


/* for each pole array, compute the maximum of the distances on the sample */
void compute_distance(simplex** poles, int size, double* distance)
{
  int i,j,k,l;
  double indices[4][3]; /* the coords of the four vertices of the simplex*/
  point v[MAXDIM];
  simplex* currSimplex;

  double maxdistance=0;
  double currdistance;

  for(l=0;l<size;l++)
  {  /* for each pole do*/
    if(poles[l]!=NULL) {
      currSimplex=poles[l];

      /* get the coordinates of the  four endpoints */
      for(j=0;j<4;j++) {
        v[j]=currSimplex->neigh[j].vert;
        for(k=0;k<3;k++) {
          indices[j][k] = toOrig( v[j][k] );
        }
      }

      /* now compute the actual distance  */
      maxdistance=0;

      for(i=0;i<4;i++) {
        for(j=i+1;j<4;j++) {
          currdistance= SQ(indices[i][0]-indices[j][0]) +
              SQ(indices[i][1]-indices[j][1])+ SQ(indices[i][2]-indices[j][2]);
          currdistance=sqrt(currdistance);
          if(maxdistance<currdistance) {
            maxdistance=currdistance;
          }
        }
      }
      distance[l]=maxdistance;
    }
  }
}

// typedef struct queuenode {
//     long pid;
//     struct queuenode *next;
// } queuenode;

void reset_next_ptrs();

// ---------------------------------------------------------------

void free_sites() // private
{ 
  int i;
  for ( i=0; i<num_blocks; ++i ) {
    if ( site_blocks[i] ) { free( site_blocks[i] ); site_blocks[i] = NULL; }
  }
  num_blocks = 0;
}

void set_clear_sites() // private
{
  memset( site_blocks, 0, MAXBLOCKS*sizeof(point) );
}

/* for priority queue */
extern int heap_size;

// int poleInput=0; /* are the poles given as input */

site new_site(site pp, long j) // private
{
  ASSERT(num_blocks + 1 < MAXBLOCKS);
  if (0 == (j % BLOCKSIZE)) {
    ASSERT(num_blocks < MAXBLOCKS);
    LOGI("new site %d %d", BLOCKSIZE, site_size );
    #ifdef FIXED_SITE_SIZE
      site ret = site_blocks[num_blocks++] = (site)malloc( BLOCKSIZE * 4*sizeof(double) );
      // FIXME ret can be NULL
    #else
      site ret = site_blocks[num_blocks++] = (site)malloc(BLOCKSIZE*site_size);
    #endif
    return ret;
  } else {
    #ifdef FIXED_SITE_SIZE
      return pp + 4;
    #else
      return pp + dim;
    #endif
  }
}

/* intermediate pole struct and output point struct */
#define BLOCK_WPOLE  1024
struct wpole * wpolebegin = NULL;
struct wpole * wpoleend = NULL;
int num_wpole = 0;

// #define BLOCK_WSTORE 1024
// struct wpole * wstorebegin = NULL;
// struct wpole * wstoreend = NULL;
// int num_wstore = 0;

#define BLOCK_WFACE  1024
struct wface * wfacebegin = NULL;
struct wface * wfaceend = NULL;
int num_wface = 0;

struct wpole * new_wpole( struct wpole * wn )
{
  int i;
  struct wpole * wp;
  if ( wn != NULL ) {
    ++num_wpole;
    return wn;
  }
  wp = ( struct wpole * )malloc( BLOCK_WPOLE * sizeof(struct wpole) );
  if ( wp == NULL ) return NULL; // FIXME
  memset( wp, 0, BLOCK_WPOLE * sizeof(struct wpole) );

  if ( wpolebegin == NULL ) wpolebegin = wp;
  for ( i=1; i<BLOCK_WPOLE; ++i ) wp[i-1].next = &(wp[i]);
  wp[BLOCK_WPOLE-1].next = NULL;
  if ( wpoleend != NULL ) wpoleend->next = wp;
  wpoleend = &(wp[BLOCK_WPOLE-1]);
  ++num_wpole;
  return wp;
}

void clear_wpoles()
{
  while ( wpolebegin != NULL ) {
    wpoleend = wpolebegin[BLOCK_WPOLE-1].next;
    free( wpolebegin );
    wpolebegin = wpoleend;
  }
  num_wpole = 0;
}

/*
struct wpole * new_wstore( struct wpole * wn )
{
  int i;
  struct wpole * wp;
  if ( wn != NULL ) {
    ++num_wstore;
    return wn;
  }
  // LOGI("new wstore %d", BLOCK_WSTORE*sizeof(struct wpole) );
  wp = ( struct wpole * )malloc( BLOCK_WSTORE * sizeof(struct wpole) );
  memset( wp, 0, BLOCK_WSTORE * sizeof(struct wpole) );
  if ( wp == NULL ) return NULL;
  if ( wstorebegin == NULL ) wstorebegin = wp;
  for ( i=1; i<BLOCK_WSTORE; ++i ) wp[i-1].next = &(wp[i]);
  wp[BLOCK_WSTORE-1].next = NULL;
  if ( wstoreend != NULL ) wstoreend->next = wp;
  wstoreend = &(wp[BLOCK_WSTORE-1]);
  ++num_wstore;
  return wp;
}

void clear_wstore()
{
  while ( wstorebegin != NULL ) {
    wstoreend = wstorebegin[BLOCK_WPOLE-1].next;
    free( wstorebegin );
    wstorebegin = wstoreend;
  }
  num_wstore = 0;
}
*/

site restore_site( struct wpole * wp, long j )
{
  int i;
  p = new_site(p,j);
  p[0] = wp->x;
  p[1] = wp->y;
  p[2] = wp->z;
  p[3] = wp->w;
  for ( i=0; i<4; ++i ) {
    p[i] = floor( toScaled(p[i]) + 0.5);
    mins[i] = (mins[i] < p[i]) ? mins[i] : p[i];
    maxs[i] = (maxs[i] > p[i]) ? maxs[i] : p[i];
  }
  return p;
}


struct wface * new_wface( struct wface * wn, int n )
{
  int i;
  struct wface * wp = NULL;
  if ( wn == NULL ) {
    // LOGI("new wface %d", BLOCK_WFACE*sizeof(struct wface) );
    wp = ( struct wface * )malloc(BLOCK_WFACE * sizeof(struct wface));
    if ( wp == NULL ) return NULL;
    memset( wp, 0, BLOCK_WFACE * sizeof(struct wface) );

    if ( wfacebegin == NULL ) wfacebegin = wp;
    for ( i=1; i<BLOCK_WFACE; ++i ) wp[i-1].next = &(wp[i]);
    wp[BLOCK_WFACE-1].next = NULL;
    if ( wfaceend != NULL ) wfaceend->next = wp;
    wfaceend = &(wp[BLOCK_WFACE-1]);
  } else {
    wp = wn;
  }
  wp->n = n;
  wp->idx = ( n <= 0 )? NULL : (long *)malloc( n * sizeof(long) );
  // LOGI("new wface idx %d", n*sizeof(long) );
  ++num_wface;
  return wp;
}

void clear_wfaces()
{
  int i;
  while ( wfacebegin != NULL ) {
    wfaceend = wfacebegin[BLOCK_WFACE-1].next;
    for ( i=0; i<BLOCK_WFACE; ++i ) {
      if ( wfacebegin[i].n > 0 ) free( wfacebegin[i].idx );
    }
    free( wfacebegin );
    wfacebegin = wfaceend;
  }
  num_wface = 0;
}

// list traversing
struct wpole * tmp_pole = NULL;
struct wface * tmp_face = NULL;

int getNrPoles()
{
  tmp_pole = wpolebegin;
  return num_wpole;
}

long getNrSites()
{
  return num_sites;
}

float getPoleX() { return (float)(tmp_pole->x); }
float getPoleY() { return (float)(tmp_pole->y); }
float getPoleZ() { return (float)(tmp_pole->z); }

int getNextPole()
{ 
  if ( tmp_pole != NULL ) tmp_pole = tmp_pole->next;
  return (tmp_pole != NULL)? 1 : 0;
}

int getNrFaces()
{
  tmp_face = wfacebegin;
  return num_wface;
}

int getFaceSize() { return (tmp_face != NULL )? tmp_face->n : 0; }
int getFaceVertex( int k ) { return (tmp_face != NULL)? tmp_face->idx[k] : -1; }

int getNextFace() 
{
  if ( tmp_face != NULL ) tmp_face = tmp_face->next;
  return (tmp_face != NULL)? 1 : 0;
}


// int *rverts;

// UNUSED
// int* select_random_points(int Nv) /* for orientation testing */
// { /* Nv : Number of vertices (sites) */
//   int i, j;
//   int *rverts;
// 
//   LOGI("random pts %d", NRAND*sizeof(int) );
//   rverts = (int*) malloc (NRAND * sizeof(int));
// 
//   // srandom(Nv);  /* seed the random number generator */
//   for (i = 0; i < NRAND; i++) {
//     j = random() % Nv;
//     rverts[i] = j;
//   }
//   return rverts;
// }

// index of a site:
//   each site hs size "dim"
//   sites are stored in blocks of BLOCKSIZE sites
// therefore
//   (1) find the site_block with pointer 'p'
//   (2) offset = (p - site_block)/dim
//   (3) i = block index
long site_numm(site p)
{
  long i,j;
  if (( vd || power_diagram) && isAtInfinity(p) // p==coordsAtInfinity
     ) {
    return -1;
  }
  if (!p) {
    return -2;
  }
  for (i = 0; i < num_blocks; i++) {
    #ifdef FIXED_SITE_SIZE
      if ((j = p - site_blocks[i]) >= 0 && j < BLOCKSIZE * 4) return j / 4 + BLOCKSIZE * i;
    #else
      if ((j = p - site_blocks[i]) >= 0 && j < BLOCKSIZE * dim) return j / dim + BLOCKSIZE * i;
    #endif
  }
  return -3;
}

// private
void read_bounding_box( long j )
{
  int i, k;
  double center[3], width;

  ominmax[0] = mins[0];
  ominmax[1] = mins[1];
  ominmax[2] = mins[2];
  ominmax[3] = maxs[0];
  ominmax[4] = maxs[1];
  ominmax[5] = maxs[2];

  center[0] = (maxs[0] - mins[0])/2;
  center[1] = (maxs[1] - mins[1])/2;
  center[2] = (maxs[2] - mins[2])/2;

  if ((maxs[0] - mins[0]) > (maxs[1] - mins[1])) {
    if ((maxs[2] - mins[2]) > (maxs[0] - mins[0])) {
      width = maxs[2] - mins[2];
    } else {
      width = maxs[0] - mins[0];
    }
  } else {
    if ((maxs[1] - mins[1]) > (maxs[2] - mins[2])) {
      width = maxs[1] - mins[1];
    } else {
      width = maxs[2] - mins[2];
    }
  }

  width *= 4;

  bound[0][0] = center[0] + width;
  bound[1][0] = bound[0][0];
  bound[2][0] = bound[0][0];
  bound[3][0] = bound[0][0];
  bound[0][1] = center[1] + width;
  bound[1][1] = bound[0][1];
  bound[4][1] = bound[0][1];
  bound[5][1] = bound[0][1];
  bound[0][2] = center[2] + width;
  bound[2][2] = bound[0][2];
  bound[4][2] = bound[0][2];
  bound[6][2] = bound[0][2];
  bound[4][0] = center[0] - width;
  bound[5][0] = bound[4][0];
  bound[6][0] = bound[4][0];
  bound[7][0] = bound[4][0];
  bound[2][1] = center[1] - width;
  bound[3][1] = bound[2][1];
  bound[6][1] = bound[2][1];
  bound[7][1] = bound[2][1];
  bound[1][2] = center[2] - width;
  bound[3][2] = bound[1][2];
  bound[5][2] = bound[1][2];
  bound[7][2] = bound[1][2];

  // for (i = 0; i < 8; i++) {
  //   PRINTD("%f %f %f\n", toOrig(bound[i][0]), toOrig(bound[i][1]), toOrig(bound[i][2]) );
  // }
  // for (k = 0; k < 3; k++) {
  //   p[k] = bound[0][k];
  // }
  for (i=0; i<8; i++) {
    p = new_site(p, j+i);
    for (k = 0; k < 3; k++) {
      p[k] = bound[i][k];
    }
  }
  maxs[0] = bound[0][0];
  mins[0] = bound[4][0];
  maxs[1] = bound[0][1];
  mins[1] = bound[2][1];
  maxs[2] = bound[0][2];
  mins[2] = bound[1][2];
}

void set_next_site( double x, double y, double z )
{
  int i;
  p = new_site( p, num_sites );
  ++ num_sites;
  p[0] = x;
  p[1] = y;
  p[2] = z;
  for ( i = 0; i<3; ++ i ) {
      p[i] = floor( toScaled(p[i]) + 0.5);
      mins[i] = (mins[i] < p[i]) ? mins[i] : p[i];
      maxs[i] = (maxs[i] > p[i]) ? maxs[i] : p[i];
  }
  if ( num_sites > 1510 && p ) LOGI("set site %ld: %.6lf %.6lf %.6lf (%lf)\n", num_sites, p[0], p[1], p[2], mult_up );
}

/* reads a site from storage we're managing outselves */
site get_site_offline(long i)
{
  if (i>=num_sites) {
    // PRINTD("bad site %ld / %ld\n", i, num_sites );
    return NULL;
  } else {
    #ifdef FIXED_SITE_SIZE
      return site_blocks[i/BLOCKSIZE]+(i%BLOCKSIZE)*4;
    #else
      return site_blocks[i/BLOCKSIZE]+(i%BLOCKSIZE)*dim;
    #endif
  }
}

void reset_sites( int dd )
{
  int k;
  LOGI("reset sites dim %d\n", dim );
  num_sites = 0; 
  dim = dd;
  vd = 1;
  power_diagram = 0;
  for ( k=0; k<MAXDIM; ++k ) {
    mins[k] =   DBL_MAX;
    maxs[k] = - DBL_MAX;
  }
}

// ---------------------------------------------------------------
// SHUFFLE

long *shufmat = NULL; // private
long s_num = 0; // private

void make_shuffle( int n_sites ) // private
{
  long i,t,j;
  s_num = 0;
  LOGI("make shuffle sites %d\n", n_sites );
  if ( shufmat ) free( shufmat );
  shufmat = (long*)malloc((n_sites+1)*sizeof(long));
  if ( shufmat != NULL ) {
    for (i = 0; i <= n_sites; i++) {
      shufmat[i] = i;
    }
    for (i = 0; i < n_sites; i++) {
      t = shufmat[i];
      j = i + (n_sites-i)*double_rand();
      shufmat[i] = shufmat[j];
      shufmat[j] = t;
    }
  }
}

void free_shuffle() // private
{
  if ( shufmat != NULL ) { free( shufmat ); shufmat = NULL; }
}

/* returns shuffled, offline sites or reads an unshuffled site */
site get_next_site(void)
{
  long k = shufmat[ s_num++ ];
  site ret = get_site_offline( k );
  return ret;
}

// --------------------------------------------------------------------

void make_output(simplex * rot,
                 void *(*visit_gen)(simplex*, visit_func* visit),
                 visit_func* visit,
                 out_func* out_funcp,
                 FILE *F)
{
    out_funcp(0,0,F,-1);
    visit(0, out_funcp);
    visit_gen(rot, visit);
    out_funcp(0,0,F,1);
    /*  efclose(F); */
}


long exec_first_part( int dim, long seed, short bad, out_func * mof )
{
  int i, k;
  long poleid = 0;
  int numbadpoles  = 0;
  int numgoodpoles = 0;
  int numopppoles  = 0;
  int numnullpoles = 0;
  int numfailpoles = 0;
  int numnoout     = 0;
  int num_poles    = 0;
  struct wpole * wp;
  struct wpole * wp_next = NULL;
  simplex * root = NULL;

  if ( mof == NULL ) mof = no_out;

  read_bounding_box(num_sites);
  num_sites += 8;

  LOGI("Step 0: Shuffling... sites %ld seed %ld\n", num_sites, seed );
  init_rand(seed);

  make_shuffle( num_sites );
  if ( shufmat == NULL) {
    LOGI("exec-1 failed make shuffle\n");
    return 0;
  }
  // get_site_n = get_site_offline; // this is used by get_next_site

  LOGI("Step 1: compute DT of input point set\n");
  root = build_convex_hull( /* get_next_site, */ site_numm, dim, vd );

  LOGI("Step 2: Find poles ... sites %ld\n", num_sites );
  pole1 = (struct simplex **) calloc(num_sites, sizeof(struct simplex *));
  if ( pole1 == NULL ) return 0;

  pole2 = (struct simplex **) calloc(num_sites, sizeof(struct simplex *));
  if ( pole2 == NULL ) return 0;

  lfs_lb = (double*) calloc(num_sites, sizeof(double));
  if ( lfs_lb == NULL ) return 0;

  exactinit(); /* Shewchuk's exact arithmetic initialization */

  LOGI("  2a: Computing Voronoi vertices and 1st poles....\n");
  make_output(root, visit_hull, compute_vv, mof, NULL);

  LOGI("  2b: Computing 2nd poles.... sites %ld\n", num_sites);
  make_output(root, visit_hull, compute_pole2, mof, NULL);

  /* poles with weights. Input to regular triangulation */

  /* initialize the sample distance info for the poles */
  pole1_distance=(double *) malloc(num_sites*sizeof(double));
  if ( pole1_distance == NULL ) return 0;

  pole2_distance=(double *) malloc(num_sites*sizeof(double));
  if ( pole2_distance == NULL ) return 0;

  LOGI("  2c: Computing distances sites %ld\n", num_sites);
  compute_distance( pole1, num_sites-8, pole1_distance );
  compute_distance( pole2, num_sites-8, pole2_distance );

  LOGI("intialize list of lists of pointers to opposite poles, size %ld\n", num_sites*2);
  opplist = (struct plist**) malloc(num_sites*2*sizeof(struct plist *));
  if ( opplist == NULL ) return 0;
  memset( opplist, 0, num_sites*2*sizeof(struct plist *) );

  // PRINTD("data about poles; adjacencies, labels, radii Sites %ld\n", num_sites);
  adjlist = (struct polelabel *) malloc(num_sites*2*sizeof(struct polelabel));
  if ( adjlist == NULL ) return 0;
  memset( adjlist, 0, num_sites*2*sizeof(struct polelabel) );
  // for ( k=0; k<num_sites*2; ++k ) adjlist[k].eptr = NULL;

  PRINTD("Step 3: Loop through sites, writing out poles\n");
  for (i=0;i<num_sites-8;i++) {
    double samp[3];
    /* rescale the sample to real input coordinates */
    double * pp = get_site_offline(i);
    for (k=0; k<3; k++) {
      samp[k] = toOrig(pp[k]);
    }

    /* output poles, both to debugging file and for weighted DT */
    /* remembers squared radius */
    if ((pole1[i]!=NULL) && (pole1[i]->status != POLE_OUTPUT)) {
      /* if second pole is closer than we think it should be... */
      if ((pole2[i]!=NULL) && bad && close_pole(samp,pole2[i]->vv,lfs_lb[i])) {
        numbadpoles++;
      } else {
        if ( ( wp = new_wpole( wp_next ) ) != NULL ) {
          wp_next = wp->next;
          storePole( wp, pole1[i], poleid++, samp, &num_poles, pole1_distance[i] );
          ++numgoodpoles;
        } else {
          // FIXME BAD FAILURE
          PRINTD("FAILURE malloc \n");
          ++numfailpoles;
        }
      }
    } else if ( pole1[i] == NULL ) {
      ++ numnullpoles;
    } else {
      ++ numnoout;
    }

    if ( (pole2[i]!=NULL) && (pole2[i]->status != POLE_OUTPUT)) {
      /* if pole is closer than we think it should be... */
      if (close_pole(samp,pole2[i]->vv,lfs_lb[i])) {
        /* remember opposite bad for late labeling */
        if (!bad) {
          adjlist[pole1[i]->poleindex].bad = BAD_POLE;
        }
        numbadpoles++;
        continue;
      }
      /* otherwise... */
      if ( ( wp = new_wpole( wp_next ) ) != NULL ) {
        wp_next = wp->next;
        storePole( wp, pole2[i],poleid++,samp,&num_poles,pole2_distance[i]);
        ++numgoodpoles;
      } else {
        // FIXME BAD FAILURE
        PRINTD("FAILURE malloc \n");
        ++numfailpoles;
      }
    } else if ( pole2[i] == NULL ) {
      ++ numnullpoles;
    } else {
      ++ numnoout;
    }

    /* keep list of opposite poles for later coloring */
    if ((pole1[i]!=NULL)&&(pole2[i]!=NULL)&&
        (pole1[i]->status == POLE_OUTPUT) &&
        (pole2[i]->status == POLE_OUTPUT))
    {
      double pole_angle;
      pole_angle = computePoleAngle(pole1[i],pole2[i],samp);
      newOpposite(pole1[i]->poleindex, pole2[i]->poleindex, pole_angle);
      newOpposite(pole2[i]->poleindex, pole1[i]->poleindex, pole_angle);
      ++numopppoles;
    }
  }
  // efclose(POLE);
  PRINTD("... wstore %d Poles %d bad %d good %d opp %d null %d no-out %d fail %d\n",
    num_wpole, num_poles, numbadpoles, numgoodpoles, numopppoles, numnullpoles, numnoout, numfailpoles );

  free_sites();
  free_hull_storage();
  free_shuffle();

  return num_poles;
}

simplex *
exec_second_part( int num_poles, long seed, int defer, double deep, double theta, out_func * mof )
{
  int i;
  struct wpole * wp = wpolebegin;
  if ( mof == NULL ) mof = no_out;

  PRINTD("exec-2 dim %d p %p num_blocks %d -> 0\n", dim, (void *)p, num_blocks);
  num_blocks = 0;

  point_size = site_size = sizeof(Coord)*dim;
  /* save points in order read */
  for ( i=0; i<4; ++i ) {
    mins[i] =  DBL_MAX;
    maxs[i] = -DBL_MAX;
  }
  for ( num_sites = 0; num_sites < num_poles; ++num_sites ) {
    restore_site(wp, num_sites); 
    wp = wp->next;
  }
  clear_wpoles();
  PRINTD("done restore sites: num_sites=%ld\n", num_sites);

  /* set up the shuffle */
  // PRINTD("shuffling... seed %ld\n", seed);
  init_rand(seed);

  make_shuffle( num_sites );
  if ( shufmat == NULL) {
    LOGI("exec-2 failed make shuffle\n");
    return NULL;
  }
  // get_site_n = get_site_offline;  /* returns stored points, unshuffled */

  /* Compute weighted DT  */
  simplex * root = build_convex_hull( /* get_next_site, */ site_numm, dim, vd);

  LOGI("Done weighted DT\n" );

  #ifdef OUT_PC
    PNF = fopen("pnf","w"); /* file of faces */
    PC  = fopen("pc","w");  /* file of points */
  #endif

  /* compute adjacencies and find angles of ball intersections */
  // queue = NULL;
  make_output(root, visit_hull, compute_3d_power_vv, mof, OUTFILE);

  /* Begin by labeling everything outside a big bounding box as outside */
  /* labeling */

  // if(!poleInput)
  { /* if we dont have the labels */
    PRINTD("num poles=%d\n", num_poles);
    if ( heap_init(num_poles) == 0 ) return 0;
    double max_min0 = 2 * ominmax[3] - ominmax[0];
    double max_min1 = 2 * ominmax[4] - ominmax[1];
    double max_min2 = 2 * ominmax[5] - ominmax[2];
    double min_max0 = 2 * ominmax[0] - ominmax[3];
    double min_max1 = 2 * ominmax[1] - ominmax[4];
    double min_max2 = 2 * ominmax[2] - ominmax[5];
    for (i = 0; i < num_poles; i++) {
      double * pp = get_site_offline(i);
      if ( (pp[0]>max_min0) || (pp[0]<min_max0) ||
           (pp[1]>max_min1) || (pp[1]<min_max1) ||
           (pp[2]>max_min2) || (pp[2]<min_max2) ) {
        adjlist[i].hid   = heap_insert(i,1.0);
        adjlist[i].out   = 1.0;
        adjlist[i].label = OUT;
      }
    }
    PRINTD("heap size %d defer %d Theta %lf \n", heap_size, defer, theta );

    while (heap_size != 0) {
      propagate( defer, theta );
    }
    PRINTD("done propagate \n");

    label_unlabeled(num_poles, deep);
    PRINTD("done label unlabeled \n");
  }
  free_shuffle();
  return root;
}

void exec_third_part( int num_poles )
{
  int i, cnt;
  struct edgesimp *eindex;
  // int k;
  // double tmp_pt[3];

  for ( i=0; i<num_poles; i++) {
    // site s = get_site_offline(i);
    // for (k=0; k<3; k++) {
    //   tmp_pt[k] = toOrig( s[k] );
    // }

    // if ( i < 10 ) {
    //   PRINTD("exec-3 %d: %f %f %f %f %d %f \n ", i, tmp_pt[0],tmp_pt[1],tmp_pt[2],
    //          adjlist[i].sqradius,adjlist[i].label,adjlist[i].samp_distance);
    // }

    if ((adjlist[i].label != IN) && (adjlist[i].label != OUT)) {
      PRINTD("pole %d label %d\n",i, adjlist[i].label);
    } else {
      // if (adjlist[i].label == IN) {
      //   fprintf(INPOLE,"%f %f %f\n",tmp_pt[0],tmp_pt[1],tmp_pt[2]);
      //   fprintf(INPBALL,"%f %f %f %f \n", tmp_pt[0],tmp_pt[1],tmp_pt[2],sqrt(adjlist[i].sqradius));
      // } else if (adjlist[i].label == OUT) {
      //   fprintf(OUTPOLE,"%f %f %f\n",tmp_pt[0],tmp_pt[1],tmp_pt[2]);
      // }

      cnt = 0;
      eindex = adjlist[i].eptr;
      while ( eindex != NULL ) {
        ++ cnt;
        if ((i < eindex->pid) && (antiLabel(adjlist[i].label) == adjlist[eindex->pid].label)) {
          construct_face( eindex->simp, eindex->kth, (i<10) );
        }
        eindex = eindex->next;
      }
      // PRINTD("pole %d followed %d points %d faces %d\n", i, cnt, num_wpole, num_wface );
    }
  }
}

void release_all()
{
  int k;
  for ( k=1; k<ARENA_MAX; ++k) freeArena( k );
  free_sites();
  free_hull_storage();

  if ( adjlist ) { free( adjlist ); adjlist = NULL; }
  if ( opplist ) { free( opplist ); opplist = NULL; }
  if ( pole2 )   { free( pole2 );   pole2   = NULL; }
  if ( pole1 )   { free( pole1 );   pole1   = NULL; }
  if ( lfs_lb )  { free( lfs_lb );  lfs_lb  = NULL; }
  if ( pole2_distance ) { free( pole2_distance ); pole2_distance = NULL; }
  if ( pole1_distance ) { free( pole1_distance ); pole1_distance = NULL; }

  heap_free();
  free_st();
}

void release_wstructs()
{
  #ifdef MAIN
    printf("main: output poles %d faces %d \n", num_wpole, num_wface );
  #else
    LOGI("clear w structs poles %d faces %d \n", num_wpole, num_wface );
  #endif
  // freeArena(0);
  clear_wfaces();
  clear_wpoles();
  reset_next_ptrs();
  // clear_wstore();
}

int driver( 
  #ifdef MAIN 
    FILE * infile, FILE * outfile, FILE * dfile,
  #endif
    long seed, double est_radius, int defer, double deep, double theta, short vol, short bad
  #ifdef MAIN
    , out_func * mof
  #endif
  )
{
  int ret = 0;
  int num_poles = 0;
  #ifdef SIGSEGV_HANDLER
  sighandler_t old_sighandler = signal( SIGSEGV, sigsegv_handler );

  if ( setjmp( env ) == 0 ) {
  #endif
    // set_clear_sites();
    LOGI("driver ... mult_up %lf seed %ld radius %lf defer %d deep %lf theta %lf vol %d bad %d\n",
      mult_up, seed, est_radius, defer, deep, theta, vol, bad );
    
    est_r = est_radius;
    vd    = vol;
    #ifdef MAIN
      #ifdef MYDEBUG
        DFILE   = dfile;
      #endif
      OUTFILE = outfile;
    #else
      out_func * mof = no_out;
    #endif

    #ifdef OUT_PC
      FILE * HEAD;
    #endif

    #ifdef DO_AXIS
      AXIS=fopen("axis","w");
      AXISFACE=fopen("axisface","w");
    #endif
    // POLE=fopen("pole","w");
    // TFILE = efopen(tmpnam(tmpfilenam), "w");

    // if (!poleInput)
    {

      #if 0
        read_next_site(-1);
        PRINTD("input dim=%d\n",dim);
        if (dim > MAXDIM) {
          panic("dimension bound MAXDIM exceeded");
        }
      #else
        dim = 3;
      #endif

      point_size = site_size = sizeof(Coord)*dim;

        // for (num_sites=0; read_next_site(num_sites); num_sites++);
      #ifdef MAIN
        // set next site from input
        { double x, y, z;
          char * line = (char *)malloc(128);
          size_t n = 128; 
          reset_sites( 3 );
          while ( getline( &line, &n, infile ) >= 0 ) {
            n = 128;
            if ( line[0] == '#' ) continue;
            if ( sscanf( line, "%lf %lf %lf", &x, &y, &z ) == 3 ) {
              set_next_site( x, y, z );
            }
          }
          free( line );
        }
      #endif 
    }
    LOGI("done; num_sites %ld. First part ...\n", num_sites);

    num_poles = exec_first_part( dim, seed, bad, mof );
    if ( num_poles > 0 ) {
      vd = 0;
      power_diagram = 1;
      dim = 4;
      LOGI("done; num_poles %d. Second part ...\n", num_poles);
      simplex * rroot = exec_second_part( num_poles, seed, defer, deep, theta, mof );
      if ( rroot != NULL ) {
        /* Enough labeling; let's look at the poles and output a crust!  */
        // INPOLE = fopen("inpole","w");
        // OUTPOLE = fopen("outpole","w");

        /* for visualization of polar balls: */
        // INPBALL = fopen("inpball","w");  /* inner poles with radii */
        // POLEINFO = fopen("tpoleinfo","w");

        LOGI("done; num_poles %d. Third part ...\n", num_poles);
        exec_third_part( num_poles );

        #ifdef OUT_PC
          efclose(PC);
          efclose(PNF);
          // efclose(POLEINFO);
          /* powercrust output done... */
          HEAD = fopen("head","w");
          fprintf(HEAD,"OFF\n");
          fprintf(HEAD,"%d %d %d\n",num_vtxs,num_faces,0);
          efclose(HEAD);
          system("cat head pc pnf > pc.off");
          system("rm head pc pnf");
        #else
          PRINTD("num out points %d faces %d\n", num_wpole, num_wface );
        #endif

        /* compute the medial axis */
        #ifdef DO_AXIS
          PRINTD("\n\n computing the medial axis ....\n");
          make_output( rroot, visit_hull, compute_axis, mof, OUTFILE);

          HEAD = fopen("head","w");
          fprintf(HEAD,"OFF\n");
          fprintf(HEAD,"%d %d %d\n",num_poles,num_axedgs,0);
          efclose(HEAD);
          efclose(AXIS);
          system("cat head pole axis > axis.off");

          HEAD=fopen("head","w");
          fprintf(HEAD,"%d %d \n", num_poles,num_axedgs);
          efclose(HEAD);

          system("cat head tpoleinfo axis > poleinfo");

          HEAD = fopen("head","w");
          fprintf(HEAD,"OFF\n");
          fprintf(HEAD,"%d %d %d\n",num_poles,num_axfaces,0);
          efclose(HEAD);
          efclose(AXISFACE);
          system("cat head pole axisface > axisface.off");
          system("rm -f head pole axis axisface tpoleinfo sp");
          /* power shape output done */

          // efclose(INPOLE);
          // efclose(OUTPOLE);
          // efclose(INPBALL);
          // efclose(TFILE);
        #endif
        ret = 1; // success
      } else {
        LOGI("failed exec 2\n");
      }
    } else {
      LOGI("failed exec 1\n");
    }
  #ifdef SIGSEGV_HANDLER
  } else {
    LOGI("return from long-jump\n");
    ret = 0;
  }
  signal( SIGSEGV, old_sighandler );
  #endif

  // { int k;
  //   for ( k=0; k<ARENA_MAX; ++k ) PRINTD("Arenas %d: %d \n", k, getNumArena(k) );
  // }
  release_all();
  return ret;
}

