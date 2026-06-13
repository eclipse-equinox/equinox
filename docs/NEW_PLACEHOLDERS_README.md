# OS-Specific Application Data Directory Placeholders

## Quick Overview

This implementation adds three new placeholders to the Eclipse launcher for OS-specific directory resolution:

```
@user.data          → User application data directory
@user.data.shared   → Shared application data directory
@user.documents     → User documents directory
```

## Platform-Specific Resolution

| Placeholder | Windows | Linux | macOS |
|-------------|---------|-------|-------|
| `@user.data` | `C:\Users\...\AppData\Roaming` | `~/.appname` | `~/Library/Application Support` |
| `@user.data.shared` | `C:\ProgramData` | `/srv` | `/Library/Application Support` |
| `@user.documents` | `C:\Users\...\Documents` | `~` | `~/Documents` |

## Quick Start

### Using in eclipse.ini

```ini
-data
@user.data/MyApp/workspace

-configuration
@user.data.shared/MyApp/config
```

### Result
- **Windows:** `C:\Users\bob\AppData\Roaming\MyApp\workspace`
- **Linux:** `/home/bob/.MyApp/workspace` (note the dot!)
- **macOS:** `/Users/bob/Library/Application Support/MyApp/workspace`

## Implementation Files

### Java Layer
- **Main.java** - Placeholder resolution logic
- **JNIBridge.java** - Native method interfaces

### Native Layer
- **eclipseJNI.c/h** - JNI callbacks
- **eclipseOS.h** - OS function declarations
- **win32/eclipseWin.c** - Windows implementation
- **gtk/eclipseGtkCommon.c** - Linux implementation
- **cocoa/eclipseCocoa.c** - macOS implementation

## Building

### Prerequisites
- **Windows:** Visual Studio 2022 (or MinGW)
- **Linux:** GCC + libgtk-3-dev
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

## Testing

1. **Modify eclipse.ini** to use a new placeholder:
   ```ini
   -data
   @user.data/TestApp/workspace
   ```

2. **Launch Eclipse** and check where the workspace is created

3. **Expected locations:**
   - Windows: `%APPDATA%\TestApp\workspace`
   - Linux: `~/.TestApp/workspace`
   - macOS: `~/Library/Application Support/TestApp/workspace`

## Documentation Structure

```
docs/
├── NEW_PLACEHOLDERS_README.md      ← You are here (Quick start)
├── IMPLEMENTATION_SUMMARY.md       ← Complete technical summary
├── IMPLEMENTATION_NOTES.md         ← Architecture details
├── USAGE_EXAMPLES.md               ← Usage guide & best practices
└── runtime-options-doc-patch.diff  ← Patch for platform docs
```

## Key Features

✅ **Special Linux Handling:** Automatically prefixes first path segment with dot  
✅ **Fallback Support:** Uses environment variables if native lib unavailable  
✅ **Backward Compatible:** Existing placeholders continue to work  
✅ **Memory Safe:** Proper allocation/deallocation in native code  

## Validation Status

| Check | Status |
|-------|--------|
| Java compilation | ✅ Verified |
| C syntax | ✅ Verified |
| JNI signatures | ✅ Match |
| Platform implementations | ✅ Complete |
| Documentation | ✅ Complete |
| Integration tests | ⚠️ Requires native build |

## Common Issues

### Placeholder Not Resolved?
- Check spelling (must be exact: `@user.data` not `@user-data`)
- Ensure native launcher library is present
- Check Eclipse log for errors

### Permission Denied?
- `@user.data.shared` may require admin privileges
- Ensure target directory is writable
- Check OS-specific permissions

### Wrong Directory?
- Verify you're on the expected OS
- Check environment variables (`$HOME`, `%APPDATA%`)
- Enable debug mode: `-debug -consoleLog` in eclipse.ini

## References

- **Implementation Details:** [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md)
- **Usage Examples:** [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md)
- **Complete Summary:** [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)

## Contributing

When modifying this feature:

1. **Update all platforms** - Windows, Linux, and macOS
2. **Test on each platform** - Cross-platform behavior varies
3. **Update documentation** - Keep docs in sync with code
4. **Verify JNI signatures** - Java and C must match exactly
5. **Check memory management** - All allocations must be freed

## License

This code is part of Eclipse Equinox and follows the Eclipse Public License 2.0.

---

**Need help?** See the other documentation files in this directory for detailed information.
