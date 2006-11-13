#**********************************************************************
# Copyright (c) 2000, 2005 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
#   
# Contributors: 
#     Kevin Cornell (Rational Software Corporation)
#********************************************************************** 

# Makefile for creating the Carbon eclipse launcher program.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch

# Define the object modules to be compiled and flags.
OBJS = eclipse.o eclipseUtil.o eclipseShm.o eclipseConfig.o eclipseCarbon.o NgImageData.o NgWinBMPFileFormat.o NgCommon.o
EXEC = $(PROGRAM_OUTPUT)
LIBS = -framework Carbon
ARCHS = -arch i386 -arch ppc
CFLAGS = -O -s \
	-Wall \
	$(ARCHS) \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-I.. -I../motif

all: $(EXEC)

.c.o: ../eclipseOS.h
	$(CC) $(CFLAGS) -c $< -o $@

eclipseUtil.o: ../eclipseUtil.c ../eclipseUtil.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseUtil.c -o $@

eclipseShm.o: ../eclipseShm.c ../eclipseShm.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseShm.c -o $@
	
eclipseConfig.o: ../eclipseConfig.c ../eclipseConfig.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseConfig.c -o $@

NgCommon.o: ../motif/NgCommon.c
	$(CC) $(CFLAGS) -c ../motif/NgCommon.c -o $@

NgWinBMPFileFormat.o: ../motif/NgWinBMPFileFormat.c
	$(CC) $(CFLAGS) -c ../motif/NgWinBMPFileFormat.c -o $@

NgImageData.o: ../motif/NgImageData.c
	$(CC) $(CFLAGS) -c ../motif/NgImageData.c -o $@

$(EXEC): $(OBJS)
	$(CC) -o $(EXEC) $(ARCHS) $(OBJS) $(LIBS)

install: all
	cp $(EXEC) $(PPC_OUTPUT_DIR)
	cp $(EXEC) $(X86_OUTPUT_DIR)
	rm -f $(EXEC) $(OBJS)

clean:
	rm -f $(EXEC) $(OBJS)
