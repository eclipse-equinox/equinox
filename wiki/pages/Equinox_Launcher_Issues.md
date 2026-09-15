This page outlines the current situation with the equinox launcher.

## JNI Launching

linux.ppc, linux.x86_64 and aix.ppc experience crashes with older vms,
and therefore currently default to running java as a separate process.
The following table outlines these platforms and their JNI launching
status with different vms.
([bug 168281](https://bugs.eclipse.org/bugs/show_bug.cgi?id=168281),
[bug 168278](https://bugs.eclipse.org/bugs/show_bug.cgi?id=168278), and
[bug 168271](https://bugs.eclipse.org/bugs/show_bug.cgi?id=168271)).

<table>
<caption>JNI launching Information</caption>
<tbody>
<tr class="odd">
<td><p>Platform</p></td>
<td><p>Reference VM</p></td>
<td><p>Other tested VMs</p></td>
</tr>
<tr class="even">
<td><p>gtk.linux.ppc</p></td>
<td><p>IBM-1.4.2sr7 - works</p></td>
<td><p>IBM 1.5.0 jcldp32dev-20070105 works<br />
IBM 1.5.0 pxp32devifx-20060124 crashes</p></td>
</tr>
<tr class="odd">
<td><p>gtk.linux.x86_64</p></td>
<td><p>Sun 5.0 Update 11 - not tested</p></td>
<td><p>IBM 1.4.2 j9xa64142-20061124 works<br />
Sun 1.5.0_09-b02 server crashes<br />
IBM 1.4.2 j9xa64142-20060120 crashes</p></td>
</tr>
<tr class="even">
<td><p>motif.aix.ppc</p></td>
<td><p>IBM-1.5.0sr4 - fails<br />
"A file or directory in the path name does not exist."</p></td>
<td></td>
</tr>
</tbody>
</table>

## Windows Console

The eclipse.exe is linked on windows as a GUI application, which means
the OS does not automatically allocate a console for it. This causes us
several console related problems. See
[bug 168726](https://bugs.eclipse.org/bugs/show_bug.cgi?id=168726),
[bug 167310](https://bugs.eclipse.org/bugs/show_bug.cgi?id=167310), and
[bug 173962](https://bugs.eclipse.org/bugs/show_bug.cgi?id=173962).
Exact behaviour depends on the specific vm.

By linking the eclipse executable as a console application, all the
console problems are solved. We are now linking a new "eclipsec.exe"
executable.

  - This executable is not branded with an icon
  - This executable is not included as a rootfile by
    org.eclipse.equinox.executable, which means it will not be included
    by default in exported RCP applications. (However the executable is
    included in the feature so can be explicitly included if the user
    desires.)

|                              |                    |                      |                       |                                               |
| ---------------------------- | ------------------ | -------------------- | --------------------- | --------------------------------------------- |
| VM                           | Java IO to console | Native IO to console | Ctrl+Break in console | redirectable IO (eclipse -debug \> debug.txt) |
| IBM 1.4.2 cn142ifx-20060209  | No                 | Yes                  | crash                 | Yes                                           |
| IBM 1.5.0 pwidevifx-20060124 | Yes                | Yes                  | Yes                   | No                                            |
| Sun 1.4.2_12                | No                 | Yes                  | No\*                  | No - Ctrl+Break stack trace does redirect     |
| Sun 1.5.0_07                | Yes                | Yes                  | No\*                  | No - Ctrl+Break stack trace does redirect     |
| Sun 1.6.0-b105               | Yes                | Yes                  | Yes                   | No                                            |

Windows Console

## Swing Applications on MacOSX

Mac is the only platform that always runs the vm in-process. The
launcher by always created the vm on the main thread. This was a problem
for Swing. See
[bug 181698](https://bugs.eclipse.org/bugs/show_bug.cgi?id=181698). An
argument "--launcher.secondThread" was added for the Mac to enable Swing
applications. SWT will not work with this argument specified.

Should we also support the old mode of forking the vm on the mac?

[Launcher](Category:Equinox "wikilink")
[Category:Launcher](Category:Launcher "wikilink")