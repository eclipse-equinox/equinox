#*******************************************************************************
# Copyright (c) 2000, 2010 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     IBM Corporation - initial API and implementation
#     Kevin Cornell (Rational Software Corporation)
#     Tom Tromey (Red Hat, Inc.)
# Martin Oberhuber (Wind River) - [176805] Support building with gcc and debug
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
# JAVA_HOME        - JAVA_HOME for JNI headers

#ifeq ($(PROGRAM_OUTPUT),)
  PROGRAM_OUTPUT=eclipse
#endif

PROGRAM_LIBRARY=$(PROGRAM_OUTPUT)_$(LIB_VERSION).so

# Define the object modules to be compiled and flags.
MAIN_OBJS = eclipseMain.o
COMMON_OBJS = eclipseConfig.o eclipseCommon.o eclipseGtkCommon.o eclipseGtkInit.o
DLL_OBJS	= eclipse.o eclipseGtk.o eclipseUtil.o eclipseJNI.o eclipseMozilla.o eclipseShm.o eclipseNix.o
PICFLAG = -K PIC
# Optimize and remove all debugging information by default
OPTFLAG = -O -s
# OPTFLAG = -g

EXEC = $(PROGRAM_OUTPUT)
DLL = $(PROGRAM_LIBRARY)
#LIBS = `pkg-config --libs-only-L gtk+-2.0` -lgtk-x11-2.0 -lgdk_pixbuf-2.0 -lgobject-2.0 -lgdk-x11-2.0 -lglib-2.0 -lthread -ldl -lc
LIBS = -lthread -ldl -lc -lrt
GTK_LIBS = -DGTK_LIB="\"libgtk-x11-2.0.so.0\"" -DGDK_LIB="\"libgdk-x11-2.0.so.0\"" -DPIXBUF_LIB="\"libgdk_pixbuf-2.0.so.0\"" -DGOBJ_LIB="\"libgobject-2.0.so.0\"" -DX11_LIB="\"libX11.so.4\""
LFLAGS = -G
CFLAGS = $(OPTFLAG) \
	-DSOLARIS \
	$(PICFLAG) \
	-DMOZILLA_FIX \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	$(GTK_LIBS) \
	-I. \
	-I.. \
	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/solaris \
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
	$(CC) -o $(EXEC) $(MAIN_OBJS) $(COMMON_OBJS) $(LIBS)

$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
	$(CC) $(LFLAGS) -o $(DLL) $(DLL_OBJS) $(COMMON_OBJS) $(LIBS)

install: all
	cp $(EXEC) $(OUTPUT_DIR)
	cp $(DLL) $(LIBRARY_DIR)
	rm -f $(EXEC) $(DLL) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)

clean:
	rm -f $(EXEC) $(DLL) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)
