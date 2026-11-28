This page is currently a place holder for a more formal guideline
document. Information here is currently in the form of rough notes and
should not be construed as definitive guidelines.

The OSGi MANIFEST.MF and Equinox plugin.xml files form part of the API
contract of a bundle. These metadata files define what a bundle exposes
to other bundles, and what it consumes from other bundles. Care must be
taken when modifying this metadata, because in many cases it can have an
impact on downstream bundles.

Topics:

  - Policy on use of [Export-Package](Export-Package "wikilink")
  - Using import-package vs require-bundle (TBD: See also
    [bug 400154](https://bugs.eclipse.org/bugs/show_bug.cgi?id=400154#c2))
  - When should required bundles be re-exported?
  - The effect of adding/removing dependencies on downstream bundles
  - Fragment dependencies - A bundle fragment inherits the
    Require-Bundle and Import-Package statements from its host. A
    fragment should not re-specify requires or imports that are already
    specified by the host, because a change in imports on the host can
    cause a fragment to fail to resolve. A fragment may specify
    additional requirements that are not specified by its host as
    needed.

[Category:API](Category:API "wikilink")
[Category:Equinox](Category:Equinox "wikilink")