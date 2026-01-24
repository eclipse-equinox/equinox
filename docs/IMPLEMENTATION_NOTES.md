# Implementation Notes for OS-Specific Application Data Directory Placeholders

## Overview

This implementation adds three new placeholders to the Eclipse launcher that resolve to OS-specific application data directories:

- `@user.data` - User-specific application data directory
- `@user.data.shared` - Shared/common application data directory  
- `@user.documents` - User documents directory

## Architecture

### Java Layer (org.eclipse.equinox.launcher)

1. **Main.java**
   - Added constants for the three new placeholders (`USER_DATA`, `USER_DATA_SHARED`, `USER_DOCUMENTS`)
   - Added placeholder resolution in `buildLocation()` method
   - Implemented three new methods:
     - `substituteUserDataVar()` - Resolves @user.data with special Linux handling (prepends dot to first path segment)
     - `substituteUserDataSharedVar()` - Resolves @user.data.shared
     - `substituteUserDocumentsVar()` - Resolves @user.documents
   - Added fallback methods that call native implementations or use environment variables:
     - `getOSUserDataDirectory()`
     - `getOSUserDataSharedDirectory()`
     - `getOSUserDocumentsDirectory()`

2. **JNIBridge.java**
   - Added three new native method declarations:
     - `_get_os_user_data_directory()`
     - `_get_os_user_data_shared_directory()`
     - `_get_os_user_documents_directory()`
   - Added corresponding public methods with error handling and lazy library loading

### Native Layer (org.eclipse.equinox.executable.feature/library)

1. **eclipseJNI.h**
   - Added JNI function name mangling definitions for the three new methods
   - Added function declarations

2. **eclipseJNI.c**
   - Registered the three new native methods in the `natives[]` array
   - Implemented JNI callback functions that:
     - Call OS-specific implementations
     - Convert C strings to Java strings
     - Handle memory management (free allocated strings)

3. **eclipseOS.h**
   - Added declarations for OS-specific functions:
     - `getOSUserDataDirectory()`
     - `getOSUserDataSharedDirectory()`
     - `getOSUserDocumentsDirectory()`

### Platform-Specific Implementations

#### Windows (win32/eclipseWin.c)

Uses the Windows Shell API (`SHGetFolderPath`) to get standard folder paths:

- `@user.data` → `CSIDL_APPDATA` (e.g., `C:\Users\username\AppData\Roaming`)
- `@user.data.shared` → `CSIDL_COMMON_APPDATA` (e.g., `C:\ProgramData`)
- `@user.documents` → `CSIDL_PERSONAL` (e.g., `C:\Users\username\Documents`)

**Dependencies:** Added `#include <shlobj.h>` for SHGetFolderPath API

#### Linux/GTK (gtk/eclipseGtkCommon.c)

Uses simple environment variable and standard paths:

- `@user.data` → `$HOME` (special handling in Java layer adds dot prefix to first path segment)
- `@user.data.shared` → `/srv` (standard system service data directory)
- `@user.documents` → `$HOME` (no standard XDG documents directory universally available)

**Note:** The Linux implementation follows the XDG Base Directory specification philosophy where application-specific data in `$HOME` should be hidden (start with a dot).

#### macOS (cocoa/eclipseCocoa.c)

Uses macOS-specific NSSearchPathForDirectoriesInDomains API:

- `@user.data` → `NSApplicationSupportDirectory` with `NSUserDomainMask` (e.g., `~/Library/Application Support`)
- `@user.data.shared` → `NSApplicationSupportDirectory` with `NSLocalDomainMask` (e.g., `/Library/Application Support`)
- `@user.documents` → `NSDocumentDirectory` with `NSUserDomainMask` (e.g., `~/Documents`)

**Dependencies:** Already included `<Cocoa/Cocoa.h>` which provides NSSearchPathForDirectoriesInDomains

## Special Handling

### Linux Hidden Directory Convention

On Linux/Unix systems (excluding macOS), the Java layer in `substituteUserDataVar()` automatically prepends a dot to the first path segment after `@user.data`. This follows the Unix convention where application data directories should be hidden.

Example:
- Input: `@user.data/myApp/workspace`
- Result: `$HOME/.myApp/workspace`

This is only applied to the first path segment - deeper segments are not modified.

### Fallback Behavior

If the native library is not available or the OS-specific call fails:

1. **Windows:**
   - `@user.data` → Uses `%APPDATA%` environment variable
   - `@user.data.shared` → Uses `%PROGRAMDATA%` environment variable or defaults to `C:\ProgramData`
   - `@user.documents` → Falls back to `user.home` system property

2. **Linux/macOS:**
   - `@user.data` → Falls back to `user.home` system property
   - `@user.data.shared` → Defaults to `/srv`
   - `@user.documents` → Falls back to `user.home` system property

## Building

### Prerequisites

- JDK 11 or later
- Platform-specific build tools:
  - **Windows:** Visual Studio 2022 or later
  - **Linux:** GCC, GTK development headers (`libgtk-3-dev`)
  - **macOS:** Xcode Command Line Tools

### Building the Native Libraries

Navigate to the appropriate platform directory and run the build script:

```bash
# Linux
cd features/org.eclipse.equinox.executable.feature/library/gtk
./build.sh

# macOS
cd features/org.eclipse.equinox.executable.feature/library/cocoa
./build.sh

# Windows
cd features\org.eclipse.equinox.executable.feature\library\win32
build.bat
```

## Testing

The implementation can be tested by:

1. Setting a property using one of the new placeholders in `eclipse.ini`:
   ```
   -Dosgi.instance.area=@user.data/myapp/workspace
   ```

2. Launching Eclipse and checking the resolved path in the configuration

3. For debugging, check the system property value at runtime:
   ```java
   System.getProperty("osgi.instance.area")
   ```

## Compatibility

- **Backward Compatible:** Yes - existing placeholders continue to work
- **Minimum Requirements:** No change - requires same JDK and native library versions as before
- **API Impact:** None - changes are internal to the launcher

## Future Enhancements

Possible improvements for future versions:

1. Support for XDG Base Directory environment variables on Linux (`$XDG_DATA_HOME`, `$XDG_CONFIG_HOME`)
2. Support for additional platform-specific folders (cache, temp, etc.)
3. Allow customization of the hidden directory behavior on Linux
4. Unit tests for placeholder resolution logic
