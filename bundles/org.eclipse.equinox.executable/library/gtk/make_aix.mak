#*******************************************************************************
# Copyright (c) 2010, 2011 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     IBM Corporation - initial API and implementation
#     Kevin Cornell (Rational Software Corporation)
#     Tom Tromey (Red Hat, Inc.)
#*******************************************************************************
include ../make_version.mak
# Makefile for creating the GTK eclipse launcher program.
#
# This makefile expects the utility "pkg-config" to be in the PATH.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch
# JAVA_HOME      - JAVA_HOME for jni headers      
#default value for PROGRAM_OUTPUT

PROGRAM_OUTPUT=eclipse
PROGRAM_LIBRARY=$(PROGRAM_OUTPUT)_$(LIB_VERSION).so


# Define the object modules to be compiled and flags.
CC=gcc
MAIN_OBJS = eclipseMain.o
COMMON_OBJS = eclipseConfig.o eclipseCommon.o eclipseGtkCommon.o eclipseGtkInit.o
DLL_OBJS	= eclipse.o eclipseGtk.o eclipseUtil.o eclipseJNI.o eclipseMozilla.o eclipseShm.o eclipseNix.o

EXEC = $(PROGRAM_OUTPUT)
DLL = $(PROGRAM_LIBRARY)
#LIBS = `pkg-config --libs-only-L gtk+-2.0` -lgtk-x11-2.0 -lgdk_pixbuf-2.0 -lgobject-2.0 -lgdk-x11-2.0 -lpthread -ldl -lX11
LIBS = -lpthread -ldl

X11_LIB_ppc = shr4.o
X11_LIB_ppc64 = shr_64.o
X11_LIB = -DX11_LIB="\"libX11.a($(X11_LIB_$(DEFAULT_OS_ARCH)))\""
GTK_LIBS = -DGTK_LIB="\"libgtk-x11-2.0.a(libgtk-x11-2.0.so.0)\"" \
		   -DGDK_LIB="\"libgdk-x11-2.0.a(libgdk-x11-2.0.so.0)\"" \
		   -DPIXBUF_LIB="\"libgdk_pixbuf-2.0.a(libgdk_pixbuf-2.0.so.0)\"" \
		   -DGOBJ_LIB="\"libgobject-2.0.a(libgobject-2.0.so.0)\"" \
		   $(X11_LIB)
		   
LFLAGS = ${M_ARCH} -shared
CFLAGS = ${M_ARCH} -g -s -Wall\
	-fpic \
	-DAIX \
	-DMOZILLA_FIX \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-D$(DEFAULT_JAVA) \
	$(GTK_LIBS) \
	-I. \
	-I.. \
	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
	`pkg-config --cflags gtk+-2.0`

all: $(EXEC) $(DLL)

eclipse.o: ../eclipse.c ../eclipseOS.h ../eclipseCommon.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c ../eclipse.c -o eclipse.o

eclipseMain.o: ../eclipseUnicode.h ../eclipseCommon.h ../eclipseMain.c 
	$(CC) $(CFLAGS) -c ../eclipseMain.c -o eclipseMain.o
	
eclipseCommon.o: ../eclipseCommon.h ../eclipseUnicode.h ../eclipseCommon.c
	$(CC) $(CFLAGS) -c ../eclipseCommon.c

eclipseGtkCommon.o: ../eclipseCommon.h ../eclipseOS.h eclipseGtk.h eclipseGtkCommon.c
	$(CC) $(CFLAGS) -c eclipseGtkCommon.c -o eclipseGtkCommon.o
	
eclipseGtkInit.o: ../eclipseCommon.h eclipseGtk.h eclipseGtkInit.c
	$(CC) $(CFLAGS) -c eclipseGtkInit.c -o eclipseGtkInit.o
	
eclipseUtil.o: ../eclipseUtil.c ../eclipseUtil.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseUtil.c -o eclipseUtil.o

eclipseJNI.o: ../eclipseJNI.c ../eclipseCommon.h ../eclipseOS.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c ../eclipseJNI.c -o eclipseJNI.o
	
eclipseConfig.o: ../eclipseConfig.c ../eclipseConfig.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseConfig.c -o eclipseConfig.o
	
eclipseMozilla.o: ../eclipseMozilla.c ../eclipseMozilla.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseMozilla.c -o eclipseMozilla.o

eclipseShm.o: ../eclipseShm.h ../eclipseUnicode.h ../eclipseShm.c
	$(CC) $(CFLAGS) -c ../eclipseShm.c -o eclipseShm.o

eclipseNix.o: ../eclipseNix.c
	$(CC) $(CFLAGS) -c ../eclipseNix.c -o eclipseNix.o

$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS)
	$(CC) ${M_ARCH} -Wl,-bM:UR -o $(EXEC) $(MAIN_OBJS) $(COMMON_OBJS) $(LIBS)
	sedmgr -c exempt $(EXEC)
	
$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
	$(CC) $(LFLAGS) -o $(DLL) $(DLL_OBJS) $(COMMON_OBJS) $(LIBS)

install: all
	cp $(EXEC) $(OUTPUT_DIR)
	cp $(DLL) $(LIBRARY_DIR)
	rm -f $(EXEC) $(DLL) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)

clean:
	rm -f $(EXEC) $(DLL) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)
