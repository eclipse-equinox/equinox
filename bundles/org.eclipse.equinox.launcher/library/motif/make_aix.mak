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
#*******************************************************************************
 
# Makefile for creating the AIX/Motif eclipse launcher program.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch
# X11_HOME	 - the full path to X11 header files
# MOTIF_HOME	 - the full path to Motif header files

# Define the object modules to be compiled and flags.
OBJS = eclipse.o eclipseUtil.o eclipseShm.o eclipseConfig.o eclipseMotif.o NgCommon.o NgImage.o NgImageData.o NgWinBMPFileFormat.o
EXEC = $(PROGRAM_OUTPUT)
LIBS = -L$(MOTIF_HOME)/lib -lXm -lXt -lX11
CFLAGS = -O -s \
	-DNO_XINERAMA_EXTENSIONS \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
    -DAIX \
	-I./ \
	-I../ \
	-I$(MOTIF_HOME)/include

all: $(EXEC)

.c.o:
	$(CC) $(CFLAGS) -c $< -o $@

eclipse.o: ../eclipse.c ../eclipseOS.h
	$(CC) $(CFLAGS) -c $< -o $@

eclipseUtil.o: ../eclipseUtil.c ../eclipseUtil.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c $< -o $@

eclipseShm.o: ../eclipseShm.c ../eclipseShm.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c $< -o $@
	
eclipseConfig.o: ../eclipseConfig.c ../eclipseConfig.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c $< -o $@

$(EXEC): $(OBJS)
	$(CC) -o $(EXEC) $(OBJS) $(LIBS)

install: all
	cp $(EXEC) $(OUTPUT_DIR)
	rm -f $(EXEC) $(OBJS)

clean:
	rm -f $(EXEC) $(OBJS)
