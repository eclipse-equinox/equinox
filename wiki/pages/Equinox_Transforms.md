There are bundles that exist in both the equinox bundles and equinox
incubator projects that allow you to provide transformations of bundle
resources at the OSGi level. Various example transformers exist (XSLT,
sed, replacement) that can be used to transform any resource in a bundle
including but not limited to plugin.xml, MANIFEST.MF, class files, etc.

## How To Run The Examples

1.  Download
    [Eclipse 3.2.1](http://archive.eclipse.org/eclipse/downloads/drops/R-3.2.1-200609210945/index.php)
    or any [subsequent
    release](http://download.eclipse.org/eclipse/equinox/). This code
    has been tested with 3.2.1, 3.3.2, and 3.4.
2.  Check out the following projects from site
    :pserver:anonymous@dev.eclipse.org:/cvsroot/rt
    org.eclipse.equinox/components/bundles:
      - *org.eclipse.equinox.transforms.hook*
      - *org.eclipse.equinox.transforms.xslt*
3.  If you would like to see the XSLT example or use the replacement and
    SED transformers you will also need to check out the following
    projects from site :pserver:anonymous@dev.eclipse.org:/cvsroot/rt
    org.eclipse.equinox/incubator/components/bundles:
      - *org.eclipse.equinox.transforms.util*
      - *org.eclipse.equinox.transforms.replace*
      - *org.eclipse.equinox.transforms.replace.images*
      - *org.eclipse.equinox.transforms.sed*
      - *org.eclipse.equinox.transforms.sed.manifest*
      - *org.eclipse.equinox.transforms.xslt.plugin*
4.  Check out *org.eclipse.osgi* from
    :pserver:anonymous@dev.eclipse.org:/cvsroot/rt
    org.eclipse.equinox/framework/bundles (see
    [bug 143696](https://bugs.eclipse.org/bugs/show_bug.cgi?id=143696)
    for why this is necessary)
5.  From the Run menu, choose Run
6.  locate the example launch you wish to run and run it. They are named
    Replacement Transform Launch, Sed Transform Launch, and XSLT
    Transform Launch. ***Please note*** that the Sed launch will not
    work if you do not have the sed program available on your path.
    Please also note that the serialization of launches between
    different versions of Eclipse and between workspaces on varying OS
    platforms can sometimes be problematic. You may need to go to the
    Plug-Ins tab in the launch configuration and choose "Add Required
    Plug-Ins" to ensure that you have all of the required bundles.

## How To Package Transformers for Distribution

The transformer bundles work via adaptor hooks. As such, they are
framework extensions and follow slightly different rules than other
bundles. For full documentation on how to use adaptor hooks in your
application please see [Adaptor Hooks](Adaptor_Hooks "wikilink").

1.  ensure that all bundles required for the desired transformations are
    available. This means typically means
    *org.eclipse.equinox.transforms.hook* , the bundle implementing the
    desired transform type (ie: *org.eclipse.equinox.transforms.xslt*),
    and the bundle that provides the transformation file(s) (ie:
    *org.eclipse.equinox.transforms.plugin*)
2.  create (either from scratch or by copying one from your existing
    product configuration) a config.ini file that has your both the
    bundle implementing the transformer type (ie:
    *org.eclipse.equinox.transforms.xslt*) as well as the bundle
    containing your particular transforms (ie:
    *org.eclipse.equinox.transforms.xslt.plugin*) listed on the
    *osgi.bundles* line with an eager start level. Ie:
    *org.eclipse.equinox.transforms.xslt@1:\\start,
    org.eclipse.equinox.transforms.xslt.plugin@1:\\start*. See the
    config.ini in *org.eclipse.equinox.transforms.xslt.plugin* for an
    example.
3.  ensure that the *org.eclipse.equinox.transforms.hook* bundle is
    expressed as framework extension bundle via the
    *osgi.framework.extensions* property in the config.ini file. Ie:
    *osgi.framework.extensions=org.eclipse.equinox.transforms.hook*

Omitting any of these steps will result in a configuration that will not
have any transforms applied. Simply having the transform bundles present
is not sufficient for them to be invoked.

## How To Write Your Own Transforms

Writing transforms that utilize an existing transform type (ie: XSLT) is
quite easy. Simply create a new bundle with an Activator. In that
activator register a service as follows:

`Properties properties = new Properties();`
`properties.put("equinox.transformerType", "xslt"); //$NON-NLS-1$
//$NON-NLS-2$`
`registration = context.registerService(URL.class.getName(),
context.getBundle().getEntry("/transform.csv"), properties);
//$NON-NLS-1$`

In this bundle create a text file (in this case /transform.csv) that
contains CSV separated values of the following form: bundle regular
expression, path regular expression, transform url. For example, the
line *org\\.eclipse\\.team\\.ui,plugin\\.xml,/actionSetDefault.xslt* in
this CSV file specifies a bundle pattern of *org\\.eclipse\\.team\\.ui*,
a path pattern of *plugin\\.xml*, and a transform url
*/actionSetDefault.xslt*. When the transformer hook intercepts a call
for a particular file in a particular bundle, all of the lines of the
CSV are examined. If there is a match between the bundle pattern and the
path pattern, the transformer of the specified type (specified by the
*equinox.transformerType* property above) is invoked with the original
input stream and an url that resolves to the resource
*actionSetDefault.xslt* located in the root of this transform bundle.
The transformer will then invoke some transformation based on the
contents of the original resource as well as the contents of the
transform url.

For an example of how a transform bundle is laid out please see
*org.eclipse.equinox.transforms.replace.images*,*org.eclipse.equinox.transforms.sed.manifest*,
and *org.eclipse.equinox.transforms.xslt.plugin*.

## How To Write Your Own Transformer

Writing your own transformer is as easy as writing an OSGi service. To
implement a transformer of your own devising, simply create a new bundle
and create a class that contains a method with the signature *public
InputStream getInputStream(InputStream inputStream, URL transformerUrl)
throws IOException*. In this method, devise some code that is capable of
returning an input stream which may or may not be derived from the
supplied input stream. To determine the type of transform to apply, you
may inspect the provided url. You may assume that this URL is resolvable
to an actual data stream of some variety. In the activator of this
bundle register a service as follows:

`Properties properties = new Properties();`
`properties.put("equinox.transformerType", "myTransformType");
//$NON-NLS-1$ //$NON-NLS-2$`
`Object transformer = new MyTransformer();`
`registration = context.registerService(Object.class.getName(),
transformer, properties);`

This associates the transformer with the type "myTransformType." When
transforms of that type are processed it will be your processor that is
used.

For an example of how a transformer bundle is laid out please see
*org.eclipse.equinox.transforms.replace*,*org.eclipse.equinox.transforms.sed*,
and *org.eclipse.equinox.transforms.xslt*.

[Transforms](Category:Equinox "wikilink")