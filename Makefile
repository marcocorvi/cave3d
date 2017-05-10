CC     = gcc
CPLUS  = g++
AR     = ar
CFLAGS = -g -O0 -Wall -DMAIN
_OBJS  = hull.o ch.o io.o crust.o power.o rand.o pointops.o fg.o math.o \
			   predicates.o heap.o label.o vv_arena.o 
OBJS   = $(patsubst %,jni/%, $(_OBJS))
_HDRS  = hull.h points.h stormacs.h
HDRS   = $(patsubst %,jni/%, $(_HDRS))
_SRC   = hull.c ch.c io.c crust.c power.c rand.c pointops.c fg.c math.c \
			   predicates.c heap.c label.c vv_arena.c
SRC    = $(patsubst %,jni/%, $(_SRC))
PROG   = powercrust
LIB    = lib$(PROG).a

default : $(PROG)

all	: $(PROG) simplify orient

$(OBJS) : $(HDRS)


hullmain.o	: $(HDRS)

$(PROG)	: $(OBJS) jni/hullmain.o jni/main.o
	mkdir -p target
	$(CC) $(CFLAGS) $(OBJS) jni/hullmain.o jni/main.o -o target/$(PROG) -lm
	$(AR) rcv target/$(LIB) $(OBJS)

# -------------------------------------------------------
# powershape.C and setNormals.C are in the original powercrust.zip
# and are not compiled here
#
# simplify: jni/powershape.C jni/sdefs.h
# 	$(CPLUS) -o target/simplify jni/powershape.C -lm
# 
# orient: jni/setNormals.C jni/ndefs.h
# 	$(CPLUS) -o target/orient jni/setNormals.C -lm
#
# -------------------------------------------------------
  
clean	:
	-rm -f $(OBJS) jni/hullmain.o
	-rm -rf target
