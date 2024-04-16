@rem *******************************************************************************
@rem  Copyright (c) 2000, 2024 IBM Corporation and others.
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
@rem  Usage: .\build.bat [<optional switches>] [clean]
@rem 
@rem    where the optional switches are:
@rem        -output <PROGRAM_OUTPUT>  - executable filename ("eclipse.exe")
@rem        -library <PROGRAM_LIBRARY>- dll filename (eclipse.dll)
@rem        -os     <DEFAULT_OS>      - default Eclipse "-os" value (win32) 
@rem        -arch   <DEFAULT_OS_ARCH> - default Eclipse "-arch" value (x86_64) 
@rem        -ws     <DEFAULT_WS>      - default Eclipse "-ws" value (win32)
@rem		-java   <JAVA_HOME>       - location of a Java SDK for JNI headers and libraries
@rem 
@rem 
@rem     This script can also be invoked with the "clean" argument.
@rem
@rem NOTE: The C compiler needs to be setup. This script has been
@rem       tested against Microsoft Visual C and C++ Compiler 2022.
@rem	
@rem Uncomment the lines below and edit MSVC_HOME to point to the
@rem correct root directory of the compiler installation, if you
@rem want this to be done by this script.
@rem 
@rem ******
@echo off

@rem Specify VisualStudio Edition: 'Community', 'Enterprise', 'Professional' etc.
IF "%MSVC_EDITION%"=="" set "MSVC_EDITION=auto"

@rem Specify VisualStudio Version: '2022', '2019' etc.
IF "%MSVC_VERSION%"=="" set "MSVC_VERSION=auto"

@rem Search for a usable Visual Studio
@rem ---------------------------------
IF "%MSVC_HOME%"=="" echo "'MSVC_HOME' was not provided, auto-searching for Visual Studio..."
@rem Bug 574007: Path used on Azure build machines
IF "%MSVC_HOME%"=="" CALL :FindVisualStudio "%ProgramFiles(x86)%\Microsoft Visual Studio\$MSVC_VERSION$\BuildTools"
@rem Bug 578519: Common installation paths; VisualStudio is installed in x64 ProgramFiles since VS2022
IF "%MSVC_HOME%"=="" CALL :FindVisualStudio "%ProgramFiles%\Microsoft Visual Studio\$MSVC_VERSION$\$MSVC_EDITION$"
IF "%MSVC_HOME%"=="" CALL :FindVisualStudio "%ProgramFiles(x86)%\Microsoft Visual Studio\$MSVC_VERSION$\$MSVC_EDITION$"
@rem Report
IF EXIST "%MSVC_HOME%" (
	echo "MSVC_HOME: %MSVC_HOME%"
) ELSE (
	echo "WARNING: Microsoft Visual Studio was not found (for edition=%MSVC_EDITION% version=%MSVC_VERSION%)"
	echo "     Refer steps for SWT Windows native setup: https://www.eclipse.org/swt/swt_win_native.php"
)
IF EXIST "%JAVA_HOME%" (
	echo "JAVA_HOME x64: %JAVA_HOME%"
) ELSE (
	echo "WARNING: x64 Java JDK not found. Please set JAVA_HOME to your JDK directory."
	echo "     Refer steps for SWT Windows native setup: https://www.eclipse.org/swt/swt_win_native.php"
)
set javaHome=%JAVA_HOME%
set makefile=make_win64.mak
call "%MSVC_HOME%\VC\Auxiliary\Build\vcvarsall.bat" x64

rem --------------------------
rem Define default values for environment variables used in the makefiles.
rem --------------------------
set programOutput=eclipse.exe
set programLibrary=eclipse.dll
set defaultOS=win32
set defaultWS=win32
set defaultOSArch=x86_64

rem --------------------------
rem Parse the command line arguments and override the default values.
rem --------------------------
set extraArgs=

:WHILE
if "%1" == "" goto WHILE_END
    if "%2" == "" (
		goto LAST_ARG )
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
set JAVA_HOME=%javaHome%
IF "%BINARIES_DIR%"=="" set "BINARIES_DIR=..\..\..\..\..\rt.equinox.binaries"
IF "%EXE_OUTPUT_DIR%"=="" set "EXE_OUTPUT_DIR=%BINARIES_DIR%\org.eclipse.equinox.executable\bin\%defaultWS%\%defaultOS%\%defaultOSArch%"
IF "%LIB_OUTPUT_DIR%"=="" set "LIB_OUTPUT_DIR=%BINARIES_DIR%\org.eclipse.equinox.launcher.%defaultWS%.%defaultOS%.%defaultOSArch%"

rem --------------------------
rem Run nmake to build the executable.
rem --------------------------
IF NOT "%extraArgs%"=="" (
	nmake -f %makefile% %extraArgs%
) ELSE (
	echo Building Windows launcher. Defaults: -ws %DEFAULT_WS% -os %DEFAULT_OS% -arch %DEFAULT_OS_ARCH%
	nmake -f %makefile% clean
	nmake -f %makefile% %1 %2 %3 %4
)

GOTO :EOF


@rem Find Visual Studio
@rem %1 = path template with '$MSVC_VERSION$' and '$MSVC_EDITION$' tokens
:FindVisualStudio
	IF "%MSVC_VERSION%"=="auto" (
		CALL :FindVisualStudio2 "%~1" "2022"
		CALL :FindVisualStudio2 "%~1" "2019"
	) ELSE (
		CALL :FindVisualStudio2 "%~1" "%MSVC_VERSION%"
	)
GOTO :EOF

@rem Find Visual Studio
@rem %1 = path template with '$MSVC_VERSION$' and '$MSVC_EDITION$' tokens
@rem %2 = value for '$MSVC_VERSION$'
:FindVisualStudio2
	IF "%MSVC_EDITION%"=="auto" (
		CALL :FindVisualStudio3 "%~1" "%~2" "Community"
		CALL :FindVisualStudio3 "%~1" "%~2" "Enterprise"
		CALL :FindVisualStudio3 "%~1" "%~2" "Professional"
	) ELSE (
		CALL :FindVisualStudio3 "%~1" "%~2" "%MSVC_EDITION%"
	)
GOTO :EOF

@rem Find Visual Studio and set '%MSVC_HOME%' on success
@rem %1 = path template with '$MSVC_VERSION$' and '$MSVC_EDITION$' tokens
@rem %2 = value for '$MSVC_VERSION$'
@rem %3 = value for '$MSVC_EDITION$'
:FindVisualStudio3
	@rem Early return if already found
	IF NOT "%MSVC_HOME%"=="" GOTO :EOF

	SET "TESTED_VS_PATH=%~1"
	@rem Substitute '$MSVC_VERSION$' and '$MSVC_EDITION$'
	CALL SET "TESTED_VS_PATH=%%TESTED_VS_PATH:$MSVC_VERSION$=%~2%%"
	CALL SET "TESTED_VS_PATH=%%TESTED_VS_PATH:$MSVC_EDITION$=%~3%%"

	@rem If the folder isn't there, then skip it without printing errors
	IF NOT EXIST "%TESTED_VS_PATH%" GOTO :EOF

	@rem Try this path
	IF NOT EXIST "%TESTED_VS_PATH%\VC\Auxiliary\Build\vcvarsall.bat" (
		echo "-- VisualStudio '%TESTED_VS_PATH%' is bad: 'vcvarsall.bat' not found"
		GOTO :EOF
	)
	echo "-- VisualStudio '%TESTED_VS_PATH%' looks good, selecting it"
	SET "MSVC_HOME=%TESTED_VS_PATH%"
GOTO :EOF
