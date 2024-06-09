This feature contains the executables and launcher bundle along with its fragments used to launch Equinox respectivly Eclipse based standalone applications.
It also hosts the native C source files and scripts to build these executable and launcher library binaries for all supported platforms.

The _Equinox launcher executable_ is the executable binary file that can be used to launch an Equinox based applications like Eclipse. It is for example named `eclipse.exe` (on Windows) or `eclipse` (on Linux and Mac).
The _launcher library_ is the native dynamic/shared library (eclipse.dll, eclipse.so, ...) that contains the compiled native code called from the [`org.eclipse.equinox.launcher.JNIBridge`](../../bundles/org.eclipse.equinox.launcher/src/org/eclipse/equinox/launcher/JNIBridge.java).

# Building the native executable and launcher binaries

## Required software

The software listed below is required to build the Equinox native binaries on the current platform targeting the same OS and processor arch.

- common
    - a JDK 1.8 or later. More recent versions can be used too. Equinox itself uses a.t.m a JDK 17 to built the official binaries.
- Windows:
    - Microsoft `Visual C Compiler 2022` or later (earlier version usually work as well), the `Visual Studio Community Edition` is sufficient.
- Linux:
    - GTK development files. The Package `libgtk-3-dev` is sufficient.
- MacOS:
    - XCode Command Line Tools (already installed with Apple `XCode`)

## Running the build

The simplest way to build and install the native binaries for the running platform is to launch the Maven launch-configuration
named `Build-Equinox-native-binaries-for-running-platform` from this project (requires `Eclipse M2E`).
It assumes the [`equinox.binaries`](https://github.com/eclipse-equinox/equinox.binaries.git) repository is checked out located next to this repository and is named `equinox.binaries`.

To just build the native binaries for a specific platform navigate to the `library/<window-system>` sub-directory of interest and run 
- `./build.sh` for Linux or MacOS
- `.\build.bat` for Windows

For the complete development setup, which is required to run the Maven build or the `equinox.launcher.tests`
1. Clone the [`equinox.binaries`](https://github.com/eclipse-equinox/equinox.binaries.git) repository co-located to this repository.
2. Run from the `library/<window-system>` sub-directory matching the platform of interest
    - `./build.sh install` for Linux or MacOS 
    - `.\build.bat install` for Windows

The `install` target additionally moves the just built binaries into the `equinox.binaries` repository clone.

The environment variable `BINARIES_DIR` can be set to specify an alternative path to the `equinox.binaries` repository to which the built binaries are moved.
To independently specify the exact directory where the native executable or launcher library is moved, set the variable `EXE_OUTPUT_DIR` respectively `LIB_OUTPUT_DIR`.
For more options and details see the `build.sh/bat` files and corresponding make-files.

### Cross compilation

On some platforms cross-compilation for other CPU architecutres is possible by invoking the `build.sh/bat` scripts with an explicit `-arch <target-arch>` argument.
Of course the required tools for the targeted architecture have to be available.
For example on a Windows computer with `x86_64` CPU, to build the binaries for Windows on ARM one can call `build.bat -arch aarch64 -java <path-to-arm-jdk>`.
