This feature contains the eclipse executables and includes the equinox launcher bundle and its fragments.
It also host the native C source files to build the executable and launcher binaries for all supported platforms.

## Building the native executable and launcher binaries

To just build the native binaries navigate to the `library/<window-system>` sub-directory of interest and run 
- `./build.sh` for Linux or MacOS
- `.\build.bat` for Windows.

For the full setup, which is required to run the Maven build or the equinox.launcher.tests
1. Clone the [`equinox.binaries`](https://github.com/eclipse-equinox/equinox.binaries.git) repository co-located to this repository.
2. Run from the `library/<window-system>` sub-directory matching the platform of interest
    - `./build.sh install` for Linux or MacOS 
    - `.\build.bat install` for Windows

The `install` target additionally moves the just built binaries into the `equinox.binaries` repository clone.

The environment variable `BINARIES_DIR` can be set to specify an alternative path of the `equinox.binaries` repository to which the built binaries are moved.
To specify the exact directory where the native executable or launcher library is moved, set the variable `EXE_OUTPUT_DIR` respectively `LIB_OUTPUT_DIR`.
For more options and details see the `build.sh/bat` files and corresponding make-files.
