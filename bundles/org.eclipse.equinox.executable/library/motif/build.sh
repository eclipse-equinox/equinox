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
#     Sumit Sarkar (Hewlett-Packard)
# Martin Oberhuber (Wind River) - [185734] Support building with gcc and debug
#*******************************************************************************
#
# Usage: sh build.sh [<optional switches>] [clean]
#
#   where the optional switches are:
#       -output <PROGRAM_OUTPUT>  - executable filename ("eclipse")
#       -os     <DEFAULT_OS>      - default Eclipse "-os" value
#       -arch   <DEFAULT_OS_ARCH> - default Eclipse "-arch" value
#       -ws     <DEFAULT_WS>      - default Eclipse "-ws" value
#		-java   <JAVA_HOME>		  - java insgtall for jni headers
#
#   All other arguments are directly passed to the "make" program.
#   This script can also be invoked with the "clean" argument.
#
#   Examples:
#   sh build.sh clean
#   sh build.sh -java /usr/j2se OPTFLAG=-g PICFLAG=-fpic

cd `dirname $0`

# Define default values for environment variables used in the makefiles.
programOutput="eclipse"
defaultOS=""
defaultOSArch=""
defaultWS="motif"
defaultJava=DEFAULT_JAVA_JNI
makefile=""
javaHome=""
outputRoot="../../bin"
if [ "$OS" = "" ];  then
    OS=`uname -s`
fi
if [ "$MODEL" = "" ];  then
    MODEL=`uname -m`
fi

case $OS in
	"AIX")
		makefile="make_aix.mak"
		defaultOS="aix"
		defaultOSArch="ppc"
		defaultWS="motif"
		MOTIF_HOME=/usr
		OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
		;;
	"Linux")
		makefile="make_linux.mak"
		defaultOS="linux"
		defaultOSArch="x86"
		defaultWS="motif"
		X11_HOME=/usr/X11R6
		MOTIF_HOME=~/motif21
		OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
		;;
	"SunOS")
#		PATH=/usr/ccs/bin:/opt/SUNWspro/bin:$PATH
		PATH=/usr/ccs/bin:/export/home/SUNWspro/bin:$PATH
		[ -d /bluebird/teamswt/swt-builddir/build/JRE/SPARC/jdk1.6.0_14 ] && javaHome="/bluebird/teamswt/swt-builddir/build/JRE/SPARC/jdk1.6.0_14"
		outputRoot="../../contributed"
		export PATH
		makefile="make_solaris.mak"
		defaultOS="solaris"
		defaultOSArch="sparc"
		defaultWS="motif"
		OS="Solaris"
		X11_HOME=/usr/openwin
		MOTIF_HOME=/usr/dt
		OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
		;;
	"HP-UX")
		X11_HOME=/usr
		MOTIF_HOME=/usr
		case $MODEL in
			"ia64")
				makefile="make_hpux_ia64_32.mak"
				defaultOS="hpux"
				defaultOSArch="ia64_32"
				defaultWS="motif"
				OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
				javaHome="/opt/java1.5"
				defaultJava=DEFAULT_JAVA_EXEC
				PATH=/opt/hp-gcc/bin:$PATH
				export PATH
				;;
        	*)
				makefile="make_hpux_PA_RISC.mak"
				defaultOS="hpux"
				defaultOSArch="PA_RISC"
				defaultWS="motif"
				OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
		esac
        ;;
	*)
		echo "Unknown OS -- build aborted"
		;;
esac

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
	elif [ "$1" = "-java" ] && [ "$2" != "" ]; then
        javaHome="$2"
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
JAVA_HOME=$javaHome
DEFAULT_JAVA=$defaultJava

LIBRARY_DIR="../../../org.eclipse.equinox.launcher/fragments/org.eclipse.equinox.launcher.$defaultWS.$defaultOS.$defaultOSArch"
OUTPUT_DIR="$outputRoot/$defaultWS/$defaultOS/$defaultOSArch"

export OUTPUT_DIR PROGRAM_OUTPUT DEFAULT_OS DEFAULT_OS_ARCH DEFAULT_WS X11_HOME MOTIF_HOME JAVA_HOME DEFAULT_JAVA LIBRARY_DIR

# If the OS is supported (a makefile exists)
if [ "$makefile" != "" ]; then
	if [ "$extraArgs" != "" ]; then
		make -f $makefile $extraArgs
	else
		echo "Building $OS launcher. Defaults: -os $DEFAULT_OS -arch $DEFAULT_OS_ARCH -ws $DEFAULT_WS"
		make -f $makefile clean
		case x$CC in
		  x*gcc*) make -f $makefile all PICFLAG=-fpic ;;
		  *)      make -f $makefile all ;;
		esac
	fi
else
	echo "Unknown OS ($OS) -- build aborted"
fi
