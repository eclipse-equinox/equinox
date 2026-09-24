## Enabling Weaving for Plug-Ins with AspectJ

As of Eclipse Juno, these are the steps required to enable Equinox
Weaving with AspectJ. Note that the `aop.xml` file is no longer
necessary.

  - Install the AspectJ Development Tooling (AJDT) and Equinox Weaving
    from the [AJDT Downloads](http://eclipse.org/ajdt/downloads/).
  - Define your aspect(s) in a bundle.
  - Configure your bundle's MANIFEST.MF to (i) export its aspects, and
    (ii) specify the bundles that its aspects advise. The former is done
    by adding a parameter on the `Export-Package` header. The latter is
    done through the `Eclipse-SupplementBundle` header. For example, a
    bundle with an aspect swing.edt.EdtRuleChecker, advising bundles
    com.example.core and com.example.ui would specify:

<!-- end list -->

    Export-Package: swing.edt;aspects="EdtRuleChecker"
    Eclipse-SupplementBundle: com.example.core, com.example.ui

  - Add the following bundles to your launch configuration
      - org.aspectj.weaver
      - org.eclipse.equinox.weaving.aspectj
      - org.eclipse.equinox.weaving.caching
      - org.eclipse.equinox.weaving.hook
  - Ensure org.eclipse.equinox.weaving.aspectj is auto-started at, say,
    level 2
  - Add the following VM argument (or to your `config.ini`) to instruct
    OSGi to load the weaving hooks:

<!-- end list -->

    -Dosgi.framework.extensions=org.eclipse.equinox.weaving.hook

### Control How Aspects are Applied with 'aspect-policy'

Aspects can be defined as opt-in (bundles have to explicitly ask for the
aspects to be installed) or opt-out (bundles must explicitly opt-out
from weaving). Opt-out is the default policy. For example, to change the
policy to opt-in:

    Export-Package: swing.debug; aspects="EdtRuleChecker"; aspect-policy:=opt-in

### Control How Aspects are Received with 'apply-aspects'

A bundle can explicitly request or forbid aspects being applied to it
through the `apply-aspects` parameter on `Import-Package`:

    Import-Package: swing.debug; apply-aspects:=false

`apply-aspects` defaults to `true`, indicating that aspects should be
woven into the bundle, whereas `false` indicates that aspects should not
be applied. These parameters override the supplying bundle's
`aspect-policy`.

The <em>Eclipse-BundleSupplement</em> in effect causes the specified
bundles to include `Import-Package: ...; apply-aspects:=true` for the
packages that are exported by the aspects bundle.

### Debugging

For debugging output, add the following VM arguments:

    -Daj.weaving.verbose=true
    -Dorg.aspectj.weaver.showWeaveInfo=true
    -Dorg.aspectj.osgi.verbose=true

The org.eclipse.osgi and org.eclipse.equinox.weaving bundles must be in
the same physical directories on disk. So, you cannot use multiple
installation locations for these bundles and if one bundle is checked
out as source, then so must the other.

## Sources

  - Martin Lippert (2009). [What's New in Equinox
    Aspects](http://www.martinlippert.org/events/JAX2009-WhatsNewInEquinoxAspects.pdf).
    JAX 2009
  - James Sugrue (2010). [Aspect Oriented Programming for Eclipse
    Plug-ins](http://java.dzone.com/articles/aspect-oriented-programming).
    dZone.
  - [JDT Weaving Features](JDT_weaving_features "wikilink")

<references />

[Weaving Quick Start](Category:Equinox "wikilink")
[Category:AJDT](Category:AJDT "wikilink")