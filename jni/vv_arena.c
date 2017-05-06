/** @file vv_arena.c
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
#include <stdlib.h>
#include <string.h>

#include "defines.h" /* must be first */
#include "debug.h"

#define ARENA_SIZE 65536

struct arena 
{
  int ptr;
  unsigned char mem[ ARENA_SIZE ];
  struct arena * next;
};

struct arena * vv_arena[]  = { NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL };
struct arena * cur_arena[] = { NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL };
int num_arena[] = { 0, 0, 0, 0, 0, 0 , 0, 0 };

void * getArena( int type, int size )
{
  struct arena * ca = cur_arena[type];
  if ( ca == NULL || ca->ptr + size > ARENA_SIZE ) {
    ca = (struct arena *)malloc( sizeof(struct arena) );
    if ( ca == NULL ) {
      fprintf(stderr, "ERROR malloc failure\n");
      return NULL;
    }
    ca->ptr = size;
    memset( ca->mem, 0, ARENA_SIZE );
    ca->next = vv_arena[type];
    vv_arena[type] = cur_arena[type] = ca;
    ++ num_arena[type];
    return (void*)(ca->mem);
  }
  void * ret = (void *)(ca->mem+ca->ptr);
  ca->ptr += size;
  return ret;
}

void freeArena( int type )
{
  while ( vv_arena[type] != NULL ) {
    cur_arena[type] = vv_arena[type]->next;
    free( vv_arena[type] );
    vv_arena[type] = cur_arena[type];
  }
  num_arena[type] = 0;
}

int getNumArena( int type ) { return num_arena[type]; }


