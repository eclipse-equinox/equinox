#*******************************************************************************
# Copyright (c) 2000, 2008 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     IBM Corporation - initial API and implementation
#     Kevin Cornell (Rational Software Corporation)
#     Sumit Sarkar (Hewlett-Packard)
#*******************************************************************************
include ../make_version.mak
# Makefile for creating the HPUX/Motif eclipse launcher program.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch
# X11_HOME	 - the full path to X11 header files
# MOTIF_HOME	 - the full path to Motif header files

#ifeq ($(PROGRAM_OUTPUT),)
#  PROGRAM_OUTPUT=eclipse
#endif

DEFAULT_JAVA=DEFAULT_JAVA_EXEC
PROGRAM_LIBRARY=eclipse_$(LIB_VERSION).so

# Define the object modules to be compiled and flags.
CC=gcc
MAIN_OBJS = eclipseMain.o
COMMON_OBJS = eclipseConfig.o eclipseCommon.o eclipseMotifCommon.o eclipseMotifInit.o
DLL_OBJS	= eclipse.o eclipseMotif.o eclipseUtil.o eclipseJNI.o eclipseNix.o eclipseShm.o \
			  NgCommon.o NgImage.o NgImageData.o NgWinBMPFileFormat.o

EXEC = $(PROGRAM_OUTPUT)
DLL = $(PROGRAM_LIBRARY)
LIBS = -L$(MOTIF_HOME)/lib -L$(X11_HOME)/lib -lpthread
MOTIF_LIBS = -DXM_LIB="\"libXm.so.1\"" -DXT_LIB="\"libXt.so.1\"" -DX11_LIB="\"libX11.so.1\""
LFLAGS = -shared -static-libgcc
# -Wl,--export-dynamic
CFLAGS = -O -s \
	-DNO_XINERAMA_EXTENSIONS \
	-DNETSCAPE_FIX \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-D$(DEFAULT_JAVA) \
	-DHPUX \
	$(MOTIF_LIBS) \
	-I./ \
	-I../ \
	-I$(MOTIF_HOME)/include \
	-I$(X11_HOME)/include \
	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/hp-ux 

all: $(EXEC) $(DLL)

.c.o:
	$(CC) $(CFLAGS) -c $< -o $@

eclipse.o: ../eclipse.c ../eclipseOS.h ../eclipseCommon.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c ../eclipse.c -o $@
	
eclipseMain.o: ../eclipseMain.c ../eclipseUnicode.h ../eclipseCommon.h  
	$(CC) $(CFLAGS) -c ../eclipseMain.c -o $@

eclipseCommon.o: ../eclipseCommon.c ../eclipseCommon.h ../eclipseUnicode.h 
	$(CC) $(CFLAGS) -c ../eclipseCommon.c -o $@
	
eclipseUtil.o: ../eclipseUtil.c ../eclipseUtil.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseUtil.c -o $@

eclipseJNI.o: ../eclipseJNI.c ../eclipseCommon.h ../eclipseOS.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c ../eclipseJNI.c -o $@
	
eclipseConfig.o: ../eclipseConfig.c ../eclipseConfig.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseConfig.c -o $@
	
eclipseShm.o: ../eclipseShm.h ../eclipseUnicode.h ../eclipseShm.c
	$(CC) $(CFLAGS) -c ../eclipseShm.c -o $@

eclipseNix.o: ../eclipseNix.c
	$(CC) $(CFLAGS) -c ../eclipseNix.c -o $@

$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS)
	$(CC) -o $(EXEC) $(MAIN_OBJS) $(COMMON_OBJS) $(LIBS)

$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
	$(CC) $(LFLAGS) -o $(DLL) $(DLL_OBJS) $(COMMON_OBJS) $(LIBS)
	
install: all
	cp $(EXEC) $(OUTPUT_DIR)
	cp  $(DLL) $(LIBRARY_DIR)
	rm -f $(EXEC) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)

clean:
	rm -f $(EXEC) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)
