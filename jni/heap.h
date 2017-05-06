/** @file heap.h
 *
 * @author marco corvi
 * @date may 2017
 *
 * @brief heap function declarations, extracted from the hull.h
 *
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
#ifndef HEAP_H
#define HEAP_H

int  heap_init(int num);
void heap_free();
void heap_heapify(int hi);
int  heap_extract_max();
int  heap_insert(int pid, double pri);
int  heap_get_size();
void heap_update(int hi, double pri);

/* heap.c */
typedef struct heap_array {
    int pid;
    double pri;
} heap_array;

#endif
