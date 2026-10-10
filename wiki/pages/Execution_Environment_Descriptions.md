In 3.3M6, the [Equinox](Equinox "wikilink")
[Launcher](Equinox_Launcher "wikilink") introduced the notion of an
[execution environment](Execution_Environments "wikilink") description
file. This is a file with extension .ee that describes a JVM and the
execution environment it represents.

## .ee File Format

Entries in the .ee file are arguments to a java vm. Each argument is on
a separate line. The arguments in this file should be all the arguments
required to configure the virtual machine for the environment the .ee
file describes. Values containing spaces do not need to be quoted.
Comments can be added by starting a line with \#, commented lines will
be ignored while processing the file.

## Equinox Launcher

The Equinox Launcher in 3.3 can be started with a -vm argument pointing
to a .ee file. This file will be read and the arguments will be used
when creating the JVM.

The Equinox Launcher defines the following arguments which it expects to
find in the .ee file. The following arguments specify the vm and
information required by the launcher to start that vm:

  - \-Dee.executable=<path to java executable>

<!-- end list -->

  -
    This argument specifies the path the java executable the launcher
    should use to start the virtual machine.

<!-- end list -->

  - \-Dee.executable.console=<path to java executable>

<!-- end list -->

  -
    This argument specifies the path the java executable the launcher
    should use to start the virtual machine when a console is desired.

<!-- end list -->

  - \-Dee.vm.library=<path to jvm shared library>

<!-- end list -->

  -
    This argument specifies the path to a shared library. The equinox
    launcher will use this library to load the vm in-process using the
    JNI Invocation API. The launcher looks for the "JNI_CreateJavaVM"
    symbol in the library.

<!-- end list -->

  - \-Dee.library.path=<list of paths>

<!-- end list -->

  -
    This arguments specifies a list of paths (separated by ';' or ':' as
    appropriate for the platform) that are required in order to load the
    ee.vm.library.

<!-- end list -->

  -
    For example, on Linux, this property represents paths that need to
    be on the LD_LIBRARY_PATH environment variable in order to load
    the jvm shared library.

In addition to being interpreted by the Equinox Launcher, these
arguments are passed to the VM so that they are available in the java
process as system properties.

### Paths

In all of the above arguments, the path used can be either a relative
path or an absolute path.

If a relative path is used, it is resolved first relative to the .ee
file. If it is not found relative to the .ee file, it is then resolved
against the working directory.

### Variable Substitution

The launcher does not perform arbitrary variable substitution. However,
in the context of the .ee file, it will replace ${ee.home} with the
directory containing the .ee file. Example:

    -Djava.library.path=${ee.home};.;C:\WINDOWS\System32

This is useful for vm arguments that require absolute paths. For
example, some vms may require a property like java.home to be set with
an absolute path, this would normally be set by the java.exe, but when
starting using the JNI invocation API, the property would need to be set
in the .ee file.

### default.ee

If the Equinox Launcher is started with the -vm argument pointing at a
directory, then it will first look for a default.ee file in that
directory. If one is found, it will be used to start the vm as outlined
below.

### \-vm \<.ee file\>

When the Equinox Launcher is started with the -vm argument pointing at a
.ee file (or at a directory containing a default.ee file as above), the
vm will be started as follows:

1.  ee.vm.library : If this property is defined, the indicated library
    will be loaded to start the vm using the JNI invocation API. The
    ee.library.path property will be used on those platforms where it
    applies.
2.  ee.executable, ee.executable.console: One of the indicated java
    executables (depending on whether or not a console is desired) will
    be used to launch the jvm in a separate process.

## EE Properties

The following is a list of proposed standard properties that and their
current interested parties. Because the .ee file is interpreted as vm
arguments, for the properties listed below, the actual entry in the .ee
file would be "-Dee.property=value".

| Property Name          | Example/Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Interested Parties |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ |
|                        |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Eclipse.exe        |
| ee.executable          | javaw.exe or j9w                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | x                  |
| ee.executable.console  | java.exe or j9.exe                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | x                  |
| ee.library.path        | where to find VM libraries. The value is used to modify the system environment variable indicating where to find shared libaries. (LD_LIBRARY_PATH on \*nix, LIBPATH on AIX, PATH on windows)                                                                                                                                                                                                                                                                                                                              | x                  |
| ee.vm.library          | shared library containing JNI entry point                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | x                  |
| ee.bootclasspath       |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |                    |
| ee.src                 | Source archive for the bootclasspath libraries. Must be a path. Each library in the bootclasspath will have the file specified by this path set as its source attachment.                                                                                                                                                                                                                                                                                                                                                    |                    |
| ee.src.map             | Mapping of class libraries to their source attachments. Must be one or more entries of the form libPath=sourcePath separated by OS specific file separators. The paths can use {$ee.home} and '..' as well as the wildcards ? (any one character) and \* (any number of characters). The source path can use the wildcards to have the source path be based on the wildcard replacement in the lib path. The wildcards in the sourcepath must exist in the same order in the lib path. Ex: lib/foo\*.???=source/src\*foo.??? |                    |
| ee.javadoc             | Javadoc location for class libraries. Must be a URL. You can use ${ee.home} and '..' segments to specify a file location relative to the ee file. If this property is not specified in the file, javadoc locations will be set to a default location based on the language level.                                                                                                                                                                                                                                            |                    |
| ee.ext.dirs            |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |                    |
| ee.endorsed.dirs       |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |                    |
| ee.additional.dirs     |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |                    |
| ee.language.level      | 1.4, 1.5                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |                    |
| ee.class.library.level | J2SE-1.4, CDC-1.0/Foundation-1.0, etc.. These match osgi defined execution environment identifiers.                                                                                                                                                                                                                                                                                                                                                                                                                          |                    |
| ee.id                  | J2SE-1.4\[jit,gc_pol\] First segment matches osgi defined execution environments, second segment lists properties for the vm that could be selected upon.                                                                                                                                                                                                                                                                                                                                                                   |                    |
| ee.name                | The ee.name is used as the JRE name when installing an EE JRE into Eclipse JDT.                                                                                                                                                                                                                                                                                                                                                                                                                                              |                    |
| ee.description         |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |                    |
| ee.copyright           |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |                    |
| <arbitrary vm args>    | arguments passed through to vm                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |                    |
|                        |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |                    |
| java.home              | The root install directory of the runtime environment or development kit. Corresponds to a value that could be used for JAVA_HOME environment variable.                                                                                                                                                                                                                                                                                                                                                                     |                    |
| ee.debug.args          | The arguments to use to launch the VM in debug mode. For example "-agentlib:jdwp=transport=dt_socket,suspend=y,address=localhost:${port}". The "${port}" variable will be substituted with a free port at launch time. When unspecified, default arguments are constructed based on the language level of the VM.                                                                                                                                                                                                           |                    |
| ee.home                | The directory containing the .ee file. Relative paths are relative to this directory. The launcher will set this property, it does not need to be specified in the file.                                                                                                                                                                                                                                                                                                                                                     | x                  |
| ee.filename            | Absolute path to the .ee file, since there could be more than one in the ee.home directory. Also set by the launcher.                                                                                                                                                                                                                                                                                                                                                                                                        | x                  |

[Category:Equinox](Category:Equinox "wikilink")
[Category:Launcher](Category:Launcher "wikilink")