/* @file main.c
 *
 * @brief sample main
 * marco corvi - may 2017
 *
 * ------------------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "defines.h"
#include "debug.h"
#include "points.h"

extern double mult_up;

typedef void out_func( point *, int, FILE*, int);

out_func vlist_out,
         ps_out,
         cpr_out,
         mp_out,
         off_out,
         vv_out;

int driver( FILE * infile, FILE * outfile, FILE * dfile,
            long seed, double est_radius, int defer, double deep, double theta, short vol, short bad,
            out_func * mof );
void release_wstructs();
int getNrPoles();
int getNrFaces();
int getNextFace();
int getFaceSize();
int getFaceVertex( int );


double theta = 0.0; /* input argument - angle defining deep intersection */
double deep = 0.0; /* input argument.. same as theta for labeling unlabled pole */
int defer = 0; /* input argument -D 1 if you don't want to propagate bad poles */

/* int  getopt(int, char**, char*); */
extern char *optarg;
extern int optind;
extern int opterr;

/* sunghee : added vlist_out */
/* functions for different formats */

void errline( const char * s)
{
  fprintf(stderr, "%s\n", s);
  return;
}

void tell_options(void)
{
  errline("options:");
  errline( "-m mult  multiply by mult before rounding;");
  errline( "-s seed  shuffle with srand(seed);");
  errline( "-i<name> read input from <name>;");
  errline( "-X<name> chatter to <name>;");
  errline( "-oF<name>  prefix of output files is <name>;");
  errline( "-t min cosine of allowed dihedral angle btwn polar balls");
  errline( "-v start with vd = 0 ");
  errline( "-w same as -t, but for trying to label unlabled poles, the second time around.");
  errline( "-D no propagation for 1st pole of non-manifold cells");
  errline( "-B throw away both poles for non-manifold cells");
  errline( "-R guess for value of r, used to eliminate bad second poles");
}

// void echo_command_line(FILE *F, int argc, char **argv)
// {
//   fprintf(F,"%%");
//   while (--argc >= 0)
//   {
//     fprintf(F, "%s%s", *argv++, (argc>0) ? " " : "");
//   }
//   fprintf(F,"\n");
// }

char *output_forms[] = {"vn", "ps", "mp", "cpr", "off"};

out_func *out_funcs[] = { &vlist_out, &ps_out, &mp_out, &cpr_out, &off_out };

int set_out_func(char *s)
{
  int i;
  for (i=0;i< sizeof(out_funcs)/(sizeof (out_func*)); i++)
  {
    if (strcmp(s,output_forms[i])==0)
    {
      return i;
    }
  }
  tell_options();
  return 0;
}

int main(int argc, char **argv)
{
  FILE * infile  = NULL;
  FILE * outfile = NULL;
  long seed = 0;
  short // shuffle = 1,
        output = 0,
        // hist = 0,
        vol = 1,
        ofn = 0,
        ifn = 0,
        bad = 0; /* for -B */
  int option;
  char ofile[50] = "",
       ifile[50] = "",
       ofilepre[50] = "";
  // FILE *INPOLE, *OUTPOLE;
  #if defined OUT_PC || defined DO_AXIS
    FILE * HEAD;
  #endif
  // FILE *POLEINFO;
  int main_out_form=0;
  int jj = 1;

  #if 0 // ndef USE_EXEC
    int k, i;
    long poleid = 0;
    int numbadpoles=0;
    int numgoodpoles=0;
    int numopppoles=0;
    simplex *root;
    struct edgesimp *eindex;
  #endif

  // double tmp_pt[3];
  out_func * mof;

  #ifdef DO_AXIS
    fprintf(stderr, "main: with Medial Axis \n");
  #else
    fprintf(stderr, "main: without Medial Axis \n");
  #endif

  /* some default values */
  double est_radius = 1;
  FILE * dfile = stderr;
  int j;

  // while ((option = getopt(argc, argv, "i:m:rs:DBo:X::f:t:w:R:p")) != EOF)
  while ((option = getopt(argc, argv, "i:j:m:rs:DBo:X::f:t:w:R:")) != EOF) 
  {
    switch (option)
    {
      case 'm' :
        sscanf(optarg,"%lf",&mult_up);
        DEBEXP(-4,mult_up);
        break;
      case 's':
        seed = atol(optarg);
        // shuffle = 1;
        break;
      case 'D':
        defer = 1;
        break;
      case 'B':
        bad = 1;
        break;
      case 'i' :
        strcpy(ifile, optarg);
        break;
      case 'X' :
        dfile = fopen(optarg, "w");
        break;
      case 'f' :
        main_out_form = set_out_func(optarg);
        break;
      case 'o':
        switch (optarg[0])
        {
          case 'o':
            output=1;
            break;
          case 'N':
            output=1;
            break; /* output is never set to zero */
          case 'v':
            // vd = vol = 1;
            vol = 0;
            break;
          // case 'h':
          //   hist = 1;
          //   break;
          case 'F':
            strcpy(ofile, optarg+1);
            break;
          default:
            errline("main: illegal option");
            exit(1);
        }
        break;
      case 't':
        sscanf(optarg,"%lf",&theta);
        break;
      case 'w':
        sscanf(optarg,"%lf",&deep);
        break;
      case 'R':
        sscanf(optarg,"%lf",&est_radius);
        break;
      case 'j':
        sscanf(optarg,"%d",&jj);
        break;
      // case 'p':
      //   poleInput=1;
      //   break;
      default :
        tell_options();
        exit(1);
    }
  }

  ifn = (strlen(ifile)!=0);
  printf("main: reading from %s\n", ifn ? ifile : "stdin");
  ofn = (strlen(ofile)!=0);

  strcpy(ofilepre, ofn ? ofile : (ifn ? ifile : "hout") );

  if (output) {
    if (ofn && main_out_form > 0) {
      strcat(ofile, ".");
      strcat(ofile, output_forms[main_out_form]);
    }
    printf("main: output to %s\n", ofn ? ofile : "nil");
  } else {
    printf("main: no output\n");
  }

  mof = out_funcs[main_out_form];

  for ( j=0; j<jj; ++j ) {
    int np, nf, k;
    infile = ifn ? fopen(ifile, "r") : stdin;
    outfile = ofn ? fopen(ofile, "w") : NULL;
    driver( infile, outfile, dfile, seed, est_radius, defer, deep, theta, vol, bad, mof );

    // get the number of poles
    np = getNrPoles();
    nf = getNrFaces();
    LOGI("Poles %d faces %d\n", np, nf );
    for ( k = 0; k<nf; ++k ) {
      int f = getNextFace(); f=f; // suppress warning
      int s = getFaceSize();
      int h;
      for ( h=0; h<s; ++h ) {
        int p = getFaceVertex(h); 
	if ( p >= np ) LOGI("Face %d [%d] has vertex %d / %d\n", f, s, p, np );
      }
    }

    release_wstructs();
    if ( infile && infile != stdin ) fclose( infile );
    if ( outfile ) fclose( outfile );
  }

  fclose(dfile);
  exit(0);
}
