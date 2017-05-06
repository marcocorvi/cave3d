/* @file hull.c
 *
 * @brief "combinatorial" functions for hull computation 
 *
 * ------------------------------------------------------------------
 * this file has been slightly modified - marco corvi may 2017
 *
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 *
 * ------------------------------------------------------------------
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

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <stdarg.h>
#include <string.h>

#include "defines.h" /* must be first */
#include "debug.h"
#include "constants.h"
#include "hull.h"


site p;
long pnum;

int rdim,   /* region dimension: (max) number of sites specifying region */
    cdim,   /* number of sites currently specifying region */
    site_size, /* size of memory needed for a site */
    point_size;  /* size of memory needed for a point */

STORAGE(simplex)

// global to be preserved between calls
simplex ** st1 = NULL;
simplex ** st2 = NULL;
long ss_visit = 2000;
long ss_search = MAXDIM;
long v_num = -1;
simplex * ns_make_facets = NULL;
int scount =0;

// #define push(st,x) *(st+tms++) = x;
// #define pop(st,x)  x = *(st + --tms);

/** starting at s, visit simplices t such that test(s,i,0) is true, and t is the i'th neighbor of s;
 * apply visit function to all visited simplices; when visit returns nonNULL, exit and return its value
 */
void *visit_triang_gen(simplex *s, visit_func *visit, test_func *test)
{
  neighbor *sn;
  void * v;
  simplex * t;
  int i;
  long tms = 0;
  void * ret = NULL;

  v_num--;
  if (!st1) {
    LOGI( "visit simplex * malloc %lu\n", (ss_visit+MAXDIM+1)*sizeof(simplex*) );
    st1 = (simplex**)malloc((ss_visit+MAXDIM+1)*sizeof(simplex*));
    if ( st1 == NULL ) return NULL; // FIXME
  }
  if (s) st1[tms++] = s;
  while (tms) {
    if (tms>ss_visit) {
      // DEBEXP(-1,tms);
      ss_visit *= 2;
      LOGI( "visit simplex * realloc %lu\n", (ss_visit+MAXDIM+1)*sizeof(simplex*) );
      st1 = (simplex**)realloc(st1, (ss_visit+MAXDIM+1)*sizeof(simplex*));
      if ( st1 == NULL ) return NULL; // FIXME
    }
    t = st1[--tms];
    if (!t || t->visit == v_num) continue;
    t->visit = v_num;
    if ((v=(*visit)(t,0))!=NULL) {
       ret = v;
       break;
    }
    for (i=-1, sn = t->neigh-1; i<cdim; i++, sn++) {
      if ((sn->simp->visit != v_num) && sn->simp && test(t,i,0)) {
        st1[tms++] = sn->simp;
      }
    }
  }
  return ret;
}

int truet(simplex *s, int i, void *dum) { return 1; }
int hullt(simplex *s, int i, void *dum) { return i > -1; }
void * facet_test(simplex *s, void *dum) { return (!s->peak.vert) ? s : NULL; }

void *visit_triang(simplex *root, visit_func *visit) /* visit the whole triangulation */
{ return visit_triang_gen(root, visit, truet); }

/* visit all simplices with facets of the current hull */
void *visit_hull(simplex *root, visit_func *visit)
{ return visit_triang_gen(visit_triang(root, &facet_test), visit, hullt); }

#ifdef MYDEBUG
  #define lookup(a,b,what,whatt)     \
  {                                  \
      int i;                         \
      neighbor *x;                   \
      for (i=0, x = a->neigh; (x->what != b) && (i<cdim) ; i++, x++); \
      if (i<cdim)                         \
          return x;                       \
      else {                              \
        DEBS(-10)                         \
          fprintf(DFILE,"adjacency failure,op_" #what ":\n"); \
          DEBTR(-10)                      \
          print_simplex_f(a, DFILE, &print_neighbor_full);    \
          print_##whatt(b, DFILE);                \
          fprintf(DFILE,"---------------------\n");       \
          print_triang(a,DFILE, &print_neighbor_full);        \
        EDEBS                        \
        exit(1);                     \
        return 0;                    \
      }                              \
  }                                   
#else
  #define lookup(a,b,what,whatt)     \
  {                                  \
      int i;                         \
      neighbor *x;                   \
      for (i=0, x = a->neigh; (x->what != b) && (i<cdim) ; i++, x++); \
      if (i<cdim)                    \
          return x;                  \
      else {                         \
        exit(1);                     \
        return 0;                    \
      }                              \
  }                                  
#endif


/* the neighbor entry of a containing b */
neighbor *op_simp(simplex *a, simplex *b) { lookup(a,b,simp,simplex); }

/* the neighbor entry of a containing b */
neighbor *op_vert(simplex *a, site b) { lookup(a,b,vert,site); }


/** make neighbor connections between newly created simplices incident to p */
void connect(simplex *s)
{
    site xf,xb,xfi;
    simplex *sb, *sf, *seen;
    int i;
    neighbor *sn;

    if (!s) return;
    ASSERT(!s->peak.vert
           && s->peak.simp->peak.vert==p
           && !op_vert(s,p)->simp->peak.vert);
    if (s->visit==pnum) return;
    s->visit = pnum;
    seen = s->peak.simp;
    xfi = op_simp(seen,s)->vert;
    for (i=0, sn = s->neigh; i<cdim; i++,sn++) {
        xb = sn->vert;
        if (p == xb) continue;
        sb = seen;
        sf = sn->simp;
        xf = xfi;
        if (!sf->peak.vert) {   /* are we done already? */
            sf = op_vert(seen,xb)->simp;
            if (sf->peak.vert) continue;                
        } else do {
            xb = xf;
            xf = op_simp(sf,sb)->vert;
            sb = sf;
            sf = op_vert(sb,xb)->simp;
        } while (sf->peak.vert);

        sn->simp = sf;
        op_vert(sf,xf)->simp = s;

        connect(sf);
    }

}

/* visit simplices s with sees(p,s), and make a facet for every neighbor of s not seen by p */
simplex * make_facets(simplex *seen)
{
    simplex *n;
    neighbor *bn;
    int i;

    if (!seen) return NULL;
    #ifdef MYDEBUG
      DEBS(-1) ASSERT(sees(p,seen) && !seen->peak.vert);
      EDEBS
    #endif
    seen->peak.vert = p;

    for (i=0,bn = seen->neigh; i<cdim; i++,bn++) {
        n = bn->simp;
        if (pnum != n->visit) {
            n->visit = pnum;
            if (sees(p,n)) make_facets(n);
        } 
        if (n->peak.vert) continue;
        copy_simp(ns_make_facets,seen);
        ns_make_facets->visit = 0;
        ns_make_facets->peak.vert = 0;
        ns_make_facets->normal = 0;
        ns_make_facets->peak.simp = seen;
        /*      ns_make_facets->Sb -= ns_make_facets->neigh[i].basis->sqb; */
        NULLIFY(basis_s,ns_make_facets->neigh[i].basis);
        ns_make_facets->neigh[i].vert = p;
        bn->simp = op_simp(n,seen)->simp = ns_make_facets;
    }
    return ns_make_facets;
}

/** p lies outside flat containing previous sites;
 *  make p a vertex of every current simplex, and create some new simplices
 */
simplex * extend_simplices(simplex *s)
{
  int i;
  int ocdim=cdim-1;
  simplex * ns;
  neighbor * nsn;

  if (s->visit == pnum) return s->peak.vert ? s->neigh[ocdim].simp : s;
  s->visit = pnum;
  s->neigh[ocdim].vert = p;
  NULLIFY(basis_s,s->normal);
  NULLIFY(basis_s,s->neigh[0].basis);
  if (!s->peak.vert) {
    s->neigh[ocdim].simp = extend_simplices(s->peak.simp);
    return s;
  } else {
    copy_simp(ns,s);
    s->neigh[ocdim].simp = ns;
    ns->peak.vert = NULL;
    ns->peak.simp = s;
    ns->neigh[ocdim] = s->peak;
    inc_ref(basis_s,s->peak.basis);
    for ( i=0, nsn=ns->neigh; i<cdim; i++, nsn++) {
      nsn->simp = extend_simplices(nsn->simp);
    }
  }
  return ns;
}

/** return a simplex s that corresponds to a facet of the current hull, and sees(p, s) */
simplex * search(simplex *root)
{
  simplex * ret = NULL;
  simplex *s;
  neighbor *sn;
  int i;
  long tms = 0;

  if ( ! st2 ) {
    LOGI( "search simplex * malloc %lu\n", (ss_search+MAXDIM+1)*sizeof(simplex*) );
    st2 = (simplex **)malloc((ss_search+MAXDIM+1)*sizeof(simplex*));
    if ( st2 == NULL ) return NULL; // FIXME
  }
  st2[tms++] = root->peak.simp;
  root->visit = pnum;
  if (!sees(p,root)) {
    for (i=0, sn=root->neigh; i<cdim; i++, sn++) st2[tms++] = sn->simp;
  }
  while (tms) {
    if (tms>ss_search) {
      ss_search *= 2;
      LOGI( "search simplex * realloc %lu\n", (ss_search+MAXDIM+1)*sizeof(simplex*) );
      st2=(simplex**)realloc(st2, (ss_search+MAXDIM+1)*sizeof(simplex*));
      if ( st2 == NULL ) return NULL; // FIXME
    }
    s = st2[--tms];
    if (s->visit == pnum) continue;
    s->visit = pnum;
    if (!sees(p,s)) continue;
    if (!s->peak.vert) {
      ret = s;
      break;
    }
    for (i=0, sn=s->neigh; i<cdim; i++,sn++) st2[tms++] = sn->simp;
  }
  return ret;
}

void free_st()
{
  if ( st1 ) { free( st1 ); st1 = NULL; }
  if ( st2 ) { free( st2 ); st2 = NULL; }
  ss_visit  = 2000;
  ss_search = MAXDIM;
  v_num = -1;
  ns_make_facets = NULL;
  scount = 0;
}

point get_another_site(void)
{
  point pnext;
  #ifdef MYDEBUG
    DEBS(-2)
      if (!(++scount%100)) PRINTD("site %d... \n", scount);
    EDEBS
  #endif
  /*  check_triang(); */
  // pnext = (*get_site)();
  pnext = get_next_site();
  if (!pnext) return NULL;
  pnum = site_num(pnext)+2;
  return pnext;
}

void buildhull (simplex *root)
{
    int k = 0;
    if ( root && root->vv ) LOGI("build hull: root %.6lf %.6lf %.6lf\n", root->vv[0], root->vv[1], root->vv[2] );
    while (cdim < rdim) {
        ++k;
        p = get_another_site();
        if (!p) {
          PRINTD("get another site returns NULL at %d \n", k);
          return;
        }
        if (out_of_flat(root,p)) {
          // PRINTD("%d out of flat true cdim %d\n", k, cdim );
          extend_simplices(root);
        } else {
          connect(make_facets(search(root)));
        }
    }
    LOGI("build hull: %d in buildhull cdim %d rdim %d\n", k, cdim, rdim );
    while ( (p = get_another_site()) != NULL )
        connect(make_facets(search(root)));
}
