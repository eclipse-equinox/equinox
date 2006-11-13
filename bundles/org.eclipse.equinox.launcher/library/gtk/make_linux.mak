#*******************************************************************************
# Copyright (c) 2000, 2005 IBM Corporation and others.
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

# Makefile for creating the GTK eclipse launcher program.
#
# This makefile expects the utility "pkg-config" to be in the PATH.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch

# Define the object modules to be compiled and flags.
CC=gcc
OBJS = eclipse.o eclipseUtil.o eclipseShm.o eclipseConfig.o eclipseMozilla.o eclipseGtk.o
EXEC = $(PROGRAM_OUTPUT)
LIBS = `pkg-config --libs-only-L gtk+-2.0` -lgtk-x11-2.0 -lgdk_pixbuf-2.0 -lgobject-2.0 -lgdk-x11-2.0
CFLAGS = -O -s \
	-fpic \
	-DLINUX \
	-DMOZILLA_FIX \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-I. \
	-I.. \
	`pkg-config --cflags gtk+-2.0`

all: $(EXEC)

eclipse.o: ../eclipse.c ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipse.c -o eclipse.o

eclipseUtil.o: ../eclipseUtil.c ../eclipseUtil.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseUtil.c -o eclipseUtil.o

eclipseShm.o: ../eclipseShm.c ../eclipseShm.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseShm.c -o eclipseShm.o

eclipseConfig.o: ../eclipseConfig.c ../eclipseConfig.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseConfig.c -o eclipseConfig.o
	
eclipseMozilla.o: ../eclipseMozilla.c ../eclipseMozilla.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseMozilla.c -o eclipseMozilla.o

$(EXEC): $(OBJS)
	$(CC) -o $(EXEC) $(OBJS) $(LIBS)

install: all
	cp $(EXEC) $(OUTPUT_DIR)
	rm -f $(EXEC) $(OBJS)

clean:
	rm -f $(EXEC) $(OBJS)
