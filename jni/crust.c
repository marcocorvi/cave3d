/* @file crust.c 
 * 
 * @brief computation of the crust
 *
 * --------------------------------------------------------
 * These file has been slightly modified. marco corvi - may 2017
 *
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 *
 * --------------------------------------------------------
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
 */
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <float.h>
#include <math.h>

#include "defines.h" /* must be first */
#include "debug.h"
#include "constants.h"
#include "hull.h"
#include "vv_arena.h"

// #define SMALL_ENOUGH .0001 // not USED (crust.c)

extern struct simplex **pole1;
extern struct simplex **pole2;
extern double* lfs_lb; /* array of estimated lower bounds on lfs */
extern struct polelabel *adjlist;
extern struct plist **opplist;
extern double est_r;  /* guess for r */

extern double ominmax[6];

// extern long   num_sites;
// extern double thinthreshold;
// extern int *rverts;

/* variables for tracking infinite loop */
/* (This should never occur, but it did in early versions) */

int loopStart = -1;
int count = 0;
int lastCount = 0;
int FALSE = (1==0);
int TRUE = (1==1);

int isAtInfinity( point );

short is_bound(simplex *s)
{
  int i;
  for (i=0;i<4;i++) {
    if (   (s->neigh[i].vert[0] > ominmax[3]) || (s->neigh[i].vert[0] < ominmax[0])
        || (s->neigh[i].vert[1] > ominmax[4]) || (s->neigh[i].vert[1] < ominmax[1])
        || (s->neigh[i].vert[2] > ominmax[4]) || (s->neigh[i].vert[2] < ominmax[2]) )
      return 1;
  }
  return 0;
}

/* computes Voronoi vertices  */
void * compute_vv( simplex * s, void * p ) //, struct simplex ** pole1 
{
  // static out_func *out_func_here;
  point v[MAXDIM];
  int i,j,k,inf=0;
  double cc[3], cond, ta[4][3];
  // double slvnum, sqrad;

  if (p) {
        // out_func_here = (out_func*)p;
        if (!s) return NULL;
  }

  for (j=0;j<cdim;j++) {
    v[j] = s->neigh[j].vert;
    /* v[j] stores coordinates of j'th vertex of simplex s; j=0..3 */
    if ( isAtInfinity( v[j] ) ) { // v[j]==coordsAtInfinity /* means simplex s is on the convex hull */
      inf=1;
      break; /* skip the rest of the for loop, ignore convex hull faces (= bounding box ) */
    }
    i=(site_num)(v[j]); /* i is the index of the vertex v[j] */
    for (k=0;k<cdim-1;k++) {
      ta[j][k] = toOrig( v[j][k] ); /* restore original coords   */
      /*    inf=0, ta[0],ta[1],ta[2],ta[3] are 4 vertices of s     */
    }
  }

  if (!inf) { /* if not faces on convex hull, compute circumcenter*/
    tetcircumcenter(ta[0], ta[1], ta[2], ta[3], cc, &cond);
    /* cc is the displacement of circumcenter from ta[0] */
    /* cond is the denominator ( orient3d ) value        */
    // sqrad = SQ(cc[0])+SQ(cc[1])+SQ(cc[2]);
    // slvnum = SQ(cond)/(sqrad*sqrad*sqrad);

    /*	  PRINTD("%f %f %f\n",cond,slvnum,sqrad);*/
    /*  sqd = 4*maxsqdist(ta[0],ta[1],ta[2],ta[3]); */
    if (cond!=0) { /* ignore them if cond = 0 */
      s->vv = (Coord*) getArena( ARENA_COORDS, sizeof(Coord)*3 );
      for (k=0;k<cdim-1;k++) {
        s->vv[k] = ta[0][k]+cc[k];
      }
      /* if (slvnum<0.00000000001) s->status = PSLV;
         else */
      s->status = VV;
  	/*fprintf(CC,"%f %f %f\n",s->vv[0],s->vv[1],s->vv[2]); */
    } else { /* if cond=0, s is SLIVER */
      /* PRINTD("cond=%f sliver!\n", cond); */
      s->vv = NULL;
      s->status = SLV;
      /* modification (no longer used) : set the previous vv as the new vv
         s->vv = prevsimp->vv;
         s->status = prevsimp->status; */
    }
  } else { /* if on conv hull */
    s->status = CNV;
    /*
       s->vv = (Coord*) malloc(sizeof(Coord)*3);
       crossabc(ta[0],ta[1],ta[2],norm);
       cross product of 3 non-infinite vertices
       check that this normal is the right sign
       pointing_in = 0;
       for (k=0; k<NRAND; k++) {
       if (dotabc(ta[0],get_site_offline(rverts[k]),norm) > SMALL_ENOUGH)
       pointing_in = 1;
       }
       if (pointing_in) {
       norm[0] *= -1; norm[1] *= -1; norm[2] *= -1;
       }
       s->status = CNV;
       s->vv[0]=norm[0];
       s->vv[1]=norm[1];
       s->vv[2]=norm[2];
    */
  }
  /*    	prevsimp = s;  */

  for (j=0;j<cdim;j++) {  /* compute 1st pole for vertex j */
    i=(site_num)(s->neigh[j].vert);
    if (i==-1) continue;

    /* Ignore poles that are too far away to matter - a relic of the
       original California-style crust. Probably no longer needed */
    if ((s->neigh[j].vert[0] < ominmax[0]) || (s->neigh[j].vert[0] > ominmax[3]) ||
      (s->neigh[j].vert[1] < ominmax[1]) || (s->neigh[j].vert[1] > ominmax[4]) ||
      (s->neigh[j].vert[2] < ominmax[2]) || (s->neigh[j].vert[2] > ominmax[5]) ) {

      /* if (i > (num_sites - 8) ) { /if bounding box vertex */
      pole1[i]=NULL;
      continue;
    } else {
      if (pole1[i]==NULL) {
        /* the vertex i is encountered for the 1st time */
        if (s->status==VV) { /* we don't store infinite poles */
          pole1[i]=s;
          continue;
        }
      }
      if (( s->status == VV) && ( pole1[i]->status == VV)) {
        if ( sqdist(pole1[i]->vv,ta[j]) < sqdist(s->vv,ta[j]) ) {
          pole1[i]=s; /* update 1st pole */
        }
      }
    }
  }
  return NULL;
}

void *compute_pole2( simplex * s, void * p ) // , struct simplex ** pole1, struct simplex ** pole2 
{
  point v[MAXDIM];
  int i,j,k,inf=0;
  double a[3];
  site t;
  double dir_s[3], dir_p[3], dist_s,dist_p, cos_sp, est_lfs;
  double cos_2r;

  if (p) {
    if (!s) return NULL;
  }

  for (j=0;j<cdim;j++) {
    v[j] = s->neigh[j].vert;
    i=(site_num)(v[j]);
    if (i==-1) inf=1;
  }

  cos_2r = cos( 2 * est_r );

  for (j=0;j<cdim;j++) {  /* compute 2nd poles */
    t=s->neigh[j].vert;
    i=(site_num)(t);
    if (i<0) continue; /* not a vertex */
    if (inf) { /* on conv hull */
      if (s->status == CNV) {
          continue;
      } else {
        PRINTE("cannot happen7\n");
      }
    }
    if (!pole1[i]) {
      /*		  PRINTE("no 1st pole\n"); */
      continue;
    }

    if (pole1[i]->vv==NULL) {
      PRINTE("cannot happen8\n");
      continue;
    }

    if (!s->vv) {
      if (s->status != SLV) PRINTE("cannot happen3\n")
        ;
      continue;
    }

    for (k=0;k<cdim-1;k++)  { /* a stores orig vertex coord */
      a[k] = toOrig( t[k] );
    }

    /* compute direction and length of vector from sample to first pole */

    dir_and_dist(a,pole1[i]->vv,dir_p,&dist_p);

    /* We have a vertex, and there is a good first pole. */
    if ((s->status==VV)&&(pole1[i]->status==VV)) {
      /* make direction vector from sample to this Voronoi vertex */
      dir_and_dist(a, s->vv, dir_s, &dist_s);

      /* cosine of angle between angle to vertex and angle to pole */
      cos_sp = dir_s[0]*dir_p[0] + dir_s[1]*dir_p[1] + dir_s[2]*dir_p[2];

      /* if there is an estimate for r, use it to estimate lfs */
      if (est_r < 1.0) {
        /* near vertices - should be close to sample (a) */
        if ((cos_sp < cos_2r) && (cos_sp > -cos_2r)) { /* use to get lower bound on lfs */
          est_lfs = dist_s /est_r * ((sqrt(1- cos_sp*cos_sp)) - est_r);
          if (est_lfs > lfs_lb[i]) lfs_lb[i] = est_lfs;
        }
      } else {
        lfs_lb[i] = 0;
      }

      if (cos_sp > 0) { /* s->vv is in the same side of pole1  */
        continue;
      }

      /* s->vv is a candidate for pole2 */
      if (!pole2[i]) { /* 1st pole2 candidate for vertex i */
        pole2[i]=s;
        continue;
      } else if (!pole2[i]->vv) { /* 2nd pole points null */
        PRINTE("cannot happen4\n");
        continue;
      } else if ((pole2[i]->status == VV) && (sqdist(a,pole2[i]->vv)<sqdist(a,s->vv)) ) {
        pole2[i]=s; /* update 2nd pole */
      }
    }
  }
  /*	out_func_here(v,cdim,DFILE,0); */
  return NULL;
}

int antiLabel(int label)
{
  if (label == IN)  return(OUT);
  if (label == OUT) return(IN);
  return(label);
}

/* labels a pole */
void labelPole(int pid,int label)
{
  adjlist[pid].label = label;
  if (pid == loopStart) {
    loopStart = -1;
  }
}

/* checks to see if list of unlabeled poles is looping infinitely */
int cantLabelAnything(int pid)
{
  if (loopStart == -1) {
    loopStart = pid;
    count = 0;
    lastCount = 0;
    return(FALSE);
  }

  if (pid == loopStart) {
    if (count == lastCount) {
      /* infinite loop! */
      PRINTE("Infinite loop! Can't label any more! \n");
      return(TRUE);
    } else {
      /* we labeled something last time through */
      lastCount = count;
      count = 0;
    }
  } else {
    /* in the middle of the loop */
    count++;
  }
  return(FALSE);
}


/* computes angle between two opposite poles */
double computePoleAngle( simplex* p1, simplex* p2, double* samp) 
{
  double x1 = p1->vv[0] - samp[0];
  double y1 = p1->vv[1] - samp[1];
  double z1 = p1->vv[2] - samp[2];
  double x2 = p2->vv[0] - samp[0];
  double y2 = p2->vv[1] - samp[1];
  double z2 = p2->vv[2] - samp[2];
  return ( x1*x2 + y1*y2 + z1*z2 ) / sqrt( (x1*x1 + y1*y1 + z1*z1) * (x2*x2 + y2*y2 + z2*z2) );
  // return ( ((p1->vv[0]-samp[0])*(p2->vv[0]-samp[0]) +
  //           (p1->vv[1]-samp[1])*(p2->vv[1]-samp[1]) +
  //           (p1->vv[2]-samp[2])*(p2->vv[2]-samp[2]) )/
  //          (sqrt(SQ(p1->vv[0]-samp[0])+
  //                SQ(p1->vv[1]-samp[1])+
  //                SQ(p1->vv[2]-samp[2]))*
  //           sqrt(SQ(p2->vv[0]-samp[0])+
  //                SQ(p2->vv[1]-samp[1])+
  //                SQ(p2->vv[2]-samp[2]))) );
}

/* Adds a new pair of opposite poles to each other's lists */
void newOpposite( int pid1, int pid2, double pole_angle)
{
  plist * newplist = (struct plist *) getArena( ARENA_PLIST, sizeof(struct plist));
  newplist->pid    = pid2;
  newplist->angle  = pole_angle;
  newplist->next   = opplist[ pid1 ]; // put newlist at head of opplist[pid1]
  opplist[ pid1 ]  = newplist;
  if (adjlist[pid1].oppradius > adjlist[pid2].sqradius) {
    // ASSERT( adjlist[pid2].sqradius > 0.0 );
    adjlist[pid1].oppradius = adjlist[pid2].sqradius;
  }
}


#ifdef USE_STORE
/* Save a pole, saving it's squared radius in adjlist */
void storePole( struct wpole * store,
                simplex* pole, int poleid,
                double* samp, int* num_poles,double distance)
{
  double r2, weight;

  r2 = SQ(pole->vv[0]-samp[0]) + SQ(pole->vv[1]-samp[1]) + SQ(pole->vv[2]-samp[2]);
  weight = SQ(pole->vv[0])+SQ(pole->vv[1])+SQ(pole->vv[2])- r2;

  pole->status = POLE_OUTPUT;
  pole->poleindex = poleid;

  /* for computing powercrust */
  store->x = pole->vv[0];
  store->y = pole->vv[1];
  store->z = pole->vv[2];
  store->w = weight;

  /* remember squared radius */
  adjlist[poleid].sqradius = r2;
  adjlist[poleid].samp_distance=distance;

  /* initialize perp dist to MA */
  adjlist[poleid].oppradius = r2;

  /* initialize */
  adjlist[poleid].grafindex = -1;

  /* keep count! */
  (*num_poles)++;
}
#else
/* Outputs a pole, saving it's squared radius in adjlist */
void outputPole( // FILE* POLE,
                 FILE* SPFILE,
                 simplex* pole, int poleid,
                 double* samp, int* num_poles,double distance)
{
  double r2, weight;

  r2 = SQ(pole->vv[0]-samp[0]) + SQ(pole->vv[1]-samp[1]) + SQ(pole->vv[2]-samp[2]);
  weight = SQ(pole->vv[0])+SQ(pole->vv[1])+SQ(pole->vv[2])- r2;

  pole->status = POLE_OUTPUT;
  pole->poleindex = poleid;

  /* debugging file */
  // fprintf(POLE,"%f %f %f\n",pole->vv[0], pole->vv[1], pole->vv[2]);

  /* for computing powercrust */
  fprintf(SPFILE,"%f %f %f %f\n",pole->vv[0], pole->vv[1], pole->vv[2], weight);

  /* remember squared radius */
  adjlist[poleid].sqradius = r2;
  adjlist[poleid].samp_distance=distance;

  /* initialize perp dist to MA */
  adjlist[poleid].oppradius = r2;

  /* initialize */
  adjlist[poleid].grafindex = -1;

  /* keep count! */
  (*num_poles)++;
}
#endif
