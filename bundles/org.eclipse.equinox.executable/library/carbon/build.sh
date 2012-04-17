#!/bin/sh
#*******************************************************************************
# Copyright (c) 2000, 2009 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     IBM Corporation - initial API and implementation
#     Kevin Cornell (Rational Software Corporation)
#*******************************************************************************
#
# Usage: sh build.sh [<optional switches>] [clean]
#
#   where the optional switches are:
#       -output <PROGRAM_OUTPUT>  - executable filename ("eclipse")
#       -os     <DEFAULT_OS>      - default Eclipse "-os" value
#       -arch   <DEFAULT_OS_ARCH> - default Eclipse "-arch" value
#       -ws     <DEFAULT_WS>      - default Eclipse "-ws" value
#
#
#    This script can also be invoked with the "clean" argument.

cd `dirname $0`

# Define default values for environment variables used in the makefiles.
programOutput="eclipse"
defaultOS="macosx"
defaultOSArch="x86"
defaultWS="carbon"
makefile="make_carbon.mak"
if [ "$OS" = "" ];  then
    OS=`uname -s`
fi

# Parse the command line arguments and override the default values.
extraArgs=""
while [ "$1" != "" ]; do
    if [ "$1" = "-os" ] && [ "$2" != "" ]; then
        defaultOS="$2"
        shift
    elif [ "$1" = "-arch" ] && [ "$2" != "" ]; then
        defaultOSArch="$2"
        shift
    elif [ "$1" = "-ws" ] && [ "$2" != "" ]; then
        defaultWS="$2"
        shift
    elif [ "$1" = "-output" ] && [ "$2" != "" ]; then
        programOutput="$2"
        shift
    else
        extraArgs="$extraArgs $1"
    fi
    shift
done

# Set up environment variables needed by the makefiles.
PROGRAM_OUTPUT="$programOutput"
DEFAULT_OS="$defaultOS"
DEFAULT_OS_ARCH="$defaultOSArch"
DEFAULT_WS="$defaultWS"
PPC_OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/ppc/Eclipse.app/Contents/MacOS"
X86_OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/x86/Eclipse.app/Contents/MacOS"
X86_64_OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/x86_64/Eclipse.app/Contents/MacOS"

if [ "$DEFAULT_WS" == "cocoa" ]; then
	makefile="make_cocoa.mak"
	export MACOSX_DEPLOYMENT_TARGET=10.5
else
	export MACOSX_DEPLOYMENT_TARGET=10.3	
fi

if [ "$DEFAULT_OS_ARCH" == "x86_64" ]; then
	echo "build x86_64"
	ARCHS="-arch x86_64"
	PROGRAM_OUTPUT_DIR=$X86_64_OUTPUT_DIR
	DEFAULT_OS_ARCH="x86_64"
else
	echo "build x86 and ppc"
	ARCHS="-arch i386 -arch ppc"
	PROGRAM_OUTPUT_DIR=$X86_OUTPUT_DIR
fi
 
export PPC_OUTPUT_DIR X86_OUTPUT_DIR X86_64_OUTPUT_DIR PROGRAM_OUTPUT DEFAULT_OS DEFAULT_OS_ARCH DEFAULT_WS ARCHS PROGRAM_OUTPUT_DIR

# If the OS is supported (a makefile exists)
if [ "$makefile" != "" ]; then
	if [ "$extraArgs" != "" ]; then
		make -f $makefile $extraArgs
	else
		echo "Building $OS launcher. Defaults: -os $DEFAULT_OS -arch $DEFAULT_OS_ARCH -ws $DEFAULT_WS"
		make -f $makefile clean
		make -f $makefile all
		make -f $makefile install
	fi
else
	echo "Unknown OS ($OS) -- build aborted"
fi
