/** @file hull.h
 *
 * @date may 2017
 *
 * @brief struct, function declarations, and externs
 *
 * --------------------------------------------------------
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
 * 
 * --------------------------------------------------------
 * The original file was a significant modification of Ken Clarkson's file hull.h
 * Here is included his copyright notice in accordance with its terms
 * as did the authors of powercrust (Nina, Sunghee and Ravi)
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
#ifndef HULL_H
#define HULL_H 

#include "points.h"
#include "stormacs.h"

#ifdef MYFILE
  FILE* efopen(char *, char *);
  void  efclose(FILE* file);
#endif

// point and site are both Coord* (array of Coord's)
// they differ by the length of the array
typedef point site;

extern site p;          /* the current site */

// extern Coord coordsAtInfinity[10];  /* point at infinity for Delaunay triang */

extern int
  rdim,   /* region dimension: (max) number of sites specifying region */
  cdim,   /* number of sites currently specifying region */
  site_size, /* size of memory needed for a site */
  point_size;  /* size of memory needed for a point */

typedef struct basis_s
{
  struct basis_s *next; /* free list */
  int ref_count;  /* storage management */
  int lscale;    /* the log base 2 of total scaling of vector */
  Coord sqa, sqb; /* sums of squared norms of a part and b part */
  Coord vecs[1]; /* the actual vectors, extended by allocating larger memory */
} basis_s;

STORAGE_GLOBALS(basis_s)


typedef struct neighbor
{
  site vert; /* vertex of simplex */
  /*        short edgestatus[3];  FIRST_EDGE if not visited
            NOT_POW if not dual to powercrust faces
            POW if dual to powercrust faces */
  struct simplex *simp; /* neighbor sharing all vertices but vert */
  basis_s *basis; /* derived vectors */
} neighbor;

typedef struct simplex
{
  struct simplex *next;   /* used in free list */
  short mark;
  site vv; /* Voronoi vertex of simplex ; sunghee */
  double sqradius; /* squared radius of Voronoi ball */
  /*        site av; */ /* averaged pole */
  /*        double cond; */
  /*    float Sb; */
  short status;/* sunghee : 0(CNV) if on conv hull so vv contains normal vector;
                  1(VV) if vv points to circumcenter of simplex;
                  -1(SLV) if cond=0 so vv points to hull
                  2(AV) if av contains averaged pole */
  long poleindex; /* for 1st DT, if status==POLE_OUTPUT, contains poleindex; for 2nd, contains vertex index for powercrust output for OFF file format */
  short edgestatus[6]; /* edge status :(01)(02)(03)(12)(13)(23)
                          FIRST_EDGE if not visited
                          VISITED
                          NOT_POW if not dual to powercrust faces
                          POW if dual to powercrust faces */
  /*  short tristatus[4];   triangle status :
      FIRST if not visited
      NO   if not a triangle
      DEG  if degenerate triangle
      SURF if surface triangle
      NORM if fails normal test
      VOR  if falis voronoi edge test
      VOR_NORM if fails both test */
  /* NOTE!!! neighbors has to be the LAST field in the simplex stucture,
     since it's length gets altered by some tricky Clarkson-move.
     Also peak has to be the one before it.
     Don't try to move these babies!! */
  long visit;     /* number of last site visiting this simplex */
  basis_s* normal;    /* normal vector pointing inward */
  neighbor peak;      /* if null, remaining vertices give facet */
  neighbor neigh[1];  /* neighbors of simplex */
} simplex;
STORAGE_GLOBALS(simplex)

#ifdef USE_STORE
typedef struct wpole /* pole coords with weight */
{
  double x, y, z;
  double w;
  struct wpole * next;
} wpole;

typedef struct wface
{
  int n; // number of points
  long * idx;
  struct wface * next;
} wface;

// struct wpole * new_wstore( struct wpole * next );
struct wpole * new_wpole( struct wpole * next );
struct wface * new_wface( struct wface * next, int n );
  
#endif

/* Ravi:  for the thinning stuff */
/* represent a node in the graph */
// 
// typedef struct spole { /* simple version to rep neighbors */
//   long index;
//   struct spole *next;
// } snode;
// 
// typedef struct vpole
// {
//   long index; /* index of the node */
//   long pindex; /* index in the actual list of poles */
//   double px;
//   double py;
//   double pz;
//   double pr;  /* the radius of the ball centered here */
//   double perpma; /* perpendicular distance from sample to medial axis */
//   double pw;
//   snode  *adj;
//   int status;  /* for thinning */
//   int label;  /* might be useful for computing the crust again */
//   long substitute; /* if removed points to the substitute node */
//   double estlfs; /* the estimated lfs of each ball */
// } vnode;

/* edges in the powerface */

typedef struct enode
{
  long sindex;
  long dindex;
} edge;

typedef struct fnode
{
  long index1;
  long index2;
  long index3;
} face;

/* end defn for medial axis thinning */

/* structure for list of opposite poles, opplist. */
typedef struct plist {
    long   pid;
    double angle;
    struct plist * next;
} plist;

/* regular triangulation edge, between pole pid to center of simp? */
typedef struct edgesimp
{
  short kth;
  double angle;   /* angle between balls */
  struct simplex * simp;
  long pid;
  struct edgesimp * next;
} edgesimp;

/* additional info about poles: label for pole, pointer to list of regular
   triangulation edges, squared radius of  polar ball. adjlist is an
   array of polelabels. */
typedef struct polelabel
{
  struct edgesimp *eptr;
  short bad;
  short label;
  double in; /* 12/7/99 Sunghee for priority queue */
  double out; /* 12/7/99 Sunghee for priority queue */
  int hid; /* 0 if not in the heap, otherwise heap index 1..heap_size*/
  double sqradius;
  double oppradius; /* minimum squared radius of this or any opposite ball */
  double samp_distance;
  int grafindex; /* index in thinning graph data structure */
} polelabel;

typedef struct temp
{
  struct simplex *simp;
  int vertptr[3];
  int novert;
  /* 0,1,2,3 : 3 vertices but ts->neigh[ti].vert are vertices of triangle */
} temp;

typedef struct tarr
{
  int tempptr[50];
  int num_tempptr;
  long vert;
} tarr;

/*
typedef struct tlist {
  int tempptr;
  struct tlist *next;
} tlist;
*/

#ifdef MYTREE
typedef struct fg_node fg;
typedef struct tree_node Tree;
struct tree_node 
{
  Tree *left, *right;
  site key;
  int size;   /* maintained to be the number of nodes rooted here */
  fg *fgs;
  Tree *next; /* freelist */
};

STORAGE_GLOBALS(Tree)


typedef struct fg_node
{
  Tree *facets;
  double dist, vol;   /* of Voronoi face dual to this */
  fg *next;       /* freelist */
  short mark;
  int ref_count;
} fg_node;

STORAGE_GLOBALS(fg)

/* from fg.c, for face graphs */
fg *build_fg(simplex*);
void print_edge_dat(fg *, FILE *);

#endif // MYTREE

typedef void* visit_func(simplex *, void *);
typedef int test_func(simplex *, int, void *);
typedef void out_func(point *, int, FILE*, int);

/* Ravi thin axis */

// void thinaxis();
// void printaxis();
// void initialize();

/* from driver, e.g., hullmain.c */

typedef site gsitef(void);

// extern gsitef *get_site;

typedef long site_n(site);
extern site_n *site_num;

site get_site_offline(long); /* sunghee */
site get_next_site(void);

double toScaled( double x );
double toOrig( double x );

typedef short zerovolf(simplex *);

extern double Huge;

void construct_face(simplex *, short, int print );

/* from segt.c or ch.c */

simplex *build_convex_hull( /* gsitef*, */ site_n*, short, short);

void free_hull_storage(void);

int sees(site, simplex *);

int out_of_flat(simplex*, site);

void set_ch_root(simplex*);

visit_func check_marks;

test_func alph_test;
void* visit_outside_ashape(simplex*, visit_func);

void get_basis_sede(simplex *);

    /* for debugging */
int check_perps(simplex*);

// void find_volumes(fg*, FILE*); // NOT USED

#define MAXPOINTS 10000
extern short mi[MAXPOINTS], mo[MAXPOINTS];

/* from hull.c */


void *visit_triang_gen(simplex *, visit_func, test_func);
void *visit_triang(simplex *, visit_func);
void* visit_hull(simplex *, visit_func);

neighbor *op_simp(simplex *a, simplex *b);

neighbor *op_vert(simplex *a, site b);

simplex *new_simp(void);

void buildhull(simplex *);

/* from io.c */

void panic(char *fmt, ...);

// void check_triang(simplex*);
// void check_new_triangs(simplex *);

short is_bound(simplex *);

#ifdef MAIN
out_func vlist_out,
         ps_out,
         cpr_out,
         mp_out,
         off_out,
         vv_out;
#endif
out_func no_out;

/* sunghee : added vlist_out */
/* functions for different formats */

/* added compute axis RAVI */
// visit_func facets_print,
//            afacets_print,
//            ridges_print;

visit_func compute_vv,
           compute_pole1,
           compute_pole2,
           test_surface,
           compute_2d_power_vv,
           compute_3d_power_vv;
           // compute_3d_power_edges,

#ifdef DO_AXIS
  visit_func compute_axis;
#endif
/* to print facets, alpha facets, ridges */
/* Sunghee added compute_cc, compute_pole1, compute_pole2, test_surface */

// void test_temp(); // undefined

/* Nina's functions in crust.c */
short is_bound(simplex *);
int antiLabel(int);
int cantLabelAnything(int);
void labelPole(int,int);
void newOpposite(int, int, double);
double computePoleAngle(simplex*, simplex*, double*);

#if 1 // def USE_STORE
void storePole( struct wpole * store,
                simplex*, int, double*, int*,double);
#else
void outputPole( // FILE*,
                 FILE*,
                 simplex*, int, double*, int*,double);
#endif

/* from pointops.c */

Coord maxdist(int,point p1, point p2);

/* from rand.c */

double double_rand(void);
void init_rand(long seed);

/* from predicates.c, math.c */
void normalize(double*);
double sqdist(double*, double*);
void dir_and_dist(double*, double*, double*, double*);
double dotabac(double*, double*, double*);
double maxsqdist(double*, double*, double*, double*);
double dotabc(double*, double*, double*);
void crossabc(double*, double*, double*, double*);
void tetcircumcenter(double*, double*, double*, double*, double*,double*);
void tricircumcenter3d(double*, double*, double*, double*,double*);
void exactinit();
double orient3d(double*, double*, double*, double*);
double orient2d(double*, double*, double*);

void free_st();

/* label.c */
// void opp_update(int, int );
// void sym_update(int, int, double);
void update_pri(int,int);
int propagate();
void label_unlabeled(int, double);

/*power.c */
int correct_orientation(double*,double*,double*,double*,double*);

// MYPRINT
typedef void print_neighbor_f(FILE*, neighbor*);

extern print_neighbor_f
     print_neighbor_full,
     print_neighbor_snum;

void print_site(site, FILE*);
// void print_normal(simplex*);
void print_point(FILE*, int, point);
void print_point_int(FILE*, int, point);
void print_extra_facets(void);
void *print_facet(FILE*, simplex*, print_neighbor_f*);
void print_basis(FILE*, basis_s*);
void *print_simplex_f(simplex*, FILE*, print_neighbor_f*);
void *print_simplex(simplex*, void*);
void print_triang(simplex*, FILE*, print_neighbor_f*);

#endif
