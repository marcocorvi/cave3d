/* @file heap.c
 *
 * @brief
 *
 * ------------------------------------------------------------------
 * this file is used only if MYTREE is defined and has been only mildly modified
 * (function renaming)
 * marco corvi, May 2017
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
 */
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <float.h>
#include <math.h>

#include "defines.h" /* must be first */
#include "debug.h"
#include "hull.h"
#include "heap.h"

#define LEFT(i) ((i)*2)
#define RIGHT(i) ((i)*2+1)
#define PARENT(i) ((i)/2)

extern struct polelabel *adjlist;

struct heap_array * heap_A = NULL;
int heap_length = 0;
int heap_size   = 0;

int heap_get_size() { return heap_size; }

int heap_init(int num)
{
  heap_A = (struct heap_array *)calloc(num, sizeof(struct heap_array));
  if ( heap_A == NULL ) return 0;
  heap_size = 0;
  heap_length = num;
  #ifdef MYDEBUG
    DEBS(-2)
      fprintf(DFILE,"heap %d initialized\n", num);
    EDEBS
  #endif
  return 1;
}

void heap_free()
{
  if ( heap_A ) free( heap_A );
  heap_A      = NULL;
  heap_size   = 0;
  heap_length = 0;
}

void heap_heapify(int hi)
{
    int largest;
    int temp;
    double td;
 
    if ((LEFT(hi) <= heap_size) && (heap_A[LEFT(hi)].pri > heap_A[hi].pri))
        largest = LEFT(hi);
    else largest = hi;
  
    if ((RIGHT(hi) <= heap_size) && (heap_A[RIGHT(hi)].pri > heap_A[largest].pri))
        largest = RIGHT(hi);
  
    if (largest != hi) {
        temp = heap_A[hi].pid;
        heap_A[hi].pid = heap_A[largest].pid;
        adjlist[heap_A[hi].pid].hid = hi;
        heap_A[largest].pid = temp;  
        adjlist[heap_A[largest].pid].hid = largest;
        td =  heap_A[hi].pri;
        heap_A[hi].pri = heap_A[largest].pri;
        heap_A[largest].pri = td;
        heap_heapify(largest);
    }

}

int heap_extract_max()
{
    int max;

    if (heap_size < 1) return -1;
    max = heap_A[1].pid;
    heap_A[1].pid = heap_A[heap_size].pid;
    heap_A[1].pri = heap_A[heap_size].pri;  
    adjlist[heap_A[1].pid].hid = 1;
    heap_size--;
    heap_heapify(1);
    return max;
}

int heap_insert(int pi, double pr)
{
    int i;

    heap_size++;
    /*printf("heap_size %d\n",heap_size); */
    i = heap_size;
    while ((i>1)&&(heap_A[PARENT(i)].pri < pr)) {
        heap_A[i].pid = heap_A[PARENT(i)].pid;
        heap_A[i].pri = heap_A[PARENT(i)].pri;
        adjlist[heap_A[i].pid].hid = i;
        i = PARENT(i);
    };
    heap_A[i].pri = pr;
    heap_A[i].pid = pi;
    adjlist[pi].hid = i;
    return i;
}

/* make the element heap_A[hi].pr = pr ... */
void heap_update(int hi, double pr) 
{
  int pi,i;

  heap_A[hi].pri = pr;
  pi = heap_A[hi].pid;

  if (pr > heap_A[PARENT(hi)].pri) { 
    i = hi;
    while ((i>1)&&(heap_A[PARENT(i)].pri < pr)) {
      heap_A[i].pid = heap_A[PARENT(i)].pid;
      heap_A[i].pri = heap_A[PARENT(i)].pri;
      adjlist[heap_A[i].pid].hid = i;
      i = PARENT(i);
    };
    heap_A[i].pri = pr;
    heap_A[i].pid = pi;
    adjlist[pi].hid = i;
  }
  else heap_heapify(hi);
}



