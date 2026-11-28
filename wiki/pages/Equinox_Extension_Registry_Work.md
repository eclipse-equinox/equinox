**WORK IN PROGRESS – SUBJECT TO CHANGE**

# Improving usability of the extension registry

This page tracks current state, plans, and intentions for the extension
registry enhancements.

Please feel free to add comments to it.

## Areas

  - \[\[Equinox Extension Registry Work Objects| Create user-specific
    objects based on the contents of the configuration

elements\]\]

  - Expand ability to programmatically modify extension registry
  - Add support for non-singleton bundles
  - Add support for switching NLS locale without restart

## Restrictions

  - At least for 3.x streams we need to continue supporting pre-1.5 VMs.
    As such, any functionality based on annotations can only be used in
    an optional role

<!-- end list -->

  - Performance: while small performance hit can be expected every time
    a new functionality is added, the performance hit should be
    reasonable for the "minimum" headless framework start (say \< 15% ?)
    and should scale well.

**WORK IN PROGRESS – SUBJECT TO CHANGE**

## Links

1.  [Bug 248340](https://bugs.eclipse.org/bugs/show_bug.cgi?id=248340):
    Improve usability of the extension registry

[Category:Equinox](Category:Equinox "wikilink")