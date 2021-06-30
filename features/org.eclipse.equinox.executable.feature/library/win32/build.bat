@rem *******************************************************************************
@rem  Copyright (c) 2000, 2021 IBM Corporation and others.
@rem 
@rem  This program and the accompanying materials
@rem  are made available under the terms of the Eclipse Public License 2.0
@rem  which accompanies this distribution, and is available at 
@rem  https://www.eclipse.org/legal/epl-2.0/
@rem
@rem SPDX-License-Identifier: EPL-2.0
@rem  
@rem  Contributors:
@rem      IBM Corporation - initial API and implementation
@rem      Kevin Cornell (Rational Software Corporation)
@rem ********************************************************************** 
@rem 
@rem  Usage: sh build.sh [<optional switches>] [clean]
@rem 
@rem    where the optional switches are:
@rem        -output <PROGRAM_OUTPUT>  - executable filename ("eclipse")
@rem        -library <PROGRAM_LIBRARY>- dll filename (eclipse.dll)
@rem        -os     <DEFAULT_OS>      - default Eclipse "-os" value (qnx) 
@rem        -arch   <DEFAULT_OS_ARCH> - default Eclipse "-arch" value (x86) 
@rem        -ws     <DEFAULT_WS>      - default Eclipse "-ws" value (photon)
@rem		-java   <JAVA_HOME>       - location of a Java SDK for JNI headers 
@rem 
@rem 
@rem     This script can also be invoked with the "clean" argument.
@rem
@rem NOTE: The C compiler needs to be setup. This script has been
@rem       tested against Microsoft Visual C and C++ Compiler 6.0.
@rem	
@rem Uncomment the lines below and edit MSVC_HOME to point to the
@rem correct root directory of the compiler installation, if you
@rem want this to be done by this script.
@rem 
@rem ******
@echo off

IF EXIST C:\BUILD\swt-builddir set LAUNCHER_BUILDDIR=C:\BUILD\swt-builddir
IF x.%LAUNCHER_BUILDDIR%==x. set LAUNCHER_BUILDDIR=S:\swt-builddir
echo LAUNCHER build dir: %LAUNCHER_BUILDDIR%

@rem Specify VisualStudio Edition: 'Community', 'Enterprise', 'Professional' etc.
IF "x.%MSVC_EDITION%"=="x." set "MSVC_EDITION=Community"

@rem Specify VisualStudio Version: '2017' or newer '2019'
IF "x.%MSVC_VERSION%"=="x." set "MSVC_VERSION=2019"

GOTO X86_64

:X86_64
shift
set defaultOSArch=x86_64
set PROCESSOR_ARCHITECTURE=AMD64
IF NOT EXIST "%MSVC_HOME%" set "MSVC_HOME=%ProgramFiles(x86)%\Microsoft Visual Studio\%MSVC_VERSION%\BuildTools"
IF NOT EXIST "%MSVC_HOME%" set "MSVC_HOME=%ProgramFiles(x86)%\Microsoft Visual Studio\%MSVC_VERSION%\%MSVC_EDITION%"
IF EXIST "%MSVC_HOME%" (
	echo "Microsoft Visual Studio %MSVC_VERSION% dir: %MSVC_HOME%"
) ELSE (
	echo "WARNING: Microsoft Visual Studio %MSVC_VERSION% was not found."
	echo "     Refer steps for SWT Windows native setup: https://www.eclipse.org/swt/swt_win_native.php"
)
IF NOT EXIST "%JAVA_HOME%" set "JAVA_HOME=%ProgramFiles%\AdoptOpenJDK\jdk-8.0.292.10-hotspot"
IF EXIST "%JAVA_HOME%" (
	echo "JAVA_HOME x64: %JAVA_HOME%"
) ELSE (
	echo "WARNING: x64 Java JDK not found. Please set JAVA_HOME to your JDK directory."
	echo "     Refer steps for SWT Windows native setup: https://www.eclipse.org/swt/swt_win_native.php"
)
set javaHome=%JAVA_HOME%
set makefile=make_win64.mak
call "%MSVC_HOME%\VC\Auxiliary\Build\vcvarsall.bat" x64
GOTO MAKE

:MAKE 
rem --------------------------
rem Define default values for environment variables used in the makefiles.
rem --------------------------
set programOutput=eclipse.exe
set programLibrary=eclipse.dll
set defaultOS=win32
set defaultWS=win32
set OS=Windows

rem --------------------------
rem Parse the command line arguments and override the default values.
rem --------------------------
set extraArgs=
:WHILE
if "%1" == "" goto WHILE_END
    if "%2" == ""       goto LAST_ARG

    if "%1" == "-os" (
		set defaultOS=%2
		shift
		goto NEXT )
    if "%1" == "-arch" (
		set defaultOSArch=%2
		shift
		goto NEXT )
    if "%1" == "-ws" (
		set defaultWS=%2
		shift
		goto NEXT )
    if "%1" == "-output" (
		set programOutput=%2
		shift
		goto NEXT )
	if "%1" == "-library" (
		set programLibrary=%2
		shift
		goto NEXT )
	if "%1" == "-java" (
		set javaHome=%2
		echo %javaHome%
		shift
		goto NEXT )
:LAST_ARG
        set extraArgs=%extraArgs% %1

:NEXT
    shift
    goto WHILE
:WHILE_END

rem --------------------------
rem Set up environment variables needed by the makefile.
rem --------------------------
set PROGRAM_OUTPUT=%programOutput%
set PROGRAM_LIBRARY=%programLibrary%
set DEFAULT_OS=%defaultOS%
set DEFAULT_OS_ARCH=%defaultOSArch%
set DEFAULT_WS=%defaultWS%
set EXEC_DIR=..\..\.\..\..\rt.equinox.binaries\org.eclipse.equinox.executable
set OUTPUT_DIR=%EXEC_DIR%\bin\%defaultWS%\%defaultOS%\%defaultOSArch%
set JAVA_HOME=%javaHome%

rem --------------------------
rem Run nmake to build the executable.
rem --------------------------
if "%extraArgs%" == "" goto MAKE_ALL

nmake -f %makefile% %extraArgs%
goto DONE

:MAKE_ALL
echo Building %OS% launcher. Defaults: -os %DEFAULT_OS% -arch %DEFAULT_OS_ARCH% -ws %DEFAULT_WS%
nmake -f %makefile% clean
nmake -f %makefile% %1 %2 %3 %4
goto DONE


:DONE
