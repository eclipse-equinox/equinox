# Implementation Summary: OS-Specific Application Data Directory Placeholders

## Overview

This implementation adds three new placeholders to the Eclipse Equinox launcher that resolve to OS-specific standard directories, improving integration with operating system conventions for application data storage.

## New Placeholders

1. **`@user.data`** - User-specific application data directory
   - Windows: `%APPDATA%` (e.g., `C:\Users\username\AppData\Roaming`)
   - Linux: `$HOME` with hidden directory convention (e.g., `~/.myApp`)
   - macOS: `~/Library/Application Support`

2. **`@user.data.shared`** - Shared/common application data directory
   - Windows: `%PROGRAMDATA%` (e.g., `C:\ProgramData`)
   - Linux: `/srv`
   - macOS: `/Library/Application Support`

3. **`@user.documents`** - User documents directory
   - Windows: User's Documents folder (e.g., `C:\Users\username\Documents`)
   - Linux: `$HOME`
   - macOS: `~/Documents`

## Changes Made

### Java Layer

#### Main.java
- Added 3 new placeholder constants
- Implemented `substituteUserDataVar()` with special Linux hidden directory handling
- Implemented `substituteUserDataSharedVar()`
- Implemented `substituteUserDocumentsVar()`
- Added 3 helper methods for OS-specific directory retrieval with fallbacks
- Modified `buildLocation()` to handle new placeholders

**Lines changed:** +88

#### JNIBridge.java
- Added 3 new native method declarations
- Implemented 3 public wrapper methods with error handling and lazy library loading

**Lines changed:** +42

### Native Layer

#### eclipseJNI.h
- Added JNI name mangling macros for 3 new methods
- Added function declarations

**Lines changed:** +21

#### eclipseJNI.c
- Registered 3 new methods in `natives[]` array
- Implemented 3 JNI callback functions with proper memory management

**Lines changed:** +35

#### eclipseOS.h
- Added declarations for 3 platform-specific functions

**Lines changed:** +6

### Platform Implementations

#### Windows (eclipseWin.c)
- Added `#include <shlobj.h>` for SHGetFolderPath API
- Implemented 3 functions using Windows Shell API:
  - `getOSUserDataDirectory()` - Uses CSIDL_APPDATA
  - `getOSUserDataSharedDirectory()` - Uses CSIDL_COMMON_APPDATA
  - `getOSUserDocumentsDirectory()` - Uses CSIDL_PERSONAL

**Lines changed:** +25

#### Linux (eclipseGtkCommon.c)
- Implemented 3 functions using environment variables:
  - `getOSUserDataDirectory()` - Returns `$HOME`
  - `getOSUserDataSharedDirectory()` - Returns `/srv`
  - `getOSUserDocumentsDirectory()` - Returns `$HOME`

**Lines changed:** +20

#### macOS (eclipseCocoa.c)
- Implemented 3 functions using NSSearchPathForDirectoriesInDomains:
  - `getOSUserDataDirectory()` - NSApplicationSupportDirectory with NSUserDomainMask
  - `getOSUserDataSharedDirectory()` - NSApplicationSupportDirectory with NSLocalDomainMask
  - `getOSUserDocumentsDirectory()` - NSDocumentDirectory with NSUserDomainMask

**Lines changed:** +27

### Documentation

#### IMPLEMENTATION_NOTES.md (NEW)
Comprehensive technical documentation covering:
- Architecture overview
- Layer-by-layer implementation details
- Platform-specific behavior
- Fallback mechanisms
- Building instructions
- Future enhancement suggestions

**Lines:** 168

#### USAGE_EXAMPLES.md (NEW)
User-facing documentation with:
- Basic usage examples
- eclipse.ini configurations
- Programmatic usage
- Platform-specific considerations
- Troubleshooting guide
- Migration guide
- Best practices

**Lines:** 247

#### runtime-options-doc-patch.diff (NEW)
Patch file for eclipse.platform.releng.aggregator repository to update the runtime options documentation with details about the three new placeholders.

**Lines:** 94

## Total Changes

- **Files Modified:** 8
- **Files Added:** 3
- **Total Lines Added:** ~525
- **Commits:** 3

## Testing Status

✅ **Java Compilation:** Verified - code compiles without errors  
✅ **C Syntax:** Verified - gcc syntax check passed  
✅ **JNI Signatures:** Verified - Java and C signatures match  
⚠️ **Integration Tests:** Require full native launcher build on target platforms  

## Building Requirements

### Prerequisites
- JDK 11 or later
- Platform-specific tools:
  - **Windows:** Visual Studio 2022 (or MinGW)
  - **Linux:** GCC, libgtk-3-dev
  - **macOS:** Xcode Command Line Tools

### Build Commands

**Windows:**
```cmd
cd features\org.eclipse.equinox.executable.feature\library\win32
build.bat
```

**Linux:**
```bash
cd features/org.eclipse.equinox.executable.feature/library/gtk
./build.sh
```

**macOS:**
```bash
cd features/org.eclipse.equinox.executable.feature/library/cocoa
./build.sh
```

## Backward Compatibility

✅ **Fully backward compatible** - no breaking changes
- Existing placeholders (`@user.home`, `@user.dir`, `@launcher.dir`) continue to work
- New placeholders are additive only
- No changes to existing API or behavior

## Security Considerations

- ✅ No elevation of privileges required for `@user.data` and `@user.documents`
- ⚠️ `@user.data.shared` may require administrator privileges on some systems (Windows, macOS)
- ✅ Path resolution uses standard OS APIs (no custom path manipulation)
- ✅ Proper memory management prevents leaks

## Known Limitations

1. **Linux Hidden Directory:** Only the first path segment after `@user.data` is hidden
2. **Shared Directory Access:** Writing to `@user.data.shared` locations requires appropriate permissions
3. **Native Library Required:** Full functionality requires the native launcher library
4. **No XDG Support:** Linux implementation doesn't use XDG_DATA_HOME (uses HOME instead)

## Future Enhancements

Potential improvements for future releases:
1. Support for XDG Base Directory environment variables on Linux
2. Additional placeholders (cache, temp, etc.)
3. Configurable hidden directory behavior on Linux
4. Unit tests for placeholder resolution
5. Integration tests for native layer

## References

- Issue: [launcher] Better integration with OS specific application data directories
- Microsoft Documentation: [Environment.SpecialFolder](https://learn.microsoft.com/en-us/dotnet/api/system.environment.specialfolder)
- Linux: [Filesystem Hierarchy Standard](https://en.wikipedia.org/wiki/Filesystem_Hierarchy_Standard)
- macOS: [NSSearchPathForDirectoriesInDomains](https://developer.apple.com/documentation/foundation/1414224-nssearchpathfordirectoriesindoma)

## Validation Checklist

- [x] Java code compiles without errors
- [x] Native C code has correct syntax
- [x] JNI method signatures match between Java and C
- [x] All platforms have implementations
- [x] Memory management is correct (allocations/deallocations paired)
- [x] Fallback mechanisms in place
- [x] Documentation is comprehensive
- [x] Usage examples provided
- [x] Platform-specific behavior documented
- [x] No breaking changes introduced
- [ ] Integration tests with built native launcher (requires platform-specific builds)

## Next Steps for Reviewers

1. **Review Code Changes:** Check Java and C implementations for correctness
2. **Build Native Libraries:** Build on Windows, Linux, and macOS to verify compilation
3. **Test Functionality:** Use placeholders in eclipse.ini and verify path resolution
4. **Review Documentation:** Ensure documentation is clear and complete
5. **Consider Edge Cases:** Test with special characters, long paths, etc.
6. **Apply Doc Patch:** Apply the patch to eclipse.platform.releng.aggregator

## Conclusion

This implementation provides a clean, minimal, and platform-aware solution for OS-specific application data directory resolution in the Eclipse launcher. The changes are surgical, maintain backward compatibility, and follow OS conventions for each supported platform.
