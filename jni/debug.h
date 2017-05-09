/** @file debug.h
 *
 * @author marco corvi
 * @date may 2017
 *
 * @brief debug macros. Some from the file hull.h, others new
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
#ifndef DEBUG_H
#define DEBUG_H

#include <stdio.h>

#define DEBUG -20

#ifdef MYDEBUG
  #include <assert.h>
  extern FILE *DFILE;

  #define ASSERT assert

  #define DEBS(qq)  {if (DEBUG>qq) {
  #define EDEBS }}
  #define DEBOUT DFILE
  #define DEB(ll,mes)    DEBS(ll) fprintf(DEBOUT,#mes "\n");fflush(DEBOUT); EDEBS
  #define DEBEXP(ll,exp) DEBS(ll) fprintf(DEBOUT,#exp "=%G\n", (double) exp); fflush(DEBOUT); EDEBS
  #define DEBTR(ll)      DEBS(ll) fprintf(DEBOUT, __FILE__ " line %d \n" ,__LINE__);fflush(DEBOUT); EDEBS
  
  #define warning(lev, x)  \
      {static int messcount;                  \
          if (++messcount<=10) {DEB(lev,x) DEBTR(lev)}    \
          if (messcount==10) DEB(lev, consider yourself warned) \
      } 
  
  
  #define SBCHECK(s) /* nothing */
  /*                              
     {double Sb_check=0;                             \
     int i;                                      \
     for (i=1;i<cdim;i++) if (s->neigh[i].basis)             \
     Sb_check+=s->neigh[i].basis->sqb;       \
     if ((float)(Sb_check - s->Sb) !=0.0)                            \
     {DEBTR DEB(bad Sb); DEBEXP(s->Sb) DEBEXP(Sb_check);print_simplex(s); exit(1);}}
  */

  #ifdef MAIN
    #define LOGI printf
    #define PRINTD(...) fprintf(DFILE, __VA_ARGS__ )
    #define PRINTE(...) fprintf(stderr, __VA_ARGS__ )
  #else
    #include <android/log.h>
    #define LOGI(x...) __android_log_print( ANDROID_LOG_INFO, "Cave3D PC", x)
    #define PRINTD(...) /* nothing */
    #define PRINTE(...) /* nothing */
  #endif

#else
  #include <stdlib.h>
  
  extern FILE * fplog;
  // #define LOGI( x... ) if ( fplog ) { fprintf( fplog, x ); fflush( fplog ); }
  #define LOGI(x...) /* nothing */

  #define ASSERT( x ) /* nothing */
  // #define ASSERT( x ) if ( ! (x) ) LOGI( "FAIL ASSERT %s\n", #x )

  #define DEBS(qq)  {if (DEBUG>qq) {
  #define EDEBS }}
  #define DEBOUT DFILE
  #define DEBTR(ll)      /* nothing */
  #define DEB(ll,mes)    /* nothing */
  #define DEBEXP(ll,exp) /* nothing */
  // #define DEBTR(ll)      /* nothing */
  // #define DEB(ll, mes)   DEBS(ll) LOGI( "%s", #mes ); EDEBS
  // #define DEBEXP(ll,exp) DEBS(ll) LOGI( "%.2f", (float)exp ); EDEBS
  
  #define warning(lev, x)  /* nothing */
  
  #define PRINTD( x... ) LOGI( x )
  #define PRINTE( x... ) LOGI( x )
  
  #define SBCHECK(s) /* nothing */
#endif

#endif
